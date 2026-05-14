package com.raitha.bharosa.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.raitha.bharosa.data.db.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.math.*

/**
 * Main repository for the Agricultural Labour Profile & Booking System
 * 
 * Implements offline-first architecture where Room database is the source of truth
 * and changes are synced to Firestore when online. When offline, operations are
 * queued for later sync.
 * 
 * Requirement 16: Offline Data Persistence
 * 
 * Architecture:
 * 1. All write operations go to Room first
 * 2. If online, immediately sync to Firestore
 * 3. If offline, queue operation for later sync
 * 4. Read operations return Room Flow for reactive updates
 * 5. Background sync from Firestore to Room when online
 */
class LabourRepository(
    private val context: Context,
    private val labourDao: LabourDao,
    private val bookingDao: BookingDao,
    private val ratingDao: RatingDao,
    private val workHistoryDao: WorkHistoryDao,
    private val attendanceDao: AttendanceDao,
    private val notificationPreferencesDao: NotificationPreferencesDao,
    private val syncQueueDao: SyncQueueDao,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val notificationService: NotificationService
) {
    
    companion object {
        private const val TAG = "LabourRepository"
        private const val MAX_RETRY_COUNT = 5
        private const val MAX_IMAGE_SIZE_KB = 500
    }
    
    // ========== Helper Functions ==========
    
    /**
     * Check if device has internet connectivity
     * Requirement 16: Offline Data Persistence
     */
    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
            as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    /**
     * Calculate distance between two GPS coordinates using Haversine formula
     * Requirement 17: Location-Based Search
     * 
     * @return Distance in kilometers
     */
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val R = 6371.0 // Earth radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
    
    /**
     * Compress image to meet size requirements
     * Requirement 19: Profile Photo Management
     * 
     * @param uri Image URI to compress
     * @return Compressed image as byte array (max 500KB)
     */
    private suspend fun compressImage(uri: Uri): ByteArray {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        
        val outputStream = ByteArrayOutputStream()
        var quality = 90
        var compressedBytes: ByteArray
        
        do {
            outputStream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            compressedBytes = outputStream.toByteArray()
            quality -= 10
        } while (compressedBytes.size > MAX_IMAGE_SIZE_KB * 1024 && quality > 0)
        
        return compressedBytes
    }
    
    /**
     * Queue an operation for sync when connectivity is restored
     * Requirement 16: Offline Data Persistence
     */
    private suspend fun queueForSync(
        entityType: String,
        entityId: String,
        operation: String,
        data: Any
    ) {
        val item = SyncQueueItem(
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            data = Gson().toJson(data),
            retryCount = 0,
            createdAt = System.currentTimeMillis(),
            lastAttempt = null
        )
        syncQueueDao.insertSyncItem(item)
        Log.d(TAG, "Queued for sync: $entityType $entityId $operation")
    }
    
    // ========== Sync Queue Management ==========
    
    /**
     * Process all pending sync queue items
     * Should be called when connectivity is restored
     * Requirement 16: Offline Data Persistence
     */
    suspend fun processSyncQueue() {
        if (!isOnline()) {
            Log.d(TAG, "Device offline, skipping sync queue processing")
            return
        }
        
        try {
            val pendingItems = syncQueueDao.getAllSyncItemsOnce()
            Log.d(TAG, "Processing ${pendingItems.size} pending sync items")
            
            for (item in pendingItems) {
                try {
                    when (item.entityType) {
                        "labourer_profile" -> syncLabourerProfileItem(item)
                        "availability" -> syncAvailabilityItem(item)
                        "booking" -> syncBookingItem(item)
                        "rating" -> syncRatingItem(item)
                        "work_history" -> syncWorkHistoryItem(item)
                        "attendance" -> syncAttendanceItem(item)
                        "notification_preferences" -> syncNotificationPreferencesItem(item)
                        else -> Log.w(TAG, "Unknown entity type: ${item.entityType}")
                    }
                    
                    // Delete successfully synced item
                    syncQueueDao.deleteSyncItemById(item.id)
                    Log.d(TAG, "Successfully synced item ${item.id}")
                } catch (e: Exception) {
                    // Increment retry count
                    syncQueueDao.incrementRetryCount(item.id, System.currentTimeMillis())
                    Log.e(TAG, "Failed to sync item ${item.id}", e)
                }
            }
            
            // Clean up failed items (retry count > MAX_RETRY_COUNT)
            syncQueueDao.deleteFailedSyncItems(MAX_RETRY_COUNT)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process sync queue", e)
        }
    }
    
    private suspend fun syncLabourerProfileItem(item: SyncQueueItem) {
        val profile = Gson().fromJson(item.data, LabourerProfile::class.java)
        val profileMap = profile.toFirestoreMap()
        
        when (item.operation) {
            "CREATE", "UPDATE" -> {
                firestore.collection("labourer_profiles")
                    .document(item.entityId)
                    .set(profileMap)
                    .await()
            }
            "DELETE" -> {
                firestore.collection("labourer_profiles")
                    .document(item.entityId)
                    .delete()
                    .await()
            }
        }
    }
    
    private suspend fun syncAvailabilityItem(item: SyncQueueItem) {
        val data = Gson().fromJson(item.data, Map::class.java) as Map<*, *>
        val status = data["status"] as String
        val timestamp = (data["timestamp"] as Double).toLong()
        
        firestore.collection("labourer_profiles")
            .document(item.entityId)
            .update(
                mapOf(
                    "availabilityStatus" to status,
                    "lastAvailabilityUpdate" to Timestamp(Date(timestamp)),
                    "updatedAt" to Timestamp(Date(timestamp))
                )
            )
            .await()
    }
    
    private suspend fun syncBookingItem(item: SyncQueueItem) {
        when (item.operation) {
            "CREATE" -> {
                val booking = Gson().fromJson(item.data, Booking::class.java)
                firestore.collection("bookings")
                    .document(item.entityId)
                    .set(booking.toFirestoreMap())
                    .await()
            }
            "UPDATE" -> {
                val booking = Gson().fromJson(item.data, Booking::class.java)
                firestore.collection("bookings")
                    .document(item.entityId)
                    .set(booking.toFirestoreMap())
                    .await()
            }
            "DELETE" -> {
                firestore.collection("bookings")
                    .document(item.entityId)
                    .delete()
                    .await()
            }
        }
    }
    
    private suspend fun syncRatingItem(item: SyncQueueItem) {
        val rating = Gson().fromJson(item.data, Rating::class.java)
        firestore.collection("ratings")
            .document(item.entityId)
            .set(rating.toFirestoreMap())
            .await()
    }
    
    private suspend fun syncWorkHistoryItem(item: SyncQueueItem) {
        val workHistory = Gson().fromJson(item.data, WorkHistory::class.java)
        firestore.collection("work_history")
            .document(item.entityId)
            .set(workHistory.toFirestoreMap())
            .await()
    }
    
    private suspend fun syncAttendanceItem(item: SyncQueueItem) {
        val attendance = Gson().fromJson(item.data, Attendance::class.java)
        firestore.collection("attendance")
            .document(item.entityId)
            .set(attendance.toFirestoreMap())
            .await()
    }
    
    private suspend fun syncNotificationPreferencesItem(item: SyncQueueItem) {
        val prefs = Gson().fromJson(item.data, NotificationPreferences::class.java)
        firestore.collection("notification_preferences")
            .document(item.entityId)
            .set(prefs.toFirestoreMap())
            .await()
    }
    
    // ========== Labourer Profile Operations ==========
    
    /**
     * Create a new labourer profile
     * Requirement 2: Labourer Profile Creation
     * Requirement 16: Offline Data Persistence
     * 
     * Offline-first approach:
     * 1. Save to Room database first (source of truth)
     * 2. If online, sync to Firestore immediately
     * 3. If offline, queue for sync when connectivity is restored
     * 
     * @param profile The labourer profile to create
     * @return Result indicating success or failure
     */
    suspend fun createLabourerProfile(profile: LabourerProfile): Result<Unit> {
        return try {
            // Save to Room first (offline-first)
            labourDao.insertLabourerProfile(profile)
            Log.d(TAG, "Saved labourer profile to Room: ${profile.userId}")
            
            // Try to sync to Firestore
            if (isOnline()) {
                val profileMap = profile.toFirestoreMap()
                firestore.collection("labourer_profiles")
                    .document(profile.userId)
                    .set(profileMap)
                    .await()
                Log.d(TAG, "Synced labourer profile to Firestore: ${profile.userId}")
            } else {
                // Queue for sync
                queueForSync("labourer_profile", profile.userId, "CREATE", profile)
                Log.d(TAG, "Queued labourer profile for sync: ${profile.userId}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create labourer profile", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing labourer profile
     * Requirement 2: Labourer Profile Creation
     * Requirement 16: Offline Data Persistence
     * 
     * Offline-first approach:
     * 1. Update Room database first
     * 2. If online, sync to Firestore immediately
     * 3. If offline, queue for sync when connectivity is restored
     * 
     * @param profile The updated labourer profile
     * @return Result indicating success or failure
     */
    suspend fun updateLabourerProfile(profile: LabourerProfile): Result<Unit> {
        return try {
            // Update Room first
            labourDao.updateLabourerProfile(profile)
            Log.d(TAG, "Updated labourer profile in Room: ${profile.userId}")
            
            // Try to sync to Firestore
            if (isOnline()) {
                firestore.collection("labourer_profiles")
                    .document(profile.userId)
                    .set(profile.toFirestoreMap())
                    .await()
                Log.d(TAG, "Synced labourer profile update to Firestore: ${profile.userId}")
            } else {
                // Queue for sync
                queueForSync("labourer_profile", profile.userId, "UPDATE", profile)
                Log.d(TAG, "Queued labourer profile update for sync: ${profile.userId}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update labourer profile", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get a labourer profile with reactive updates
     * Requirement 2: Labourer Profile Creation
     * Requirement 7: Labourer Profile Viewing
     * Requirement 16: Offline Data Persistence
     * Requirement 18: Real-Time Availability Updates
     * 
     * Returns a Flow from Room database for reactive UI updates.
     * If online, triggers background sync from Firestore to ensure data is up-to-date.
     * 
     * @param userId The user ID of the labourer
     * @return Flow emitting the labourer profile or null if not found
     */
    fun getLabourerProfile(userId: String): Flow<LabourerProfile?> {
        // Trigger background sync from Firestore if online
        // Note: This should be called from a coroutine scope (e.g., viewModelScope)
        // The caller is responsible for launching this in the appropriate scope
        
        // Return Room flow for reactive updates
        return labourDao.getLabourerProfile(userId)
    }
    
    /**
     * Sync a labourer profile from Firestore to Room
     * Should be called in background when online to ensure local data is up-to-date
     * Requirement 16: Offline Data Persistence
     * Requirement 18: Real-Time Availability Updates
     * 
     * @param userId The user ID of the labourer to sync
     */
    suspend fun syncLabourerProfileFromFirestore(userId: String) {
        if (!isOnline()) {
            Log.d(TAG, "Device offline, skipping profile sync from Firestore")
            return
        }
        
        try {
            val doc = firestore.collection("labourer_profiles")
                .document(userId)
                .get()
                .await()
            
            if (doc.exists()) {
                val profile = doc.toLabourerProfile()
                labourDao.insertLabourerProfile(profile)
                Log.d(TAG, "Synced labourer profile from Firestore to Room: $userId")
            } else {
                Log.w(TAG, "Labourer profile not found in Firestore: $userId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync labourer profile from Firestore", e)
        }
    }
    
    /**
     * Update labourer availability status with real-time sync
     * Requirement 5: Labourer Availability Management
     * Requirement 18: Real-Time Availability Updates
     * Requirement 16: Offline Data Persistence
     * 
     * Updates availability status in Room and Firestore.
     * If offline, queues the update for sync when connectivity is restored.
     * 
     * @param userId The user ID of the labourer
     * @param status The new availability status
     * @return Result indicating success or failure
     */
    suspend fun updateAvailabilityStatus(
        userId: String, 
        status: AvailabilityStatus
    ): Result<Unit> {
        return try {
            val timestamp = System.currentTimeMillis()
            
            // Update Room first
            labourDao.updateAvailabilityStatus(userId, status, timestamp)
            Log.d(TAG, "Updated availability status in Room: $userId -> ${status.name}")
            
            // Try to sync to Firestore
            if (isOnline()) {
                firestore.collection("labourer_profiles")
                    .document(userId)
                    .update(
                        mapOf(
                            "availabilityStatus" to status.name,
                            "lastAvailabilityUpdate" to Timestamp(Date(timestamp)),
                            "updatedAt" to Timestamp(Date(timestamp))
                        )
                    )
                    .await()
                Log.d(TAG, "Synced availability status to Firestore: $userId -> ${status.name}")
            } else {
                // Queue for sync
                queueForSync("availability", userId, "UPDATE", mapOf(
                    "status" to status.name,
                    "timestamp" to timestamp
                ))
                Log.d(TAG, "Queued availability status for sync: $userId -> ${status.name}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update availability status", e)
            Result.failure(e)
        }
    }
    
    // ========== Profile Photo Operations ==========
    
    /**
     * Upload a profile photo for a labourer
     * Requirement 19: Profile Photo Management
     * 
     * Compresses the image to max 500KB, uploads to Firebase Storage,
     * and returns the download URL.
     * 
     * @param userId The user ID of the labourer
     * @param imageUri The URI of the image to upload
     * @return Result containing the download URL or error
     */
    suspend fun uploadProfilePhoto(userId: String, imageUri: Uri): Result<String> {
        return try {
            // Compress image
            val compressedBytes = compressImage(imageUri)
            Log.d(TAG, "Compressed image to ${compressedBytes.size / 1024} KB")
            
            // Upload to Firebase Storage
            val filename = "profile_photos/${userId}/${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child(filename)
            
            storageRef.putBytes(compressedBytes).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            Log.d(TAG, "Uploaded profile photo for $userId: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload profile photo", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete a profile photo from Firebase Storage
     * Requirement 19: Profile Photo Management
     * 
     * Deletes the photo from Firebase Storage using the download URL.
     * 
     * @param photoUrl The download URL of the photo to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteProfilePhoto(photoUrl: String): Result<Unit> {
        return try {
            val storageRef = storage.getReferenceFromUrl(photoUrl)
            storageRef.delete().await()
            
            Log.d(TAG, "Deleted profile photo: $photoUrl")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete profile photo", e)
            Result.failure(e)
        }
    }
    
    // ========== Booking Operations ==========
    
    /**
     * Create a new booking with conflict detection
     * Requirement 8: Booking Creation
     * Requirement 9: Emergency Hiring
     * Requirement 16: Offline Data Persistence
     * 
     * Offline-first approach:
     * 1. Check for booking conflicts on the same date
     * 2. Save to Room database first (source of truth)
     * 3. If online, sync to Firestore immediately and send notifications
     * 4. If offline, queue for sync when connectivity is restored
     * 
     * @param booking The booking to create
     * @return Result containing the booking ID or error
     */
    suspend fun createBooking(booking: Booking): Result<String> {
        return try {
            // Check for conflicts
            val conflicts = bookingDao.hasConflictingBooking(
                booking.labourerId,
                booking.workDate
            )
            
            if (conflicts > 0) {
                return Result.failure(Exception("Labourer already has a booking on this date"))
            }
            
            // Save to Room first (offline-first)
            bookingDao.insertBooking(booking)
            Log.d(TAG, "Saved booking to Room: ${booking.bookingId}")
            
            // Try to sync to Firestore
            if (isOnline()) {
                try {
                    // Ensure user document exists before creating booking (fallback safety check)
                    val userId = booking.farmerId
                    val userDoc = firestore.collection("users").document(userId).get().await()
                    if (!userDoc.exists()) {
                        Log.w(TAG, "User document doesn't exist for $userId, creating it now")
                        firestore.collection("users").document(userId)
                            .set(
                                mapOf("role" to "FARMER"),
                                com.google.firebase.firestore.SetOptions.merge()
                            )
                            .await()
                        Log.d(TAG, "Created user document for $userId")
                    }
                    
                    // Now create the booking
                    firestore.collection("bookings")
                        .document(booking.bookingId)
                        .set(booking.toFirestoreMap())
                        .await()
                    Log.d(TAG, "Synced booking to Firestore: ${booking.bookingId}")
                    
                    // Send notifications to labourer
                    sendBookingNotification(booking)
                } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                    // Handle Firestore-specific errors
                    when (e.code) {
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                            Log.e(TAG, "Permission denied creating booking. User: ${booking.farmerId}, Booking: ${booking.bookingId}", e)
                            return Result.failure(Exception("Permission denied. Please check your network connection and try again."))
                        }
                        else -> {
                            Log.e(TAG, "Firestore error creating booking: ${e.code}", e)
                            // Queue for sync and continue
                            queueForSync("booking", booking.bookingId, "CREATE", booking)
                            Log.d(TAG, "Queued booking for sync after Firestore error: ${booking.bookingId}")
                        }
                    }
                }
            } else {
                // Queue for sync
                queueForSync("booking", booking.bookingId, "CREATE", booking)
                Log.d(TAG, "Queued booking for sync: ${booking.bookingId}")
            }
            
            Result.success(booking.bookingId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create booking", e)
            Result.failure(e)
        }
    }
    
    /**
     * Accept a booking and update labourer availability
     * Requirement 11: Booking Management for Labourers
     * Requirement 18: Real-Time Availability Updates
     * Requirement 16: Offline Data Persistence
     * 
     * Offline-first approach:
     * 1. Update booking status in Room database
     * 2. If online, sync to Firestore, update availability, and send confirmation
     * 3. If offline, queue for sync when connectivity is restored
     * 
     * @param bookingId The ID of the booking to accept
     * @return Result indicating success or failure
     */
    suspend fun acceptBooking(bookingId: String): Result<Unit> {
        return try {
            val timestamp = System.currentTimeMillis()
            bookingDao.acceptBooking(bookingId, BookingStatus.ACCEPTED, timestamp)
            Log.d(TAG, "Accepted booking in Room: $bookingId")
            
            val booking = bookingDao.getBookingOnce(bookingId)
                ?: return Result.failure(Exception("Booking not found"))
            
            // Try to sync to Firestore
            if (isOnline()) {
                firestore.collection("bookings")
                    .document(bookingId)
                    .update(
                        mapOf(
                            "status" to "ACCEPTED",
                            "acceptedAt" to Timestamp(Date(timestamp)),
                            "updatedAt" to Timestamp(Date(timestamp))
                        )
                    )
                    .await()
                Log.d(TAG, "Synced booking acceptance to Firestore: $bookingId")
                
                // Update labourer availability to BOOKED
                updateAvailabilityStatus(booking.labourerId, AvailabilityStatus.BOOKED)
                
                // Send confirmation to farmer
                sendBookingConfirmation(booking)
            } else {
                // Queue for sync
                queueForSync("booking", bookingId, "UPDATE", mapOf(
                    "status" to "ACCEPTED",
                    "timestamp" to timestamp
                ))
                Log.d(TAG, "Queued booking acceptance for sync: $bookingId")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to accept booking", e)
            Result.failure(e)
        }
    }
    
    /**
     * Decline a booking and notify the farmer
     * Requirement 11: Booking Management for Labourers
     * Requirement 10: SMS and WhatsApp Notifications
     * Requirement 16: Offline Data Persistence
     * 
     * Offline-first approach:
     * 1. Update booking status in Room database
     * 2. If online, sync to Firestore and send notification to farmer
     * 3. If offline, queue for sync when connectivity is restored
     * 
     * @param bookingId The ID of the booking to decline
     * @return Result indicating success or failure
     */
    suspend fun declineBooking(bookingId: String): Result<Unit> {
        return try {
            val timestamp = System.currentTimeMillis()
            bookingDao.updateBookingStatus(bookingId, BookingStatus.DECLINED, timestamp)
            Log.d(TAG, "Declined booking in Room: $bookingId")
            
            val booking = bookingDao.getBookingOnce(bookingId)
                ?: return Result.failure(Exception("Booking not found"))
            
            // Try to sync to Firestore
            if (isOnline()) {
                firestore.collection("bookings")
                    .document(bookingId)
                    .update(
                        mapOf(
                            "status" to "DECLINED",
                            "updatedAt" to Timestamp(Date(timestamp))
                        )
                    )
                    .await()
                Log.d(TAG, "Synced booking decline to Firestore: $bookingId")
                
                // Notify farmer
                sendBookingDeclinedNotification(booking)
            } else {
                // Queue for sync
                queueForSync("booking", bookingId, "UPDATE", mapOf(
                    "status" to "DECLINED",
                    "timestamp" to timestamp
                ))
                Log.d(TAG, "Queued booking decline for sync: $bookingId")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decline booking", e)
            Result.failure(e)
        }
    }
    
    /**
     * Complete a booking and create work history entry
     * Requirement 12: Booking Management for Farmers
     * Requirement 14: Work History and Attendance Tracking
     * Requirement 16: Offline Data Persistence
     * 
     * Offline-first approach:
     * 1. Update booking status with actual hours and payment in Room
     * 2. Create work history entry
     * 3. Update labourer's completed bookings count
     * 4. If online, sync to Firestore
     * 5. If offline, queue for sync when connectivity is restored
     * 
     * @param bookingId The ID of the booking to complete
     * @param actualHours The actual hours worked
     * @param actualPayment The actual payment amount
     * @return Result indicating success or failure
     */
    suspend fun completeBooking(
        bookingId: String,
        actualHours: Int,
        actualPayment: Int
    ): Result<Unit> {
        return try {
            val timestamp = System.currentTimeMillis()
            bookingDao.completeBooking(
                bookingId,
                BookingStatus.COMPLETED,
                timestamp,
                actualHours,
                actualPayment
            )
            Log.d(TAG, "Completed booking in Room: $bookingId")
            
            val booking = bookingDao.getBookingOnce(bookingId)
                ?: return Result.failure(Exception("Booking not found"))
            
            // Create work history entry
            createWorkHistoryEntry(booking, actualHours, actualPayment)
            
            // Update labourer stats
            labourDao.incrementCompletedBookings(booking.labourerId, timestamp)
            
            // Try to sync to Firestore
            if (isOnline()) {
                firestore.collection("bookings")
                    .document(bookingId)
                    .update(
                        mapOf(
                            "status" to "COMPLETED",
                            "completedAt" to Timestamp(Date(timestamp)),
                            "actualHours" to actualHours,
                            "actualPayment" to actualPayment,
                            "updatedAt" to Timestamp(Date(timestamp))
                        )
                    )
                    .await()
                Log.d(TAG, "Synced booking completion to Firestore: $bookingId")
            } else {
                // Queue for sync
                queueForSync("booking", bookingId, "UPDATE", mapOf(
                    "status" to "COMPLETED",
                    "timestamp" to timestamp,
                    "actualHours" to actualHours,
                    "actualPayment" to actualPayment
                ))
                Log.d(TAG, "Queued booking completion for sync: $bookingId")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete booking", e)
            Result.failure(e)
        }
    }
    
    /**
     * Cancel a booking and notify the labourer
     * Requirement 12: Booking Management for Farmers
     * Requirement 10: SMS and WhatsApp Notifications
     * Requirement 16: Offline Data Persistence
     * 
     * Offline-first approach:
     * 1. Update booking status with cancellation reason in Room
     * 2. If online, sync to Firestore and send notification to labourer
     * 3. If offline, queue for sync when connectivity is restored
     * 
     * @param bookingId The ID of the booking to cancel
     * @param reason The reason for cancellation
     * @return Result indicating success or failure
     */
    suspend fun cancelBooking(bookingId: String, reason: String): Result<Unit> {
        return try {
            val timestamp = System.currentTimeMillis()
            val booking = bookingDao.getBookingOnce(bookingId)
                ?: return Result.failure(Exception("Booking not found"))
            
            val updatedBooking = booking.copy(
                status = BookingStatus.CANCELLED,
                cancelledAt = timestamp,
                cancellationReason = reason,
                updatedAt = timestamp
            )
            
            bookingDao.updateBooking(updatedBooking)
            Log.d(TAG, "Cancelled booking in Room: $bookingId")
            
            // Try to sync to Firestore
            if (isOnline()) {
                firestore.collection("bookings")
                    .document(bookingId)
                    .update(
                        mapOf(
                            "status" to "CANCELLED",
                            "cancelledAt" to Timestamp(Date(timestamp)),
                            "cancellationReason" to reason,
                            "updatedAt" to Timestamp(Date(timestamp))
                        )
                    )
                    .await()
                Log.d(TAG, "Synced booking cancellation to Firestore: $bookingId")
                
                // Notify labourer
                sendBookingCancelledNotification(booking)
            } else {
                // Queue for sync
                queueForSync("booking", bookingId, "UPDATE", updatedBooking)
                Log.d(TAG, "Queued booking cancellation for sync: $bookingId")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel booking", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get farmer bookings with reactive updates
     * Requirement 12: Booking Management for Farmers
     * 
     * @param farmerId The farmer's user ID
     * @return Flow emitting list of bookings
     */
    fun getFarmerBookings(farmerId: String): Flow<List<Booking>> {
        return bookingDao.getFarmerBookings(farmerId)
    }
    
    /**
     * Get labourer bookings with reactive updates
     * Requirement 11: Booking Management for Labourers
     * 
     * @param labourerId The labourer's user ID
     * @return Flow emitting list of bookings
     */
    fun getLabourerBookings(labourerId: String): Flow<List<Booking>> {
        return bookingDao.getLabourerBookings(labourerId)
    }
    
    // ========== Work History Operations ==========
    
    /**
     * Create a work history entry from a completed booking
     * Requirement 14: Work History and Attendance Tracking
     * Requirement 16: Offline Data Persistence
     * 
     * @param booking The completed booking
     * @param actualHours The actual hours worked
     * @param actualPayment The actual payment received
     */
    private suspend fun createWorkHistoryEntry(
        booking: Booking,
        actualHours: Int,
        actualPayment: Int
    ) {
        try {
            val workHistory = WorkHistory(
                workId = java.util.UUID.randomUUID().toString(),
                labourerId = booking.labourerId,
                bookingId = booking.bookingId,
                farmerName = booking.farmerName,
                workDate = booking.workDate,
                workType = booking.workType,
                actualHours = actualHours,
                paymentReceived = actualPayment,
                ratingReceived = null,
                completedAt = System.currentTimeMillis()
            )
            
            workHistoryDao.insertWorkHistory(workHistory)
            Log.d(TAG, "Created work history entry: ${workHistory.workId}")
            
            if (isOnline()) {
                firestore.collection("work_history")
                    .document(workHistory.workId)
                    .set(workHistory.toFirestoreMap())
                    .await()
                Log.d(TAG, "Synced work history to Firestore: ${workHistory.workId}")
            } else {
                queueForSync("work_history", workHistory.workId, "CREATE", workHistory)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create work history entry", e)
        }
    }
    
    /**
     * Get work history for a labourer with reactive updates
     * Requirement 14: Work History and Attendance Tracking
     * 
     * @param labourerId The labourer's user ID
     * @return Flow emitting list of work history entries
     */
    fun getWorkHistory(labourerId: String): Flow<List<WorkHistory>> {
        return workHistoryDao.getWorkHistory(labourerId)
    }
    
    /**
     * Get work history statistics for a labourer in a date range
     * Requirement 14: Work History and Attendance Tracking
     * 
     * @param labourerId The labourer's user ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Work history statistics
     */
    suspend fun getWorkHistoryStats(
        labourerId: String,
        startDate: Long,
        endDate: Long
    ): WorkHistoryStats {
        val totalEarnings = workHistoryDao.getTotalEarnings(labourerId, startDate, endDate) ?: 0
        val totalDays = workHistoryDao.getTotalDaysWorked(labourerId, startDate, endDate)
        
        return WorkHistoryStats(
            totalEarnings = totalEarnings,
            totalDaysWorked = totalDays
        )
    }
    
    // ========== Notification Operations ==========
    
    /**
     * Send booking notification to labourer
     * Requirement 10: SMS and WhatsApp Notifications
     * Requirement 22: Notification Preferences
     * 
     * @param booking The booking to notify about
     */
    private suspend fun sendBookingNotification(booking: Booking) {
        try {
            val prefs = notificationPreferencesDao.getNotificationPreferencesOnce(booking.labourerId)
            val profile = labourDao.getLabourerProfileOnce(booking.labourerId)
            
            if (prefs?.newBookingsEnabled != false) {
                val message = formatBookingNotification(booking, profile?.preferredLanguage ?: "en")
                
                if (prefs?.smsEnabled != false) {
                    notificationService.sendSMS(booking.labourerPhone, message)
                }
                
                if (prefs?.whatsappEnabled != false) {
                    notificationService.sendWhatsApp(booking.labourerPhone, message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send booking notification", e)
        }
    }
    
    /**
     * Send booking confirmation to farmer
     * Requirement 10: SMS and WhatsApp Notifications
     * Requirement 22: Notification Preferences
     * 
     * @param booking The confirmed booking
     */
    private suspend fun sendBookingConfirmation(booking: Booking) {
        try {
            val prefs = notificationPreferencesDao.getNotificationPreferencesOnce(booking.farmerId)
            
            if (prefs?.bookingConfirmationsEnabled != false) {
                val message = formatConfirmationNotification(booking, "en")
                
                if (prefs?.smsEnabled != false) {
                    notificationService.sendSMS(booking.farmerPhone, message)
                }
                
                if (prefs?.whatsappEnabled != false) {
                    notificationService.sendWhatsApp(booking.farmerPhone, message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send booking confirmation", e)
        }
    }
    
    /**
     * Send booking declined notification to farmer
     * Requirement 10: SMS and WhatsApp Notifications
     * 
     * @param booking The declined booking
     */
    private suspend fun sendBookingDeclinedNotification(booking: Booking) {
        try {
            val prefs = notificationPreferencesDao.getNotificationPreferencesOnce(booking.farmerId)
            
            if (prefs?.bookingConfirmationsEnabled != false) {
                val message = formatBookingDeclinedNotification(booking, "en")
                
                if (prefs?.smsEnabled != false) {
                    notificationService.sendSMS(booking.farmerPhone, message)
                }
                
                if (prefs?.whatsappEnabled != false) {
                    notificationService.sendWhatsApp(booking.farmerPhone, message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send booking declined notification", e)
        }
    }
    
    /**
     * Send booking cancelled notification to labourer
     * Requirement 10: SMS and WhatsApp Notifications
     * 
     * @param booking The cancelled booking
     */
    private suspend fun sendBookingCancelledNotification(booking: Booking) {
        try {
            val prefs = notificationPreferencesDao.getNotificationPreferencesOnce(booking.labourerId)
            
            if (prefs?.bookingConfirmationsEnabled != false) {
                val message = formatBookingCancelledNotification(booking, "en")
                
                if (prefs?.smsEnabled != false) {
                    notificationService.sendSMS(booking.labourerPhone, message)
                }
                
                if (prefs?.whatsappEnabled != false) {
                    notificationService.sendWhatsApp(booking.labourerPhone, message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send booking cancelled notification", e)
        }
    }
    
    // ========== Rating Operations ==========
    
    /**
     * Submit a rating for a completed booking
     * Requirement 13: Rating and Review System
     * Requirement 16: Offline Data Persistence
     * 
     * Offline-first approach:
     * 1. Check for duplicate ratings (prevent multiple ratings for same booking)
     * 2. Save rating to Room database first
     * 3. If online, sync to Firestore and recalculate labourer's average rating
     * 4. If offline, queue for sync when connectivity is restored
     * 
     * @param rating The rating to submit
     * @return Result indicating success or failure
     */
    suspend fun submitRating(rating: Rating): Result<Unit> {
        return try {
            // Check if booking has already been rated
            val existing = ratingDao.getRatingForBooking(rating.bookingId)
            if (existing != null) {
                Log.w(TAG, "Booking ${rating.bookingId} has already been rated")
                return Result.failure(Exception("This booking has already been rated"))
            }
            
            // Save rating to Room first (offline-first)
            ratingDao.insertRating(rating)
            Log.d(TAG, "Saved rating to Room: ${rating.ratingId}")
            
            // Try to sync to Firestore
            if (isOnline()) {
                firestore.collection("ratings")
                    .document(rating.ratingId)
                    .set(rating.toFirestoreMap())
                    .await()
                Log.d(TAG, "Synced rating to Firestore: ${rating.ratingId}")
                
                // Recalculate labourer's average rating
                updateLabourerRating(rating.labourerId)
            } else {
                // Queue for sync
                queueForSync("rating", rating.ratingId, "CREATE", rating)
                Log.d(TAG, "Queued rating for sync: ${rating.ratingId}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit rating", e)
            Result.failure(e)
        }
    }
    
    /**
     * Recalculate and update a labourer's average rating
     * Requirement 13: Rating and Review System
     * Requirement 16: Offline Data Persistence
     * 
     * This method:
     * 1. Calculates the average rating from all ratings in Room database
     * 2. Gets the total count of ratings
     * 3. Updates the labourer profile in Room with new statistics
     * 4. If online, syncs the updated statistics to Firestore
     * 
     * @param labourerId The user ID of the labourer whose rating should be updated
     */
    private suspend fun updateLabourerRating(labourerId: String) {
        try {
            // Calculate average rating and total count from Room database
            val avgRating = ratingDao.getAverageRating(labourerId) ?: 0.0
            val totalRatings = ratingDao.getTotalRatings(labourerId)
            
            Log.d(TAG, "Calculated rating for $labourerId: avg=$avgRating, total=$totalRatings")
            
            // Update labourer profile in Room
            val timestamp = System.currentTimeMillis()
            labourDao.updateRating(labourerId, avgRating, totalRatings, timestamp)
            Log.d(TAG, "Updated labourer rating in Room: $labourerId")
            
            // Try to sync to Firestore
            if (isOnline()) {
                firestore.collection("labourer_profiles")
                    .document(labourerId)
                    .update(
                        mapOf(
                            "averageRating" to avgRating,
                            "totalRatings" to totalRatings,
                            "updatedAt" to Timestamp(Date(timestamp))
                        )
                    )
                    .await()
                Log.d(TAG, "Synced labourer rating to Firestore: $labourerId")
            } else {
                Log.d(TAG, "Device offline, rating update will sync later: $labourerId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update labourer rating for $labourerId", e)
            // Don't throw - this is a background operation that shouldn't fail the main rating submission
        }
    }
    
    /**
     * Get all ratings for a labourer with reactive updates
     * Requirement 13: Rating and Review System
     * 
     * @param labourerId The user ID of the labourer
     * @return Flow emitting list of ratings
     */
    fun getLabourerRatings(labourerId: String): Flow<List<Rating>> {
        return ratingDao.getLabourerRatings(labourerId)
    }
    
    // ========== Real-Time Sync Operations ==========
    
    /**
     * Set up real-time listeners for bookings and profiles
     * Requirement 18: Real-Time Availability Updates
     * Requirement 16: Offline Data Persistence
     * 
     * This method sets up Firestore real-time listeners that automatically
     * update the local Room database when changes occur in Firestore.
     * Should be called when the app starts or when user logs in.
     * 
     * @param userId The user ID to listen for changes
     * @param userRole The role of the user (FARMER or LABOURER)
     */
    fun setupRealtimeListeners(userId: String, userRole: UserRole) {
        if (!isOnline()) {
            Log.d(TAG, "Device offline, skipping real-time listener setup")
            return
        }
        
        try {
            when (userRole) {
                UserRole.LABOURER -> {
                    // Listen to labourer's own profile changes
                    setupLabourerProfileListener(userId)
                    // Listen to bookings for this labourer
                    setupLabourerBookingsListener(userId)
                }
                UserRole.FARMER -> {
                    // Listen to bookings for this farmer
                    setupFarmerBookingsListener(userId)
                }
            }
            Log.d(TAG, "Real-time listeners set up for user: $userId, role: $userRole")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set up real-time listeners", e)
        }
    }
    
    /**
     * Set up real-time listener for a labourer's profile
     * Requirement 18: Real-Time Availability Updates
     * 
     * Listens for changes to the labourer's profile in Firestore and
     * automatically updates the local Room database. This ensures
     * availability status and other profile changes are synced in real-time.
     * 
     * @param labourerId The labourer's user ID
     */
    private fun setupLabourerProfileListener(labourerId: String) {
        firestore.collection("labourer_profiles")
            .document(labourerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to labourer profile changes", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val remoteProfile = snapshot.toLabourerProfile()
                        
                        // Launch coroutine to update Room database
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                // Get local profile for conflict resolution
                                val localProfile = labourDao.getLabourerProfileOnce(labourerId)
                                
                                if (localProfile != null) {
                                    // Resolve conflict: Firestore wins
                                    val resolvedProfile = resolveProfileConflict(localProfile, remoteProfile)
                                    labourDao.updateLabourerProfile(resolvedProfile)
                                    Log.d(TAG, "Updated labourer profile from Firestore: $labourerId")
                                } else {
                                    // No local profile, just insert remote
                                    labourDao.insertLabourerProfile(remoteProfile)
                                    Log.d(TAG, "Inserted labourer profile from Firestore: $labourerId")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to update local profile from Firestore", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse labourer profile from Firestore", e)
                    }
                }
            }
    }
    
    /**
     * Set up real-time listener for a labourer's bookings
     * Requirement 18: Real-Time Availability Updates
     * Requirement 11: Booking Management for Labourers
     * 
     * Listens for changes to bookings where the user is the labourer.
     * Updates local Room database when bookings are created, updated, or deleted.
     * 
     * @param labourerId The labourer's user ID
     */
    private fun setupLabourerBookingsListener(labourerId: String) {
        firestore.collection("bookings")
            .whereEqualTo("labourerId", labourerId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to labourer bookings", error)
                    return@addSnapshotListener
                }
                
                if (snapshots != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            for (change in snapshots.documentChanges) {
                                val bookingId = change.document.id
                                
                                when (change.type) {
                                    com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                    com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                        try {
                                            val remoteBooking = change.document.toBooking()
                                            
                                            // Get local booking for conflict resolution
                                            val localBooking = bookingDao.getBookingOnce(bookingId)
                                            
                                            if (localBooking != null) {
                                                // Resolve conflict: Firestore wins
                                                val resolvedBooking = resolveBookingConflict(localBooking, remoteBooking)
                                                bookingDao.updateBooking(resolvedBooking)
                                                Log.d(TAG, "Updated booking from Firestore: $bookingId")
                                            } else {
                                                // No local booking, just insert remote
                                                bookingDao.insertBooking(remoteBooking)
                                                Log.d(TAG, "Inserted booking from Firestore: $bookingId")
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Failed to parse booking from Firestore: $bookingId", e)
                                        }
                                    }
                                    com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                        try {
                                            val localBooking = bookingDao.getBookingOnce(bookingId)
                                            if (localBooking != null) {
                                                bookingDao.deleteBooking(localBooking)
                                                Log.d(TAG, "Deleted booking from local: $bookingId")
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Failed to delete booking: $bookingId", e)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to process booking changes", e)
                        }
                    }
                }
            }
    }
    
    /**
     * Set up real-time listener for a farmer's bookings
     * Requirement 18: Real-Time Availability Updates
     * Requirement 12: Booking Management for Farmers
     * 
     * Listens for changes to bookings where the user is the farmer.
     * Updates local Room database when bookings are created, updated, or deleted.
     * 
     * @param farmerId The farmer's user ID
     */
    private fun setupFarmerBookingsListener(farmerId: String) {
        firestore.collection("bookings")
            .whereEqualTo("farmerId", farmerId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to farmer bookings", error)
                    return@addSnapshotListener
                }
                
                if (snapshots != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            for (change in snapshots.documentChanges) {
                                val bookingId = change.document.id
                                
                                when (change.type) {
                                    com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                    com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                        try {
                                            val remoteBooking = change.document.toBooking()
                                            
                                            // Get local booking for conflict resolution
                                            val localBooking = bookingDao.getBookingOnce(bookingId)
                                            
                                            if (localBooking != null) {
                                                // Resolve conflict: Firestore wins
                                                val resolvedBooking = resolveBookingConflict(localBooking, remoteBooking)
                                                bookingDao.updateBooking(resolvedBooking)
                                                Log.d(TAG, "Updated booking from Firestore: $bookingId")
                                            } else {
                                                // No local booking, just insert remote
                                                bookingDao.insertBooking(remoteBooking)
                                                Log.d(TAG, "Inserted booking from Firestore: $bookingId")
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Failed to parse booking from Firestore: $bookingId", e)
                                        }
                                    }
                                    com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                        try {
                                            val localBooking = bookingDao.getBookingOnce(bookingId)
                                            if (localBooking != null) {
                                                bookingDao.deleteBooking(localBooking)
                                                Log.d(TAG, "Deleted booking from local: $bookingId")
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Failed to delete booking: $bookingId", e)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to process booking changes", e)
                        }
                    }
                }
            }
    }
    
    /**
     * Resolve conflict between local and remote labourer profile
     * Requirement 16: Offline Data Persistence
     * 
     * Conflict resolution strategy: Firestore wins
     * This ensures that the most recent server-side data is always used,
     * preventing conflicts when multiple devices update the same profile.
     * 
     * @param localProfile The local profile from Room database
     * @param remoteProfile The remote profile from Firestore
     * @return The resolved profile (always returns remote profile)
     */
    private fun resolveProfileConflict(
        localProfile: LabourerProfile,
        remoteProfile: LabourerProfile
    ): LabourerProfile {
        // Conflict resolution strategy: Firestore wins
        // This is the simplest and most reliable strategy for multi-device sync
        Log.d(TAG, "Resolving profile conflict for ${localProfile.userId}: Firestore wins")
        return remoteProfile
    }
    
    /**
     * Resolve conflict between local and remote booking
     * Requirement 16: Offline Data Persistence
     * 
     * Conflict resolution strategy: Firestore wins
     * This ensures that booking status changes from other users
     * (e.g., labourer accepting a booking) are always reflected locally.
     * 
     * @param localBooking The local booking from Room database
     * @param remoteBooking The remote booking from Firestore
     * @return The resolved booking (always returns remote booking)
     */
    private fun resolveBookingConflict(
        localBooking: Booking,
        remoteBooking: Booking
    ): Booking {
        // Conflict resolution strategy: Firestore wins
        // This ensures that status changes from other users are always applied
        Log.d(TAG, "Resolving booking conflict for ${localBooking.bookingId}: Firestore wins")
        return remoteBooking
    }
    
    // ========== Search Operations ==========
    
    /**
     * Search for labourers with multiple filter criteria
     * Requirement 6: Farmer Search and Filter
     * Requirement 17: Location-Based Search
     * Requirement 21: Search Performance Optimization
     * Requirement 16: Offline Data Persistence
     * 
     * Offline-first approach:
     * 1. Try local cache first (Room database)
     * 2. If online, fetch from Firestore with filters
     * 3. Apply location-based filtering using Haversine distance
     * 4. Filter by wage range
     * 5. Cache results in Room
     * 6. Return filtered and sorted results
     * 
     * @param skills List of skills to filter by (optional)
     * @param locationRadius Maximum distance in kilometers (optional)
     * @param farmerLocation Farmer's GPS coordinates for distance calculation (optional)
     * @param minWage Minimum daily wage filter (optional)
     * @param maxWage Maximum daily wage filter (optional)
     * @param minRating Minimum average rating filter (optional)
     * @return Result containing list of matching labourer profiles
     */
    suspend fun searchLabourers(
        skills: List<Skill>?,
        locationRadius: Double?,
        farmerLocation: LatLng?,
        minWage: Int?,
        maxWage: Int?,
        minRating: Double?
    ): Result<List<LabourerProfile>> {
        return try {
            // First try local cache (Room database)
            val localResults = labourDao.searchLabourers(
                skill = skills?.firstOrNull()?.name,
                minWage = minWage,
                maxWage = maxWage,
                minRating = minRating
            )
            Log.d(TAG, "Found ${localResults.size} labourers in local cache")
            
            // If online, fetch from Firestore with filters
            if (isOnline()) {
                Log.d(TAG, "Device online, fetching from Firestore")
                
                // Start with base query for available labourers
                var query: Query = firestore.collection("labourer_profiles")
                    .whereEqualTo("availabilityStatus", "AVAILABLE")
                
                // Apply skill filter if specified
                if (skills != null && skills.isNotEmpty()) {
                    query = query.whereArrayContainsAny("skills", skills.map { it.name })
                    Log.d(TAG, "Applied skill filter: ${skills.map { it.name }}")
                }
                
                // Apply minimum rating filter if specified
                if (minRating != null) {
                    query = query.whereGreaterThanOrEqualTo("averageRating", minRating)
                    Log.d(TAG, "Applied minimum rating filter: $minRating")
                }
                
                // Execute query with limit
                val snapshot = query.limit(50).get().await()
                val profiles = snapshot.documents.mapNotNull { 
                    try {
                        it.toLabourerProfile()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse labourer profile: ${it.id}", e)
                        null
                    }
                }
                Log.d(TAG, "Fetched ${profiles.size} profiles from Firestore")
                
                // Apply location-based filtering if specified
                val locationFilteredProfiles = if (locationRadius != null && farmerLocation != null) {
                    profiles.filter { profile ->
                        val distance = calculateDistance(
                            farmerLocation.latitude,
                            farmerLocation.longitude,
                            profile.latitude,
                            profile.longitude
                        )
                        val withinRadius = distance <= locationRadius
                        if (!withinRadius) {
                            Log.d(TAG, "Filtered out ${profile.name}: distance $distance km > $locationRadius km")
                        }
                        withinRadius
                    }
                } else {
                    profiles
                }
                Log.d(TAG, "After location filtering: ${locationFilteredProfiles.size} profiles")
                
                // Apply wage range filtering
                val wageFilteredProfiles = locationFilteredProfiles.filter { profile ->
                    when {
                        minWage != null && profile.dailyWage != null && profile.dailyWage < minWage -> {
                            Log.d(TAG, "Filtered out ${profile.name}: wage ${profile.dailyWage} < $minWage")
                            false
                        }
                        maxWage != null && profile.dailyWage != null && profile.dailyWage > maxWage -> {
                            Log.d(TAG, "Filtered out ${profile.name}: wage ${profile.dailyWage} > $maxWage")
                            false
                        }
                        else -> true
                    }
                }
                Log.d(TAG, "After wage filtering: ${wageFilteredProfiles.size} profiles")
                
                // Cache results in Room database
                wageFilteredProfiles.forEach { profile ->
                    try {
                        labourDao.insertLabourerProfile(profile)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to cache profile: ${profile.userId}", e)
                    }
                }
                Log.d(TAG, "Cached ${wageFilteredProfiles.size} profiles in Room database")
                
                Result.success(wageFilteredProfiles)
            } else {
                // Device offline, return cached results
                Log.d(TAG, "Device offline, returning cached results")
                
                // Apply location filtering to cached results if specified
                val locationFilteredResults = if (locationRadius != null && farmerLocation != null) {
                    localResults.filter { profile ->
                        val distance = calculateDistance(
                            farmerLocation.latitude,
                            farmerLocation.longitude,
                            profile.latitude,
                            profile.longitude
                        )
                        distance <= locationRadius
                    }
                } else {
                    localResults
                }
                
                Result.success(locationFilteredResults)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search labourers", e)
            Result.failure(e)
        }
    }
}

// ========== Extension Functions for Firestore Conversion ==========

/**
 * Convert LabourerProfile to Firestore map
 * Requirement 16: Offline Data Persistence
 */
fun LabourerProfile.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "userId" to userId,
        "name" to name,
        "phoneNumber" to phoneNumber,
        "age" to age,
        "gender" to gender,
        "village" to village,
        "district" to district,
        "latitude" to latitude,
        "longitude" to longitude,
        "skills" to skills.map { it.name },
        "experienceYears" to experienceYears.mapKeys { it.key.name },
        "pricingType" to pricingType.name,
        "dailyWage" to dailyWage,
        "hourlyRate" to hourlyRate,
        "profilePhotoUrls" to profilePhotoUrls,
        "availabilityStatus" to availabilityStatus.name,
        "futureAvailability" to futureAvailability?.map { mapOf(
            "startDate" to it.startDate,
            "endDate" to it.endDate,
            "startTime" to it.startTime,
            "endTime" to it.endTime
        ) },
        "averageRating" to averageRating,
        "totalRatings" to totalRatings,
        "completedBookings" to completedBookings,
        "createdAt" to Timestamp(Date(createdAt)),
        "updatedAt" to Timestamp(Date(updatedAt)),
        "lastAvailabilityUpdate" to Timestamp(Date(lastAvailabilityUpdate)),
        "preferredLanguage" to preferredLanguage
    )
}

/**
 * Convert Booking to Firestore map
 * Requirement 16: Offline Data Persistence
 */
fun Booking.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "bookingId" to bookingId,
        "farmerId" to farmerId,
        "farmerName" to farmerName,
        "farmerPhone" to farmerPhone,
        "farmerLocation" to farmerLocation,
        "labourerId" to labourerId,
        "labourerName" to labourerName,
        "labourerPhone" to labourerPhone,
        "workDate" to Timestamp(Date(workDate)),
        "startTime" to startTime,
        "estimatedHours" to estimatedHours,
        "workType" to workType.name,
        "specialInstructions" to specialInstructions,
        "isEmergency" to isEmergency,
        "estimatedPayment" to estimatedPayment,
        "actualHours" to actualHours,
        "actualPayment" to actualPayment,
        "status" to status.name,
        "paymentStatus" to paymentStatus.name,
        "paymentTransactionId" to paymentTransactionId,
        "createdAt" to Timestamp(Date(createdAt)),
        "updatedAt" to Timestamp(Date(updatedAt)),
        "acceptedAt" to acceptedAt?.let { Timestamp(Date(it)) },
        "completedAt" to completedAt?.let { Timestamp(Date(it)) },
        "cancelledAt" to cancelledAt?.let { Timestamp(Date(it)) },
        "cancellationReason" to cancellationReason
    )
}

/**
 * Convert Rating to Firestore map
 * Requirement 16: Offline Data Persistence
 */
fun Rating.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "ratingId" to ratingId,
        "bookingId" to bookingId,
        "labourerId" to labourerId,
        "farmerId" to farmerId,
        "farmerName" to farmerName,
        "rating" to rating,
        "review" to review,
        "createdAt" to Timestamp(Date(createdAt))
    )
}

/**
 * Convert WorkHistory to Firestore map
 * Requirement 16: Offline Data Persistence
 */
fun WorkHistory.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "workId" to workId,
        "labourerId" to labourerId,
        "bookingId" to bookingId,
        "farmerName" to farmerName,
        "workDate" to Timestamp(Date(workDate)),
        "workType" to workType.name,
        "actualHours" to actualHours,
        "paymentReceived" to paymentReceived,
        "ratingReceived" to ratingReceived,
        "completedAt" to Timestamp(Date(completedAt))
    )
}

/**
 * Convert Attendance to Firestore map
 * Requirement 16: Offline Data Persistence
 */
fun Attendance.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "attendanceId" to attendanceId,
        "bookingId" to bookingId,
        "labourerId" to labourerId,
        "checkInTime" to checkInTime?.let { Timestamp(Date(it)) },
        "checkOutTime" to checkOutTime?.let { Timestamp(Date(it)) },
        "actualHours" to actualHours,
        "notes" to notes,
        "createdAt" to Timestamp(Date(createdAt)),
        "updatedAt" to Timestamp(Date(updatedAt))
    )
}

/**
 * Convert NotificationPreferences to Firestore map
 * Requirement 16: Offline Data Persistence
 */
fun NotificationPreferences.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "userId" to userId,
        "smsEnabled" to smsEnabled,
        "whatsappEnabled" to whatsappEnabled,
        "inAppEnabled" to inAppEnabled,
        "newBookingsEnabled" to newBookingsEnabled,
        "bookingConfirmationsEnabled" to bookingConfirmationsEnabled,
        "paymentConfirmationsEnabled" to paymentConfirmationsEnabled,
        "ratingsReceivedEnabled" to ratingsReceivedEnabled,
        "updatedAt" to Timestamp(Date(updatedAt))
    )
}

/**
 * Convert Firestore DocumentSnapshot to LabourerProfile
 * Requirement 16: Offline Data Persistence
 * Requirement 18: Real-Time Availability Updates
 */
fun DocumentSnapshot.toLabourerProfile(): LabourerProfile {
    val data = this.data ?: throw IllegalArgumentException("Document data is null")
    
    // Parse skills from string list
    val skillsList = (data["skills"] as? List<*>)?.mapNotNull { skillName ->
        try {
            Skill.valueOf(skillName.toString())
        } catch (e: Exception) {
            null
        }
    } ?: emptyList()
    
    // Parse experience years map
    val experienceMap = (data["experienceYears"] as? Map<*, *>)?.mapNotNull { (key, value) ->
        try {
            val skill = Skill.valueOf(key.toString())
            val years = (value as? Number)?.toInt() ?: 0
            skill to years
        } catch (e: Exception) {
            null
        }
    }?.toMap() ?: emptyMap()
    
    // Parse pricing type
    val pricingType = try {
        PricingType.valueOf(data["pricingType"] as? String ?: "DAILY_WAGE")
    } catch (e: Exception) {
        PricingType.DAILY_WAGE
    }
    
    // Parse availability status
    val availabilityStatus = try {
        AvailabilityStatus.valueOf(data["availabilityStatus"] as? String ?: "UNAVAILABLE")
    } catch (e: Exception) {
        AvailabilityStatus.UNAVAILABLE
    }
    
    // Parse future availability windows
    val futureAvailability = (data["futureAvailability"] as? List<*>)?.mapNotNull { item ->
        try {
            val window = item as? Map<*, *>
            if (window != null) {
                AvailabilityWindow(
                    startDate = (window["startDate"] as? Number)?.toLong() ?: 0L,
                    endDate = (window["endDate"] as? Number)?.toLong() ?: 0L,
                    startTime = window["startTime"] as? String ?: "00:00",
                    endTime = window["endTime"] as? String ?: "23:59"
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    // Parse timestamps
    val createdAt = (data["createdAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
    val updatedAt = (data["updatedAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
    val lastAvailabilityUpdate = (data["lastAvailabilityUpdate"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
    
    return LabourerProfile(
        userId = this.id,
        name = data["name"] as? String ?: "",
        phoneNumber = data["phoneNumber"] as? String ?: "",
        age = (data["age"] as? Number)?.toInt() ?: 0,
        gender = data["gender"] as? String ?: "",
        village = data["village"] as? String ?: "",
        district = data["district"] as? String ?: "",
        latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
        longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
        skills = skillsList,
        experienceYears = experienceMap,
        pricingType = pricingType,
        dailyWage = (data["dailyWage"] as? Number)?.toInt(),
        hourlyRate = (data["hourlyRate"] as? Number)?.toInt(),
        profilePhotoUrls = (data["profilePhotoUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        availabilityStatus = availabilityStatus,
        futureAvailability = futureAvailability,
        averageRating = (data["averageRating"] as? Number)?.toDouble() ?: 0.0,
        totalRatings = (data["totalRatings"] as? Number)?.toInt() ?: 0,
        completedBookings = (data["completedBookings"] as? Number)?.toInt() ?: 0,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastAvailabilityUpdate = lastAvailabilityUpdate,
        preferredLanguage = data["preferredLanguage"] as? String ?: "en"
    )
}

/**
 * Convert Firestore DocumentSnapshot to Booking
 * Requirement 16: Offline Data Persistence
 * Requirement 18: Real-Time Availability Updates
 */
fun DocumentSnapshot.toBooking(): Booking {
    val data = this.data ?: throw IllegalArgumentException("Document data is null")
    
    // Parse work type (Skill enum)
    val workType = try {
        Skill.valueOf(data["workType"] as? String ?: "HARVESTING")
    } catch (e: Exception) {
        Skill.HARVESTING
    }
    
    // Parse booking status
    val status = try {
        BookingStatus.valueOf(data["status"] as? String ?: "PENDING")
    } catch (e: Exception) {
        BookingStatus.PENDING
    }
    
    // Parse payment status
    val paymentStatus = try {
        PaymentStatus.valueOf(data["paymentStatus"] as? String ?: "PENDING")
    } catch (e: Exception) {
        PaymentStatus.PENDING
    }
    
    // Parse timestamps
    val workDate = (data["workDate"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
    val createdAt = (data["createdAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
    val updatedAt = (data["updatedAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
    val acceptedAt = (data["acceptedAt"] as? Timestamp)?.toDate()?.time
    val completedAt = (data["completedAt"] as? Timestamp)?.toDate()?.time
    val cancelledAt = (data["cancelledAt"] as? Timestamp)?.toDate()?.time
    
    return Booking(
        bookingId = this.id,
        farmerId = data["farmerId"] as? String ?: "",
        farmerName = data["farmerName"] as? String ?: "",
        farmerPhone = data["farmerPhone"] as? String ?: "",
        farmerLocation = data["farmerLocation"] as? String ?: "",
        labourerId = data["labourerId"] as? String ?: "",
        labourerName = data["labourerName"] as? String ?: "",
        labourerPhone = data["labourerPhone"] as? String ?: "",
        workDate = workDate,
        startTime = data["startTime"] as? String ?: "00:00",
        estimatedHours = (data["estimatedHours"] as? Number)?.toInt() ?: 0,
        workType = workType,
        specialInstructions = data["specialInstructions"] as? String,
        isEmergency = (data["isEmergency"] as? Boolean) ?: false,
        estimatedPayment = (data["estimatedPayment"] as? Number)?.toInt() ?: 0,
        actualHours = (data["actualHours"] as? Number)?.toInt(),
        actualPayment = (data["actualPayment"] as? Number)?.toInt(),
        status = status,
        paymentStatus = paymentStatus,
        paymentTransactionId = data["paymentTransactionId"] as? String,
        createdAt = createdAt,
        updatedAt = updatedAt,
        acceptedAt = acceptedAt,
        completedAt = completedAt,
        cancelledAt = cancelledAt,
        cancellationReason = data["cancellationReason"] as? String
    )
}

/**
 * Data class for work history statistics
 * Requirement 14: Work History and Attendance Tracking
 */
data class WorkHistoryStats(
    val totalEarnings: Int,
    val totalDaysWorked: Int
)

// ========== Notification Message Templates ==========

/**
 * Format booking notification message for labourer
 * Requirement 10: SMS and WhatsApp Notifications
 * Requirement 4: Multi-Language Support
 * 
 * @param booking The booking to format
 * @param language Language code ("en" or "kn")
 * @return Formatted notification message
 */
fun formatBookingNotification(booking: Booking, language: String): String {
    return if (language == "kn") {
        """
        🌾 ಹೊಸ ಕೆಲಸದ ವಿನಂತಿ!
        
        ರೈತ: ${booking.farmerName}
        ದಿನಾಂಕ: ${formatDate(booking.workDate)}
        ಸಮಯ: ${booking.startTime}
        ಕೆಲಸ: ${booking.workType.displayNameKn}
        ಸ್ಥಳ: ${booking.farmerLocation}
        ಅಂದಾಜು ಸಮಯ: ${booking.estimatedHours} ಗಂಟೆಗಳು
        ಪಾವತಿ: ₹${booking.estimatedPayment}
        
        ${if (booking.isEmergency) "⚠️ ತುರ್ತು ಕೆಲಸ!" else ""}
        
        ಆ್ಯಪ್ ತೆರೆದು ಸ್ವೀಕರಿಸಿ ಅಥವಾ ನಿರಾಕರಿಸಿ.
        """.trimIndent()
    } else {
        """
        🌾 New Work Request!
        
        Farmer: ${booking.farmerName}
        Date: ${formatDate(booking.workDate)}
        Time: ${booking.startTime}
        Work: ${booking.workType.displayNameEn}
        Location: ${booking.farmerLocation}
        Estimated Hours: ${booking.estimatedHours} hours
        Payment: ₹${booking.estimatedPayment}
        
        ${if (booking.isEmergency) "⚠️ URGENT WORK!" else ""}
        
        Open app to accept or decline.
        """.trimIndent()
    }
}

/**
 * Format booking confirmation message for farmer
 * Requirement 10: SMS and WhatsApp Notifications
 * Requirement 4: Multi-Language Support
 * 
 * @param booking The confirmed booking
 * @param language Language code ("en" or "kn")
 * @return Formatted confirmation message
 */
fun formatConfirmationNotification(booking: Booking, language: String): String {
    return if (language == "kn") {
        """
        ✅ ಬುಕಿಂಗ್ ದೃಢೀಕರಣ
        
        ${booking.labourerName} ನಿಮ್ಮ ಕೆಲಸದ ವಿನಂತಿಯನ್ನು ಸ್ವೀಕರಿಸಿದ್ದಾರೆ!
        
        ದಿನಾಂಕ: ${formatDate(booking.workDate)}
        ಸಮಯ: ${booking.startTime}
        ಕೆಲಸ: ${booking.workType.displayNameKn}
        ಸಂಪರ್ಕ: ${booking.labourerPhone}
        
        ಧನ್ಯವಾದಗಳು!
        """.trimIndent()
    } else {
        """
        ✅ Booking Confirmed
        
        ${booking.labourerName} has accepted your work request!
        
        Date: ${formatDate(booking.workDate)}
        Time: ${booking.startTime}
        Work: ${booking.workType.displayNameEn}
        Contact: ${booking.labourerPhone}
        
        Thank you!
        """.trimIndent()
    }
}

/**
 * Format booking declined message for farmer
 * Requirement 10: SMS and WhatsApp Notifications
 * 
 * @param booking The declined booking
 * @param language Language code ("en" or "kn")
 * @return Formatted declined message
 */
fun formatBookingDeclinedNotification(booking: Booking, language: String): String {
    return if (language == "kn") {
        """
        ❌ ಬುಕಿಂಗ್ ನಿರಾಕರಣೆ
        
        ${booking.labourerName} ನಿಮ್ಮ ಕೆಲಸದ ವಿನಂತಿಯನ್ನು ನಿರಾಕರಿಸಿದ್ದಾರೆ.
        
        ದಿನಾಂಕ: ${formatDate(booking.workDate)}
        ಕೆಲಸ: ${booking.workType.displayNameKn}
        
        ದಯವಿಟ್ಟು ಬೇರೆ ಕಾರ್ಮಿಕರನ್ನು ಹುಡುಕಿ.
        """.trimIndent()
    } else {
        """
        ❌ Booking Declined
        
        ${booking.labourerName} has declined your work request.
        
        Date: ${formatDate(booking.workDate)}
        Work: ${booking.workType.displayNameEn}
        
        Please search for another labourer.
        """.trimIndent()
    }
}

/**
 * Format booking cancelled message for labourer
 * Requirement 10: SMS and WhatsApp Notifications
 * 
 * @param booking The cancelled booking
 * @param language Language code ("en" or "kn")
 * @return Formatted cancelled message
 */
fun formatBookingCancelledNotification(booking: Booking, language: String): String {
    return if (language == "kn") {
        """
        🚫 ಬುಕಿಂಗ್ ರದ್ದು
        
        ${booking.farmerName} ಅವರು ಬುಕಿಂಗ್ ರದ್ದುಗೊಳಿಸಿದ್ದಾರೆ.
        
        ದಿನಾಂಕ: ${formatDate(booking.workDate)}
        ಕೆಲಸ: ${booking.workType.displayNameKn}
        ${if (booking.cancellationReason != null) "ಕಾರಣ: ${booking.cancellationReason}" else ""}
        """.trimIndent()
    } else {
        """
        🚫 Booking Cancelled
        
        ${booking.farmerName} has cancelled the booking.
        
        Date: ${formatDate(booking.workDate)}
        Work: ${booking.workType.displayNameEn}
        ${if (booking.cancellationReason != null) "Reason: ${booking.cancellationReason}" else ""}
        """.trimIndent()
    }
}

/**
 * Format date for notification messages
 * 
 * @param timestamp Unix timestamp in milliseconds
 * @return Formatted date string
 */
private fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

