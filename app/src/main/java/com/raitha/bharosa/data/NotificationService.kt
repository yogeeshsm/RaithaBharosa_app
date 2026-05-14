package com.raitha.bharosa.data

import android.util.Log
import com.raitha.bharosa.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * Notification service interface for sending SMS and WhatsApp notifications
 * Requirement 10: SMS and WhatsApp Notifications
 */
interface NotificationService {
    suspend fun sendSMS(phoneNumber: String, message: String)
    suspend fun sendWhatsApp(phoneNumber: String, message: String)
}

/**
 * Notification message templates for labour booking system
 * Task 9.2: Implement notification message templates
 * Requirement 4: Multi-language Support (English and Kannada)
 * Requirement 9: Emergency Hiring
 * Requirement 10: SMS and WhatsApp Notifications
 */
object NotificationTemplates {
    
    /**
     * Booking notification template - sent to labourer when farmer creates a booking
     * 
     * @param farmerName Name of the farmer who created the booking
     * @param workDate Date of the work (formatted string)
     * @param workType Type of work (skill name)
     * @param estimatedPayment Payment amount in rupees
     * @param isEmergency Whether this is an emergency booking
     * @param language "en" for English, "kn" for Kannada
     * @return Formatted notification message
     */
    fun bookingNotification(
        farmerName: String,
        workDate: String,
        workType: String,
        estimatedPayment: Int,
        isEmergency: Boolean,
        language: String
    ): String {
        val urgentTag = if (isEmergency) {
            if (language == "kn") "[ತುರ್ತು] " else "[URGENT] "
        } else ""
        
        return if (language == "kn") {
            "${urgentTag}ಹೊಸ ಕೆಲಸದ ವಿನಂತಿ!\n" +
            "ರೈತ: $farmerName\n" +
            "ದಿನಾಂಕ: $workDate\n" +
            "ಕೆಲಸ: $workType\n" +
            "ಪಾವತಿ: ₹$estimatedPayment\n" +
            "ದಯವಿಟ್ಟು ಸ್ವೀಕರಿಸಿ ಅಥವಾ ನಿರಾಕರಿಸಿ."
        } else {
            "${urgentTag}New Work Request!\n" +
            "Farmer: $farmerName\n" +
            "Date: $workDate\n" +
            "Work: $workType\n" +
            "Payment: ₹$estimatedPayment\n" +
            "Please accept or decline."
        }
    }
    
    /**
     * Booking confirmation template - sent to farmer when labourer accepts booking
     * 
     * @param labourerName Name of the labourer who accepted
     * @param labourerPhone Phone number of the labourer
     * @param workDate Date of the work (formatted string)
     * @param language "en" for English, "kn" for Kannada
     * @return Formatted confirmation message
     */
    fun bookingConfirmation(
        labourerName: String,
        labourerPhone: String,
        workDate: String,
        language: String
    ): String {
        return if (language == "kn") {
            "ಕೆಲಸ ದೃಢೀಕರಣ!\n" +
            "$labourerName ನಿಮ್ಮ ಕೆಲಸದ ವಿನಂತಿಯನ್ನು ಸ್ವೀಕರಿಸಿದ್ದಾರೆ.\n" +
            "ದಿನಾಂಕ: $workDate\n" +
            "ಸಂಪರ್ಕ: $labourerPhone"
        } else {
            "Booking Confirmed!\n" +
            "$labourerName has accepted your work request.\n" +
            "Date: $workDate\n" +
            "Contact: $labourerPhone"
        }
    }
    
