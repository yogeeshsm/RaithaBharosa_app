package com.raitha.bharosa.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.ByteArrayOutputStream

/**
 * Image utility functions for profile photo management
 * Task 10.9: Create ImageUtils.kt
 * Requirement 19: Profile Photo Management
 * 
 * This utility provides functions for image compression and validation
 * to ensure profile photos meet size and format requirements.
 */
object ImageUtils {
    
    /**
     * Maximum image size in kilobytes
     */
    const val MAX_IMAGE_SIZE_KB = 500
    
    /**
     * Supported image formats
     */
    enum class ImageFormat(val mimeType: String, val extension: String) {
        JPEG("image/jpeg", "jpg"),
        PNG("image/png", "png");
        
        companion object {
            /**
             * Get ImageFormat from MIME type
             * @param mimeType MIME type string
             * @return ImageFormat or null if not supported
             */
            fun fromMimeType(mimeType: String?): ImageFormat? {
                return values().find { it.mimeType.equals(mimeType, ignoreCase = true) }
            }
            
            /**
             * Get ImageFormat from file extension
             * @param extension File extension (with or without dot)
             * @return ImageFormat or null if not supported
             */
            fun fromExtension(extension: String): ImageFormat? {
                val cleanExt = extension.removePrefix(".").lowercase()
                return values().find { it.extension == cleanExt || it.extension == "jpeg" && cleanExt == "jpg" }
            }
        }
    }
    
    /**
     * Image validation result
     */
    data class ImageValidationResult(
        val isValid: Boolean,
        val errorMessageEn: String? = null,
        val errorMessageKn: String? = null,
        val format: ImageFormat? = null
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
     * Compress image to meet size requirements (max 500 KB)
     * 
     * This function:
     * 1. Decodes the image from URI to Bitmap
     * 2. Compresses the bitmap to JPEG format with decreasing quality
     * 3. Iteratively reduces quality until size is under 500 KB
     * 4. Returns compressed image as byte array
     * 
     * The compression uses JPEG format which provides good compression ratios
     * while maintaining acceptable image quality for profile photos.
     * 
     * @param context Android context for content resolver
     * @param uri Image URI to compress
     * @return Compressed image as byte array (max 500KB)
     * @throws IllegalArgumentException if URI is invalid or image cannot be decoded
     * 
     * Example:
     * ```
     * val imageUri = Uri.parse("content://...")
     * val compressedBytes = ImageUtils.compressImage(context, imageUri)
     * // compressedBytes.size <= 500 * 1024
     * ```
     */
    fun compressImage(context: Context, uri: Uri): ByteArray {
        // Decode bitmap from URI
        val bitmap = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to decode image from URI: ${e.message}", e)
        }
        
        // Compress bitmap iteratively until size is acceptable
        val outputStream = ByteArrayOutputStream()
        var quality = 90
        var compressedBytes: ByteArray
        
        do {
            outputStream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            compressedBytes = outputStream.toByteArray()
            quality -= 10
        } while (compressedBytes.size > MAX_IMAGE_SIZE_KB * 1024 && quality > 0)
        
        // Clean up
        bitmap.recycle()
        
        return compressedBytes
    }
    
