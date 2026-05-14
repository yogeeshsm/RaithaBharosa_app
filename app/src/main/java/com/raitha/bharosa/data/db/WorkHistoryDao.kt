package com.raitha.bharosa.data.db

import androidx.room.*
import com.raitha.bharosa.data.WorkHistory
import com.raitha.bharosa.data.Skill
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Work History operations
 * 
 * Provides database access methods for:
 * - Work history insert operations (Requirement 14: Work History and Attendance Tracking)
 * - Queries by date range and work type (Requirement 14: Work History and Attendance Tracking)
 * - Earnings and days worked calculations (Requirement 14: Work History and Attendance Tracking)
 * - Analytics and reporting (Requirement 24: Analytics and Reporting)
 * 
 * Part of Phase 2: Data Layer (Room + Firestore)
 * Task 6.4: Create WorkHistoryDao
 */
@Dao
interface WorkHistoryDao {
    
    // ========== Work History CRUD Operations ==========
    
    /**
     * Insert or replace a work history entry
     * Requirement 14: Work History and Attendance Tracking
     * Requirement 16: Offline Data Persistence
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkHistory(workHistory: WorkHistory)
    
    /**
     * Update an existing work history entry
     * Requirement 14: Work History and Attendance Tracking
     */
    @Update
    suspend fun updateWorkHistory(workHistory: WorkHistory)
    
    /**
     * Delete a work history entry
     * Requirement 23: Security and Privacy (data deletion)
     */
    @Delete
    suspend fun deleteWorkHistory(workHistory: WorkHistory)
    
    /**
     * Get a work history entry by ID (one-time fetch)
     */
    @Query("SELECT * FROM work_history WHERE workId = :workId")
    suspend fun getWorkHistoryById(workId: String): WorkHistory?
    
    /**
     * Get a work history entry by booking ID
     * Used to check if work history already exists for a booking
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("SELECT * FROM work_history WHERE bookingId = :bookingId")
    suspend fun getWorkHistoryByBookingId(bookingId: String): WorkHistory?
    
    // ========== Queries for Labourer Work History ==========
    
    /**
     * Get all work history for a labourer with reactive updates
     * Sorted by work date (most recent first)
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("SELECT * FROM work_history WHERE labourerId = :labourerId ORDER BY workDate DESC")
    fun getWorkHistory(labourerId: String): Flow<List<WorkHistory>>
    
    /**
     * Get all work history for a labourer (one-time fetch)
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("SELECT * FROM work_history WHERE labourerId = :labourerId ORDER BY workDate DESC")
    suspend fun getWorkHistoryOnce(labourerId: String): List<WorkHistory>
    
    /**
     * Get work history by date range for a labourer
     * Used for filtering work history by specific time periods
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        SELECT * FROM work_history 
        WHERE labourerId = :labourerId 
        AND workDate BETWEEN :startDate AND :endDate
        ORDER BY workDate DESC
    """)
    suspend fun getWorkHistoryByDateRange(
        labourerId: String, 
        startDate: Long, 
        endDate: Long
    ): List<WorkHistory>
    
    /**
     * Get work history by date range with reactive updates
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        SELECT * FROM work_history 
        WHERE labourerId = :labourerId 
        AND workDate BETWEEN :startDate AND :endDate
        ORDER BY workDate DESC
    """)
    fun getWorkHistoryByDateRangeFlow(
        labourerId: String, 
        startDate: Long, 
        endDate: Long
    ): Flow<List<WorkHistory>>
    
    /**
     * Get work history by work type for a labourer
     * Used for filtering by specific skill/work type
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        SELECT * FROM work_history 
        WHERE labourerId = :labourerId 
        AND workType = :workType
        ORDER BY workDate DESC
    """)
    suspend fun getWorkHistoryByType(labourerId: String, workType: Skill): List<WorkHistory>
    
    /**
     * Get work history by work type with reactive updates
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        SELECT * FROM work_history 
        WHERE labourerId = :labourerId 
        AND workType = :workType
        ORDER BY workDate DESC
    """)
    fun getWorkHistoryByTypeFlow(labourerId: String, workType: Skill): Flow<List<WorkHistory>>
    
    /**
     * Get recent work history for a labourer (limited to specified count)
     * Used for displaying recent work on profile or dashboard
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        SELECT * FROM work_history 
        WHERE labourerId = :labourerId 
        ORDER BY workDate DESC 
        LIMIT :limit
    """)
    suspend fun getRecentWorkHistory(labourerId: String, limit: Int = 10): List<WorkHistory>
    
    // ========== Earnings Calculations ==========
    
    /**
     * Calculate total earnings for a labourer in a date range
     * Returns null if no work history exists in the range
     * Requirement 14: Work History and Attendance Tracking
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT SUM(paymentReceived) FROM work_history 
        WHERE labourerId = :labourerId 
        AND workDate BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalEarnings(labourerId: String, startDate: Long, endDate: Long): Int?
    
    /**
     * Calculate total earnings for a labourer (all time)
     * Requirement 24: Analytics and Reporting
     */
    @Query("SELECT SUM(paymentReceived) FROM work_history WHERE labourerId = :labourerId")
    suspend fun getTotalEarningsAllTime(labourerId: String): Int?
    
