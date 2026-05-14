package com.raitha.bharosa.data.db

import androidx.room.*
import com.raitha.bharosa.data.Booking
import com.raitha.bharosa.data.BookingStatus
import com.raitha.bharosa.data.PaymentStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Booking operations
 * 
 * Provides database access methods for:
 * - Booking CRUD operations (Requirement 8: Booking Creation)
 * - Queries by farmer ID and labourer ID (Requirements 11, 12: Booking Management)
 * - Status-based queries (Requirements 11, 12: Booking Management)
 * - Conflict detection query (Requirement 8: Booking Creation)
 * 
 * Part of Phase 2: Data Layer (Room + Firestore)
 */
@Dao
interface BookingDao {
    
    // ========== Booking CRUD Operations ==========
    
    /**
     * Insert or replace a booking
     * Requirement 8: Booking Creation
     * Requirement 16: Offline Data Persistence
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking)
    
    /**
     * Update an existing booking
     * Requirement 11: Booking Management for Labourers
     * Requirement 12: Booking Management for Farmers
     */
    @Update
    suspend fun updateBooking(booking: Booking)
    
    /**
     * Get a booking by ID with reactive updates
     * Returns Flow for real-time updates in UI
     * Requirement 8: Booking Creation
     */
    @Query("SELECT * FROM bookings WHERE bookingId = :bookingId")
    fun getBooking(bookingId: String): Flow<Booking?>
    
    /**
     * Get a booking by ID (one-time fetch)
     * Used for non-reactive operations
     */
    @Query("SELECT * FROM bookings WHERE bookingId = :bookingId")
    suspend fun getBookingOnce(bookingId: String): Booking?
    
    /**
     * Delete a booking
     * Requirement 12: Booking Management for Farmers (cancellation)
     */
    @Delete
    suspend fun deleteBooking(booking: Booking)
    
    // ========== Queries by Farmer ID ==========
    
    /**
     * Get all bookings for a farmer with reactive updates
     * Sorted by work date (most recent first)
     * Requirement 12: Booking Management for Farmers
     */
    @Query("SELECT * FROM bookings WHERE farmerId = :farmerId ORDER BY workDate DESC")
    fun getFarmerBookings(farmerId: String): Flow<List<Booking>>
    
    /**
     * Get farmer bookings by status with reactive updates
     * Requirement 12: Booking Management for Farmers
     */
    @Query("""
        SELECT * FROM bookings 
        WHERE farmerId = :farmerId AND status = :status 
        ORDER BY workDate DESC
    """)
    fun getFarmerBookingsByStatus(farmerId: String, status: BookingStatus): Flow<List<Booking>>
    
    /**
     * Get farmer bookings by status (one-time fetch)
     * Requirement 12: Booking Management for Farmers
     */
    @Query("""
        SELECT * FROM bookings 
        WHERE farmerId = :farmerId AND status = :status 
        ORDER BY workDate DESC
    """)
    suspend fun getFarmerBookingsByStatusOnce(farmerId: String, status: BookingStatus): List<Booking>
    
    /**
     * Get pending bookings for a farmer
     * Requirement 12: Booking Management for Farmers
     */
    @Query("""
        SELECT * FROM bookings 
        WHERE farmerId = :farmerId AND status = 'PENDING' 
        ORDER BY workDate ASC
    """)
    fun getFarmerPendingBookings(farmerId: String): Flow<List<Booking>>
    
    /**
     * Get accepted bookings for a farmer
     * Requirement 12: Booking Management for Farmers
     */
    @Query("""
        SELECT * FROM bookings 
        WHERE farmerId = :farmerId AND status = 'ACCEPTED' 
        ORDER BY workDate ASC
    """)
    fun getFarmerAcceptedBookings(farmerId: String): Flow<List<Booking>>
    
    /**
     * Get completed bookings for a farmer
     * Requirement 12: Booking Management for Farmers
     * Requirement 13: Rating and Review System
     */
    @Query("""
        SELECT * FROM bookings 
        WHERE farmerId = :farmerId AND status = 'COMPLETED' 
        ORDER BY completedAt DESC
    """)
    fun getFarmerCompletedBookings(farmerId: String): Flow<List<Booking>>
    
    /**
     * Get count of farmer bookings by status
     * Requirement 24: Analytics and Reporting
     */
    @Query("SELECT COUNT(*) FROM bookings WHERE farmerId = :farmerId AND status = :status")
    suspend fun getFarmerBookingsCountByStatus(farmerId: String, status: BookingStatus): Int
    
    // ========== Queries by Labourer ID ==========
    
