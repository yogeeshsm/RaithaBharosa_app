package com.raitha.bharosa.data.db

import androidx.room.*
import com.raitha.bharosa.data.Attendance
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Attendance operations
 * 
 * Provides database access methods for:
 * - Attendance CRUD operations (Requirement 14: Work History and Attendance Tracking)
 * - Check-in and check-out updates (Requirement 14: Work History and Attendance Tracking)
 * - Queries by booking ID and labourer ID (Requirement 14: Work History and Attendance Tracking)
 * 
 * Part of Phase 2: Data Layer (Room + Firestore)
 * Task 6.5: Create AttendanceDao
 */
@Dao
interface AttendanceDao {
    
    // ========== Attendance CRUD Operations ==========
    
    /**
     * Insert or replace an attendance record
     * Requirement 14: Work History and Attendance Tracking
     * Requirement 16: Offline Data Persistence
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance)
    
    /**
     * Update an existing attendance record
     * Requirement 14: Work History and Attendance Tracking
     */
    @Update
    suspend fun updateAttendance(attendance: Attendance)
    
    /**
     * Delete an attendance record
     * Requirement 23: Security and Privacy (data deletion)
     */
    @Delete
    suspend fun deleteAttendance(attendance: Attendance)
    
    /**
     * Get an attendance record by ID (one-time fetch)
     */
    @Query("SELECT * FROM attendance WHERE attendanceId = :attendanceId")
    suspend fun getAttendanceById(attendanceId: String): Attendance?
    
    /**
     * Get an attendance record by ID with reactive updates
     */
    @Query("SELECT * FROM attendance WHERE attendanceId = :attendanceId")
    fun getAttendanceByIdFlow(attendanceId: String): Flow<Attendance?>
    
    // ========== Queries by Booking ID ==========
    