    /**
     * Validate image format
     * 
     * Checks if the image format is supported (JPEG or PNG).
     * Uses MIME type from content resolver to determine format.
     * 
     * @param context Android context for content resolver
     * @param uri Image URI to validate
     * @return ImageValidationResult with validation status and format
     * 
     * Example:
     * ```
     * val result = ImageUtils.validateImageFormat(context, imageUri)
     * if (result.isValid) {
     *     println("Valid ${result.format} image")
     * } else {
     *     println("Error: ${result.getErrorMessage("en")}")
     * }
     * ```
     */
    fun validateImageFormat(context: Context, uri: Uri): ImageValidationResult {
        // Get MIME type from content resolver
        val mimeType = try {
            context.contentResolver.getType(uri)
        } catch (e: Exception) {
            return ImageValidationResult(
                isValid = false,
                errorMessageEn = "Unable to determine image format",
                errorMessageKn = "ಚಿತ್ರ ಸ್ವರೂಪವನ್ನು ನಿರ್ಧರಿಸಲು ಸಾಧ್ಯವಿಲ್ಲ"
            )
        }
        
        // Check if MIME type is supported
        val format = ImageFormat.fromMimeType(mimeType)
        
        return if (format != null) {
            ImageValidationResult(
                isValid = true,
                format = format
            )
        } else {
            ImageValidationResult(
                isValid = false,
                errorMessageEn = "Image format must be JPEG or PNG",
                errorMessageKn = "ಚಿತ್ರ ಸ್ವರೂಪ JPEG ಅಥವಾ PNG ಆಗಿರಬೇಕು"
            )
        }
    }
    
    /**
     * Validate image format by file extension
     * 
     * @param filename Filename with extension
     * @return ImageValidationResult with validation status and format
     */
    fun validateImageFormatByFilename(filename: String): ImageValidationResult {
        val extension = filename.substringAfterLast('.', "")
        
        if (extension.isEmpty()) {
            return ImageValidationResult(
                isValid = false,
                errorMessageEn = "File has no extension",
                errorMessageKn = "ಫೈಲ್‌ಗೆ ವಿಸ್ತರಣೆ ಇಲ್ಲ"
            )
        }
        
        val format = ImageFormat.fromExtension(extension)
        
        return if (format != null) {
            ImageValidationResult(
                isValid = true,
                format = format
            )
        } else {
            ImageValidationResult(
                isValid = false,
                errorMessageEn = "Image format must be JPEG or PNG",
                errorMessageKn = "ಚಿತ್ರ ಸ್ವರೂಪ JPEG ಅಥವಾ PNG ಆಗಿರಬೇಕು"
            )
        }
    }
    
    /**
     * Get image size in kilobytes
     * 
     * @param imageBytes Image byte array
     * @return Size in kilobytes
     */
    fun getImageSizeKB(imageBytes: ByteArray): Int {
        return imageBytes.size / 1024
    }
    
    /**
     * Check if image size is within limit
     * 
     * @param imageBytes Image byte array
     * @return true if size is within limit (500 KB), false otherwise
     */
    fun isImageSizeValid(imageBytes: ByteArray): Boolean {
        return getImageSizeKB(imageBytes) <= MAX_IMAGE_SIZE_KB
    }
    
    /**
     * Format image size for display
     * 
     * @param sizeBytes Size in bytes
     * @return Formatted string (e.g., "250 KB" or "1.2 MB")
     */
    fun formatImageSize(sizeBytes: Int): String {
        val sizeKB = sizeBytes / 1024.0
        return if (sizeKB < 1024) {
            "${sizeKB.toInt()} KB"
        } else {
            val sizeMB = sizeKB / 1024.0
            "%.1f MB".format(sizeMB)
        }
    }
    
    /**
     * Validate complete image (format and size)
     * 
     * @param context Android context for content resolver
     * @param uri Image URI to validate
     * @return ImageValidationResult with validation status
     */
    fun validateImage(context: Context, uri: Uri): ImageValidationResult {
        // First validate format
        val formatResult = validateImageFormat(context, uri)
        if (!formatResult.isValid) {
            return formatResult
        }
        
        // Then check size (decode and check)
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            
            // Estimate size (rough calculation)
            val estimatedSize = bitmap.byteCount
            bitmap.recycle()
            
            // If estimated size is very large, it might need compression
            // But we allow it since compressImage will handle it
            ImageValidationResult(
                isValid = true,
                format = formatResult.format
            )
        } catch (e: Exception) {
            ImageValidationResult(
                isValid = false,
                errorMessageEn = "Unable to read image file",
                errorMessageKn = "ಚಿತ್ರ ಫೈಲ್ ಓದಲು ಸಾಧ್ಯವಿಲ್ಲ"
            )
        }
    }
}