    /**
     * Get all bookings for a labourer with reactive updates
     * Sorted by work date (most recent first)
     * Requirement 11: Booking Management for Labourers
     */
    @Query("SELECT * FROM bookings WHERE labourerId = :labourerId ORDER BY workDate DESC")
    fun getLabourerBookings(labourerId: String): Flow<List<Booking>>
    
    /**
     * Get labourer bookings by status with reactive updates
     * Requirement 11: Booking Management for Labourers
     */
    @Query("""
        SELECT * FROM bookings 
        WHERE labourerId = :labourerId AND status = :status 
        ORDER BY workDate DESC
    """)
    fun getLabourerBookingsByStatus(labourerId: String, status: BookingStatus): Flow<List<Booking>>
    
    /**
     * Get labourer bookings by status (one-time fetch)
     * Requirement 11: Booking Management for Labourers
     */
    @Query("""
        SELECT * FROM bookings 
        WHERE labourerId = :labourerId AND status = :status 
        ORDER BY workDate DESC
    """)
    suspend fun getLabourerBookingsByStatusOnce(labourerId: String, status: BookingStatus): List<Booking>
    
    /**
     * Get pending bookings for a labourer
     * Sorted by emergency status (urgent first) and creation time
     * Requirement 9: Emergency Hiring
     * Requirement 11: Booking Management for Labourers
     */
    @Query("""
        SELECT * FROM bookings 
        WHERE labourerId = :labourerId AND status = 'PENDING' 
        ORDER BY isEmergency DESC, createdAt ASC
    """)
    fun getPendingBookingsForLabourer(labourerId: String): Flow<List<Booking>>
    
    /**
     * Get accepted bookings for a labourer
     * Requirement 11: Booking Management for Labourers
     */
    @Query("""
        SELECT * FROM bookings 
        WHERE labourerId = :labourerId AND status = 'ACCEPTED' 
        ORDER BY workDate ASC
    """)
    fun getLabourerAcceptedBookings(labourerId: String): Flow<List<Booking>>
    
    /**
     * Get completed bookings for a labourer
     * Requirement 11: Booking Management for Labourers
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        SELECT * FROM bookings 
        WHERE labourerId = :labourerId AND status = 'COMPLETED' 
        ORDER BY completedAt DESC
    """)
    fun getLabourerCompletedBookings(labourerId: String): Flow<List<Booking>>
    
    /**
     * Get count of labourer bookings by status
     * Requirement 24: Analytics and Reporting
     */
    @Query("SELECT COUNT(*) FROM bookings WHERE labourerId = :labourerId AND status = :status")
    suspend fun getLabourerBookingsCountByStatus(labourerId: String, status: BookingStatus): Int
    
    // ========== Status-Based Queries ==========
    
    /**
     * Get all bookings by status
     * Requirement 11: Booking Management for Labourers
     * Requirement 12: Booking Management for Farmers
     */
    @Query("SELECT * FROM bookings WHERE status = :status ORDER BY workDate DESC")
    fun getBookingsByStatus(status: BookingStatus): Flow<List<Booking>>
    
    /**
     * Get all pending bookings
     * Requirement 11: Booking Management for Labourers
     */
    @Query("SELECT * FROM bookings WHERE status = 'PENDING' ORDER BY isEmergency DESC, createdAt ASC")
    fun getAllPendingBookings(): Flow<List<Booking>>
    
    /**
     * Get all accepted bookings
     * Requirement 11: Booking Management for Labourers
     * Requirement 12: Booking Management for Farmers
     */
    @Query("SELECT * FROM bookings WHERE status = 'ACCEPTED' ORDER BY workDate ASC")
    fun getAllAcceptedBookings(): Flow<List<Booking>>
    
    /**
     * Get all completed bookings
     * Requirement 13: Rating and Review System
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("SELECT * FROM bookings WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun getAllCompletedBookings(): Flow<List<Booking>>
    
    /**
     * Get emergency bookings for a labourer
     * Requirement 9: Emergency Hiring
     */
    @Query("""
        SELECT * FROM bookings 
        WHERE labourerId = :labourerId 
        AND isEmergency = 1 
        AND status = 'PENDING'
        ORDER BY createdAt ASC
    """)
    fun getEmergencyBookingsForLabourer(labourerId: String): Flow<List<Booking>>
    
    /**
     * Get bookings by payment status
     * Requirement 15: Payment Integration
     */
    @Query("""
        SELECT * FROM bookings 
        WHERE paymentStatus = :paymentStatus 
        ORDER BY completedAt DESC
    """)
    fun getBookingsByPaymentStatus(paymentStatus: PaymentStatus): Flow<List<Booking>>
    
    /**
     * Get unpaid completed bookings for a farmer
     * Requirement 15: Payment Integration
     */
    @Query("""
        SELECT * FROM bookings 
        WHERE farmerId = :farmerId 
        AND status = 'COMPLETED' 
        AND paymentStatus != 'PAID'
        ORDER BY completedAt ASC
    """)
    fun getUnpaidBookingsForFarmer(farmerId: String): Flow<List<Booking>>
    