    /**
     * Calculate average earnings per day for a labourer in a date range
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT AVG(paymentReceived) FROM work_history 
        WHERE labourerId = :labourerId 
        AND workDate BETWEEN :startDate AND :endDate
    """)
    suspend fun getAverageEarningsPerDay(labourerId: String, startDate: Long, endDate: Long): Double?
    
    /**
     * Calculate total earnings by work type for a labourer
     * Used for earnings breakdown by skill
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT SUM(paymentReceived) FROM work_history 
        WHERE labourerId = :labourerId 
        AND workType = :workType
    """)
    suspend fun getTotalEarningsByWorkType(labourerId: String, workType: Skill): Int?
    
    /**
     * Get earnings breakdown by work type for a labourer
     * Returns list of work types with their total earnings
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT workType, SUM(paymentReceived) as totalEarnings, COUNT(*) as workCount
        FROM work_history 
        WHERE labourerId = :labourerId
        GROUP BY workType
        ORDER BY totalEarnings DESC
    """)
    suspend fun getEarningsBreakdownByWorkType(labourerId: String): List<WorkTypeEarnings>
    
    /**
     * Get monthly earnings for a labourer
     * Returns earnings grouped by month for the past specified number of months
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT 
            strftime('%Y-%m', workDate / 1000, 'unixepoch') as month,
            SUM(paymentReceived) as totalEarnings,
            COUNT(*) as workCount
        FROM work_history 
        WHERE labourerId = :labourerId
        AND workDate >= :startDate
        GROUP BY month
        ORDER BY month DESC
    """)
    suspend fun getMonthlyEarnings(labourerId: String, startDate: Long): List<MonthlyEarnings>
    
    // ========== Days Worked Calculations ==========
    
    /**
     * Calculate total days worked for a labourer in a date range
     * Requirement 14: Work History and Attendance Tracking
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT COUNT(*) FROM work_history 
        WHERE labourerId = :labourerId 
        AND workDate BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalDaysWorked(labourerId: String, startDate: Long, endDate: Long): Int
    
    /**
     * Calculate total days worked for a labourer (all time)
     * Requirement 24: Analytics and Reporting
     */
    @Query("SELECT COUNT(*) FROM work_history WHERE labourerId = :labourerId")
    suspend fun getTotalDaysWorkedAllTime(labourerId: String): Int
    
    /**
     * Calculate total hours worked for a labourer in a date range
     * Requirement 14: Work History and Attendance Tracking
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT SUM(actualHours) FROM work_history 
        WHERE labourerId = :labourerId 
        AND workDate BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalHoursWorked(labourerId: String, startDate: Long, endDate: Long): Int?
    
    /**
     * Calculate total hours worked for a labourer (all time)
     * Requirement 24: Analytics and Reporting
     */
    @Query("SELECT SUM(actualHours) FROM work_history WHERE labourerId = :labourerId")
    suspend fun getTotalHoursWorkedAllTime(labourerId: String): Int?
    
    /**
     * Calculate average hours worked per day for a labourer in a date range
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT AVG(actualHours) FROM work_history 
        WHERE labourerId = :labourerId 
        AND workDate BETWEEN :startDate AND :endDate
    """)
    suspend fun getAverageHoursPerDay(labourerId: String, startDate: Long, endDate: Long): Double?
    
    /**
     * Get days worked by work type for a labourer
     * Returns count of days worked for each skill
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT workType, COUNT(*) as daysWorked, SUM(actualHours) as totalHours
        FROM work_history 
        WHERE labourerId = :labourerId
        GROUP BY workType
        ORDER BY daysWorked DESC
    """)
    suspend fun getDaysWorkedByWorkType(labourerId: String): List<WorkTypeDays>
    
    // ========== Rating Statistics ==========
    
    /**
     * Get average rating received for a labourer
     * Excludes entries with null ratings
     * Requirement 13: Rating and Review System
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT AVG(ratingReceived) FROM work_history 
        WHERE labourerId = :labourerId 
        AND ratingReceived IS NOT NULL
    """)
    suspend fun getAverageRatingFromWorkHistory(labourerId: String): Double?
    
    /**
     * Get count of work entries with ratings
     * Requirement 13: Rating and Review System
     */
    @Query("""
        SELECT COUNT(*) FROM work_history 
        WHERE labourerId = :labourerId 
        AND ratingReceived IS NOT NULL
    """)
    suspend fun getRatedWorkCount(labourerId: String): Int
    
    /**
     * Get count of work entries without ratings
     * Requirement 13: Rating and Review System
     */
    @Query("""
        SELECT COUNT(*) FROM work_history 
        WHERE labourerId = :labourerId 
        AND ratingReceived IS NULL
    """)
    suspend fun getUnratedWorkCount(labourerId: String): Int
    
    // ========== Farmer Statistics ==========
    
