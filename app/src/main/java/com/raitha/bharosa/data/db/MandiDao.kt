package com.raitha.bharosa.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data-Access Object for mandi price rows.
 *
 * All heavy queries return a [Flow] so the UI reacts automatically whenever
 * the cache is refreshed.
 */
@Dao
interface MandiDao {

    // ──────────────────────────── INSERT / UPSERT ────────────────────────────

    /**
     * Insert or fully replace records that arrive from the network.
     * REPLACE strategy updates [cachedAt] so the expiry timer resets.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<MandiEntity>)

    // ──────────────────────────── QUERIES ────────────────────────────────────

    /** Live prices for a commodity in a given state (most recent first). */
    @Query("""
        SELECT * FROM mandi_prices
        WHERE commodity LIKE '%' || :commodity || '%'
          AND state LIKE '%' || :state || '%'
        ORDER BY arrivalDate DESC
        LIMIT :limit
    """)
    fun observeLivePrices(commodity: String, state: String, limit: Int = 50): Flow<List<MandiEntity>>

    /** One-shot fetch — used for offline-fallback in the repository. */
    @Query("""
        SELECT * FROM mandi_prices
        WHERE commodity LIKE '%' || :commodity || '%'
          AND state LIKE '%' || :state || '%'
        ORDER BY arrivalDate DESC
        LIMIT :limit
    """)
    suspend fun getLivePrices(commodity: String, state: String, limit: Int = 50): List<MandiEntity>

    /** Nearby markets for a commodity filtered by district. */
    @Query("""
        SELECT * FROM mandi_prices
        WHERE commodity LIKE '%' || :commodity || '%'
          AND state   LIKE '%' || :state   || '%'
          AND district LIKE '%' || :district || '%'
        ORDER BY arrivalDate DESC
        LIMIT :limit
    """)
    suspend fun getNearbyPrices(
        commodity: String,
        state: String,
        district: String,
        limit: Int = 50
    ): List<MandiEntity>

    /** Historical trend data – more records for charting. */
    @Query("""
        SELECT * FROM mandi_prices
        WHERE commodity LIKE '%' || :commodity || '%'
          AND state LIKE '%' || :state || '%'
        ORDER BY arrivalDate DESC
        LIMIT :limit
    """)
    suspend fun getHistoricalPrices(commodity: String, state: String, limit: Int = 100): List<MandiEntity>

    // ──────────────────────────── CACHE MANAGEMENT ───────────────────────────

    /** Returns the newest [cachedAt] timestamp for a commodity+state combo. */
    @Query("""
        SELECT MAX(cachedAt) FROM mandi_prices
        WHERE commodity LIKE '%' || :commodity || '%'
          AND state LIKE '%' || :state || '%'
    """)
    suspend fun getLatestCacheTime(commodity: String, state: String): Long?

    /** Delete rows older than [expiryMs] to keep the DB lean. */
    @Query("DELETE FROM mandi_prices WHERE cachedAt < :expiryMs")
    suspend fun deleteExpiredRecords(expiryMs: Long)

    /** Full wipe (used on sign-out or manual cache clear). */
    @Query("DELETE FROM mandi_prices")
    suspend fun clearAll()

    /** Count of rows currently in the table. */
    @Query("SELECT COUNT(*) FROM mandi_prices")
    suspend fun count(): Int
}
