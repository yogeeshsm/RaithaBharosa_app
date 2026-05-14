package com.raitha.bharosa.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.raitha.bharosa.data.CropType
import com.raitha.bharosa.data.UserRole
import com.raitha.bharosa.data.Skill
import com.raitha.bharosa.data.AvailabilityStatus
import com.raitha.bharosa.data.BookingStatus
import com.raitha.bharosa.data.PaymentStatus
import com.raitha.bharosa.data.PricingType
import com.raitha.bharosa.data.AvailabilityWindow

class Converters {
    private val gson = Gson()
    
    // Existing converters
    @TypeConverter
    fun fromCropType(value: CropType): String {
        return value.name
    }

    @TypeConverter
    fun toCropType(value: String): CropType {
        return try {
            CropType.valueOf(value)
        } catch (e: Exception) {
            CropType.Rice // Default fallback
        }
    }
    
    // New converters for labour system
    
    // Skill list converter
    @TypeConverter
    fun fromSkillList(value: List<Skill>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toSkillList(value: String): List<Skill> {
        val type = object : TypeToken<List<Skill>>() {}.type
        return gson.fromJson(value, type)
    }
    
    // Skill-to-Int map converter (for experience years)
    @TypeConverter
    fun fromSkillExperienceMap(value: Map<Skill, Int>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toSkillExperienceMap(value: String): Map<Skill, Int> {
        val type = object : TypeToken<Map<Skill, Int>>() {}.type
        return gson.fromJson(value, type)
    }
    
    // String list converter (for photo URLs)
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }
    
    // AvailabilityWindow list converter
    @TypeConverter
    fun fromAvailabilityWindowList(value: List<AvailabilityWindow>?): String? {
        return value?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toAvailabilityWindowList(value: String?): List<AvailabilityWindow>? {
        return value?.let {
            val type = object : TypeToken<List<AvailabilityWindow>>() {}.type
            gson.fromJson(it, type)
        }
    }
    
    // Enum converters for all new enums
    
    @TypeConverter
    fun fromUserRole(value: UserRole): String {
        return value.name
    }
    
    @TypeConverter
    fun toUserRole(value: String): UserRole {
        return UserRole.valueOf(value)
    }
    
    @TypeConverter
    fun fromSkill(value: Skill): String {
        return value.name
    }
    
    @TypeConverter
    fun toSkill(value: String): Skill {
        return Skill.valueOf(value)
    }
    
    @TypeConverter
    fun fromAvailabilityStatus(value: AvailabilityStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toAvailabilityStatus(value: String): AvailabilityStatus {
        return AvailabilityStatus.valueOf(value)
    }
    
    @TypeConverter
    fun fromBookingStatus(value: BookingStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toBookingStatus(value: String): BookingStatus {
        return BookingStatus.valueOf(value)
    }
    
    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toPaymentStatus(value: String): PaymentStatus {
        return PaymentStatus.valueOf(value)
    }
    
    @TypeConverter
    fun fromPricingType(value: PricingType): String {
        return value.name
    }
    
    @TypeConverter
    fun toPricingType(value: String): PricingType {
        return PricingType.valueOf(value)
    }
}
