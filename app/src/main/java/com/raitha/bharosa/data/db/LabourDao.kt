package com.raitha.bharosa.data.db

import androidx.room.*
import com.raitha.bharosa.data.LabourerProfile
import com.raitha.bharosa.data.AvailabilityStatus
import com.raitha.bharosa.data.Skill
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Labourer Profile operations
 * 
 * Provides database access methods for:
 * - Labourer profile CRUD operations (Requirement 2: Labourer Profile Management)
 * - Search queries with filters (Requirement 6: Search and Discovery)
 * - Availability status updates (Requirement 5: Availability Management)
 * - Rating updates (Requirement 13: Rating and Reviews)
 * 
 * Part of Phase 2: Data Layer (Room + Firestore)
 */
@Dao
interface LabourDao {
    
    // ========== Labourer Profile CRUD Operations ==========
    
    /**
     * Insert or replace a labourer profile
     * Requirement 2: Labourer Profile Creation
     * Requirement 16: Offline Data Persistence
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabourerProfile(profile: LabourerProfile)
    
    /**
     * Update an existing labourer profile
     * Requirement 2: Labourer Profile Management
     */
    @Update
    suspend fun updateLabourerProfile(profile: LabourerProfile)
    
    /**
     * Get a labourer profile by user ID with reactive updates
     * Returns Flow for real-time updates in UI
     * Requirement 2: Labourer Profile Viewing
     * Requirement 7: Labourer Profile Viewing (for farmers)
     */
    @Query("SELECT * FROM labourer_profiles WHERE userId = :userId")
    fun getLabourerProfile(userId: String): Flow<LabourerProfile?>
    
    /**
     * Get a labourer profile by user ID (one-time fetch)
     * Used for non-reactive operations
     */
    @Query("SELECT * FROM labourer_profiles WHERE userId = :userId")
    suspend fun getLabourerProfileOnce(userId: String): LabourerProfile?
    
    /**
     * Delete a labourer profile
     * Requirement 23: Security and Privacy (data deletion)
     */
    @Delete
    suspend fun deleteLabourerProfile(profile: LabourerProfile)
    
    // ========== Search Queries with Filters ==========
    
    /**
     * Get all available labourers with reactive updates
     * Requirement 6: Farmer Search and Filter
     * Requirement 18: Real-Time Availability Updates
     */
    @Query("SELECT * FROM labourer_profiles WHERE availabilityStatus = 'AVAILABLE'")
    fun getAvailableLabourers(): Flow<List<LabourerProfile>>
    
    /**
     * Search labourers with multiple filter criteria
     * Supports filtering by:
     * - Skill (optional)
     * - Wage range (optional min/max)
     * - Minimum rating (optional)
     * 
     * Results are sorted by rating (highest first) and limited to 50
     * 
     * Requirement 6: Farmer Search and Filter
     * Requirement 21: Search Performance Optimization
     */
    @Query("""
        SELECT * FROM labourer_profiles 
        WHERE availabilityStatus = 'AVAILABLE'
        AND (:skill IS NULL OR skills LIKE '%' || :skill || '%')
        AND (:minWage IS NULL OR dailyWage >= :minWage)
        AND (:maxWage IS NULL OR dailyWage <= :maxWage)
        AND (:minRating IS NULL OR averageRating >= :minRating)
        ORDER BY averageRating DESC
        LIMIT 50
    """)
    suspend fun searchLabourers(
        skill: String?,
        minWage: Int?,
        maxWage: Int?,
        minRating: Double?
    ): List<LabourerProfile>
    
    /**
     * Search labourers by specific skill
     * Requirement 6: Farmer Search and Filter
     */
    @Query("""
        SELECT * FROM labourer_profiles 
        WHERE availabilityStatus = 'AVAILABLE'
        AND skills LIKE '%' || :skill || '%'
        ORDER BY averageRating DESC
        LIMIT 50
    """)
    suspend fun searchLabourersBySkill(skill: String): List<LabourerProfile>
    
    /**
     * Search labourers by district (location-based)
     * Requirement 17: Location-Based Search
     */
    @Query("""
        SELECT * FROM labourer_profiles 
        WHERE availabilityStatus = 'AVAILABLE'
        AND district = :district
        ORDER BY averageRating DESC
        LIMIT 50
    """)
    suspend fun searchLabourersByDistrict(district: String): List<LabourerProfile>
    
    /**
     * Search labourers by village (more specific location)
     * Requirement 17: Location-Based Search
     */
    @Query("""
        SELECT * FROM labourer_profiles 
        WHERE availabilityStatus = 'AVAILABLE'
        AND village = :village
        ORDER BY averageRating DESC
        LIMIT 50
    """)
    suspend fun searchLabourersByVillage(village: String): List<LabourerProfile>
    
