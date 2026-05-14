package com.raitha.bharosa.utils

import android.content.Context
import android.util.Log

/**
 * Example usage of CredentialManager for accessing external service credentials.
 * 
 * This file demonstrates how to:
 * 1. Initialize the CredentialManager
 * 2. Access different service credentials
 * 3. Validate credentials before use
 * 4. Handle missing or invalid credentials
 * 
 * NOTE: This is an example file for reference. Delete or move to a test package
 * if not needed in production.
 */
object CredentialManagerExample {
    
    private const val TAG = "CredentialExample"
    
    /**
     * Example: Initialize and validate credentials on app startup
     */
    fun initializeCredentials(context: Context) {
        Log.d(TAG, "Initializing credentials...")
        
        // Get CredentialManager instance
        val credentialManager = CredentialManager.getInstance(context)
        
        // Validate all credentials
        val validation = credentialManager.validateLabourSystemCredentials()
        
        if (validation.isValid) {
            Log.d(TAG, "✅ All credentials are configured correctly")
        } else {
            Log.e(TAG, "❌ Credential validation failed")
            Log.e(TAG, validation.getErrorMessage())
            
            // Generate and log detailed report
            val report = CredentialValidator.generateSetupReport(context)
            Log.e(TAG, report)
        }
    }
    
    /**
     * Example: Using Twilio credentials for SMS
     */
    fun sendSMSExample(context: Context, phoneNumber: String, message: String) {
        val credentialManager = CredentialManager.getInstance(context)
        
        // Check if Twilio is configured
        if (!credentialManager.isTwilioConfigured()) {
            Log.e(TAG, "Twilio is not configured. Cannot send SMS.")
            return
        }
        
        // Get Twilio credentials
        val accountSid = credentialManager.getTwilioAccountSid()
        val authToken = credentialManager.getTwilioAuthToken()
        val twilioPhone = credentialManager.getTwilioPhoneNumber()
        
        Log.d(TAG, "Sending SMS via Twilio...")
        Log.d(TAG, "From: $twilioPhone")
        Log.d(TAG, "To: $phoneNumber")
        
        // Use credentials with Twilio SDK or API
        // Example: NotificationService.sendSMS(phoneNumber, message)
    }
    
    /**
     * Example: Using WhatsApp credentials for messaging
     */
    fun sendWhatsAppExample(context: Context, phoneNumber: String, message: String) {
        val credentialManager = CredentialManager.getInstance(context)
        
        // Check if WhatsApp is configured
        if (!credentialManager.isWhatsAppConfigured()) {
            Log.e(TAG, "WhatsApp is not configured. Cannot send message.")
            return
        }
        
        // Get WhatsApp credentials
        val apiKey = credentialManager.getWhatsAppApiKey()
        val phoneNumberId = credentialManager.getWhatsAppPhoneNumberId()
        val businessAccountId = credentialManager.getWhatsAppBusinessAccountId()
        
        Log.d(TAG, "Sending WhatsApp message...")
        Log.d(TAG, "Phone Number ID: $phoneNumberId")
        Log.d(TAG, "To: $phoneNumber")
        
        // Use credentials with WhatsApp Business API
        // Example: NotificationService.sendWhatsApp(phoneNumber, message)
    }
    
    /**
     * Example: Using Razorpay credentials for payment
     */
    fun processPaymentExample(context: Context, amount: Int, bookingId: String) {
        val credentialManager = CredentialManager.getInstance(context)
        
        // Check if Razorpay is configured
        if (!credentialManager.isRazorpayConfigured()) {
            Log.e(TAG, "Razorpay is not configured. Cannot process payment.")
            return
        }
        
        // Get Razorpay credentials
        val keyId = credentialManager.getRazorpayKeyId()
        val keySecret = credentialManager.getRazorpayKeySecret()
        
        // Check if using test or live mode
        val mode = if (keyId.startsWith("rzp_test_")) "TEST" else "LIVE"
        Log.d(TAG, "Processing payment via Razorpay ($mode mode)...")
        Log.d(TAG, "Amount: ₹$amount")
        Log.d(TAG, "Booking ID: $bookingId")
        
        // Use credentials with Razorpay SDK
        // Example: RazorpayPaymentManager.initiatePayment(amount, bookingId, ...)
    }
    
