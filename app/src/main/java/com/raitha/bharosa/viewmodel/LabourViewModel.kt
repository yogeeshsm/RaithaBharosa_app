package com.raitha.bharosa.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.raitha.bharosa.data.*
import com.raitha.bharosa.data.SeedDataHelper
import com.raitha.bharosa.data.db.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.*

/**
 * ViewModel for the Agricultural Labour Profile & Booking System
 * 
 * Manages UI state and coordinates between the UI layer and repository layer.
 * Implements reactive state management using StateFlow for all UI screens.
 * 
 * Requirement 25: Integration with Existing App Features
 */
class LabourViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = LabourRepository(
        context = application,
        labourDao = AppDatabase.getInstance(application).labourDao(),
        bookingDao = AppDatabase.getInstance(application).bookingDao(),
        ratingDao = AppDatabase.getInstance(application).ratingDao(),
        workHistoryDao = AppDatabase.getInstance(application).workHistoryDao(),
        attendanceDao = AppDatabase.getInstance(application).attendanceDao(),
        notificationPreferencesDao = AppDatabase.getInstance(application).notificationPreferencesDao(),
        syncQueueDao = AppDatabase.getInstance(application).syncQueueDao(),
        firestore = FirebaseFirestore.getInstance(),
        storage = FirebaseStorage.getInstance(),
        notificationService = NotificationServiceImpl()
    )
    
    private val auth = FirebaseAuth.getInstance()
    
    // ========== State Flows ==========
    
    private val _labourerProfile = MutableStateFlow<LabourerProfile?>(null)
    val labourerProfile: StateFlow<LabourerProfile?> = _labourerProfile.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<LabourerProfile>>(emptyList())
    val searchResults: StateFlow<List<LabourerProfile>> = _searchResults.asStateFlow()
    
    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings.asStateFlow()
    
    private val _workHistory = MutableStateFlow<List<WorkHistory>>(emptyList())
    val workHistory: StateFlow<List<WorkHistory>> = _workHistory.asStateFlow()
    
    private val _workHistoryStats = MutableStateFlow<WorkHistoryStats?>(null)
    val workHistoryStats: StateFlow<WorkHistoryStats?> = _workHistoryStats.asStateFlow()
    
    private val _uiState = MutableStateFlow<LabourUiState>(LabourUiState.Idle)
    val uiState: StateFlow<LabourUiState> = _uiState.asStateFlow()
    
    private val _notificationPreferences = MutableStateFlow<NotificationPreferences?>(null)
    val notificationPreferences: StateFlow<NotificationPreferences?> = 
        _notificationPreferences.asStateFlow()
    
    init {
        // Start sync process
        viewModelScope.launch {
            repository.processSyncQueue()
        }
        
        // Load user data
        auth.currentUser?.let { user ->
            loadLabourerProfile(user.uid)
            loadBookings(user.uid)
            loadNotificationPreferences(user.uid)
        }
    }
    
    // ========== Labourer Profile Operations ==========
    
    /**
     * Create a new labourer profile
     * Requirement 2: Labourer Profile Creation
     */
    fun createLabourerProfile(
        name: String,
        phoneNumber: String,
        age: Int,
        gender: String,
        village: String,
        district: String,
        latitude: Double,
        longitude: Double,
        skills: List<Skill>,
        experienceYears: Map<Skill, Int>,
        pricingType: PricingType,
        dailyWage: Int?,
        hourlyRate: Int?,
        preferredLanguage: String
    ) {
        viewModelScope.launch {
            _uiState.value = LabourUiState.Loading
            
            val userId = auth.currentUser?.uid ?: run {
                _uiState.value = LabourUiState.Error("User not authenticated")
                return@launch
            }
            
            val profile = LabourerProfile(
                userId = userId,
                name = name,
                phoneNumber = phoneNumber,
                age = age,
                gender = gender,
                village = village,
                district = district,
                latitude = latitude,
                longitude = longitude,
                skills = skills,
                experienceYears = experienceYears,
                pricingType = pricingType,
                dailyWage = dailyWage,
                hourlyRate = hourlyRate,
                profilePhotoUrls = emptyList(),
                availabilityStatus = AvailabilityStatus.AVAILABLE,
                futureAvailability = null,
                averageRating = 0.0,
                totalRatings = 0,
                completedBookings = 0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                lastAvailabilityUpdate = System.currentTimeMillis(),
                preferredLanguage = preferredLanguage
            )
            
            repository.createLabourerProfile(profile).fold(
                onSuccess = {
                    _labourerProfile.value = profile
                    _uiState.value = LabourUiState.Success("Profile created successfully")
                },
                onFailure = { error ->
                    _uiState.value = LabourUiState.Error(error.message ?: "Failed to create profile")
                }
            )
        }
    }
    
    /**
     * Update existing labourer profile
     * Requirement 2: Labourer Profile Creation
     */
    fun updateLabourerProfile(profile: LabourerProfile) {
        viewModelScope.launch {
            _uiState.value = LabourUiState.Loading
            
            val updatedProfile = profile.copy(updatedAt = System.currentTimeMillis())
            
            repository.updateLabourerProfile(updatedProfile).fold(
                onSuccess = {
                    _labourerProfile.value = updatedProfile
                    _uiState.value = LabourUiState.Success("Profile updated successfully")
                },
                onFailure = { error ->
                    _uiState.value = LabourUiState.Error(error.message ?: "Failed to update profile")
                }
            )
        }
    }
    
    /**
     * Load labourer profile from repository
     */
    private fun loadLabourerProfile(userId: String) {
        viewModelScope.launch {
            repository.getLabourerProfile(userId).collect { profile ->
                _labourerProfile.value = profile
            }
        }
    }
    
    /**
     * Update labourer availability status
     * Requirement 5: Labourer Availability Management
     * Requirement 18: Real-Time Availability Updates
     */
    fun updateAvailabilityStatus(status: AvailabilityStatus) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            
            repository.updateAvailabilityStatus(userId, status).fold(
                onSuccess = {
                    _labourerProfile.value = _labourerProfile.value?.copy(
                        availabilityStatus = status,
                        lastAvailabilityUpdate = System.currentTimeMillis()
                    )
                },
                onFailure = { error ->
                    _uiState.value = LabourUiState.Error(error.message ?: "Failed to update availability")
                }
            )
        }
    }
    
    /**
     * Upload profile photo
     * Requirement 19: Profile Photo Management
     */
    fun uploadProfilePhoto(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = LabourUiState.Loading
            
            val userId = auth.currentUser?.uid ?: return@launch
            
            repository.uploadProfilePhoto(userId, imageUri).fold(
                onSuccess = { downloadUrl ->
                    val currentProfile = _labourerProfile.value ?: return@fold
                    val updatedUrls = currentProfile.profilePhotoUrls + downloadUrl
                    
                    if (updatedUrls.size <= 3) {
                        val updatedProfile = currentProfile.copy(
                            profilePhotoUrls = updatedUrls,
                            updatedAt = System.currentTimeMillis()
                        )
                        updateLabourerProfile(updatedProfile)
                    } else {
                        _uiState.value = LabourUiState.Error("Maximum 3 photos allowed")
                    }
                },
                onFailure = { error ->
                    _uiState.value = LabourUiState.Error(error.message ?: "Failed to upload photo")
                }
            )
        }
    }
    
    /**
     * Delete profile photo
     * Requirement 19: Profile Photo Management
     */
    fun deleteProfilePhoto(photoUrl: String) {
        viewModelScope.launch {
            repository.deleteProfilePhoto(photoUrl).fold(
                onSuccess = {
                    val currentProfile = _labourerProfile.value ?: return@fold
                    val updatedUrls = currentProfile.profilePhotoUrls.filter { it != photoUrl }
                    val updatedProfile = currentProfile.copy(
                        profilePhotoUrls = updatedUrls,
                        updatedAt = System.currentTimeMillis()
                    )
                    updateLabourerProfile(updatedProfile)
                },
                onFailure = { error ->
                    _uiState.value = LabourUiState.Error(error.message ?: "Failed to delete photo")
                }
            )
        }
    }
    
    // ========== Search Operations ==========
    
    /**
     * Search for available labourers with filters
     * Requirement 6: Search and Filter Functionality
     * Requirement 17: Location-Based Search
     * Requirement 21: Advanced Search Filters
     */
    fun searchLabourers(
        skills: List<Skill>?,
        locationRadius: Double?,
        farmerLocation: LatLng?,
        minWage: Int?,
        maxWage: Int?,
        minRating: Double?,
        sortBy: SortOption = SortOption.RATING
    ) {
        viewModelScope.launch {
            _uiState.value = LabourUiState.Loading
            
            repository.searchLabourers(
                skills = skills,
                locationRadius = locationRadius,
                farmerLocation = farmerLocation,
                minWage = minWage,
                maxWage = maxWage,
                minRating = minRating
            ).fold(
                onSuccess = { results ->
                    val sortedResults = when (sortBy) {
                        SortOption.RATING -> results.sortedByDescending { it.averageRating }
                        SortOption.PRICE_LOW -> results.sortedBy { it.dailyWage ?: Int.MAX_VALUE }
                        SortOption.DISTANCE -> {
                            if (farmerLocation != null) {
                                results.sortedBy { profile ->
                                    calculateDistance(
                                        farmerLocation.latitude,
                                        farmerLocation.longitude,
                                        profile.latitude,
                                        profile.longitude
                                    )
                                }
                            } else {
                                results
                            }
                        }
                    }
                    _searchResults.value = sortedResults
                    _uiState.value = LabourUiState.Idle
                },
                onFailure = { error ->
                    _uiState.value = LabourUiState.Error(error.message ?: "Search failed")
                }
            )
        }
    }
    
    /**
     * Load initial labourers for the search screen.
     * Auto-seeds Firestore with sample data if it is empty,
     * then runs a default search to populate results.
     * Requirement 6: Search and Filter Functionality
     */
    fun loadInitialLabourers() {
        viewModelScope.launch {
            _uiState.value = LabourUiState.Loading
            try {
                // Auto-seed if Firestore has no data
                val isSeeded = SeedDataHelper.isDataSeeded()
                if (!isSeeded) {
                    SeedDataHelper.seedLabourData()
                }
            } catch (e: Exception) {
                // Seed failure is non-fatal; proceed to search anyway
            }

            // Run default search with no filters
            repository.searchLabourers(
                skills = null,
                locationRadius = null,
                farmerLocation = null,
                minWage = null,
                maxWage = null,
                minRating = null
            ).fold(
                onSuccess = { results ->
                    _searchResults.value = results.sortedByDescending { it.averageRating }
                    _uiState.value = LabourUiState.Idle
                },
                onFailure = { error ->
                    _uiState.value = LabourUiState.Error(error.message ?: "Failed to load labourers")
                }
            )
        }
    }

    /**
     * Clear search results
     */
    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }
    
    // ========== Booking Operations ==========
    
    /**
     * Create a new booking
     * Requirement 8: Booking Creation
     * Requirement 9: Emergency Hiring
     */
    fun createBooking(
        labourerId: String,
        labourerName: String,
        labourerPhone: String,
        workDate: Long,
        startTime: String,
        estimatedHours: Int,
        workType: Skill,
        specialInstructions: String?,
        isEmergency: Boolean,
        estimatedPayment: Int,
        farmerName: String,
        farmerPhone: String,
        farmerLocation: String
    ) {
        viewModelScope.launch {
            _uiState.value = LabourUiState.Loading
            
            val farmerId = auth.currentUser?.uid ?: run {
                _uiState.value = LabourUiState.Error("User not authenticated")
                return@launch
            }
            
            val booking = Booking(
                bookingId = UUID.randomUUID().toString(),
                farmerId = farmerId,
                farmerName = farmerName,
                farmerPhone = farmerPhone,
                farmerLocation = farmerLocation,
                labourerId = labourerId,
                labourerName = labourerName,
                labourerPhone = labourerPhone,
                workDate = workDate,
                startTime = startTime,
                estimatedHours = estimatedHours,
                workType = workType,
                specialInstructions = specialInstructions,
                isEmergency = isEmergency,
                estimatedPayment = estimatedPayment,
                actualHours = null,
                actualPayment = null,
                status = BookingStatus.PENDING,
                paymentStatus = PaymentStatus.PENDING,
                paymentTransactionId = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                acceptedAt = null,
                completedAt = null,
                cancelledAt = null,
                cancellationReason = null
            )
            
            repository.createBooking(booking).fold(
                onSuccess = { bookingId ->
                    _uiState.value = LabourUiState.Success("Booking created successfully")
                    loadBookings(farmerId)
                },
                onFailure = { error ->
                    _uiState.value = LabourUiState.Error(error.message ?: "Failed to create booking")
                }
            )
        }
    }
    
    /**
     * Accept a booking
     * Requirement 11: Booking Management for Labourers
     */
    fun acceptBooking(bookingId: String) {
        viewModelScope.launch {
            _uiState.value = LabourUiState.Loading
            
            repository.acceptBooking(bookingId).fold(
                onSuccess = {
                    _uiState.value = LabourUiState.Success("Booking accepted")
                    auth.currentUser?.let { loadBookings(it.uid) }
                },
                onFailure = { error ->
                    _uiState.value = LabourUiState.Error(error.message ?: "Failed to accept booking")
                }
            )
        }
    }
    
    /**
     * Decline a booking
     * Requirement 11: Booking Management for Labourers
     */
    fun declineBooking(bookingId: String) {
        viewModelScope.launch {
            _uiState.value = LabourUiState.Loading
            
            repository.declineBooking(bookingId).fold(
                onSuccess = {
                    _uiState.value = LabourUiState.Success("Booking declined")
                    auth.currentUser?.let { loadBookings(it.uid) }
                },
                onFailure = { error ->
                    _uiState.value = LabourUiState.Error(error.message ?: "Failed to decline booking")
                }
            )
        }
    }
    
    /**
     * Complete a booking
     * Requirement 12: Booking Management for Farmers
     */
    fun completeBooking(bookingId: String, actualHours: Int, actualPayment: Int) {
        viewModelScope.launch {
            _uiState.value = LabourUiState.Loading
            
            repository.completeBooking(bookingId, actualHours, actualPayment).fold(
                onSuccess = {
                    _uiState.value = LabourUiState.Success("Booking completed")
                    auth.currentUser?.let { loadBookings(it.uid) }
                },
                onFailure = { error ->
                    _uiState.value = LabourUiState.Error(error.message ?: "Failed to complete booking")
                }
            )
        }
    }
    
    /**
     * Cancel a booking
     * Requirement 12: Booking Management for Farmers
     */
    fun cancelBooking(bookingId: String, reason: String) {
        viewModelScope.launch {
            _uiState.value = LabourUiState.Loading
            
            repository.cancelBooking(bookingId, reason).fold(
                onSuccess = {
                    _uiState.value = LabourUiState.Success("Booking cancelled")
                    auth.currentUser?.let { loadBookings(it.uid) }
                },
                onFailure = { error ->
                    _uiState.value = LabourUiState.Error(error.message ?: "Failed to cancel booking")
                }
            )
        }
    }
    
    /**
     * Get farmer bookings
     * Requirement 12: Booking Management for Farmers
     */
    fun getFarmerBookings() {
        auth.currentUser?.let { user ->
            viewModelScope.launch {
                repository.getFarmerBookings(user.uid).collect { bookings ->
                    _bookings.value = bookings
                }
            }
        }
    }
    
    /**
     * Get labourer bookings
     * Requirement 11: Booking Management for Labourers
     */
    fun getLabourerBookings() {
        auth.currentUser?.let { user ->
            viewModelScope.launch {
                repository.getLabourerBookings(user.uid).collect { bookings ->
                    _bookings.value = bookings
                }
            }
        }
    }
    
    /**
     * Load bookings for current user
     */
    private fun loadBookings(userId: String) {
        viewModelScope.launch {
            // Determine if user is farmer or labourer
            val profile = _labourerProfile.value
            if (profile != null) {
                // User is a labourer
                repository.getLabourerBookings(userId).collect { bookings ->
                    _bookings.value = bookings
                }
            } else {
                // User is a farmer
                repository.getFarmerBookings(userId).collect { bookings ->
                    _bookings.value = bookings
                }
            }
        }
    }
    
    // ========== Rating Operations ==========
    
    /**
     * Submit a rating for a labourer
     * Requirement 13: Rating and Review System
     */
    fun submitRating(
        bookingId: String,
        labourerId: String,
        rating: Int,
        review: String?
    ) {
        viewModelScope.launch {
            _uiState.value = LabourUiState.Loading
            
            val farmerId = auth.currentUser?.uid ?: return@launch
            val farmerName = "Farmer" // Get from profile
            
            val ratingEntity = Rating(
                ratingId = UUID.randomUUID().toString(),
                bookingId = bookingId,
                labourerId = labourerId,
                farmerId = farmerId,
                farmerName = farmerName,
                rating = rating,
                review = review,
                createdAt = System.currentTimeMillis()
            )
            
            repository.submitRating(ratingEntity).fold(
                onSuccess = {
                    _uiState.value = LabourUiState.Success("Rating submitted")
                },
                onFailure = { error ->
                    _uiState.value = LabourUiState.Error(error.message ?: "Failed to submit rating")
                }
            )
        }
    }
    
    /**
     * Get labourer ratings
     * Requirement 13: Rating and Review System
     */
    fun getLabourerRatings(labourerId: String) {
        viewModelScope.launch {
            repository.getLabourerRatings(labourerId).collect { ratings ->
                // Store in a separate state if needed
            }
        }
    }
    
    // ========== Work History Operations ==========
    
    /**
     * Load work history for a labourer
     * Requirement 14: Work History and Attendance Tracking
     */
    fun loadWorkHistory(labourerId: String) {
        viewModelScope.launch {
            repository.getWorkHistory(labourerId).collect { history ->
                _workHistory.value = history
            }
        }
    }
    
    /**
     * Load work history statistics
     * Requirement 14: Work History and Attendance Tracking
     * Requirement 24: Analytics Dashboard
     */
    fun loadWorkHistoryStats(labourerId: String, period: Period) {
        viewModelScope.launch {
            val (startDate, endDate) = period.getDateRange()
            
            val stats = repository.getWorkHistoryStats(labourerId, startDate, endDate)
            _workHistoryStats.value = stats
        }
    }
    
    /**
     * Export work history to PDF
     * Requirement 14: Work History and Attendance Tracking
     */
    fun exportWorkHistoryPdf(labourerId: String) {
        viewModelScope.launch {
            // Implementation will use PdfUtils from utils package
            _uiState.value = LabourUiState.Loading
            // TODO: Implement PDF export
            _uiState.value = LabourUiState.Success("PDF export not yet implemented")
        }
    }
    
    // ========== Notification Preferences ==========
    
    /**
     * Load notification preferences
     * Requirement 22: Notification Preferences
     */
    private fun loadNotificationPreferences(userId: String) {
        viewModelScope.launch {
            AppDatabase.getInstance(getApplication()).notificationPreferencesDao()
                .getNotificationPreferences(userId).collect { prefs ->
                    _notificationPreferences.value = prefs
                }
        }
    }
    
    /**
     * Update notification preferences
     * Requirement 22: Notification Preferences
     */
    fun updateNotificationPreferences(preferences: NotificationPreferences) {
        viewModelScope.launch {
            try {
                AppDatabase.getInstance(getApplication()).notificationPreferencesDao()
                    .updateNotificationPreferences(preferences)
                _notificationPreferences.value = preferences
            } catch (e: Exception) {
                _uiState.value = LabourUiState.Error(e.message ?: "Failed to update preferences")
            }
        }
    }
    
    /**
     * Get notification preferences
     * Requirement 22: Notification Preferences
     */
    fun getNotificationPreferences() {
        auth.currentUser?.let { user ->
            loadNotificationPreferences(user.uid)
        }
    }
    
    // ========== Helper Functions ==========
    
    /**
     * Calculate distance between two GPS coordinates using Haversine formula
     * Requirement 17: Location-Based Search
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
     * Clear UI state
     */
    fun clearUiState() {
        _uiState.value = LabourUiState.Idle
    }
}

// ========== UI State Classes ==========

/**
 * UI state for Labour screens
 */
sealed class LabourUiState {
    object Idle : LabourUiState()
    object Loading : LabourUiState()
    data class Success(val message: String) : LabourUiState()
    data class Error(val message: String) : LabourUiState()
}

/**
 * Sort options for search results
 * Requirement 21: Advanced Search Filters
 */
enum class SortOption {
    RATING,
    PRICE_LOW,
    DISTANCE
}

/**
 * Time period for work history statistics
 * Requirement 14: Work History and Attendance Tracking
 * Requirement 24: Analytics Dashboard
 */
enum class Period {
    THIS_MONTH,
    THIS_YEAR,
    ALL_TIME;
    
    fun getDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        
        return when (this) {
            THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                Pair(calendar.timeInMillis, endDate)
            }
            THIS_YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                Pair(calendar.timeInMillis, endDate)
            }
            ALL_TIME -> Pair(0L, endDate)
        }
    }
}
