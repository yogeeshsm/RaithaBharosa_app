package com.raitha.bharosa.utils

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Properties

/**
 * Secure Credential Manager for accessing API keys and credentials
 * from the .env file in a safe and centralized manner.
 * 
 * This class provides secure access to:
 * - Twilio SMS API credentials
 * - WhatsApp Business API credentials
 * - Razorpay Payment Gateway credentials
 * - Other API keys
 * 
 * Usage:
 * ```
 * val credentialManager = CredentialManager.getInstance(context)
 * val twilioSid = credentialManager.getTwilioAccountSid()
 * ```
 */
class CredentialManager private constructor(context: Context) {
    
    private val properties = Properties()
    private val tag = "CredentialManager"
    
    init {
        loadCredentials(context)
    }
    
    /**
     * Load credentials from the .env file
     */
    private fun loadCredentials(context: Context) {
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open(".env")
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            reader.use { r ->
                r.forEachLine { line ->
                    // Skip empty lines and comments
                    if (line.isNotBlank() && !line.trim().startsWith("#")) {
                        val parts = line.split("=", limit = 2)
                        if (parts.size == 2) {
                            val key = parts[0].trim()
                            val value = parts[1].trim()
                            properties[key] = value
                        }
                    }
                }
            }
            
            Log.d(tag, "Credentials loaded successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error loading credentials from .env file", e)
        }
    }
    
    // ========== Twilio SMS API Credentials ==========
    
    /**
     * Get Twilio Account SID
     * @return Twilio Account SID or empty string if not found
     */
    fun getTwilioAccountSid(): String {
        return getCredential("TWILIO_ACCOUNT_SID")
    }
    
    /**
     * Get Twilio Auth Token
     * @return Twilio Auth Token or empty string if not found
     */
    fun getTwilioAuthToken(): String {
        return getCredential("TWILIO_AUTH_TOKEN")
    }
    
    /**
     * Get Twilio Phone Number
     * @return Twilio Phone Number or empty string if not found
     */
    fun getTwilioPhoneNumber(): String {
        return getCredential("TWILIO_PHONE_NUMBER")
    }
    
    /**
     * Check if Twilio credentials are configured
     * @return true if all Twilio credentials are present
     */
    fun isTwilioConfigured(): Boolean {
        return getTwilioAccountSid().isNotEmpty() &&
                getTwilioAuthToken().isNotEmpty() &&
                getTwilioPhoneNumber().isNotEmpty() &&
                !getTwilioAccountSid().contains("your_") &&
                !getTwilioAuthToken().contains("your_")
    }
    
    // ========== WhatsApp Business API Credentials ==========
    
    /**
     * Get WhatsApp API Key
     * @return WhatsApp API Key or empty string if not found
     */
    fun getWhatsAppApiKey(): String {
        return getCredential("WHATSAPP_API_KEY")
    }
    
    /**
     * Get WhatsApp Phone Number ID
     * @return WhatsApp Phone Number ID or empty string if not found
     */
    fun getWhatsAppPhoneNumberId(): String {
        return getCredential("WHATSAPP_PHONE_NUMBER_ID")
    }
    
    /**
     * Get WhatsApp Business Account ID
     * @return WhatsApp Business Account ID or empty string if not found
     */
    fun getWhatsAppBusinessAccountId(): String {
        return getCredential("WHATSAPP_BUSINESS_ACCOUNT_ID")
    }
    
    /**
     * Check if WhatsApp credentials are configured
     * @return true if all WhatsApp credentials are present
     */
    fun isWhatsAppConfigured(): Boolean {
        return getWhatsAppApiKey().isNotEmpty() &&
                getWhatsAppPhoneNumberId().isNotEmpty() &&
                !getWhatsAppApiKey().contains("your_")
    }
    
    // ========== Razorpay Payment Gateway Credentials ==========
    
    /**
     * Get Razorpay Key ID
     * @return Razorpay Key ID or empty string if not found
     */
    fun getRazorpayKeyId(): String {
        return getCredential("RAZORPAY_KEY_ID")
    }
    
    /**
     * Get Razorpay Key Secret
     * @return Razorpay Key Secret or empty string if not found
     */
    fun getRazorpayKeySecret(): String {
        return getCredential("RAZORPAY_KEY_SECRET")
    }
    
    /**
     * Check if Razorpay credentials are configured
     * @return true if all Razorpay credentials are present
     */
    fun isRazorpayConfigured(): Boolean {
        return getRazorpayKeyId().isNotEmpty() &&
                getRazorpayKeySecret().isNotEmpty() &&
                !getRazorpayKeyId().contains("your_") &&
                !getRazorpayKeySecret().contains("your_")
    }
    
    // ========== Other API Credentials ==========
    
    /**
     * Get Weather API Key
     * @return Weather API Key or empty string if not found
     */
    fun getWeatherApiKey(): String {
        return getCredential("WEATHER_API")
    }
    
    /**
     * Get Mandi API Key
     * @return Mandi API Key or empty string if not found
     */
    fun getMandiApiKey(): String {
        return getCredential("Current Daily Price of Various Commodities from Various Markets (Mandi)")
    }
    
    // ========== Generic Credential Access ==========
    
    /**
     * Get a credential by key
     * @param key The credential key
     * @return The credential value or empty string if not found
     */
    fun getCredential(key: String): String {
        return properties.getProperty(key, "").also {
            if (it.isEmpty()) {
                Log.w(tag, "Credential not found for key: $key")
            }
        }
    }
    
    /**
     * Check if a credential exists
     * @param key The credential key
     * @return true if the credential exists and is not empty
     */
    fun hasCredential(key: String): Boolean {
        val value = properties.getProperty(key, "")
        return value.isNotEmpty() && !value.contains("your_")
    }
    
    /**
     * Get all credential keys (for debugging purposes only)
     * WARNING: Do not use in production to expose credential keys
     * @return Set of all credential keys
     */
    fun getAllKeys(): Set<String> {
        return properties.stringPropertyNames()
    }
    
    /**
     * Validate all required credentials for the Labour Booking System
     * @return ValidationResult with status and missing credentials
     */
    fun validateLabourSystemCredentials(): ValidationResult {
        val missingCredentials = mutableListOf<String>()
        
        // Check Twilio credentials
        if (!isTwilioConfigured()) {
            if (getTwilioAccountSid().isEmpty() || getTwilioAccountSid().contains("your_")) {
                missingCredentials.add("TWILIO_ACCOUNT_SID")
            }
            if (getTwilioAuthToken().isEmpty() || getTwilioAuthToken().contains("your_")) {
                missingCredentials.add("TWILIO_AUTH_TOKEN")
            }
            if (getTwilioPhoneNumber().isEmpty() || getTwilioPhoneNumber().contains("your_")) {
                missingCredentials.add("TWILIO_PHONE_NUMBER")
            }
        }
        
        // Check WhatsApp credentials
        if (!isWhatsAppConfigured()) {
            if (getWhatsAppApiKey().isEmpty() || getWhatsAppApiKey().contains("your_")) {
                missingCredentials.add("WHATSAPP_API_KEY")
            }
            if (getWhatsAppPhoneNumberId().isEmpty() || getWhatsAppPhoneNumberId().contains("your_")) {
                missingCredentials.add("WHATSAPP_PHONE_NUMBER_ID")
            }
        }
        
        // Check Razorpay credentials
        if (!isRazorpayConfigured()) {
            if (getRazorpayKeyId().isEmpty() || getRazorpayKeyId().contains("your_")) {
                missingCredentials.add("RAZORPAY_KEY_ID")
            }
            if (getRazorpayKeySecret().isEmpty() || getRazorpayKeySecret().contains("your_")) {
                missingCredentials.add("RAZORPAY_KEY_SECRET")
            }
        }
        
        return ValidationResult(
            isValid = missingCredentials.isEmpty(),
            missingCredentials = missingCredentials
        )
    }
    
    /**
     * Result of credential validation
     */
    data class ValidationResult(
        val isValid: Boolean,
        val missingCredentials: List<String>
    ) {
        fun getErrorMessage(): String {
            return if (isValid) {
                "All credentials are configured"
            } else {
                "Missing or invalid credentials: ${missingCredentials.joinToString(", ")}"
            }
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: CredentialManager? = null
        
        /**
         * Get singleton instance of CredentialManager
         * @param context Application context
         * @return CredentialManager instance
         */
        fun getInstance(context: Context): CredentialManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CredentialManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