    /**
     * Example: Checking individual service availability
     */
    fun checkServiceAvailability(context: Context) {
        val credentialManager = CredentialManager.getInstance(context)
        
        Log.d(TAG, "Checking service availability...")
        
        // Check Twilio
        if (credentialManager.isTwilioConfigured()) {
            Log.d(TAG, "✅ Twilio SMS: Available")
        } else {
            Log.w(TAG, "⚠️  Twilio SMS: Not configured")
        }
        
        // Check WhatsApp
        if (credentialManager.isWhatsAppConfigured()) {
            Log.d(TAG, "✅ WhatsApp: Available")
        } else {
            Log.w(TAG, "⚠️  WhatsApp: Not configured")
        }
        
        // Check Razorpay
        if (credentialManager.isRazorpayConfigured()) {
            Log.d(TAG, "✅ Razorpay: Available")
        } else {
            Log.w(TAG, "⚠️  Razorpay: Not configured")
        }
    }
    
    /**
     * Example: Graceful degradation when services are unavailable
     */
    fun sendNotificationWithFallback(context: Context, phoneNumber: String, message: String) {
        val credentialManager = CredentialManager.getInstance(context)
        
        // Try WhatsApp first (preferred)
        if (credentialManager.isWhatsAppConfigured()) {
            Log.d(TAG, "Sending notification via WhatsApp...")
            // sendWhatsApp(phoneNumber, message)
            return
        }
        
        // Fallback to SMS
        if (credentialManager.isTwilioConfigured()) {
            Log.d(TAG, "WhatsApp not available, falling back to SMS...")
            // sendSMS(phoneNumber, message)
            return
        }
        
        // No notification service available
        Log.e(TAG, "No notification service configured. Cannot send notification.")
        // Show in-app notification or queue for later
    }
    
    /**
     * Example: Production readiness check
     */
    fun checkProductionReadiness(context: Context): Boolean {
        Log.d(TAG, "Checking production readiness...")
        
        val isReady = CredentialValidator.isProductionReady(context)
        
        if (isReady) {
            Log.d(TAG, "✅ App is ready for production deployment")
        } else {
            Log.e(TAG, "❌ App is NOT ready for production")
            Log.e(TAG, "Please complete the following:")
            
            val credentialManager = CredentialManager.getInstance(context)
            
            // Check each service
            if (!credentialManager.isTwilioConfigured()) {
                Log.e(TAG, "  - Configure Twilio SMS credentials")
            }
            
            if (!credentialManager.isWhatsAppConfigured()) {
                Log.e(TAG, "  - Configure WhatsApp Business API credentials")
            }
            
            if (!credentialManager.isRazorpayConfigured()) {
                Log.e(TAG, "  - Configure Razorpay credentials")
            }
            
            // Check if using test mode
            val razorpayKeyId = credentialManager.getRazorpayKeyId()
            if (razorpayKeyId.startsWith("rzp_test_")) {
                Log.e(TAG, "  - Switch Razorpay to LIVE mode for production")
            }
        }
        
        return isReady
    }
    
    /**
     * Example: Getting other API credentials
     */
    fun getOtherCredentials(context: Context) {
        val credentialManager = CredentialManager.getInstance(context)
        
        // Get Weather API key
        val weatherApiKey = credentialManager.getWeatherApiKey()
        if (weatherApiKey.isNotEmpty()) {
            Log.d(TAG, "Weather API Key: ${weatherApiKey.take(10)}...")
        }
        
        // Get Mandi API key
        val mandiApiKey = credentialManager.getMandiApiKey()
        if (mandiApiKey.isNotEmpty()) {
            Log.d(TAG, "Mandi API Key: ${mandiApiKey.take(10)}...")
        }
        
        // Get any custom credential
        val customKey = credentialManager.getCredential("CUSTOM_API_KEY")
        if (customKey.isNotEmpty()) {
            Log.d(TAG, "Custom API Key found")
        }
    }
}
