package com.raitha.bharosa.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity that caches one Agmarknet mandi price record locally (SQLite).
 *
 * Primary key is a composite fingerprint of state+district+market+commodity+arrivalDate
 * so duplicate API responses never create duplicate rows.
 *
 * [cachedAt] is epoch-ms and is used to expire records older than [CACHE_EXPIRY_MS].
 */
@Entity(tableName = "mandi_prices")
data class MandiEntity(
    @PrimaryKey
    val id: String,                 // "${state}_${district}_${market}_${commodity}_${arrivalDate}"
    val state: String = "",
    val district: String = "",
    val market: String = "",
    val commodity: String = "",
    val variety: String = "",
    val arrivalDate: String = "",
    val minPrice: String = "",
    val maxPrice: String = "",
    val modalPrice: String = "",
    val cachedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /** 24 hours – refresh from network once a day */
        const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1_000L
    }
}