    /**
     * Get all labourer profiles (for admin/debugging purposes)
     */
    @Query("SELECT * FROM labourer_profiles ORDER BY createdAt DESC")
    fun getAllLabourerProfiles(): Flow<List<LabourerProfile>>
    
    // ========== Availability Status Updates ==========
    
    /**
     * Update availability status for a labourer
     * Requirement 5: Labourer Availability Management
     * Requirement 18: Real-Time Availability Updates
     */
    @Query("""
        UPDATE labourer_profiles 
        SET availabilityStatus = :status, 
            lastAvailabilityUpdate = :timestamp,
            updatedAt = :timestamp
        WHERE userId = :userId
    """)
    suspend fun updateAvailabilityStatus(
        userId: String, 
        status: AvailabilityStatus, 
        timestamp: Long
    )
    
    /**
     * Set labourer as unavailable (when booking is accepted)
     * Requirement 5: Labourer Availability Management
     * Requirement 18: Real-Time Availability Updates
     */
    @Query("""
        UPDATE labourer_profiles 
        SET availabilityStatus = 'UNAVAILABLE',
            lastAvailabilityUpdate = :timestamp,
            updatedAt = :timestamp
        WHERE userId = :userId
    """)
    suspend fun markAsUnavailable(userId: String, timestamp: Long)
    
    /**
     * Set labourer as available
     * Requirement 5: Labourer Availability Management
     */
    @Query("""
        UPDATE labourer_profiles 
        SET availabilityStatus = 'AVAILABLE',
            lastAvailabilityUpdate = :timestamp,
            updatedAt = :timestamp
        WHERE userId = :userId
    """)
    suspend fun markAsAvailable(userId: String, timestamp: Long)
    
    /**
     * Set labourer as booked (when booking is confirmed)
     * Requirement 5: Labourer Availability Management
     */
    @Query("""
        UPDATE labourer_profiles 
        SET availabilityStatus = 'BOOKED',
            lastAvailabilityUpdate = :timestamp,
            updatedAt = :timestamp
        WHERE userId = :userId
    """)
    suspend fun markAsBooked(userId: String, timestamp: Long)
    
    // ========== Rating Updates ==========
    
    /**
     * Update labourer's average rating and total rating count
     * Requirement 13: Rating and Review System
     */
    @Query("""
        UPDATE labourer_profiles 
        SET averageRating = :rating, 
            totalRatings = :count,
            updatedAt = :timestamp
        WHERE userId = :userId
    """)
    suspend fun updateRating(userId: String, rating: Double, count: Int, timestamp: Long)
    
    /**
     * Increment completed bookings count
     * Requirement 13: Rating and Review System
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        UPDATE labourer_profiles 
        SET completedBookings = completedBookings + 1,
            updatedAt = :timestamp
        WHERE userId = :userId
    """)
    suspend fun incrementCompletedBookings(userId: String, timestamp: Long)
    
    /**
     * Get labourer's current rating statistics
     * Requirement 13: Rating and Review System
     */
    @Query("SELECT averageRating, totalRatings FROM labourer_profiles WHERE userId = :userId")
    suspend fun getRatingStats(userId: String): RatingStats?
    
    // ========== Profile Statistics ==========
    
    /**
     * Get count of available labourers
     * Requirement 24: Analytics and Reporting
     */
    @Query("SELECT COUNT(*) FROM labourer_profiles WHERE availabilityStatus = 'AVAILABLE'")
    suspend fun getAvailableLabourersCount(): Int
    
    /**
     * Get count of labourers by skill
     * Requirement 24: Analytics and Reporting
     */
    @Query("SELECT COUNT(*) FROM labourer_profiles WHERE skills LIKE '%' || :skill || '%'")
    suspend fun getLabourersCountBySkill(skill: String): Int
    
    /**
     * Get top-rated labourers
     * Requirement 6: Farmer Search and Filter
     * Requirement 13: Rating and Review System
     */
    @Query("""
        SELECT * FROM labourer_profiles 
        WHERE availabilityStatus = 'AVAILABLE'
        AND totalRatings >= :minRatingsCount
        ORDER BY averageRating DESC, totalRatings DESC
        LIMIT :limit
    """)
    suspend fun getTopRatedLabourers(minRatingsCount: Int = 5, limit: Int = 10): List<LabourerProfile>
    
    /**
     * Get most experienced labourers for a specific skill
     * Requirement 6: Farmer Search and Filter
     */
    @Query("""
        SELECT * FROM labourer_profiles 
        WHERE availabilityStatus = 'AVAILABLE'
        AND skills LIKE '%' || :skill || '%'
        ORDER BY completedBookings DESC
        LIMIT :limit
    """)
    suspend fun getMostExperiencedLabourers(skill: String, limit: Int = 10): List<LabourerProfile>
}

/**
 * Data class for rating statistics
 * Used by getRatingStats query
 */
data class RatingStats(
    val averageRating: Double,
    val totalRatings: Int
)
