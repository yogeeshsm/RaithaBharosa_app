package com.raitha.bharosa.utils

import android.content.Context
import android.util.Log

/**
 * Utility class to validate external service credentials
 * and provide helpful feedback during development and setup.
 */
object CredentialValidator {
    
    private const val TAG = "CredentialValidator"
    
    /**
     * Validate all credentials and log detailed results
     * @param context Application context
     * @return true if all credentials are valid
     */
    fun validateAllCredentials(context: Context): Boolean {
        val credentialManager = CredentialManager.getInstance(context)
        
        Log.d(TAG, "========================================")
        Log.d(TAG, "Starting Credential Validation")
        Log.d(TAG, "========================================")
        
        var allValid = true
        
        // Validate Twilio
        allValid = validateTwilio(credentialManager) && allValid
        
        // Validate WhatsApp
        allValid = validateWhatsApp(credentialManager) && allValid
        
        // Validate Razorpay
        allValid = validateRazorpay(credentialManager) && allValid
        
        Log.d(TAG, "========================================")
        if (allValid) {
            Log.d(TAG, "✅ All credentials are configured correctly!")
        } else {
            Log.e(TAG, "❌ Some credentials are missing or invalid")
            Log.e(TAG, "Please check EXTERNAL_SERVICES_SETUP.md for setup instructions")
        }
        Log.d(TAG, "========================================")
        
        return allValid
    }
    
    /**
     * Validate Twilio credentials
     */
    private fun validateTwilio(credentialManager: CredentialManager): Boolean {
        Log.d(TAG, "\n--- Twilio SMS API ---")
        
        val accountSid = credentialManager.getTwilioAccountSid()
        val authToken = credentialManager.getTwilioAuthToken()
        val phoneNumber = credentialManager.getTwilioPhoneNumber()
        
        val sidValid = validateCredential(
            "TWILIO_ACCOUNT_SID",
            accountSid,
            "Should start with 'AC' and be 34 characters long"
        ) { it.startsWith("AC") && it.length == 34 }
        
        val tokenValid = validateCredential(
            "TWILIO_AUTH_TOKEN",
            authToken,
            "Should be 32 characters long"
        ) { it.length == 32 }
        
        val phoneValid = validateCredential(
            "TWILIO_PHONE_NUMBER",
            phoneNumber,
            "Should be in E.164 format (e.g., +919876543210)"
        ) { it.startsWith("+") && it.length >= 10 }
        
        val isConfigured = credentialManager.isTwilioConfigured()
        
        return if (isConfigured && sidValid && tokenValid && phoneValid) {
            Log.d(TAG, "✅ Twilio credentials are valid")
            true
        } else {
            Log.e(TAG, "❌ Twilio credentials are invalid or missing")
            false
        }
    }
    
    /**
     * Validate WhatsApp credentials
     */
    private fun validateWhatsApp(credentialManager: CredentialManager): Boolean {
        Log.d(TAG, "\n--- WhatsApp Business API ---")
        
        val apiKey = credentialManager.getWhatsAppApiKey()
        val phoneNumberId = credentialManager.getWhatsAppPhoneNumberId()
        val businessAccountId = credentialManager.getWhatsAppBusinessAccountId()
        
        val keyValid = validateCredential(
            "WHATSAPP_API_KEY",
            apiKey,
            "Should be a valid Meta access token (starts with 'EAA')"
        ) { it.startsWith("EAA") && it.length > 50 }
        
        val phoneIdValid = validateCredential(
            "WHATSAPP_PHONE_NUMBER_ID",
            phoneNumberId,
            "Should be a numeric ID"
        ) { it.all { char -> char.isDigit() } && it.length > 5 }
        
        val businessIdValid = validateCredential(
            "WHATSAPP_BUSINESS_ACCOUNT_ID",
            businessAccountId,
            "Should be a numeric ID"
        ) { it.all { char -> char.isDigit() } && it.length > 5 }
        
        val isConfigured = credentialManager.isWhatsAppConfigured()
        
        return if (isConfigured && keyValid && phoneIdValid) {
            Log.d(TAG, "✅ WhatsApp credentials are valid")
            true
        } else {
            Log.e(TAG, "❌ WhatsApp credentials are invalid or missing")
            if (!businessIdValid) {
                Log.w(TAG, "⚠️  Business Account ID is optional but recommended")
            }
            false
        }
    }
    
    /**
     * Validate Razorpay credentials
     */
    private fun validateRazorpay(credentialManager: CredentialManager): Boolean {
        Log.d(TAG, "\n--- Razorpay Payment Gateway ---")
        
        val keyId = credentialManager.getRazorpayKeyId()
        val keySecret = credentialManager.getRazorpayKeySecret()
        
        val keyIdValid = validateCredential(
            "RAZORPAY_KEY_ID",
            keyId,
            "Should start with 'rzp_test_' or 'rzp_live_'"
        ) { it.startsWith("rzp_test_") || it.startsWith("rzp_live_") }
        
        val keySecretValid = validateCredential(
            "RAZORPAY_KEY_SECRET",
            keySecret,
            "Should be a valid secret key"
        ) { it.length > 10 }
        
        val isConfigured = credentialManager.isRazorpayConfigured()
        
        // Check if using test or live mode
        if (keyId.startsWith("rzp_test_")) {
            Log.d(TAG, "ℹ️  Using Razorpay TEST mode")
        } else if (keyId.startsWith("rzp_live_")) {
            Log.d(TAG, "ℹ️  Using Razorpay LIVE mode")
        }
        
        return if (isConfigured && keyIdValid && keySecretValid) {
            Log.d(TAG, "✅ Razorpay credentials are valid")
            true
        } else {
            Log.e(TAG, "❌ Razorpay credentials are invalid or missing")
            false
        }
    }
    