    /**
     * Get unpaid completed bookings for a labourer
     * Requirement 15: Payment Integration
     */
    @Query("""
        SELECT * FROM bookings 
        WHERE labourerId = :labourerId 
        AND status = 'COMPLETED' 
        AND paymentStatus != 'PAID'
        ORDER BY completedAt ASC
    """)
    fun getUnpaidBookingsForLabourer(labourerId: String): Flow<List<Booking>>
    
    // ========== Conflict Detection Query ==========
    
    /**
     * Check if a labourer has conflicting bookings on a specific date
     * Returns count of conflicting bookings (PENDING or ACCEPTED status)
     * Requirement 8: Booking Creation (conflict detection)
     */
    @Query("""
        SELECT COUNT(*) FROM bookings 
        WHERE labourerId = :labourerId 
        AND workDate = :date 
        AND status IN ('PENDING', 'ACCEPTED')
    """)
    suspend fun hasConflictingBooking(labourerId: String, date: Long): Int
    
    /**
     * Check if a labourer has conflicting bookings in a date range
     * Returns count of conflicting bookings
     * Requirement 8: Booking Creation (conflict detection)
     */
    @Query("""
        SELECT COUNT(*) FROM bookings 
        WHERE labourerId = :labourerId 
        AND workDate BETWEEN :startDate AND :endDate 
        AND status IN ('PENDING', 'ACCEPTED')
    """)
    suspend fun hasConflictingBookingInRange(
        labourerId: String, 
        startDate: Long, 
        endDate: Long
    ): Int
    
    /**
     * Get conflicting bookings for a labourer on a specific date
     * Returns list of conflicting bookings for detailed conflict resolution
     * Requirement 8: Booking Creation (conflict detection)
     */
    @Query("""
        SELECT * FROM bookings 
        WHERE labourerId = :labourerId 
        AND workDate = :date 
        AND status IN ('PENDING', 'ACCEPTED')
        ORDER BY createdAt ASC
    """)
    suspend fun getConflictingBookings(labourerId: String, date: Long): List<Booking>
    
    // ========== Booking Status Updates ==========
    
