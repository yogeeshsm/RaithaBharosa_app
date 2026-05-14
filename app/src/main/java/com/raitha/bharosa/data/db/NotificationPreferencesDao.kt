package com.raitha.bharosa.data.db

import androidx.room.*
import com.raitha.bharosa.data.NotificationPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for NotificationPreferences operations
 * 
 * Provides database access methods for:
 * - Notification preferences CRUD operations (Requirement 22: Notification Preferences)
 * - Queries for user notification settings (Requirement 22: Notification Preferences)
 * - Preference updates for specific notification types (Requirement 10: SMS and WhatsApp Notifications)
 * 
 * Part of Phase 2: Data Layer (Room + Firestore)
 * Task 6.6: Create NotificationPreferencesDao
 */
@Dao
interface NotificationPreferencesDao {
    
    // ========== NotificationPreferences CRUD Operations ==========
    
    /**
     * Insert or replace notification preferences for a user
     * Requirement 22: Notification Preferences
     * Requirement 16: Offline Data Persistence
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationPreferences(preferences: NotificationPreferences)
    
    /**
     * Update existing notification preferences
     * Requirement 22: Notification Preferences
     */
    @Update
    suspend fun updateNotificationPreferences(preferences: NotificationPreferences)
    
    /**
     * Delete notification preferences for a user
     * Requirement 23: Security and Privacy (data deletion)
     */
    @Delete
    suspend fun deleteNotificationPreferences(preferences: NotificationPreferences)
    
    /**
     * Get notification preferences for a user with reactive updates
     * Returns Flow for real-time updates in UI
     * Requirement 22: Notification Preferences
     */
    @Query("SELECT * FROM notification_preferences WHERE userId = :userId")
    fun getNotificationPreferences(userId: String): Flow<NotificationPreferences?>
    
    /**
     * Get notification preferences for a user (one-time fetch)
     * Used for non-reactive operations
     * Requirement 22: Notification Preferences
     */
    @Query("SELECT * FROM notification_preferences WHERE userId = :userId")
    suspend fun getNotificationPreferencesOnce(userId: String): NotificationPreferences?
    
    /**
     * Check if notification preferences exist for a user
     * Returns true if preferences exist, false otherwise
     * Requirement 22: Notification Preferences
     */
    @Query("SELECT EXISTS(SELECT 1 FROM notification_preferences WHERE userId = :userId)")
    suspend fun hasNotificationPreferences(userId: String): Boolean
    
    // ========== Channel-Specific Preference Updates ==========
    
    /**
     * Enable or disable SMS notifications for a user
     * Requirement 10: SMS and WhatsApp Notifications
     * Requirement 22: Notification Preferences
     */
    @Query("""
        UPDATE notification_preferences 
        SET smsEnabled = :enabled, updatedAt = :timestamp 
        WHERE userId = :userId
    """)
    suspend fun updateSmsEnabled(userId: String, enabled: Boolean, timestamp: Long)
    
    /**
     * Enable or disable WhatsApp notifications for a user
     * Requirement 10: SMS and WhatsApp Notifications
     * Requirement 22: Notification Preferences
     */
    @Query("""
        UPDATE notification_preferences 
        SET whatsappEnabled = :enabled, updatedAt = :timestamp 
        WHERE userId = :userId
    """)
    suspend fun updateWhatsappEnabled(userId: String, enabled: Boolean, timestamp: Long)
    
    /**
     * Enable or disable in-app notifications for a user
     * Requirement 22: Notification Preferences
     */
    @Query("""
        UPDATE notification_preferences 
        SET inAppEnabled = :enabled, updatedAt = :timestamp 
        WHERE userId = :userId
    """)
    suspend fun updateInAppEnabled(userId: String, enabled: Boolean, timestamp: Long)
    
    // ========== Event-Specific Preference Updates ==========
    
    /**
     * Enable or disable new booking notifications
     * Requirement 8: Booking Creation
     * Requirement 22: Notification Preferences
     */
    @Query("""
        UPDATE notification_preferences 
        SET newBookingsEnabled = :enabled, updatedAt = :timestamp 
        WHERE userId = :userId
    """)
    suspend fun updateNewBookingsEnabled(userId: String, enabled: Boolean, timestamp: Long)
    
    /**
     * Enable or disable booking confirmation notifications
     * Requirement 11: Booking Management for Labourers
     * Requirement 22: Notification Preferences
     */
    @Query("""
        UPDATE notification_preferences 
        SET bookingConfirmationsEnabled = :enabled, updatedAt = :timestamp 
        WHERE userId = :userId
    """)
    suspend fun updateBookingConfirmationsEnabled(userId: String, enabled: Boolean, timestamp: Long)
    
