package com.raitha.bharosa.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User role classification for the Agricultural Labour Booking System
 * Requirement 1: User Role Management
 */
enum class UserRole {
    FARMER,
    LABOURER
}

/**
 * Agricultural skills with multi-language support (English and Kannada)
 * Requirement 2: Labourer Profile Creation
 * Requirement 4: Multi-Language Support
 */
enum class Skill(val displayNameEn: String, val displayNameKn: String) {
    HARVESTING("Harvesting", "ಕೊಯ್ಲು"),
    PLANTING("Planting", "ನೆಡುವಿಕೆ"),
    IRRIGATION("Irrigation", "ನೀರಾವರಿ"),
    PESTICIDE_SPRAYING("Pesticide Spraying", "ಕೀಟನಾಶಕ ಸಿಂಪಡಿಸುವಿಕೆ"),
    WEEDING("Weeding", "ಕಳೆ ತೆಗೆಯುವಿಕೆ"),
    FERTILIZER_APPLICATION("Fertilizer Application", "ಗೊಬ್ಬರ ಹಾಕುವಿಕೆ"),
    MACHINERY_OPERATION("Machinery Operation", "ಯಂತ್ರ ನಿರ್ವಹಣೆ"),
    LIVESTOCK_CARE("Livestock Care", "ಜಾನುವಾರು ಆರೈಕೆ");
    
    /**
     * Get display name based on language preference
     * @param lang Language code ("en" for English, "kn" for Kannada)
     * @return Localized display name
     */
    fun getDisplayName(lang: String): String {
        return if (lang == "kn") displayNameKn else displayNameEn
    }
}

/**
 * Real-time availability status for labourers
 * Requirement 5: Labourer Availability Management
 * Requirement 18: Real-Time Availability Updates
 */
enum class AvailabilityStatus {
    AVAILABLE,
    UNAVAILABLE,
    BOOKED
}

/**
 * Booking lifecycle status
 * Requirement 8: Booking Creation
 * Requirement 11: Booking Management for Labourers
 * Requirement 12: Booking Management for Farmers
 */
enum class BookingStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    COMPLETED,
    CANCELLED
}

/**
 * Payment transaction status
 * Requirement 15: Payment Integration
 */
enum class PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED
}

/**
 * Pricing model for labourer services
 * Requirement 2: Labourer Profile Creation
 */
enum class PricingType {
    DAILY_WAGE,
    HOURLY_RATE
}

/**
 * GPS coordinates for location-based operations
 * Requirement 17: Location-Based Search
 */
data class LatLng(
    val latitude: Double,
    val longitude: Double
)

/**
 * Availability window for future booking dates
 * Requirement 5: Labourer Availability Management
 */
data class AvailabilityWindow(
    val startDate: Long,
    val endDate: Long,
    val startTime: String, // HH:mm format
    val endTime: String
)

/**
 * Labourer profile entity with comprehensive work details
 * Requirement 2: Labourer Profile Creation
 * Requirement 5: Labourer Availability Management
 * Requirement 13: Rating and Review System
 * Requirement 16: Offline Data Persistence
 */
@Entity(tableName = "labourer_profiles")
data class LabourerProfile(
    @PrimaryKey val userId: String,
    val name: String,
    val phoneNumber: String,
    val age: Int,
    val gender: String,
    val village: String,
    val district: String,
    val latitude: Double,
    val longitude: Double,
    val skills: List<Skill>,
    val experienceYears: Map<Skill, Int>, // Skill to years of experience
    val pricingType: PricingType,
    val dailyWage: Int?, // ₹300-₹2000
    val hourlyRate: Int?, // ₹40-₹300
    val profilePhotoUrls: List<String>, // Max 3 photos
    val availabilityStatus: AvailabilityStatus,
    val futureAvailability: List<AvailabilityWindow>?,
    val averageRating: Double,
    val totalRatings: Int,
    val completedBookings: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val lastAvailabilityUpdate: Long,
    val preferredLanguage: String // "en" or "kn"
)