    /**
     * Validate a single credential with custom validation logic
     */
    private fun validateCredential(
        name: String,
        value: String,
        hint: String,
        validator: (String) -> Boolean
    ): Boolean {
        return when {
            value.isEmpty() -> {
                Log.e(TAG, "❌ $name: Not set")
                Log.e(TAG, "   Hint: $hint")
                false
            }
            value.contains("your_") -> {
                Log.e(TAG, "❌ $name: Using placeholder value")
                Log.e(TAG, "   Hint: $hint")
                false
            }
            !validator(value) -> {
                Log.e(TAG, "❌ $name: Invalid format")
                Log.e(TAG, "   Hint: $hint")
                false
            }
            else -> {
                Log.d(TAG, "✅ $name: Valid")
                true
            }
        }
    }
    
    /**
     * Generate a credential setup report
     * @param context Application context
     * @return Human-readable report string
     */
    fun generateSetupReport(context: Context): String {
        val credentialManager = CredentialManager.getInstance(context)
        val validation = credentialManager.validateLabourSystemCredentials()
        
        val report = StringBuilder()
        report.appendLine("=== External Services Setup Report ===\n")
        
        // Overall status
        if (validation.isValid) {
            report.appendLine("Status: ✅ All services configured\n")
        } else {
            report.appendLine("Status: ❌ Configuration incomplete\n")
        }
        
        // Twilio status
        report.appendLine("Twilio SMS API:")
        if (credentialManager.isTwilioConfigured()) {
            report.appendLine("  ✅ Configured")
            report.appendLine("  Account SID: ${maskCredential(credentialManager.getTwilioAccountSid())}")
            report.appendLine("  Phone Number: ${credentialManager.getTwilioPhoneNumber()}")
        } else {
            report.appendLine("  ❌ Not configured")
            report.appendLine("  Missing: TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_PHONE_NUMBER")
        }
        report.appendLine()
        
        // WhatsApp status
        report.appendLine("WhatsApp Business API:")
        if (credentialManager.isWhatsAppConfigured()) {
            report.appendLine("  ✅ Configured")
            report.appendLine("  API Key: ${maskCredential(credentialManager.getWhatsAppApiKey())}")
            report.appendLine("  Phone Number ID: ${credentialManager.getWhatsAppPhoneNumberId()}")
        } else {
            report.appendLine("  ❌ Not configured")
            report.appendLine("  Missing: WHATSAPP_API_KEY, WHATSAPP_PHONE_NUMBER_ID")
        }
        report.appendLine()
        
        // Razorpay status
        report.appendLine("Razorpay Payment Gateway:")
        if (credentialManager.isRazorpayConfigured()) {
            report.appendLine("  ✅ Configured")
            val keyId = credentialManager.getRazorpayKeyId()
            val mode = if (keyId.startsWith("rzp_test_")) "TEST" else "LIVE"
            report.appendLine("  Mode: $mode")
            report.appendLine("  Key ID: ${maskCredential(keyId)}")
        } else {
            report.appendLine("  ❌ Not configured")
            report.appendLine("  Missing: RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET")
        }
        report.appendLine()
        
        // Missing credentials
        if (!validation.isValid) {
            report.appendLine("Missing Credentials:")
            validation.missingCredentials.forEach { credential ->
                report.appendLine("  - $credential")
            }
            report.appendLine()
            report.appendLine("Please refer to EXTERNAL_SERVICES_SETUP.md for setup instructions.")
        }
        
        report.appendLine("\n======================================")
        
        return report.toString()
    }
    
    /**
     * Mask sensitive credential for display
     */
    private fun maskCredential(credential: String): String {
        return if (credential.length > 8) {
            "${credential.take(4)}...${credential.takeLast(4)}"
        } else {
            "****"
        }
    }
    
    /**
     * Check if the app is ready for production
     * @param context Application context
     * @return true if all production requirements are met
     */
    fun isProductionReady(context: Context): Boolean {
        val credentialManager = CredentialManager.getInstance(context)
        
        // Check if all credentials are configured
        if (!credentialManager.validateLabourSystemCredentials().isValid) {
            Log.e(TAG, "Production check failed: Missing credentials")
            return false
        }
        
        // Check if using live Razorpay keys
        val razorpayKeyId = credentialManager.getRazorpayKeyId()
        if (razorpayKeyId.startsWith("rzp_test_")) {
            Log.w(TAG, "Production check warning: Using Razorpay TEST mode")
            Log.w(TAG, "Switch to LIVE mode keys before production deployment")
            return false
        }
        
        Log.d(TAG, "✅ App is ready for production deployment")
        return true
    }
}