    /**
     * Enable or disable payment confirmation notifications
     * Requirement 15: Payment Integration
     * Requirement 22: Notification Preferences
     */
    @Query("""
        UPDATE notification_preferences 
        SET paymentConfirmationsEnabled = :enabled, updatedAt = :timestamp 
        WHERE userId = :userId
    """)
    suspend fun updatePaymentConfirmationsEnabled(userId: String, enabled: Boolean, timestamp: Long)
    
    /**
     * Enable or disable rating received notifications
     * Requirement 13: Rating and Review System
     * Requirement 22: Notification Preferences
     */
    @Query("""
        UPDATE notification_preferences 
        SET ratingsReceivedEnabled = :enabled, updatedAt = :timestamp 
        WHERE userId = :userId
    """)
    suspend fun updateRatingsReceivedEnabled(userId: String, enabled: Boolean, timestamp: Long)
    
    // ========== Bulk Preference Updates ==========
    
    /**
     * Enable all notification channels for a user
     * Requirement 22: Notification Preferences
     */
    @Query("""
        UPDATE notification_preferences 
        SET smsEnabled = 1, 
            whatsappEnabled = 1, 
            inAppEnabled = 1, 
            updatedAt = :timestamp 
        WHERE userId = :userId
    """)
    suspend fun enableAllChannels(userId: String, timestamp: Long)
    
    /**
     * Disable all notification channels for a user
     * Requirement 22: Notification Preferences
     */
    @Query("""
        UPDATE notification_preferences 
        SET smsEnabled = 0, 
            whatsappEnabled = 0, 
            inAppEnabled = 0, 
            updatedAt = :timestamp 
        WHERE userId = :userId
    """)
    suspend fun disableAllChannels(userId: String, timestamp: Long)
    
    /**
     * Enable all event types for a user
     * Requirement 22: Notification Preferences
     */
    @Query("""
        UPDATE notification_preferences 
        SET newBookingsEnabled = 1, 
            bookingConfirmationsEnabled = 1, 
            paymentConfirmationsEnabled = 1, 
            ratingsReceivedEnabled = 1, 
            updatedAt = :timestamp 
        WHERE userId = :userId
    """)
    suspend fun enableAllEventTypes(userId: String, timestamp: Long)
    
    /**
     * Disable all event types for a user
     * Requirement 22: Notification Preferences
     */
    @Query("""
        UPDATE notification_preferences 
        SET newBookingsEnabled = 0, 
            bookingConfirmationsEnabled = 0, 
            paymentConfirmationsEnabled = 0, 
            ratingsReceivedEnabled = 0, 
            updatedAt = :timestamp 
        WHERE userId = :userId
    """)
    suspend fun disableAllEventTypes(userId: String, timestamp: Long)
    
    /**
     * Reset notification preferences to default values
     * All notifications enabled by default
     * Requirement 22: Notification Preferences
     */
    @Query("""
        UPDATE notification_preferences 
        SET smsEnabled = 1, 
            whatsappEnabled = 1, 
            inAppEnabled = 1, 
            newBookingsEnabled = 1, 
            bookingConfirmationsEnabled = 1, 
            paymentConfirmationsEnabled = 1, 
            ratingsReceivedEnabled = 1, 
            updatedAt = :timestamp 
        WHERE userId = :userId
    """)
    suspend fun resetToDefaults(userId: String, timestamp: Long)
    
    // ========== Preference Queries ==========
    
    /**
     * Check if SMS notifications are enabled for a user
     * Requirement 10: SMS and WhatsApp Notifications
     * Requirement 22: Notification Preferences
     */
    @Query("SELECT smsEnabled FROM notification_preferences WHERE userId = :userId")
    suspend fun isSmsEnabled(userId: String): Boolean?
    
    /**
     * Check if WhatsApp notifications are enabled for a user
     * Requirement 10: SMS and WhatsApp Notifications
     * Requirement 22: Notification Preferences
     */
    @Query("SELECT whatsappEnabled FROM notification_preferences WHERE userId = :userId")
    suspend fun isWhatsappEnabled(userId: String): Boolean?
    
    /**
     * Check if in-app notifications are enabled for a user
     * Requirement 22: Notification Preferences
     */
    @Query("SELECT inAppEnabled FROM notification_preferences WHERE userId = :userId")
    suspend fun isInAppEnabled(userId: String): Boolean?
    
    /**
     * Check if new booking notifications are enabled for a user
     * Requirement 8: Booking Creation
     * Requirement 22: Notification Preferences
     */
    @Query("SELECT newBookingsEnabled FROM notification_preferences WHERE userId = :userId")
    suspend fun isNewBookingsEnabled(userId: String): Boolean?
    