    /**
     * Update booking status
     * Requirement 11: Booking Management for Labourers
     * Requirement 12: Booking Management for Farmers
     */
    @Query("""
        UPDATE bookings 
        SET status = :status, updatedAt = :timestamp 
        WHERE bookingId = :bookingId
    """)
    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus, timestamp: Long)
    
    /**
     * Accept a booking (update status and set acceptedAt timestamp)
     * Requirement 11: Booking Management for Labourers
     */
    @Query("""
        UPDATE bookings 
        SET status = :status, acceptedAt = :timestamp, updatedAt = :timestamp 
        WHERE bookingId = :bookingId
    """)
    suspend fun acceptBooking(bookingId: String, status: BookingStatus, timestamp: Long)
    
    /**
     * Decline a booking
     * Requirement 11: Booking Management for Labourers
     */
    @Query("""
        UPDATE bookings 
        SET status = 'DECLINED', updatedAt = :timestamp 
        WHERE bookingId = :bookingId
    """)
    suspend fun declineBooking(bookingId: String, timestamp: Long)
    
    /**
     * Complete a booking (update status, set completion time, and actual hours/payment)
     * Requirement 12: Booking Management for Farmers
     * Requirement 14: Work History and Attendance Tracking
     */
    @Query("""
        UPDATE bookings 
        SET status = :status, 
            completedAt = :timestamp, 
            actualHours = :actualHours, 
            actualPayment = :actualPayment, 
            updatedAt = :timestamp 
        WHERE bookingId = :bookingId
    """)
    suspend fun completeBooking(
        bookingId: String, 
        status: BookingStatus, 
        timestamp: Long, 
        actualHours: Int, 
        actualPayment: Int
    )
    
    /**
     * Cancel a booking
     * Requirement 12: Booking Management for Farmers
     */
    @Query("""
        UPDATE bookings 
        SET status = 'CANCELLED', 
            cancelledAt = :timestamp, 
            cancellationReason = :reason,
            updatedAt = :timestamp 
        WHERE bookingId = :bookingId
    """)
    suspend fun cancelBooking(bookingId: String, timestamp: Long, reason: String?)
    
    // ========== Payment Status Updates ==========
    
    /**
     * Update payment status for a booking
     * Requirement 15: Payment Integration
     */
    @Query("""
        UPDATE bookings 
        SET paymentStatus = :paymentStatus, 
            paymentTransactionId = :transactionId, 
            updatedAt = :timestamp 
        WHERE bookingId = :bookingId
    """)
    suspend fun updatePaymentStatus(
        bookingId: String, 
        paymentStatus: PaymentStatus, 
        transactionId: String?, 
        timestamp: Long
    )
    
    /**
     * Mark payment as completed
     * Requirement 15: Payment Integration
     */
    @Query("""
        UPDATE bookings 
        SET paymentStatus = 'PAID', 
            paymentTransactionId = :transactionId, 
            updatedAt = :timestamp 
        WHERE bookingId = :bookingId
    """)
    suspend fun markPaymentCompleted(bookingId: String, transactionId: String, timestamp: Long)
    
    /**
     * Mark payment as failed
     * Requirement 15: Payment Integration
     */
    @Query("""
        UPDATE bookings 
        SET paymentStatus = 'FAILED', 
            updatedAt = :timestamp 
        WHERE bookingId = :bookingId
    """)
    suspend fun markPaymentFailed(bookingId: String, timestamp: Long)
    
    // ========== Analytics and Statistics ==========
    
    /**
     * Get total bookings count for a labourer
     * Requirement 24: Analytics and Reporting
     */
    @Query("SELECT COUNT(*) FROM bookings WHERE labourerId = :labourerId")
    suspend fun getTotalBookingsForLabourer(labourerId: String): Int
    
    /**
     * Get completed bookings count for a labourer
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT COUNT(*) FROM bookings 
        WHERE labourerId = :labourerId AND status = 'COMPLETED'
    """)
    suspend fun getCompletedBookingsCountForLabourer(labourerId: String): Int
    
    /**
     * Get total bookings count for a farmer
     * Requirement 24: Analytics and Reporting
     */
    @Query("SELECT COUNT(*) FROM bookings WHERE farmerId = :farmerId")
    suspend fun getTotalBookingsForFarmer(farmerId: String): Int
    
    /**
     * Get bookings by date range for a labourer
     * Requirement 14: Work History and Attendance Tracking
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT * FROM bookings 
        WHERE labourerId = :labourerId 
        AND workDate BETWEEN :startDate AND :endDate
        ORDER BY workDate DESC
    """)
    suspend fun getLabourerBookingsByDateRange(
        labourerId: String, 
        startDate: Long, 
        endDate: Long
    ): List<Booking>
    
    /**
     * Get bookings by date range for a farmer
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT * FROM bookings 
        WHERE farmerId = :farmerId 
        AND workDate BETWEEN :startDate AND :endDate
        ORDER BY workDate DESC
    """)
    suspend fun getFarmerBookingsByDateRange(
        farmerId: String, 
        startDate: Long, 
        endDate: Long
    ): List<Booking>
    
    /**
     * Get total earnings for a labourer in a date range
     * Requirement 14: Work History and Attendance Tracking
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT SUM(actualPayment) FROM bookings 
        WHERE labourerId = :labourerId 
        AND status = 'COMPLETED'
        AND workDate BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalEarningsForLabourer(
        labourerId: String, 
        startDate: Long, 
        endDate: Long
    ): Int?
    
    /**
     * Get acceptance rate for a labourer
     * Returns count of accepted bookings
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT COUNT(*) FROM bookings 
        WHERE labourerId = :labourerId AND status = 'ACCEPTED'
    """)
    suspend fun getAcceptedBookingsCount(labourerId: String): Int
    
    /**
     * Get declined bookings count for a labourer
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT COUNT(*) FROM bookings 
        WHERE labourerId = :labourerId AND status = 'DECLINED'
    """)
    suspend fun getDeclinedBookingsCount(labourerId: String): Int
    
    /**
     * Get most frequent farmers for a labourer
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT farmerId, farmerName, COUNT(*) as bookingCount 
        FROM bookings 
        WHERE labourerId = :labourerId AND status = 'COMPLETED'
        GROUP BY farmerId 
        ORDER BY bookingCount DESC 
        LIMIT :limit
    """)
    suspend fun getMostFrequentFarmers(labourerId: String, limit: Int = 5): List<FarmerBookingStats>
    
    /**
     * Get bookings that need rating (completed but not yet rated)
     * Requirement 13: Rating and Review System
     */
    @Query("""
        SELECT * FROM bookings 
        WHERE farmerId = :farmerId 
        AND status = 'COMPLETED'
        AND bookingId NOT IN (SELECT bookingId FROM ratings)
        ORDER BY completedAt DESC
    """)
    fun getBookingsNeedingRating(farmerId: String): Flow<List<Booking>>
}

/**
 * Data class for farmer booking statistics
 * Used by getMostFrequentFarmers query
 */
data class FarmerBookingStats(
    val farmerId: String,
    val farmerName: String,
    val bookingCount: Int
)
