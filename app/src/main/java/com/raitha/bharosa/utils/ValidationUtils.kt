package com.raitha.bharosa.utils

import com.raitha.bharosa.data.Skill

/**
 * Validation utility functions for labour booking system
 * Task 10.5: Create ValidationUtils.kt
 * Requirement 2: Labourer Profile Creation
 * Requirement 20: Input Validation and Error Handling
 * 
 * This utility provides validation functions for user inputs to ensure
 * data integrity and provide clear error messages to users.
 */
object ValidationUtils {
    
    /**
     * Validation result with success/failure and error message
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessageEn: String? = null,
        val errorMessageKn: String? = null
    ) {
        /**
         * Get error message in specified language
         * @param language "en" for English, "kn" for Kannada
         * @return Localized error message or null if valid
         */
        fun getErrorMessage(language: String): String? {
            return if (language == "kn") errorMessageKn else errorMessageEn
        }
    }
    
    /**
     * Validate phone number format
     * 
     * Requirements:
     * - Must be exactly 10 digits
     * - Must contain only numeric characters
     * - Must not start with 0 or 1 (invalid Indian mobile numbers)
     * 
     * @param phoneNumber Phone number string to validate
     * @return ValidationResult with success/failure and error message
     * 
     * Example:
     * ```
     * validatePhoneNumber("9876543210") // Valid
     * validatePhoneNumber("987654321")  // Invalid (9 digits)
     * validatePhoneNumber("0987654321") // Invalid (starts with 0)
     * validatePhoneNumber("98765abcde") // Invalid (contains letters)
     * ```
     */
    fun validatePhoneNumber(phoneNumber: String): ValidationResult {
        val cleaned = phoneNumber.trim()
        
        // Check if empty
        if (cleaned.isEmpty()) {
            return ValidationResult(
                isValid = false,
                errorMessageEn = "Phone number is required",
                errorMessageKn = "ದೂರವಾಣಿ ಸಂಖ್ಯೆ ಅಗತ್ಯವಿದೆ"
            )
        }
        
        // Check if contains only digits
        if (!cleaned.all { it.isDigit() }) {
            return ValidationResult(
                isValid = false,
                errorMessageEn = "Phone number must contain only digits",
                errorMessageKn = "ದೂರವಾಣಿ ಸಂಖ್ಯೆ ಕೇವಲ ಅಂಕೆಗಳನ್ನು ಹೊಂದಿರಬೇಕು"
            )
        }
        
        // Check if exactly 10 digits
        if (cleaned.length != 10) {
            return ValidationResult(
                isValid = false,
                errorMessageEn = "Phone number must be exactly 10 digits",
                errorMessageKn = "ದೂರವಾಣಿ ಸಂಖ್ಯೆ ನಿಖರವಾಗಿ 10 ಅಂಕೆಗಳಾಗಿರಬೇಕು"
            )
        }
        
        // Check if starts with valid digit (not 0 or 1)
        if (cleaned[0] == '0' || cleaned[0] == '1') {
            return ValidationResult(
                isValid = false,
                errorMessageEn = "Phone number must start with digits 2-9",
                errorMessageKn = "ದೂರವಾಣಿ ಸಂಖ್ಯೆ 2-9 ಅಂಕೆಗಳಿಂದ ಪ್ರಾರಂಭವಾಗಬೇಕು"
            )
        }
        
        return ValidationResult(isValid = true)
    }
    
    /**
     * Validate age for labourer profile
     * 
     * Requirements:
     * - Must be between 18 and 70 years (inclusive)
     * - Must be a positive integer
     * 
     * @param age Age in years
     * @return ValidationResult with success/failure and error message
     * 
     * Example:
     * ```
     * validateAge(25)  // Valid
     * validateAge(17)  // Invalid (too young)
     * validateAge(71)  // Invalid (too old)
     * validateAge(-5)  // Invalid (negative)
     * ```
     */
    fun validateAge(age: Int): ValidationResult {
        return when {
            age < 0 -> ValidationResult(
                isValid = false,
                errorMessageEn = "Age cannot be negative",
                errorMessageKn = "ವಯಸ್ಸು ಋಣಾತ್ಮಕವಾಗಿರಬಾರದು"
            )
            age < 18 -> ValidationResult(
                isValid = false,
                errorMessageEn = "Age must be at least 18 years",
                errorMessageKn = "ವಯಸ್ಸು ಕನಿಷ್ಠ 18 ವರ್ಷಗಳಾಗಿರಬೇಕು"
            )
            age > 70 -> ValidationResult(
                isValid = false,
                errorMessageEn = "Age must be 70 years or less",
                errorMessageKn = "ವಯಸ್ಸು 70 ವರ್ಷಗಳು ಅಥವಾ ಕಡಿಮೆಯಾಗಿರಬೇಕು"
            )
            else -> ValidationResult(isValid = true)
        }
    }
    
    /**
     * Validate daily wage amount
     * 
     * Requirements:
     * - Must be between ₹300 and ₹2000 (inclusive)
     * - Must be a positive integer
     * 
     * @param dailyWage Daily wage amount in rupees
     * @return ValidationResult with success/failure and error message
     * 
     * Example:
     * ```
     * validateDailyWage(500)   // Valid
     * validateDailyWage(250)   // Invalid (too low)
     * validateDailyWage(2500)  // Invalid (too high)
     * validateDailyWage(-100)  // Invalid (negative)
     * ```
     */
    fun validateDailyWage(dailyWage: Int): ValidationResult {
        return when {
            dailyWage < 0 -> ValidationResult(
                isValid = false,
                errorMessageEn = "Daily wage cannot be negative",
                errorMessageKn = "ದೈನಂದಿನ ವೇತನ ಋಣಾತ್ಮಕವಾಗಿರಬಾರದು"
            )
            dailyWage < 300 -> ValidationResult(
                isValid = false,
                errorMessageEn = "Daily wage must be at least ₹300",
                errorMessageKn = "ದೈನಂದಿನ ವೇತನ ಕನಿಷ್ಠ ₹300 ಆಗಿರಬೇಕು"
            )
            dailyWage > 2000 -> ValidationResult(
                isValid = false,
                errorMessageEn = "Daily wage must be ₹2000 or less",
                errorMessageKn = "ದೈನಂದಿನ ವೇತನ ₹2000 ಅಥವಾ ಕಡಿಮೆಯಾಗಿರಬೇಕು"
            )
            else -> ValidationResult(isValid = true)
        }
    }
    
    /**
     * Validate hourly rate amount
     * 
     * Requirements:
     * - Must be between ₹40 and ₹300 (inclusive)
     * - Must be a positive integer
     * 
     * @param hourlyRate Hourly rate amount in rupees
     * @return ValidationResult with success/failure and error message
     * 
     * Example:
     * ```
     * validateHourlyRate(100)  // Valid
     * validateHourlyRate(30)   // Invalid (too low)
     * validateHourlyRate(350)  // Invalid (too high)
     * validateHourlyRate(-50)  // Invalid (negative)
     * ```
     */
    fun validateHourlyRate(hourlyRate: Int): ValidationResult {
        return when {
            hourlyRate < 0 -> ValidationResult(
                isValid = false,
                errorMessageEn = "Hourly rate cannot be negative",
                errorMessageKn = "ಗಂಟೆಯ ದರ ಋಣಾತ್ಮಕವಾಗಿರಬಾರದು"
            )
            hourlyRate < 40 -> ValidationResult(
                isValid = false,
                errorMessageEn = "Hourly rate must be at least ₹40",
                errorMessageKn = "ಗಂಟೆಯ ದರ ಕನಿಷ್ಠ ₹40 ಆಗಿರಬೇಕು"
            )
            hourlyRate > 300 -> ValidationResult(
                isValid = false,
                errorMessageEn = "Hourly rate must be ₹300 or less",
                errorMessageKn = "ಗಂಟೆಯ ದರ ₹300 ಅಥವಾ ಕಡಿಮೆಯಾಗಿರಬೇಕು"
            )
            else -> ValidationResult(isValid = true)
        }
    }
    
    /**
     * Validate skills selection
     * 
     * Requirements:
     * - Must have at least one skill selected
     * - Skills list cannot be empty
     * 
     * @param skills List of selected skills
     * @return ValidationResult with success/failure and error message
     * 
     * Example:
     * ```
     * validateSkills(listOf(Skill.HARVESTING, Skill.PLANTING))  // Valid
     * validateSkills(listOf(Skill.WEEDING))                     // Valid
     * validateSkills(emptyList())                               // Invalid
     * ```
     */
    fun validateSkills(skills: List<Skill>): ValidationResult {
        return if (skills.isEmpty()) {
            ValidationResult(
                isValid = false,
                errorMessageEn = "At least one skill must be selected",
                errorMessageKn = "ಕನಿಷ್ಠ ಒಂದು ಕೌಶಲ್ಯವನ್ನು ಆಯ್ಕೆ ಮಾಡಬೇಕು"
            )
        } else {
            ValidationResult(isValid = true)
        }
    }
    
    /**
     * Validate name field
     * 
     * Requirements:
     * - Must not be empty or blank
     * - Must be at least 2 characters
     * - Must be at most 100 characters
     * 
     * @param name Name string to validate
     * @return ValidationResult with success/failure and error message
     */
    fun validateName(name: String): ValidationResult {
        val trimmed = name.trim()
        
        return when {
            trimmed.isEmpty() -> ValidationResult(
                isValid = false,
                errorMessageEn = "Name is required",
                errorMessageKn = "ಹೆಸರು ಅಗತ್ಯವಿದೆ"
            )
            trimmed.length < 2 -> ValidationResult(
                isValid = false,
                errorMessageEn = "Name must be at least 2 characters",
                errorMessageKn = "ಹೆಸರು ಕನಿಷ್ಠ 2 ಅಕ್ಷರಗಳಾಗಿರಬೇಕು"
            )
            trimmed.length > 100 -> ValidationResult(
                isValid = false,
                errorMessageEn = "Name must be 100 characters or less",
                errorMessageKn = "ಹೆಸರು 100 ಅಕ್ಷರಗಳು ಅಥವಾ ಕಡಿಮೆಯಾಗಿರಬೇಕು"
            )
            else -> ValidationResult(isValid = true)
        }
    }
    
    /**
     * Validate review text
     * 
     * Requirements:
     * - Must be 500 characters or less
     * - Can be empty (optional field)
     * 
     * @param review Review text to validate
     * @return ValidationResult with success/failure and error message
     */
    fun validateReview(review: String?): ValidationResult {
        if (review == null) {
            return ValidationResult(isValid = true)
        }
        
        return if (review.length > 500) {
            ValidationResult(
                isValid = false,
                errorMessageEn = "Review must be 500 characters or less",
                errorMessageKn = "ವಿಮರ್ಶೆ 500 ಅಕ್ಷರಗಳು ಅಥವಾ ಕಡಿಮೆಯಾಗಿರಬೇಕು"
            )
        } else {
            ValidationResult(isValid = true)
        }
    }
    
    /**
     * Validate rating value
     * 
     * Requirements:
     * - Must be between 1 and 5 (inclusive)
     * 
     * @param rating Rating value (1-5 stars)
     * @return ValidationResult with success/failure and error message
     */
    fun validateRating(rating: Int): ValidationResult {
        return when {
            rating < 1 -> ValidationResult(
                isValid = false,
                errorMessageEn = "Rating must be at least 1 star",
                errorMessageKn = "ರೇಟಿಂಗ್ ಕನಿಷ್ಠ 1 ನಕ್ಷತ್ರವಾಗಿರಬೇಕು"
            )
            rating > 5 -> ValidationResult(
                isValid = false,
                errorMessageEn = "Rating must be 5 stars or less",
                errorMessageKn = "ರೇಟಿಂಗ್ 5 ನಕ್ಷತ್ರಗಳು ಅಥವಾ ಕಡಿಮೆಯಾಗಿರಬೇಕು"
            )
            else -> ValidationResult(isValid = true)
        }
    }
}