/**
 * Extended farmer profile entity for labour booking system
 * Requirement 8: Booking Creation
 * Requirement 16: Offline Data Persistence
 * Requirement 17: Location-Based Search
 */
@Entity(tableName = "farmer_profiles_labour")
data class FarmerProfileLabour(
    @PrimaryKey val userId: String,
    val name: String,
    val phoneNumber: String,
    val village: String,
    val district: String,
    val latitude: Double,
    val longitude: Double,
    val landArea: Double,
    val primaryCrop: CropType,
    val createdAt: Long,
    val updatedAt: Long,
    val preferredLanguage: String
)

/**
 * Booking entity for work arrangements between farmers and labourers
 * Requirement 8: Booking Creation
 * Requirement 9: Emergency Hiring
 * Requirement 11: Booking Management for Labourers
 * Requirement 12: Booking Management for Farmers
 * Requirement 15: Payment Integration
 * Requirement 16: Offline Data Persistence
 */
@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey val bookingId: String,
    val farmerId: String,
    val farmerName: String,
    val farmerPhone: String,
    val farmerLocation: String,
    val labourerId: String,
    val labourerName: String,
    val labourerPhone: String,
    val workDate: Long,
    val startTime: String, // HH:mm format
    val estimatedHours: Int,
    val workType: Skill,
    val specialInstructions: String?,
    val isEmergency: Boolean,
    val estimatedPayment: Int,
    val actualHours: Int?,
    val actualPayment: Int?,
    val status: BookingStatus,
    val paymentStatus: PaymentStatus,
    val paymentTransactionId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val acceptedAt: Long?,
    val completedAt: Long?,
    val cancelledAt: Long?,
    val cancellationReason: String?
)

/**
 * Rating entity for labourer feedback
 * Requirement 13: Rating and Review System
 * Requirement 16: Offline Data Persistence
 */
@Entity(tableName = "ratings")
data class Rating(
    @PrimaryKey val ratingId: String,
    val bookingId: String,
    val labourerId: String,
    val farmerId: String,
    val farmerName: String,
    val rating: Int, // 1-5
    val review: String?, // Max 500 characters
    val createdAt: Long
)

/**
 * Work history entity for tracking completed jobs
 * Requirement 14: Work History and Attendance Tracking
 * Requirement 16: Offline Data Persistence
 */
@Entity(tableName = "work_history")
data class WorkHistory(
    @PrimaryKey val workId: String,
    val labourerId: String,
    val bookingId: String,
    val farmerName: String,
    val workDate: Long,
    val workType: Skill,
    val actualHours: Int,
    val paymentReceived: Int,
    val ratingReceived: Int?,
    val completedAt: Long
)

/**
 * Attendance entity for tracking work hours
 * Requirement 14: Work History and Attendance Tracking
 * Requirement 16: Offline Data Persistence
 */
@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey val attendanceId: String,
    val bookingId: String,
    val labourerId: String,
    val checkInTime: Long?,
    val checkOutTime: Long?,
    val actualHours: Int?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Notification preferences entity for user notification settings
 * Requirement 10: SMS and WhatsApp Notifications
 * Requirement 22: Notification Preferences
 * Requirement 16: Offline Data Persistence
 */
@Entity(tableName = "notification_preferences")
data class NotificationPreferences(
    @PrimaryKey val userId: String,
    val smsEnabled: Boolean,
    val whatsappEnabled: Boolean,
    val inAppEnabled: Boolean,
    val newBookingsEnabled: Boolean,
    val bookingConfirmationsEnabled: Boolean,
    val paymentConfirmationsEnabled: Boolean,
    val ratingsReceivedEnabled: Boolean,
    val updatedAt: Long
)

/**
 * Sync queue entity for offline-first data synchronization
 * Requirement 16: Offline Data Persistence
 */
@Entity(tableName = "sync_queue")
data class SyncQueueItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entityType: String, // "booking", "profile", "rating", etc.
    val entityId: String,
    val operation: String, // "CREATE", "UPDATE", "DELETE"
    val data: String, // JSON serialized entity
    val retryCount: Int,
    val createdAt: Long,
    val lastAttempt: Long?
)