    /**
     * Check if booking confirmation notifications are enabled for a user
     * Requirement 11: Booking Management for Labourers
     * Requirement 22: Notification Preferences
     */
    @Query("SELECT bookingConfirmationsEnabled FROM notification_preferences WHERE userId = :userId")
    suspend fun isBookingConfirmationsEnabled(userId: String): Boolean?
    
    /**
     * Check if payment confirmation notifications are enabled for a user
     * Requirement 15: Payment Integration
     * Requirement 22: Notification Preferences
     */
    @Query("SELECT paymentConfirmationsEnabled FROM notification_preferences WHERE userId = :userId")
    suspend fun isPaymentConfirmationsEnabled(userId: String): Boolean?
    
    /**
     * Check if rating received notifications are enabled for a user
     * Requirement 13: Rating and Review System
     * Requirement 22: Notification Preferences
     */
    @Query("SELECT ratingsReceivedEnabled FROM notification_preferences WHERE userId = :userId")
    suspend fun isRatingsReceivedEnabled(userId: String): Boolean?
    
    /**
     * Get users with SMS notifications enabled
     * Used for bulk notification operations
     * Requirement 10: SMS and WhatsApp Notifications
     */
    @Query("SELECT * FROM notification_preferences WHERE smsEnabled = 1")
    suspend fun getUsersWithSmsEnabled(): List<NotificationPreferences>
    
    /**
     * Get users with WhatsApp notifications enabled
     * Used for bulk notification operations
     * Requirement 10: SMS and WhatsApp Notifications
     */
    @Query("SELECT * FROM notification_preferences WHERE whatsappEnabled = 1")
    suspend fun getUsersWithWhatsappEnabled(): List<NotificationPreferences>
    
    /**
     * Get users with in-app notifications enabled
     * Used for bulk notification operations
     * Requirement 22: Notification Preferences
     */
    @Query("SELECT * FROM notification_preferences WHERE inAppEnabled = 1")
    suspend fun getUsersWithInAppEnabled(): List<NotificationPreferences>
    
    /**
     * Get users with new booking notifications enabled
     * Used for targeted notification delivery
     * Requirement 8: Booking Creation
     */
    @Query("SELECT * FROM notification_preferences WHERE newBookingsEnabled = 1")
    suspend fun getUsersWithNewBookingsEnabled(): List<NotificationPreferences>
    
    /**
     * Get users with all notifications disabled
     * Used for identifying users who opted out
     * Requirement 22: Notification Preferences
     */
    @Query("""
        SELECT * FROM notification_preferences 
        WHERE smsEnabled = 0 
        AND whatsappEnabled = 0 
        AND inAppEnabled = 0
    """)
    suspend fun getUsersWithAllNotificationsDisabled(): List<NotificationPreferences>
    
    /**
     * Check if user should receive notification for a specific event type
     * Combines channel and event type checks
     * Requirement 22: Notification Preferences
     */
    @Query("""
        SELECT 
            CASE 
                WHEN :channel = 'SMS' THEN smsEnabled
                WHEN :channel = 'WHATSAPP' THEN whatsappEnabled
                WHEN :channel = 'IN_APP' THEN inAppEnabled
                ELSE 0
            END as channelEnabled,
            CASE 
                WHEN :eventType = 'NEW_BOOKING' THEN newBookingsEnabled
                WHEN :eventType = 'BOOKING_CONFIRMATION' THEN bookingConfirmationsEnabled
                WHEN :eventType = 'PAYMENT_CONFIRMATION' THEN paymentConfirmationsEnabled
                WHEN :eventType = 'RATING_RECEIVED' THEN ratingsReceivedEnabled
                ELSE 0
            END as eventEnabled
        FROM notification_preferences 
        WHERE userId = :userId
    """)
    suspend fun getNotificationEligibility(
        userId: String, 
        channel: String, 
        eventType: String
    ): NotificationEligibility?
    
    // ========== Analytics and Statistics ==========
    
    /**
     * Get count of users with SMS enabled
     * Requirement 24: Analytics and Reporting
     */
    @Query("SELECT COUNT(*) FROM notification_preferences WHERE smsEnabled = 1")
    suspend fun getSmsEnabledCount(): Int
    
    /**
     * Get count of users with WhatsApp enabled
     * Requirement 24: Analytics and Reporting
     */
    @Query("SELECT COUNT(*) FROM notification_preferences WHERE whatsappEnabled = 1")
    suspend fun getWhatsappEnabledCount(): Int
    
    /**
     * Get count of users with in-app notifications enabled
     * Requirement 24: Analytics and Reporting
     */
    @Query("SELECT COUNT(*) FROM notification_preferences WHERE inAppEnabled = 1")
    suspend fun getInAppEnabledCount(): Int
    
