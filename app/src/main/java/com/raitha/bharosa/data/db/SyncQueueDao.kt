package com.raitha.bharosa.data.db

import androidx.room.*
import com.raitha.bharosa.data.SyncQueueItem
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Sync Queue operations
 * 
 * Provides database access methods for:
 * - Sync queue CRUD operations (Requirement 16: Offline Data Persistence)
 * - Retry count management (Requirement 16: Offline Data Persistence)
 * - Pending sync item queries (Requirement 16: Offline Data Persistence)
 * 
 * The sync queue tracks operations that need to be synced to Firestore
 * when connectivity is restored, enabling offline-first functionality.
 * 
 * Part of Phase 2: Data Layer (Room + Firestore)
 */
@Dao
interface SyncQueueDao {
    
    // ========== Sync Queue CRUD Operations ==========
    
    /**
     * Insert a new sync queue item
     * Used when an operation needs to be queued for sync
     * Requirement 16: Offline Data Persistence
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(item: SyncQueueItem): Long
    
    /**
     * Insert multiple sync queue items
     * Used for batch operations
     * Requirement 16: Offline Data Persistence
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItems(items: List<SyncQueueItem>): List<Long>
    
    /**
     * Update an existing sync queue item
     * Used when updating retry count or last attempt timestamp
     * Requirement 16: Offline Data Persistence
     */
    @Update
    suspend fun updateSyncItem(item: SyncQueueItem)
    
    /**
     * Delete a sync queue item
     * Used when sync is successful or item should be removed
     * Requirement 16: Offline Data Persistence
     */
    @Delete
    suspend fun deleteSyncItem(item: SyncQueueItem)
    
    /**
     * Delete sync queue item by ID
     * Requirement 16: Offline Data Persistence
     */
    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteSyncItemById(id: Int)
    
    /**
     * Delete multiple sync queue items
     * Used for batch cleanup after successful sync
     * Requirement 16: Offline Data Persistence
     */
    @Delete
    suspend fun deleteSyncItems(items: List<SyncQueueItem>)
    