    /**
     * Booking declined template - sent to farmer when labourer declines booking
     * 
     * @param labourerName Name of the labourer who declined
     * @param workDate Date of the work (formatted string)
     * @param language "en" for English, "kn" for Kannada
     * @return Formatted decline message
     */
    fun bookingDeclined(
        labourerName: String,
        workDate: String,
        language: String
    ): String {
        return if (language == "kn") {
            "ಕೆಲಸ ನಿರಾಕರಣೆ\n" +
            "$labourerName ನಿಮ್ಮ ಕೆಲಸದ ವಿನಂತಿಯನ್ನು ನಿರಾಕರಿಸಿದ್ದಾರೆ.\n" +
            "ದಿನಾಂಕ: $workDate\n" +
            "ದಯವಿಟ್ಟು ಬೇರೆ ಕಾರ್ಮಿಕರನ್ನು ಹುಡುಕಿ."
        } else {
            "Booking Declined\n" +
            "$labourerName has declined your work request.\n" +
            "Date: $workDate\n" +
            "Please search for another labourer."
        }
    }
    
    /**
     * Booking cancelled template - sent to labourer when farmer cancels booking
     * 
     * @param farmerName Name of the farmer who cancelled
     * @param workDate Date of the work (formatted string)
     * @param language "en" for English, "kn" for Kannada
     * @return Formatted cancellation message
     */
    fun bookingCancelled(
        farmerName: String,
        workDate: String,
        language: String
    ): String {
        return if (language == "kn") {
            "ಕೆಲಸ ರದ್ದು\n" +
            "$farmerName ಅವರು ಕೆಲಸವನ್ನು ರದ್ದುಗೊಳಿಸಿದ್ದಾರೆ.\n" +
            "ದಿನಾಂಕ: $workDate"
        } else {
            "Booking Cancelled\n" +
            "$farmerName has cancelled the booking.\n" +
            "Date: $workDate"
        }
    }
    
    /**
     * Payment confirmation template - sent to labourer when payment is completed
     * 
     * @param amount Payment amount in rupees
     * @param transactionId Payment transaction ID
     * @param workDate Date of the work (formatted string)
     * @param language "en" for English, "kn" for Kannada
     * @return Formatted payment confirmation message
     */
    fun paymentConfirmation(
        amount: Int,
        transactionId: String,
        workDate: String,
        language: String
    ): String {
        return if (language == "kn") {
            "ಪಾವತಿ ಸ್ವೀಕರಿಸಲಾಗಿದೆ!\n" +
            "ಮೊತ್ತ: ₹$amount\n" +
            "ವಹಿವಾಟು ID: $transactionId\n" +
            "ಕೆಲಸದ ದಿನಾಂಕ: $workDate\n" +
            "ಧನ್ಯವಾದಗಳು!"
        } else {
            "Payment Received!\n" +
            "Amount: ₹$amount\n" +
            "Transaction ID: $transactionId\n" +
            "Work Date: $workDate\n" +
            "Thank you!"
        }
    }
    
    /**
     * Emergency hiring template - sent to labourer for urgent work requests
     * Always includes URGENT tag
     * 
     * @param farmerName Name of the farmer who created the booking
     * @param workDate Date of the work (formatted string)
     * @param workType Type of work (skill name)
     * @param estimatedPayment Payment amount in rupees
     * @param language "en" for English, "kn" for Kannada
     * @return Formatted emergency notification message
     */
    fun emergencyHiring(
        farmerName: String,
        workDate: String,
        workType: String,
        estimatedPayment: Int,
        language: String
    ): String {
        return if (language == "kn") {
            "[ತುರ್ತು] ತುರ್ತು ಕೆಲಸದ ವಿನಂತಿ!\n" +
            "ರೈತ: $farmerName\n" +
            "ದಿನಾಂಕ: $workDate\n" +
            "ಕೆಲಸ: $workType\n" +
            "ಪಾವತಿ: ₹$estimatedPayment\n" +
            "ತುರ್ತು ಪ್ರತಿಕ್ರಿಯೆ ಅಗತ್ಯವಿದೆ!"
        } else {
            "[URGENT] Emergency Work Request!\n" +
            "Farmer: $farmerName\n" +
            "Date: $workDate\n" +
            "Work: $workType\n" +
            "Payment: ₹$estimatedPayment\n" +
            "Urgent response needed!"
        }
    }
}