    /**
     * Get notification preference statistics
     * Returns comprehensive statistics about notification preferences
     * Requirement 24: Analytics and Reporting
     */
    @Query("""
        SELECT 
            COUNT(*) as totalUsers,
            SUM(CASE WHEN smsEnabled = 1 THEN 1 ELSE 0 END) as smsEnabledCount,
            SUM(CASE WHEN whatsappEnabled = 1 THEN 1 ELSE 0 END) as whatsappEnabledCount,
            SUM(CASE WHEN inAppEnabled = 1 THEN 1 ELSE 0 END) as inAppEnabledCount,
            SUM(CASE WHEN newBookingsEnabled = 1 THEN 1 ELSE 0 END) as newBookingsEnabledCount,
            SUM(CASE WHEN bookingConfirmationsEnabled = 1 THEN 1 ELSE 0 END) as bookingConfirmationsEnabledCount,
            SUM(CASE WHEN paymentConfirmationsEnabled = 1 THEN 1 ELSE 0 END) as paymentConfirmationsEnabledCount,
            SUM(CASE WHEN ratingsReceivedEnabled = 1 THEN 1 ELSE 0 END) as ratingsReceivedEnabledCount
        FROM notification_preferences
    """)
    suspend fun getNotificationStatistics(): NotificationStatistics?
    
    /**
     * Get most recently updated notification preferences
     * Used for monitoring recent preference changes
     * Requirement 22: Notification Preferences
     */
    @Query("SELECT * FROM notification_preferences ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentlyUpdatedPreferences(limit: Int = 10): List<NotificationPreferences>
    
    /**
     * Get notification preferences last updated before a specific timestamp
     * Used for identifying stale preferences
     */
    @Query("SELECT * FROM notification_preferences WHERE updatedAt < :timestamp")
    suspend fun getPreferencesUpdatedBefore(timestamp: Long): List<NotificationPreferences>
    
    // ========== Bulk Operations ==========
    
    /**
     * Insert multiple notification preferences
     * Used for batch operations or data migration
     * Requirement 16: Offline Data Persistence
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationPreferencesBatch(preferencesList: List<NotificationPreferences>)
    
    /**
     * Delete notification preferences for a user by userId
     * Used for account deletion
     * Requirement 23: Security and Privacy (data deletion)
     */
    @Query("DELETE FROM notification_preferences WHERE userId = :userId")
    suspend fun deleteNotificationPreferencesByUserId(userId: String)
    
    /**
     * Delete all notification preferences
     * Used for data cleanup or testing
     */
    @Query("DELETE FROM notification_preferences")
    suspend fun deleteAllNotificationPreferences()
    
    /**
     * Get all notification preferences (for admin/debugging purposes)
     */
    @Query("SELECT * FROM notification_preferences ORDER BY updatedAt DESC")
    fun getAllNotificationPreferences(): Flow<List<NotificationPreferences>>
    
    /**
     * Get all notification preferences (one-time fetch)
     */
    @Query("SELECT * FROM notification_preferences ORDER BY updatedAt DESC")
    suspend fun getAllNotificationPreferencesOnce(): List<NotificationPreferences>
    
    /**
     * Get total count of notification preferences in the system
     * Requirement 24: Analytics and Reporting
     */
    @Query("SELECT COUNT(*) FROM notification_preferences")
    suspend fun getTotalPreferencesCount(): Int
}

/**
 * Data class for notification eligibility check
 * Used by getNotificationEligibility query
 */
data class NotificationEligibility(
    val channelEnabled: Boolean,
    val eventEnabled: Boolean
) {
    /**
     * Check if notification should be sent
     * Both channel and event type must be enabled
     */
    fun shouldSendNotification(): Boolean = channelEnabled && eventEnabled
}

/**
 * Data class for comprehensive notification preference statistics
 * Used by getNotificationStatistics query
 */
data class NotificationStatistics(
    val totalUsers: Int,
    val smsEnabledCount: Int,
    val whatsappEnabledCount: Int,
    val inAppEnabledCount: Int,
    val newBookingsEnabledCount: Int,
    val bookingConfirmationsEnabledCount: Int,
    val paymentConfirmationsEnabledCount: Int,
    val ratingsReceivedEnabledCount: Int
) {
    /**
     * Calculate percentage of users with SMS enabled
     */
    fun getSmsEnabledPercentage(): Double = 
        if (totalUsers > 0) (smsEnabledCount.toDouble() / totalUsers) * 100 else 0.0
    
    /**
     * Calculate percentage of users with WhatsApp enabled
     */
    fun getWhatsappEnabledPercentage(): Double = 
        if (totalUsers > 0) (whatsappEnabledCount.toDouble() / totalUsers) * 100 else 0.0
    
    /**
     * Calculate percentage of users with in-app notifications enabled
     */
    fun getInAppEnabledPercentage(): Double = 
        if (totalUsers > 0) (inAppEnabledCount.toDouble() / totalUsers) * 100 else 0.0
}