    /**
     * Get most frequent farmers for a labourer
     * Returns list of farmers with work count
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT farmerName, COUNT(*) as workCount, SUM(paymentReceived) as totalEarnings
        FROM work_history 
        WHERE labourerId = :labourerId
        GROUP BY farmerName
        ORDER BY workCount DESC
        LIMIT :limit
    """)
    suspend fun getMostFrequentFarmers(labourerId: String, limit: Int = 5): List<FarmerWorkStats>
    
    /**
     * Get work history with a specific farmer
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        SELECT * FROM work_history 
        WHERE labourerId = :labourerId 
        AND farmerName = :farmerName
        ORDER BY workDate DESC
    """)
    suspend fun getWorkHistoryWithFarmer(labourerId: String, farmerName: String): List<WorkHistory>
    
    // ========== General Statistics ==========
    
    /**
     * Get total count of all work history entries in the system
     * Requirement 24: Analytics and Reporting
     */
    @Query("SELECT COUNT(*) FROM work_history")
    suspend fun getTotalWorkHistoryCount(): Int
    
    /**
     * Get all work history entries (for admin/debugging purposes)
     */
    @Query("SELECT * FROM work_history ORDER BY workDate DESC")
    fun getAllWorkHistory(): Flow<List<WorkHistory>>
    
    /**
     * Check if work history exists for a booking
     * Returns true if work history exists, false otherwise
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("SELECT EXISTS(SELECT 1 FROM work_history WHERE bookingId = :bookingId)")
    suspend fun hasWorkHistoryForBooking(bookingId: String): Boolean
    
    /**
     * Get work history statistics for a labourer
     * Returns comprehensive statistics including total days, hours, and earnings
     * Requirement 14: Work History and Attendance Tracking
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT 
            COUNT(*) as totalDays,
            SUM(actualHours) as totalHours,
            SUM(paymentReceived) as totalEarnings,
            AVG(paymentReceived) as avgEarningsPerDay,
            AVG(actualHours) as avgHoursPerDay,
            AVG(ratingReceived) as avgRating
        FROM work_history 
        WHERE labourerId = :labourerId
    """)
    suspend fun getWorkHistoryStatistics(labourerId: String): WorkHistoryStatistics?
    
    /**
     * Get work history statistics for a date range
     * Requirement 14: Work History and Attendance Tracking
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT 
            COUNT(*) as totalDays,
            SUM(actualHours) as totalHours,
            SUM(paymentReceived) as totalEarnings,
            AVG(paymentReceived) as avgEarningsPerDay,
            AVG(actualHours) as avgHoursPerDay,
            AVG(ratingReceived) as avgRating
        FROM work_history 
        WHERE labourerId = :labourerId
        AND workDate BETWEEN :startDate AND :endDate
    """)
    suspend fun getWorkHistoryStatisticsByDateRange(
        labourerId: String, 
        startDate: Long, 
        endDate: Long
    ): WorkHistoryStatistics?
    
    // ========== Bulk Operations ==========
    
    /**
     * Insert multiple work history entries
     * Used for batch operations or data migration
     * Requirement 16: Offline Data Persistence
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkHistoryBatch(workHistoryList: List<WorkHistory>)
    
    /**
     * Delete all work history for a labourer
     * Used for account deletion
     * Requirement 23: Security and Privacy (data deletion)
     */
    @Query("DELETE FROM work_history WHERE labourerId = :labourerId")
    suspend fun deleteAllWorkHistoryForLabourer(labourerId: String)
    
    /**
     * Delete work history older than a specific date
     * Used for data cleanup or archival
     */
    @Query("DELETE FROM work_history WHERE workDate < :beforeDate")
    suspend fun deleteWorkHistoryBeforeDate(beforeDate: Long)
}

/**
 * Data class for work type earnings breakdown
 * Used by getEarningsBreakdownByWorkType query
 */
data class WorkTypeEarnings(
    val workType: Skill,
    val totalEarnings: Int,
    val workCount: Int
)

/**
 * Data class for monthly earnings
 * Used by getMonthlyEarnings query
 */
data class MonthlyEarnings(
    val month: String,
    val totalEarnings: Int,
    val workCount: Int
)

/**
 * Data class for work type days statistics
 * Used by getDaysWorkedByWorkType query
 */
data class WorkTypeDays(
    val workType: Skill,
    val daysWorked: Int,
    val totalHours: Int
)

/**
 * Data class for farmer work statistics
 * Used by getMostFrequentFarmers query
 */
data class FarmerWorkStats(
    val farmerName: String,
    val workCount: Int,
    val totalEarnings: Int
)

/**
 * Data class for comprehensive work history statistics
 * Used by getWorkHistoryStatistics and getWorkHistoryStatisticsByDateRange queries
 */
data class WorkHistoryStatistics(
    val totalDays: Int,
    val totalHours: Int?,
    val totalEarnings: Int?,
    val avgEarningsPerDay: Double?,
    val avgHoursPerDay: Double?,
    val avgRating: Double?
)
