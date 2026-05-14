package com.raitha.bharosa.utils

/**
 * Rating calculation utility functions
 * Task 10.7: Create RatingUtils.kt
 * Requirement 13: Rating and Review System
 * 
 * This utility provides functions for calculating and managing ratings
 * in the labour booking system.
 */
object RatingUtils {
    
    /**
     * Calculate new average rating when a new rating is added
     * 
     * This function uses the incremental average formula to efficiently
     * calculate the new average without needing to store all individual ratings:
     * 
     * newAverage = (oldAverage * oldCount + newRating) / (oldCount + 1)
     * 
     * Properties:
     * - The result is always between 1.0 and 5.0 (inclusive) when ratings are in range
     * - Adding a higher rating increases the average
     * - Adding a lower rating decreases the average
     * - The average converges towards the new rating as more ratings are added
     * 
     * @param currentAverage Current average rating (0.0 if no ratings yet)
     * @param currentCount Current number of ratings (0 if no ratings yet)
     * @param newRating New rating to add (must be between 1 and 5)
     * @return New average rating rounded to 2 decimal places
     * 
     * Example:
     * ```
     * // First rating
     * calculateNewAverageRating(0.0, 0, 5) // Returns 5.0
     * 
     * // Adding second rating
     * calculateNewAverageRating(5.0, 1, 3) // Returns 4.0
     * 
     * // Adding third rating
     * calculateNewAverageRating(4.0, 2, 4) // Returns 4.0
     * 
     * // Adding fourth rating
     * calculateNewAverageRating(4.0, 3, 2) // Returns 3.5
     * ```
     */
    fun calculateNewAverageRating(
        currentAverage: Double,
        currentCount: Int,
        newRating: Int
    ): Double {
        // Validate inputs
        require(currentCount >= 0) { "Current count cannot be negative" }
        require(newRating in 1..5) { "New rating must be between 1 and 5" }
        require(currentAverage >= 0.0) { "Current average cannot be negative" }
        
        // Handle first rating
        if (currentCount == 0) {
            return newRating.toDouble()
        }
        
        // Calculate new average using incremental formula
        val totalRating = currentAverage * currentCount + newRating
        val newCount = currentCount + 1
        val newAverage = totalRating / newCount
        
        // Round to 2 decimal places
        return (newAverage * 100).toLong() / 100.0
    }
    
    /**
     * Format rating for display
     * 
     * @param rating Rating value (0.0 to 5.0)
     * @return Formatted string (e.g., "4.5" or "5.0")
     */
    fun formatRating(rating: Double): String {
        return "%.1f".format(rating)
    }
    
    /**
     * Get star representation of rating
     * 
     * @param rating Rating value (0.0 to 5.0)
     * @return String with star symbols (e.g., "★★★★☆" for 4.0)
     */
    fun getStarRepresentation(rating: Double): String {
        val fullStars = rating.toInt()
        val hasHalfStar = (rating - fullStars) >= 0.5
        val emptyStars = 5 - fullStars - if (hasHalfStar) 1 else 0
        
        return "★".repeat(fullStars) +
               (if (hasHalfStar) "⯨" else "") +
               "☆".repeat(emptyStars)
    }
    
    /**
     * Calculate rating distribution
     * 
     * @param ratings List of all ratings (1-5)
     * @return Map of rating value to count (e.g., {5: 10, 4: 5, 3: 2, 2: 1, 1: 0})
     */
    fun calculateRatingDistribution(ratings: List<Int>): Map<Int, Int> {
        val distribution = mutableMapOf(
            5 to 0,
            4 to 0,
            3 to 0,
            2 to 0,
            1 to 0
        )
        
        ratings.forEach { rating ->
            if (rating in 1..5) {
                distribution[rating] = distribution[rating]!! + 1
            }
        }
        
        return distribution
    }
    
    /**
     * Calculate percentage of ratings at each level
     * 
     * @param ratings List of all ratings (1-5)
     * @return Map of rating value to percentage (e.g., {5: 50.0, 4: 25.0, ...})
     */
    fun calculateRatingPercentages(ratings: List<Int>): Map<Int, Double> {
        if (ratings.isEmpty()) {
            return mapOf(5 to 0.0, 4 to 0.0, 3 to 0.0, 2 to 0.0, 1 to 0.0)
        }
        
        val distribution = calculateRatingDistribution(ratings)
        val total = ratings.size.toDouble()
        
        return distribution.mapValues { (_, count) ->
            (count / total * 100 * 10).toLong() / 10.0 // Round to 1 decimal place
        }
    }
    
    /**
     * Check if rating is excellent (4.5 or higher)
     * 
     * @param rating Rating value
     * @return true if rating is excellent, false otherwise
     */
    fun isExcellentRating(rating: Double): Boolean {
        return rating >= 4.5
    }
    
    /**
     * Check if rating is good (3.5 or higher)
     * 
     * @param rating Rating value
     * @return true if rating is good, false otherwise
     */
    fun isGoodRating(rating: Double): Boolean {
        return rating >= 3.5
    }
    
    /**
     * Get rating category
     * 
     * @param rating Rating value
     * @return Category string ("Excellent", "Good", "Average", "Poor")
     */
    fun getRatingCategory(rating: Double): String {
        return when {
            rating >= 4.5 -> "Excellent"
            rating >= 3.5 -> "Good"
            rating >= 2.5 -> "Average"
            else -> "Poor"
        }
    }
    
    /**
     * Get rating category in Kannada
     * 
     * @param rating Rating value
     * @return Category string in Kannada
     */
    fun getRatingCategoryKannada(rating: Double): String {
        return when {
            rating >= 4.5 -> "ಅತ್ಯುತ್ತಮ"
            rating >= 3.5 -> "ಉತ್ತಮ"
            rating >= 2.5 -> "ಸರಾಸರಿ"
            else -> "ಕಳಪೆ"
        }
    }
}