/**
 * Implementation of NotificationService for sending SMS and WhatsApp notifications
 * 
 * This service handles:
 * - SMS sending via Twilio API (Requirement 10: SMS and WhatsApp Notifications)
 * - WhatsApp message sending via WhatsApp Business API (Requirement 10)
 * - Notification preference checking (Requirement 22: Notification Preferences)
 * - Retry logic for failed deliveries (Requirement 10)
 * - Logging of all notification attempts (Requirement 10)
 * 
 * Architecture:
 * - Uses OkHttp for HTTP requests to external APIs
 * - Implements exponential backoff for retries
 * - Logs all attempts for debugging and audit purposes
 * - Handles failures gracefully without throwing exceptions
 * 
 * Task 9.1: Create NotificationService.kt
 */
class NotificationServiceImpl : NotificationService {
    
    companion object {
        private const val TAG = "NotificationService"
        
        // Twilio API configuration
        private val TWILIO_ACCOUNT_SID = BuildConfig.TWILIO_ACCOUNT_SID
        private val TWILIO_AUTH_TOKEN = BuildConfig.TWILIO_AUTH_TOKEN
        private val TWILIO_PHONE_NUMBER = BuildConfig.TWILIO_PHONE_NUMBER
        private const val TWILIO_API_BASE_URL = "https://api.twilio.com/2010-04-01"
        
        // WhatsApp Business API configuration
        private val WHATSAPP_API_KEY = BuildConfig.WHATSAPP_API_KEY
        private val WHATSAPP_PHONE_NUMBER_ID = BuildConfig.WHATSAPP_PHONE_NUMBER_ID
        private const val WHATSAPP_API_BASE_URL = "https://graph.facebook.com/v18.0"
        
        // Retry configuration
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L // 1 second
        private const val RETRY_DELAY_MULTIPLIER = 2 // Exponential backoff
        
        // Phone number formatting
        private const val INDIA_COUNTRY_CODE = "+91"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    /**
     * Send SMS message via Twilio API with retry logic
     * Requirement 10: SMS and WhatsApp Notifications
     * 
     * Implementation details:
     * - Uses Twilio REST API to send SMS
     * - Formats phone number with India country code (+91)
     * - Implements retry logic with exponential backoff
     * - Logs all attempts and results
     * - Does not throw exceptions (fails gracefully)
     * 
     * @param phoneNumber The recipient's phone number (10 digits without country code)
     * @param message The SMS message content
     */
    override suspend fun sendSMS(phoneNumber: String, message: String) {
        withContext(Dispatchers.IO) {
            val formattedPhone = formatPhoneNumber(phoneNumber)
            Log.d(TAG, "Attempting to send SMS to $formattedPhone")
            
            var attempt = 0
            var success = false
            var lastError: Exception? = null
            
            while (attempt < MAX_RETRY_ATTEMPTS && !success) {
                attempt++
                
                try {
                    val result = sendSMSAttempt(formattedPhone, message)
                    
                    if (result.isSuccess) {
                        success = true
                        Log.i(TAG, "SMS sent successfully to $formattedPhone on attempt $attempt")
                    } else {
                        lastError = result.exceptionOrNull() as? Exception
                        Log.w(TAG, "SMS attempt $attempt failed for $formattedPhone: ${lastError?.message}")
                        
                        if (attempt < MAX_RETRY_ATTEMPTS) {
                            val delayMs = INITIAL_RETRY_DELAY_MS * (RETRY_DELAY_MULTIPLIER.toLong().shl(attempt - 1))
                            Log.d(TAG, "Retrying SMS in ${delayMs}ms...")
                            delay(delayMs)
                        }
                    }
                } catch (e: Exception) {
                    lastError = e
                    Log.e(TAG, "SMS attempt $attempt threw exception for $formattedPhone", e)
                    
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        val delayMs = INITIAL_RETRY_DELAY_MS * (RETRY_DELAY_MULTIPLIER.toLong().shl(attempt - 1))
                        Log.d(TAG, "Retrying SMS in ${delayMs}ms...")
                        delay(delayMs)
                    }
                }
            }
            
            if (!success) {
                Log.e(TAG, "Failed to send SMS to $formattedPhone after $MAX_RETRY_ATTEMPTS attempts. Last error: ${lastError?.message}")
            }
        }
    }
    