    /**
     * Delete all sync queue items for a specific entity
     * Used when an entity is deleted or no longer needs sync
     * Requirement 16: Offline Data Persistence
     */
    @Query("DELETE FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteSyncItemsByEntity(entityType: String, entityId: String)
    
    // ========== Pending Sync Item Queries ==========
    
    /**
     * Get all pending sync items with reactive updates
     * Returns Flow for real-time updates in sync manager
     * Requirement 16: Offline Data Persistence
     */
    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    fun getAllSyncItems(): Flow<List<SyncQueueItem>>
    
    /**
     * Get all pending sync items (one-time fetch)
     * Ordered by creation time (oldest first) for FIFO processing
     * Requirement 16: Offline Data Persistence
     */
    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getAllSyncItemsOnce(): List<SyncQueueItem>
    
    /**
     * Get sync items by entity type
     * Useful for syncing specific entity types first
     * Requirement 16: Offline Data Persistence
     */
    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType ORDER BY createdAt ASC")
    suspend fun getSyncItemsByType(entityType: String): List<SyncQueueItem>
    
    /**
     * Get sync items by entity type with reactive updates
     * Requirement 16: Offline Data Persistence
     */
    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType ORDER BY createdAt ASC")
    fun getSyncItemsByTypeFlow(entityType: String): Flow<List<SyncQueueItem>>
    
    /**
     * Get sync item by entity ID and type
     * Used to check if an entity already has a pending sync operation
     * Requirement 16: Offline Data Persistence
     */
    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId LIMIT 1")
    suspend fun getSyncItemByEntity(entityType: String, entityId: String): SyncQueueItem?
    
    /**
     * Get sync item by ID
     * Requirement 16: Offline Data Persistence
     */
    @Query("SELECT * FROM sync_queue WHERE id = :id")
    suspend fun getSyncItemById(id: Int): SyncQueueItem?
    
    // ========== Retry Count Management ==========
    
    /**
     * Increment retry count for a sync item
     * Used when a sync attempt fails
     * Requirement 16: Offline Data Persistence
     */
    @Query("""
        UPDATE sync_queue 
        SET retryCount = retryCount + 1, 
            lastAttempt = :timestamp 
        WHERE id = :id
    """)
    suspend fun incrementRetryCount(id: Int, timestamp: Long)
    
    /**
     * Reset retry count for a sync item
     * Used when retry logic needs to be reset
     * Requirement 16: Offline Data Persistence
     */
    @Query("UPDATE sync_queue SET retryCount = 0, lastAttempt = NULL WHERE id = :id")
    suspend fun resetRetryCount(id: Int)
    
    /**
     * Update last attempt timestamp
     * Used to track when the last sync attempt was made
     * Requirement 16: Offline Data Persistence
     */
    @Query("UPDATE sync_queue SET lastAttempt = :timestamp WHERE id = :id")
    suspend fun updateLastAttempt(id: Int, timestamp: Long)
    
    /**
     * Get sync items that have exceeded max retry count
     * Used to identify items that need special handling or removal
     * Requirement 16: Offline Data Persistence
     */
    @Query("SELECT * FROM sync_queue WHERE retryCount >= :maxRetries ORDER BY createdAt ASC")
    suspend fun getFailedSyncItems(maxRetries: Int = 5): List<SyncQueueItem>
    
    /**
     * Get sync items that are ready for retry
     * Filters items that haven't exceeded max retries
     * Requirement 16: Offline Data Persistence
     */
    @Query("SELECT * FROM sync_queue WHERE retryCount < :maxRetries ORDER BY createdAt ASC")
    suspend fun getRetryableSyncItems(maxRetries: Int = 5): List<SyncQueueItem>
    
    /**
     * Get sync items that haven't been attempted yet
     * Useful for prioritizing new items over retries
     * Requirement 16: Offline Data Persistence
     */
    @Query("SELECT * FROM sync_queue WHERE lastAttempt IS NULL ORDER BY createdAt ASC")
    suspend fun getUntriedSyncItems(): List<SyncQueueItem>
    
    /**
     * Get sync items that need retry based on time threshold
     * Returns items where last attempt was before the threshold timestamp
     * Requirement 16: Offline Data Persistence
     */
    @Query("""
        SELECT * FROM sync_queue 
        WHERE retryCount < :maxRetries 
        AND (lastAttempt IS NULL OR lastAttempt < :thresholdTimestamp)
        ORDER BY createdAt ASC
    """)
    suspend fun getSyncItemsForRetry(maxRetries: Int = 5, thresholdTimestamp: Long): List<SyncQueueItem>
    
    // ========== Statistics and Monitoring ==========
    
    /**
     * Get count of pending sync items
     * Requirement 16: Offline Data Persistence
     */
    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getPendingSyncCount(): Int
    
    /**
     * Get count of pending sync items with reactive updates
     * Useful for displaying sync status in UI
     * Requirement 16: Offline Data Persistence
     */
    @Query("SELECT COUNT(*) FROM sync_queue")
    fun getPendingSyncCountFlow(): Flow<Int>
    
    /**
     * Get count of sync items by entity type
     * Requirement 16: Offline Data Persistence
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE entityType = :entityType")
    suspend fun getSyncCountByType(entityType: String): Int
    
    /**
     * Get count of failed sync items (exceeded max retries)
     * Requirement 16: Offline Data Persistence
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE retryCount >= :maxRetries")
    suspend fun getFailedSyncCount(maxRetries: Int = 5): Int
    
    /**
     * Get oldest sync item timestamp
     * Useful for monitoring sync queue age
     * Requirement 16: Offline Data Persistence
     */
    @Query("SELECT MIN(createdAt) FROM sync_queue")
    suspend fun getOldestSyncItemTimestamp(): Long?
    
    /**
     * Check if entity has pending sync operation
     * Returns true if entity has any pending sync items
     * Requirement 16: Offline Data Persistence
     */
    @Query("SELECT COUNT(*) > 0 FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun hasPendingSync(entityType: String, entityId: String): Boolean
    
    // ========== Cleanup Operations ==========
    
    /**
     * Delete all sync items
     * Used for clearing the entire queue (use with caution)
     * Requirement 16: Offline Data Persistence
     */
    @Query("DELETE FROM sync_queue")
    suspend fun deleteAllSyncItems()
    
    /**
     * Delete sync items older than a specific timestamp
     * Used for cleaning up stale items
     * Requirement 16: Offline Data Persistence
     */
    @Query("DELETE FROM sync_queue WHERE createdAt < :timestamp")
    suspend fun deleteOldSyncItems(timestamp: Long)
    
    /**
     * Delete failed sync items (exceeded max retries)
     * Used to clean up items that can't be synced
     * Requirement 16: Offline Data Persistence
     */
    @Query("DELETE FROM sync_queue WHERE retryCount >= :maxRetries")
    suspend fun deleteFailedSyncItems(maxRetries: Int = 5)
    
    /**
     * Delete sync items by operation type
     * Useful for cleaning up specific operation types
     * Requirement 16: Offline Data Persistence
     */
    @Query("DELETE FROM sync_queue WHERE operation = :operation")
    suspend fun deleteSyncItemsByOperation(operation: String)
}
