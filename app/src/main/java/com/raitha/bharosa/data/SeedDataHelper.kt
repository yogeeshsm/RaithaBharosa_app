package com.raitha.bharosa.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Helper class to seed sample labour data to Firestore
 * This eliminates the need for external Node.js scripts
 */
object SeedDataHelper {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * Seed 12 sample labourer profiles to Firestore
     * Call this once to populate the database with test data
     */
    suspend fun seedLabourData(): Result<String> {
        return try {
            val labourers = getSampleLabourers()
            val batch = firestore.batch()
            
            labourers.forEach { labourer ->
                val docRef = firestore.collection("labourer_profiles").document(labourer.userId)
                batch.set(docRef, labourer.toMap())
            }
            
            batch.commit().await()
            
            Result.success("Successfully seeded ${labourers.size} labourer profiles!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if data already exists
     */
    suspend fun isDataSeeded(): Boolean {
        return try {
            val snapshot = firestore.collection("labourer_profiles")
                .limit(1)
                .get()
                .await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Clear all seeded data (for testing)
     */
    suspend fun clearSeedData(): Result<String> {
        return try {
            val snapshot = firestore.collection("labourer_profiles")
                .whereGreaterThanOrEqualTo("userId", "labourer_001")
                .whereLessThanOrEqualTo("userId", "labourer_012")
                .get()
                .await()
            
            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            
            Result.success("Successfully cleared ${snapshot.size()} labourer profiles!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getSampleLabourers(): List<SampleLabourer> {
        val currentTime = System.currentTimeMillis()
        
        return listOf(
            SampleLabourer(
                userId = "labourer_001",
                name = "Ravi Kumar",
                phoneNumber = "9876543210",
                age = 35,
                gender = "Male",
                village = "Kanakapura",
                district = "Ramanagara",
                latitude = 12.5489,
                longitude = 77.4253,
                skills = listOf("PLOUGHING", "SOWING", "HARVESTING"),
                experienceYears = mapOf("PLOUGHING" to 10, "SOWING" to 8, "HARVESTING" to 12),
                pricingType = "DAILY_WAGE",
                dailyWage = 600,
                hourlyRate = null,
                availabilityStatus = "AVAILABLE",
                averageRating = 4.5,
                totalRatings = 23,
                completedBookings = 45,
                createdAt = currentTime,
                updatedAt = currentTime,
                lastAvailabilityUpdate = currentTime
            ),
            SampleLabourer(
                userId = "labourer_002",
                name = "Lakshmi Devi",
                phoneNumber = "9876543211",
                age = 28,
                gender = "Female",
                village = "Bidadi",
                district = "Ramanagara",
                latitude = 12.7953,
                longitude = 77.3831,
                skills = listOf("WEEDING", "HARVESTING", "TRANSPLANTING"),
                experienceYears = mapOf("WEEDING" to 6, "HARVESTING" to 7, "TRANSPLANTING" to 5),
                pricingType = "DAILY_WAGE",
                dailyWage = 500,
                hourlyRate = null,
                availabilityStatus = "AVAILABLE",
                averageRating = 4.8,
                totalRatings = 31,
                completedBookings = 62,
                createdAt = currentTime,
                updatedAt = currentTime,
                lastAvailabilityUpdate = currentTime
            ),
            SampleLabourer(
                userId = "labourer_003",
                name = "Manjunath Gowda",
                phoneNumber = "9876543212",
                age = 42,
                gender = "Male",
                village = "Harohalli",
                district = "Ramanagara",
                latitude = 12.7500,
                longitude = 77.4167,
                skills = listOf("PLOUGHING", "IRRIGATION", "PESTICIDE_SPRAYING"),
                experienceYears = mapOf("PLOUGHING" to 15, "IRRIGATION" to 12, "PESTICIDE_SPRAYING" to 10),
                pricingType = "DAILY_WAGE",
                dailyWage = 700,
                hourlyRate = null,
                availabilityStatus = "AVAILABLE",
                averageRating = 4.6,
                totalRatings = 28,
                completedBookings = 58,
                createdAt = currentTime,
                updatedAt = currentTime,
                lastAvailabilityUpdate = currentTime
            ),
            SampleLabourer(
                userId = "labourer_004",
                name = "Suma Bai",
                phoneNumber = "9876543213",
                age = 32,
                gender = "Female",
                village = "Channapatna",
                district = "Ramanagara",
                latitude = 12.6515,
                longitude = 77.2066,
                skills = listOf("WEEDING", "HARVESTING", "SOWING"),
                experienceYears = mapOf("WEEDING" to 8, "HARVESTING" to 9, "SOWING" to 7),
                pricingType = "HOURLY_RATE",
                dailyWage = null,
                hourlyRate = 70,
                availabilityStatus = "AVAILABLE",
                averageRating = 4.7,
                totalRatings = 19,
                completedBookings = 38,
                createdAt = currentTime,
                updatedAt = currentTime,
                lastAvailabilityUpdate = currentTime
            ),
            SampleLabourer(
                userId = "labourer_005",
                name = "Nagaraj Reddy",
                phoneNumber = "9876543214",
                age = 38,
                gender = "Male",
                village = "Magadi",
                district = "Ramanagara",
                latitude = 12.9577,
                longitude = 77.2244,
                skills = listOf("PLOUGHING", "HARVESTING", "THRESHING"),
                experienceYears = mapOf("PLOUGHING" to 12, "HARVESTING" to 13, "THRESHING" to 10),
                pricingType = "DAILY_WAGE",
                dailyWage = 650,
                hourlyRate = null,
                availabilityStatus = "AVAILABLE",
                averageRating = 4.4,
                totalRatings = 25,
                completedBookings = 51,
                createdAt = currentTime,
                updatedAt = currentTime,
                lastAvailabilityUpdate = currentTime
            ),
            SampleLabourer(
                userId = "labourer_006",
                name = "Kavitha Rani",
                phoneNumber = "9876543215",
                age = 26,
                gender = "Female",
                village = "Ramanagara",
                district = "Ramanagara",
                latitude = 12.7175,
                longitude = 77.2806,
                skills = listOf("TRANSPLANTING", "WEEDING", "HARVESTING"),
                experienceYears = mapOf("TRANSPLANTING" to 5, "WEEDING" to 6, "HARVESTING" to 5),
                pricingType = "HOURLY_RATE",
                dailyWage = null,
                hourlyRate = 65,
                availabilityStatus = "AVAILABLE",
                averageRating = 4.9,
                totalRatings = 15,
                completedBookings = 29,
                createdAt = currentTime,
                updatedAt = currentTime,
                lastAvailabilityUpdate = currentTime
            ),
            SampleLabourer(
                userId = "labourer_007",
                name = "Basavaraj Patil",
                phoneNumber = "9876543216",
                age = 45,
                gender = "Male",
                village = "Kanakapura",
                district = "Ramanagara",
                latitude = 12.5400,
                longitude = 77.4200,
                skills = listOf("IRRIGATION", "PESTICIDE_SPRAYING", "FERTILIZER_APPLICATION"),
                experienceYears = mapOf("IRRIGATION" to 18, "PESTICIDE_SPRAYING" to 15, "FERTILIZER_APPLICATION" to 12),
                pricingType = "DAILY_WAGE",
                dailyWage = 750,
                hourlyRate = null,
                availabilityStatus = "AVAILABLE",
                averageRating = 4.7,
                totalRatings = 34,
                completedBookings = 72,
                createdAt = currentTime,
                updatedAt = currentTime,
                lastAvailabilityUpdate = currentTime
            ),
            SampleLabourer(
                userId = "labourer_008",
                name = "Geetha Kumari",
                phoneNumber = "9876543217",
                age = 30,
                gender = "Female",
                village = "Bidadi",
                district = "Ramanagara",
                latitude = 12.8000,
                longitude = 77.3900,
                skills = listOf("SOWING", "WEEDING", "HARVESTING"),
                experienceYears = mapOf("SOWING" to 7, "WEEDING" to 8, "HARVESTING" to 7),
                pricingType = "DAILY_WAGE",
                dailyWage = 550,
                hourlyRate = null,
                availabilityStatus = "BOOKED",
                futureAvailability = currentTime + (3 * 24 * 60 * 60 * 1000), // Available in 3 days
                averageRating = 4.6,
                totalRatings = 22,
                completedBookings = 44,
                createdAt = currentTime,
                updatedAt = currentTime,
                lastAvailabilityUpdate = currentTime
            ),
            SampleLabourer(
                userId = "labourer_009",
                name = "Suresh Babu",
                phoneNumber = "9876543218",
                age = 40,
                gender = "Male",
                village = "Harohalli",
                district = "Ramanagara",
                latitude = 12.7600,
                longitude = 77.4100,
                skills = listOf("PLOUGHING", "THRESHING", "HARVESTING"),
                experienceYears = mapOf("PLOUGHING" to 14, "THRESHING" to 12, "HARVESTING" to 15),
                pricingType = "DAILY_WAGE",
                dailyWage = 680,
                hourlyRate = null,
                availabilityStatus = "AVAILABLE",
                averageRating = 4.5,
                totalRatings = 27,
                completedBookings = 56,
                createdAt = currentTime,
                updatedAt = currentTime,
                lastAvailabilityUpdate = currentTime
            ),
            SampleLabourer(
                userId = "labourer_010",
                name = "Anitha Devi",
                phoneNumber = "9876543219",
                age = 29,
                gender = "Female",
                village = "Channapatna",
                district = "Ramanagara",
                latitude = 12.6600,
                longitude = 77.2100,
                skills = listOf("TRANSPLANTING", "WEEDING", "HARVESTING"),
                experienceYears = mapOf("TRANSPLANTING" to 6, "WEEDING" to 7, "HARVESTING" to 6),
                pricingType = "HOURLY_RATE",
                dailyWage = null,
                hourlyRate = 68,
                availabilityStatus = "AVAILABLE",
                averageRating = 4.8,
                totalRatings = 18,
                completedBookings = 35,
                createdAt = currentTime,
                updatedAt = currentTime,
                lastAvailabilityUpdate = currentTime
            ),
            SampleLabourer(
                userId = "labourer_011",
                name = "Venkatesh Gowda",
                phoneNumber = "9876543220",
                age = 36,
                gender = "Male",
                village = "Magadi",
                district = "Ramanagara",
                latitude = 12.9500,
                longitude = 77.2300,
                skills = listOf("IRRIGATION", "PESTICIDE_SPRAYING", "PLOUGHING"),
                experienceYears = mapOf("IRRIGATION" to 11, "PESTICIDE_SPRAYING" to 9, "PLOUGHING" to 10),
                pricingType = "DAILY_WAGE",
                dailyWage = 720,
                hourlyRate = null,
                availabilityStatus = "AVAILABLE",
                averageRating = 4.6,
                totalRatings = 24,
                completedBookings = 49,
                createdAt = currentTime,
                updatedAt = currentTime,
                lastAvailabilityUpdate = currentTime
            ),
            SampleLabourer(
                userId = "labourer_012",
                name = "Pushpa Rani",
                phoneNumber = "9876543221",
                age = 33,
                gender = "Female",
                village = "Ramanagara",
                district = "Ramanagara",
                latitude = 12.7200,
                longitude = 77.2850,
                skills = listOf("WEEDING", "HARVESTING", "SOWING"),
                experienceYears = mapOf("WEEDING" to 9, "HARVESTING" to 10, "SOWING" to 8),
                pricingType = "DAILY_WAGE",
                dailyWage = 520,
                hourlyRate = null,
                availabilityStatus = "AVAILABLE",
                averageRating = 4.7,
                totalRatings = 21,
                completedBookings = 42,
                createdAt = currentTime,
                updatedAt = currentTime,
                lastAvailabilityUpdate = currentTime
            )
        )
    }
    
    private data class SampleLabourer(
        val userId: String,
        val name: String,
        val phoneNumber: String,
        val age: Int,
        val gender: String,
        val village: String,
        val district: String,
        val latitude: Double,
        val longitude: Double,
        val skills: List<String>,
        val experienceYears: Map<String, Int>,
        val pricingType: String,
        val dailyWage: Int?,
        val hourlyRate: Int?,
        val availabilityStatus: String,
        val futureAvailability: Long? = null,
        val averageRating: Double,
        val totalRatings: Int,
        val completedBookings: Int,
        val createdAt: Long,
        val updatedAt: Long,
        val lastAvailabilityUpdate: Long
    ) {
        fun toMap(): Map<String, Any?> = mapOf(
            "userId" to userId,
            "name" to name,
            "phoneNumber" to phoneNumber,
            "age" to age,
            "gender" to gender,
            "village" to village,
            "district" to district,
            "latitude" to latitude,
            "longitude" to longitude,
            "skills" to skills,
            "experienceYears" to experienceYears,
            "pricingType" to pricingType,
            "dailyWage" to dailyWage,
            "hourlyRate" to hourlyRate,
            "profilePhotoUrls" to emptyList<String>(),
            "availabilityStatus" to availabilityStatus,
            "futureAvailability" to futureAvailability,
            "averageRating" to averageRating,
            "totalRatings" to totalRatings,
            "completedBookings" to completedBookings,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "lastAvailabilityUpdate" to lastAvailabilityUpdate,
            "preferredLanguage" to "kn"
        )
    }
}