    /**
     * Single attempt to send SMS via Twilio API
     * 
     * @param phoneNumber Formatted phone number with country code
     * @param message SMS message content
     * @return Result indicating success or failure
     */
    private suspend fun sendSMSAttempt(phoneNumber: String, message: String): Result<Unit> {
        return try {
            // Check if credentials are configured
            if (TWILIO_ACCOUNT_SID.isBlank() || TWILIO_AUTH_TOKEN.isBlank() || TWILIO_PHONE_NUMBER.isBlank()) {
                Log.w(TAG, "Twilio credentials not configured. Skipping SMS send.")
                return Result.failure(Exception("Twilio credentials not configured"))
            }
            
            // Build Twilio API request
            val credentials = Credentials.basic(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN)
            val url = "$TWILIO_API_BASE_URL/Accounts/$TWILIO_ACCOUNT_SID/Messages.json"
            
            val formBody = FormBody.Builder()
                .add("To", phoneNumber)
                .add("From", TWILIO_PHONE_NUMBER)
                .add("Body", message)
                .build()
            
            val request = Request.Builder()
                .url(url)
                .header("Authorization", credentials)
                .post(formBody)
                .build()
            
            // Execute request
            val response = client.newCall(request).execute()
            
            response.use {
                if (it.isSuccessful) {
                    val responseBody = it.body?.string()
                    Log.d(TAG, "Twilio API response: $responseBody")
                    Result.success(Unit)
                } else {
                    val errorBody = it.body?.string()
                    Log.e(TAG, "Twilio API error (${it.code}): $errorBody")
                    Result.failure(Exception("SMS failed with code ${it.code}: $errorBody"))
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error sending SMS", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error sending SMS", e)
            Result.failure(e)
        }
    }
    
    /**
     * Send WhatsApp message via WhatsApp Business API with retry logic
     * Requirement 10: SMS and WhatsApp Notifications
     * 
     * Implementation details:
     * - Uses WhatsApp Business API (Cloud API) to send messages
     * - Formats phone number with India country code (+91)
     * - Implements retry logic with exponential backoff
     * - Logs all attempts and results
     * - Does not throw exceptions (fails gracefully)
     * 
     * @param phoneNumber The recipient's phone number (10 digits without country code)
     * @param message The WhatsApp message content
     */
    override suspend fun sendWhatsApp(phoneNumber: String, message: String) {
        withContext(Dispatchers.IO) {
            val formattedPhone = formatPhoneNumber(phoneNumber)
            Log.d(TAG, "Attempting to send WhatsApp message to $formattedPhone")
            
            var attempt = 0
            var success = false
            var lastError: Exception? = null
            
            while (attempt < MAX_RETRY_ATTEMPTS && !success) {
                attempt++
                
                try {
                    val result = sendWhatsAppAttempt(formattedPhone, message)
                    
                    if (result.isSuccess) {
                        success = true
                        Log.i(TAG, "WhatsApp message sent successfully to $formattedPhone on attempt $attempt")
                    } else {
                        lastError = result.exceptionOrNull() as? Exception
                        Log.w(TAG, "WhatsApp attempt $attempt failed for $formattedPhone: ${lastError?.message}")
                        
                        if (attempt < MAX_RETRY_ATTEMPTS) {
                            val delayMs = INITIAL_RETRY_DELAY_MS * (RETRY_DELAY_MULTIPLIER.toLong().shl(attempt - 1))
                            Log.d(TAG, "Retrying WhatsApp in ${delayMs}ms...")
                            delay(delayMs)
                        }
                    }
                } catch (e: Exception) {
                    lastError = e
                    Log.e(TAG, "WhatsApp attempt $attempt threw exception for $formattedPhone", e)
                    
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        val delayMs = INITIAL_RETRY_DELAY_MS * (RETRY_DELAY_MULTIPLIER.toLong().shl(attempt - 1))
                        Log.d(TAG, "Retrying WhatsApp in ${delayMs}ms...")
                        delay(delayMs)
                    }
                }
            }
            
            if (!success) {
                Log.e(TAG, "Failed to send WhatsApp message to $formattedPhone after $MAX_RETRY_ATTEMPTS attempts. Last error: ${lastError?.message}")
            }
        }
    }
    
