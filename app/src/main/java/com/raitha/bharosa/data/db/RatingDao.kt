package com.raitha.bharosa.data.db

import androidx.room.*
import com.raitha.bharosa.data.Rating
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Rating operations
 * 
 * Provides database access methods for:
 * - Rating insert operations (Requirement 13: Rating and Review System)
 * - Queries for labourer ratings (Requirement 13: Rating and Review System)
 * - Average rating calculation (Requirement 13: Rating and Review System)
 * 
 * Part of Phase 2: Data Layer (Room + Firestore)
 * Task 6.3: Create RatingDao
 */
@Dao
interface RatingDao {
    
    // ========== Rating CRUD Operations ==========
    
    /**
     * Insert or replace a rating
     * Requirement 13: Rating and Review System
     * Requirement 16: Offline Data Persistence
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRating(rating: Rating)
    
    /**
     * Update an existing rating
     * Requirement 13: Rating and Review System
     */
    @Update
    suspend fun updateRating(rating: Rating)
    
    /**
     * Delete a rating
     * Requirement 23: Security and Privacy (data deletion)
     */
    @Delete
    suspend fun deleteRating(rating: Rating)
    
    /**
     * Get a rating by ID (one-time fetch)
     */
    @Query("SELECT * FROM ratings WHERE ratingId = :ratingId")
    suspend fun getRatingById(ratingId: String): Rating?
    
    // ========== Queries for Labourer Ratings ==========
    
    /**
     * Get all ratings for a labourer with reactive updates
     * Sorted by creation date (most recent first)
     * Requirement 13: Rating and Review System
     * Requirement 7: Labourer Profile Viewing
     */
    @Query("SELECT * FROM ratings WHERE labourerId = :labourerId ORDER BY createdAt DESC")
    fun getLabourerRatings(labourerId: String): Flow<List<Rating>>
    
    /**
     * Get all ratings for a labourer (one-time fetch)
     * Requirement 13: Rating and Review System
     */
    @Query("SELECT * FROM ratings WHERE labourerId = :labourerId ORDER BY createdAt DESC")
    suspend fun getLabourerRatingsOnce(labourerId: String): List<Rating>
    
    /**
     * Get recent ratings for a labourer (limited to 5)
     * Used for profile preview display
     * Requirement 7: Labourer Profile Viewing
     * Requirement 13: Rating and Review System
     */
    @Query("SELECT * FROM ratings WHERE labourerId = :labourerId ORDER BY createdAt DESC LIMIT 5")
    suspend fun getRecentRatings(labourerId: String): List<Rating>
    