    /**
     * Get attendance record for a specific booking
     * Used to track check-in/check-out for a work session
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("SELECT * FROM attendance WHERE bookingId = :bookingId")
    suspend fun getAttendanceForBooking(bookingId: String): Attendance?
    
    /**
     * Get attendance record for a booking with reactive updates
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("SELECT * FROM attendance WHERE bookingId = :bookingId")
    fun getAttendanceForBookingFlow(bookingId: String): Flow<Attendance?>
    
    /**
     * Check if attendance record exists for a booking
     * Returns true if attendance exists, false otherwise
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("SELECT EXISTS(SELECT 1 FROM attendance WHERE bookingId = :bookingId)")
    suspend fun hasAttendanceForBooking(bookingId: String): Boolean
    
    // ========== Queries by Labourer ID ==========
    
    /**
     * Get all attendance records for a labourer with reactive updates
     * Sorted by creation date (most recent first)
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("SELECT * FROM attendance WHERE labourerId = :labourerId ORDER BY createdAt DESC")
    fun getLabourerAttendance(labourerId: String): Flow<List<Attendance>>
    
    /**
     * Get all attendance records for a labourer (one-time fetch)
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("SELECT * FROM attendance WHERE labourerId = :labourerId ORDER BY createdAt DESC")
    suspend fun getLabourerAttendanceOnce(labourerId: String): List<Attendance>
    
    /**
     * Get recent attendance records for a labourer (limited to specified count)
     * Used for displaying recent attendance on dashboard
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        SELECT * FROM attendance 
        WHERE labourerId = :labourerId 
        ORDER BY createdAt DESC 
        LIMIT :limit
    """)
    suspend fun getRecentAttendance(labourerId: String, limit: Int = 10): List<Attendance>
    
    /**
     * Get attendance records by date range for a labourer
     * Used for filtering attendance by specific time periods
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        SELECT * FROM attendance 
        WHERE labourerId = :labourerId 
        AND createdAt BETWEEN :startDate AND :endDate
        ORDER BY createdAt DESC
    """)
    suspend fun getLabourerAttendanceByDateRange(
        labourerId: String, 
        startDate: Long, 
        endDate: Long
    ): List<Attendance>
    
    /**
     * Get attendance records by date range with reactive updates
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        SELECT * FROM attendance 
        WHERE labourerId = :labourerId 
        AND createdAt BETWEEN :startDate AND :endDate
        ORDER BY createdAt DESC
    """)
    fun getLabourerAttendanceByDateRangeFlow(
        labourerId: String, 
        startDate: Long, 
        endDate: Long
    ): Flow<List<Attendance>>
    
    // ========== Check-In and Check-Out Updates ==========
    
    /**
     * Record check-in time for an attendance record
     * Updates checkInTime and updatedAt timestamp
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        UPDATE attendance 
        SET checkInTime = :time, updatedAt = :timestamp 
        WHERE attendanceId = :attendanceId
    """)
    suspend fun recordCheckIn(attendanceId: String, time: Long, timestamp: Long)
    
    /**
     * Record check-out time and calculate actual hours worked
     * Updates checkOutTime, actualHours, and updatedAt timestamp
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        UPDATE attendance 
        SET checkOutTime = :time, actualHours = :hours, updatedAt = :timestamp 
        WHERE attendanceId = :attendanceId
    """)
    suspend fun recordCheckOut(attendanceId: String, time: Long, hours: Int, timestamp: Long)
    
    /**
     * Update notes for an attendance record
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        UPDATE attendance 
        SET notes = :notes, updatedAt = :timestamp 
        WHERE attendanceId = :attendanceId
    """)
    suspend fun updateNotes(attendanceId: String, notes: String?, timestamp: Long)
    
    /**
     * Update actual hours worked for an attendance record
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        UPDATE attendance 
        SET actualHours = :hours, updatedAt = :timestamp 
        WHERE attendanceId = :attendanceId
    """)
    suspend fun updateActualHours(attendanceId: String, hours: Int, timestamp: Long)
    
    // ========== Attendance Status Queries ==========
    
    /**
     * Get attendance records with check-in but no check-out
     * Used to identify ongoing work sessions
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        SELECT * FROM attendance 
        WHERE labourerId = :labourerId 
        AND checkInTime IS NOT NULL 
        AND checkOutTime IS NULL
        ORDER BY checkInTime DESC
    """)
    suspend fun getActiveAttendance(labourerId: String): List<Attendance>
    
    /**
     * Get attendance records with check-in but no check-out (reactive)
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        SELECT * FROM attendance 
        WHERE labourerId = :labourerId 
        AND checkInTime IS NOT NULL 
        AND checkOutTime IS NULL
        ORDER BY checkInTime DESC
    """)
    fun getActiveAttendanceFlow(labourerId: String): Flow<List<Attendance>>
    
    /**
     * Get completed attendance records (both check-in and check-out recorded)
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        SELECT * FROM attendance 
        WHERE labourerId = :labourerId 
        AND checkInTime IS NOT NULL 
        AND checkOutTime IS NOT NULL
        ORDER BY checkOutTime DESC
    """)
    suspend fun getCompletedAttendance(labourerId: String): List<Attendance>
    
    /**
     * Get completed attendance records with reactive updates
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        SELECT * FROM attendance 
        WHERE labourerId = :labourerId 
        AND checkInTime IS NOT NULL 
        AND checkOutTime IS NOT NULL
        ORDER BY checkOutTime DESC
    """)
    fun getCompletedAttendanceFlow(labourerId: String): Flow<List<Attendance>>
    
    /**
     * Get attendance records without check-in
     * Used to identify pending attendance records
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        SELECT * FROM attendance 
        WHERE labourerId = :labourerId 
        AND checkInTime IS NULL
        ORDER BY createdAt DESC
    """)
    suspend fun getPendingAttendance(labourerId: String): List<Attendance>
    
    /**
     * Check if labourer has an active check-in (no check-out)
     * Returns true if there's an active session, false otherwise
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM attendance 
            WHERE labourerId = :labourerId 
            AND checkInTime IS NOT NULL 
            AND checkOutTime IS NULL
        )
    """)
    suspend fun hasActiveCheckIn(labourerId: String): Boolean
    
    /**
     * Get the most recent active attendance record for a labourer
     * Returns null if no active attendance exists
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        SELECT * FROM attendance 
        WHERE labourerId = :labourerId 
        AND checkInTime IS NOT NULL 
        AND checkOutTime IS NULL
        ORDER BY checkInTime DESC 
        LIMIT 1
    """)
    suspend fun getMostRecentActiveAttendance(labourerId: String): Attendance?
    
    // ========== Analytics and Statistics ==========
    
    /**
     * Calculate total hours worked for a labourer in a date range
     * Requirement 14: Work History and Attendance Tracking
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT SUM(actualHours) FROM attendance 
        WHERE labourerId = :labourerId 
        AND checkOutTime IS NOT NULL
        AND createdAt BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalHoursWorked(labourerId: String, startDate: Long, endDate: Long): Int?
    
    /**
     * Calculate total hours worked for a labourer (all time)
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT SUM(actualHours) FROM attendance 
        WHERE labourerId = :labourerId 
        AND checkOutTime IS NOT NULL
    """)
    suspend fun getTotalHoursWorkedAllTime(labourerId: String): Int?
    
    /**
     * Calculate average hours worked per session for a labourer
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT AVG(actualHours) FROM attendance 
        WHERE labourerId = :labourerId 
        AND actualHours IS NOT NULL
    """)
    suspend fun getAverageHoursPerSession(labourerId: String): Double?
    
    /**
     * Get count of completed attendance records for a labourer
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT COUNT(*) FROM attendance 
        WHERE labourerId = :labourerId 
        AND checkInTime IS NOT NULL 
        AND checkOutTime IS NOT NULL
    """)
    suspend fun getCompletedAttendanceCount(labourerId: String): Int
    
    /**
     * Get count of attendance records in a date range
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT COUNT(*) FROM attendance 
        WHERE labourerId = :labourerId 
        AND createdAt BETWEEN :startDate AND :endDate
    """)
    suspend fun getAttendanceCountByDateRange(
        labourerId: String, 
        startDate: Long, 
        endDate: Long
    ): Int
    
    /**
     * Get attendance statistics for a labourer
     * Returns comprehensive statistics including total sessions, hours, and averages
     * Requirement 14: Work History and Attendance Tracking
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT 
            COUNT(*) as totalSessions,
            SUM(actualHours) as totalHours,
            AVG(actualHours) as avgHoursPerSession,
            COUNT(CASE WHEN checkInTime IS NOT NULL AND checkOutTime IS NULL THEN 1 END) as activeSessions,
            COUNT(CASE WHEN checkInTime IS NOT NULL AND checkOutTime IS NOT NULL THEN 1 END) as completedSessions
        FROM attendance 
        WHERE labourerId = :labourerId
    """)
    suspend fun getAttendanceStatistics(labourerId: String): AttendanceStatistics?
    
    /**
     * Get attendance statistics for a date range
     * Requirement 14: Work History and Attendance Tracking
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT 
            COUNT(*) as totalSessions,
            SUM(actualHours) as totalHours,
            AVG(actualHours) as avgHoursPerSession,
            COUNT(CASE WHEN checkInTime IS NOT NULL AND checkOutTime IS NULL THEN 1 END) as activeSessions,
            COUNT(CASE WHEN checkInTime IS NOT NULL AND checkOutTime IS NOT NULL THEN 1 END) as completedSessions
        FROM attendance 
        WHERE labourerId = :labourerId
        AND createdAt BETWEEN :startDate AND :endDate
    """)
    suspend fun getAttendanceStatisticsByDateRange(
        labourerId: String, 
        startDate: Long, 
        endDate: Long
    ): AttendanceStatistics?
    
    // ========== Bulk Operations ==========
    
    /**
     * Insert multiple attendance records
     * Used for batch operations or data migration
     * Requirement 16: Offline Data Persistence
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceBatch(attendanceList: List<Attendance>)
    
    /**
     * Delete all attendance records for a labourer
     * Used for account deletion
     * Requirement 23: Security and Privacy (data deletion)
     */
    @Query("DELETE FROM attendance WHERE labourerId = :labourerId")
    suspend fun deleteAllAttendanceForLabourer(labourerId: String)
    
    /**
     * Delete attendance records older than a specific date
     * Used for data cleanup or archival
     */
    @Query("DELETE FROM attendance WHERE createdAt < :beforeDate")
    suspend fun deleteAttendanceBeforeDate(beforeDate: Long)
    
    /**
     * Get all attendance records (for admin/debugging purposes)
     */
    @Query("SELECT * FROM attendance ORDER BY createdAt DESC")
    fun getAllAttendance(): Flow<List<Attendance>>
    
    /**
     * Get total count of all attendance records in the system
     * Requirement 24: Analytics and Reporting
     */
    @Query("SELECT COUNT(*) FROM attendance")
    suspend fun getTotalAttendanceCount(): Int
}

/**
 * Data class for comprehensive attendance statistics
 * Used by getAttendanceStatistics and getAttendanceStatisticsByDateRange queries
 */
data class AttendanceStatistics(
    val totalSessions: Int,
    val totalHours: Int?,
    val avgHoursPerSession: Double?,
    val activeSessions: Int,
    val completedSessions: Int
)