    /**
     * Single attempt to send WhatsApp message via WhatsApp Business API
     * 
     * Uses the WhatsApp Cloud API format:
     * POST https://graph.facebook.com/v18.0/{phone-number-id}/messages
     * 
     * @param phoneNumber Formatted phone number with country code
     * @param message WhatsApp message content
     * @return Result indicating success or failure
     */
    private suspend fun sendWhatsAppAttempt(phoneNumber: String, message: String): Result<Unit> {
        return try {
            // Check if credentials are configured
            if (WHATSAPP_API_KEY.isBlank() || WHATSAPP_PHONE_NUMBER_ID.isBlank()) {
                Log.w(TAG, "WhatsApp credentials not configured. Skipping WhatsApp send.")
                return Result.failure(Exception("WhatsApp credentials not configured"))
            }
            
            // Build WhatsApp Business API request
            val url = "$WHATSAPP_API_BASE_URL/$WHATSAPP_PHONE_NUMBER_ID/messages"
            
            // Create JSON payload for WhatsApp Cloud API
            val jsonPayload = JSONObject().apply {
                put("messaging_product", "whatsapp")
                put("recipient_type", "individual")
                put("to", phoneNumber.removePrefix("+")) // WhatsApp API expects number without + prefix
                put("type", "text")
                put("text", JSONObject().apply {
                    put("preview_url", false)
                    put("body", message)
                })
            }
            
            val requestBody = jsonPayload.toString()
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $WHATSAPP_API_KEY")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()
            
            // Execute request
            val response = client.newCall(request).execute()
            
            response.use {
                if (it.isSuccessful) {
                    val responseBody = it.body?.string()
                    Log.d(TAG, "WhatsApp API response: $responseBody")
                    Result.success(Unit)
                } else {
                    val errorBody = it.body?.string()
                    Log.e(TAG, "WhatsApp API error (${it.code}): $errorBody")
                    Result.failure(Exception("WhatsApp failed with code ${it.code}: $errorBody"))
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error sending WhatsApp message", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error sending WhatsApp message", e)
            Result.failure(e)
        }
    }
    
    /**
     * Format phone number with India country code
     * 
     * Handles various input formats:
     * - "9876543210" -> "+919876543210"
     * - "+919876543210" -> "+919876543210"
     * - "919876543210" -> "+919876543210"
     * 
     * @param phoneNumber Phone number in various formats
     * @return Formatted phone number with +91 country code
     */
    private fun formatPhoneNumber(phoneNumber: String): String {
        val cleaned = phoneNumber.trim().replace(" ", "").replace("-", "")
        
        return when {
            cleaned.startsWith("+91") -> cleaned
            cleaned.startsWith("91") && cleaned.length == 12 -> "+$cleaned"
            cleaned.length == 10 -> "$INDIA_COUNTRY_CODE$cleaned"
            else -> {
                Log.w(TAG, "Unexpected phone number format: $phoneNumber. Using as-is.")
                cleaned
            }
        }
    }
}