    /**
     * Get recent ratings for a labourer with custom limit
     * Requirement 7: Labourer Profile Viewing
     */
    @Query("SELECT * FROM ratings WHERE labourerId = :labourerId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentRatingsWithLimit(labourerId: String, limit: Int): List<Rating>
    
    /**
     * Get rating for a specific booking
     * Used to check if a booking has already been rated
     * Requirement 13: Rating and Review System
     */
    @Query("SELECT * FROM ratings WHERE bookingId = :bookingId")
    suspend fun getRatingForBooking(bookingId: String): Rating?
    
    /**
     * Get all ratings by a specific farmer
     * Requirement 13: Rating and Review System
     */
    @Query("SELECT * FROM ratings WHERE farmerId = :farmerId ORDER BY createdAt DESC")
    fun getFarmerRatings(farmerId: String): Flow<List<Rating>>
    
    /**
     * Get all ratings by a specific farmer (one-time fetch)
     */
    @Query("SELECT * FROM ratings WHERE farmerId = :farmerId ORDER BY createdAt DESC")
    suspend fun getFarmerRatingsOnce(farmerId: String): List<Rating>
    
    // ========== Average Rating Calculation ==========
    
    /**
     * Calculate average rating for a labourer
     * Returns null if no ratings exist
     * Requirement 13: Rating and Review System
     */
    @Query("SELECT AVG(rating) FROM ratings WHERE labourerId = :labourerId")
    suspend fun getAverageRating(labourerId: String): Double?
    
    /**
     * Get total count of ratings for a labourer
     * Requirement 13: Rating and Review System
     */
    @Query("SELECT COUNT(*) FROM ratings WHERE labourerId = :labourerId")
    suspend fun getTotalRatings(labourerId: String): Int
    
    /**
     * Get rating statistics for a labourer
     * Returns both average rating and total count
     * Requirement 13: Rating and Review System
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT 
            COALESCE(AVG(rating), 0.0) as averageRating,
            COUNT(*) as totalRatings
        FROM ratings 
        WHERE labourerId = :labourerId
    """)
    suspend fun getRatingStatistics(labourerId: String): RatingStatistics?
    
    /**
     * Get count of ratings by star value for a labourer
     * Used for rating distribution display (e.g., 5 stars: 10, 4 stars: 5, etc.)
     * Requirement 13: Rating and Review System
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT rating, COUNT(*) as count 
        FROM ratings 
        WHERE labourerId = :labourerId 
        GROUP BY rating 
        ORDER BY rating DESC
    """)
    suspend fun getRatingDistribution(labourerId: String): List<RatingDistribution>
    
    // ========== Rating Filters and Queries ==========
    
    /**
     * Get ratings with minimum star value for a labourer
     * Requirement 13: Rating and Review System
     */
    @Query("""
        SELECT * FROM ratings 
        WHERE labourerId = :labourerId 
        AND rating >= :minRating
        ORDER BY createdAt DESC
    """)
    suspend fun getRatingsByMinimumValue(labourerId: String, minRating: Int): List<Rating>
    
    /**
     * Get ratings with reviews (non-null review text) for a labourer
     * Requirement 13: Rating and Review System
     */
    @Query("""
        SELECT * FROM ratings 
        WHERE labourerId = :labourerId 
        AND review IS NOT NULL 
        AND review != ''
        ORDER BY createdAt DESC
    """)
    suspend fun getRatingsWithReviews(labourerId: String): List<Rating>
    
    /**
     * Get ratings with reviews (reactive)
     * Requirement 13: Rating and Review System
     */
    @Query("""
        SELECT * FROM ratings 
        WHERE labourerId = :labourerId 
        AND review IS NOT NULL 
        AND review != ''
        ORDER BY createdAt DESC
    """)
    fun getRatingsWithReviewsFlow(labourerId: String): Flow<List<Rating>>
    
    /**
     * Get ratings by date range for a labourer
     * Requirement 14: Work History and Attendance Tracking
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT * FROM ratings 
        WHERE labourerId = :labourerId 
        AND createdAt BETWEEN :startDate AND :endDate
        ORDER BY createdAt DESC
    """)
    suspend fun getRatingsByDateRange(
        labourerId: String, 
        startDate: Long, 
        endDate: Long
    ): List<Rating>
    
    // ========== Analytics and Statistics ==========
    
    /**
     * Get count of 5-star ratings for a labourer
     * Requirement 24: Analytics and Reporting
     */
    @Query("SELECT COUNT(*) FROM ratings WHERE labourerId = :labourerId AND rating = 5")
    suspend fun getFiveStarRatingsCount(labourerId: String): Int
    
    /**
     * Get count of ratings below a threshold for a labourer
     * Useful for identifying performance issues
     * Requirement 24: Analytics and Reporting
     */
    @Query("SELECT COUNT(*) FROM ratings WHERE labourerId = :labourerId AND rating < :threshold")
    suspend fun getLowRatingsCount(labourerId: String, threshold: Int = 3): Int
    
    /**
     * Get most recent rating for a labourer
     * Requirement 13: Rating and Review System
     */
    @Query("SELECT * FROM ratings WHERE labourerId = :labourerId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getMostRecentRating(labourerId: String): Rating?
    
    /**
     * Get all ratings (for admin/debugging purposes)
     */
    @Query("SELECT * FROM ratings ORDER BY createdAt DESC")
    fun getAllRatings(): Flow<List<Rating>>
    
    /**
     * Get total count of all ratings in the system
     * Requirement 24: Analytics and Reporting
     */
    @Query("SELECT COUNT(*) FROM ratings")
    suspend fun getTotalRatingsCount(): Int
    
    /**
     * Check if a booking has been rated
     * Returns true if rating exists, false otherwise
     * Requirement 13: Rating and Review System
     */
    @Query("SELECT EXISTS(SELECT 1 FROM ratings WHERE bookingId = :bookingId)")
    suspend fun isBookingRated(bookingId: String): Boolean
    
    /**
     * Get labourers with highest average ratings
     * Minimum rating count threshold to ensure statistical significance
     * Requirement 13: Rating and Review System
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT labourerId, AVG(rating) as avgRating, COUNT(*) as ratingCount
        FROM ratings
        GROUP BY labourerId
        HAVING ratingCount >= :minRatingsCount
        ORDER BY avgRating DESC
        LIMIT :limit
    """)
    suspend fun getTopRatedLabourers(minRatingsCount: Int = 5, limit: Int = 10): List<LabourerRatingStats>
}

/**
 * Data class for rating statistics
 * Used by getRatingStatistics query
 */
data class RatingStatistics(
    val averageRating: Double,
    val totalRatings: Int
)

/**
 * Data class for rating distribution
 * Used by getRatingDistribution query
 */
data class RatingDistribution(
    val rating: Int,
    val count: Int
)

/**
 * Data class for labourer rating statistics
 * Used by getTopRatedLabourers query
 */
data class LabourerRatingStats(
    val labourerId: String,
    val avgRating: Double,
    val ratingCount: Int
)
