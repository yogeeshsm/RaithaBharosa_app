package com.raitha.bharosa.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CropType(val displayName: String) {
    Rice("Rice"),
    Maize("Maize"),
    Wheat("Wheat"),
    Cotton("Cotton"),
    Groundnut("Groundnut"),
    Chickpea("Chickpea"),
    Mustard("Mustard"),
    Moong("Moong"),
    Watermelon("Watermelon")
}

enum class Season { Kharif, Rabi, Zaid }

enum class SoilStatus { Poor, Fair, Good, Excellent }

data class CartItem(
    val id: String,
    val name: String,
    val price: Int,
    val quantity: Int,
    val image: String = "",
    val desc: String = ""
) {
    val product: ProductRef get() = ProductRef(id, name, price, desc)
    data class ProductRef(val id: String, val name: String, val price: Int, val desc: String = "")
}

data class GovScheme(
    val id: String,
    val name: String,
    val nameKn: String,
    val description: String,
    val descriptionKn: String,
    val url: String,
    val tag: String,
    val documents: List<String> = emptyList(),
    val documentsKn: List<String> = emptyList(),
    val deadline: String = "",
    val deadlineKn: String = "",
    val contact: String = "",
    val contactKn: String = ""
) {
    val portal: String get() = url
    val ministry: String get() = tag
}

data class SoilAnalysisItem(
    val nutrient: String,
    val nutrientKn: String,
    val status: String,
    val statusKn: String,
    val recommendation: String,
    val recommendationKn: String,
    val fertilizer: String
)

data class SoilHealthCard(
    val score: Int,
    val grade: SoilStatus,
    val analysis: List<SoilAnalysisItem>
)

@Entity(tableName = "soil_data")
data class SoilData(
    @PrimaryKey(autoGenerate = true) val dbId: Int = 0,
    val n: Double,
    val p: Double,
    val k: Double,
    val moisture: Double,
    val temperature: Double,
    val pH: Double,
    val organicMatter: Double,
    val timestamp: String
)

@Entity(tableName = "profile")
data class FarmerProfile(
    @PrimaryKey val dbId: Int = 1,
    val name: String,
    val village: String,
    val district: String,
    val landArea: Double,
    val primaryCrop: CropType
)

data class Comment(
    val id: String,
    val authorName: String,
    val message: String,
    val timestamp: String
) {
    val author: String get() = authorName
}

data class Post(
    val id: String,
    val authorName: String,
    val authorCrop: CropType,
    val message: String,
    val timestamp: String,
    val likes: Int,
    val isLiked: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val category: String,
    val topic: String
) {
    val author: String get() = authorName
    val crop: String get() = authorCrop.displayName
    val likedByMe: Boolean get() = isLiked
    val time: String get() = timestamp.take(10)
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String,
    val title: String,
    val dueDate: String,
    val isCompleted: Boolean,
    val category: String,
    val titleKn: String = title,
    val time: String = dueDate.take(10),
    val priority: String = "Medium"
) {
    val done: Boolean get() = isCompleted
}

data class Decision(
    val type: String,
    val status: String,
    val message: String,
    val messageKn: String,
    val reason: String,
    val reasonKn: String
)

data class FertilizerRec(
    val type: String,
    val fertilizer: String,
    val quantity: Int,
    val explanation: String,
    val explanationKn: String
)

data class SeasonAdvice(
    val crops: List<CropType>,
    val window: String,
    val tip: String
)

data class RotationSuggestion(val crop: String, val benefit: String, val season: String)

data class RotationAdvice(
    val next: List<CropType>,
    val reason: String,
    val suggestions: List<RotationSuggestion> = emptyList()
)

data class WeatherDay(
    val day: String,
    val temp: Int,
    val condition: String,
    val humidity: Int,
    val advice: String,
    val adviceKn: String
)

data class Product(
    val id: String,
    val name: String,
    val price: Int,
    val desc: String,
    val originalPrice: Int = (price * 1.25).toInt(),
    val category: String = "Fertilizer",
    val badge: String = "Popular",
    val description: String = desc
)

data class AnalysisResult(
    val score: Int,
    val decisions: List<Decision>,
    val healthCard: SoilHealthCard,
    val fertilizerRecs: List<FertilizerRec>
)

val GOV_SCHEMES = listOf(
    GovScheme(
        id = "pmkisan",
        name = "PM-KISAN",
        nameKn = "ಪಿಎಂ-ಕಿಸಾನ್",
        description = "Income support of ₹6,000/year to all landholding farmers.",
        descriptionKn = "ಎಲ್ಲಾ ಭೂಹಿಡುವಳಿ ರೈತರಿಗೆ ವರ್ಷಕ್ಕೆ ₹6,000 ಆದಾಯ ಬೆಂಬಲ.",
        url = "https://pmkisan.gov.in/",
        tag = "Financial",
        documents = listOf("Aadhaar Card", "Land Ownership Records (RTC)", "Bank Account Details"),
        documentsKn = listOf("ಆಧಾರ್ ಕಾರ್ಡ್", "ಭೂ ಮಾಲೀಕತ್ವದ ದಾಖಲೆಗಳು (RTC)", "ಬ್ಯಾಂಕ್ ಉಳಿತಾಯ ಖಾತೆ"),
        deadline = "Ongoing Enrollment",
        deadlineKn = "ನಿರಂತರ ನೋಂದಣಿ",
        contact = "155261 / 1800115526 (Toll Free)"
    ),
    GovScheme(
        id = "pmfby",
        name = "PM Fasal Bima Yojana",
        nameKn = "ಪಿಎಂ ಫಸಲ್ ಬಿಮಾ ಯೋಜನೆ",
        description = "Crop insurance against natural calamities and pests.",
        descriptionKn = "ನೈಸರ್ಗಿಕ ವಿಕೋಪ ಮತ್ತು ಕೀಟಗಳ ವಿರುದ್ಧ ಬೆಳೆ ವಿಮೆ.",
        url = "https://pmfby.gov.in/",
        tag = "Insurance",
        documents = listOf("RTC (Pahani)", "Sowing Certificate", "Aadhaar Card", "Bank Passbook"),
        documentsKn = listOf("ಆರ್‌ಟಿಸಿ (ಪಹಣಿ)", "ಬಿತ್ತನೆ ದೃಢೀಕರಣ ಪತ್ರ", "ಆಧಾರ್ ಕಾರ್ಡ್", "ಬ್ಯಾಂಕ್ ಪಾಸ್ ಬುಕ್"),
        deadline = "July 31st for Kharif / Dec 31st for Rabi",
        deadlineKn = "ಖಾರಿಫ್‌ಗೆ ಜುಲೈ 31 / ರಬಿಗೆ ಡಿಸೆಂಬರ್ 31",
        contact = "Contact Local Bank or CS Centres"
    ),
    GovScheme(
        id = "shc",
        name = "Soil Health Card",
        nameKn = "ಮಣ್ಣಿನ ಆರೋಗ್ಯ ಕಾರ್ಡ್",
        description = "Detailed analysis of soil and fertilizer recommendations.",
        descriptionKn = "ಮಣ್ಣಿನ ವಿವರವಾದ ವಿಶ್ಲೇಷಣೆ ಮತ್ತು ರಸಗೊಬ್ಬರ ಶಿಫಾರಸುಗಳು.",
        url = "https://www.soilhealth.dac.gov.in/",
        tag = "Soil",
        documents = listOf("Soil Sample", "Aadhaar Card", "Mobile Number"),
        documentsKn = listOf("ಮಣ್ಣಿನ ಮಾದರಿ", "ಆಧಾರ್ ಕಾರ್ಡ್", "ಮೊಬೈಲ್ ಸಂಖ್ಯೆ"),
        deadline = "Visit local Raitha Samparka Kendra",
        deadlineKn = "ಸ್ಥಳೀಯ ರೈತ ಸಂಪರ್ಕ ಕೇಂದ್ರಕ್ಕೆ ಭೇಟಿ ನೀಡಿ",
        contact = "Nearest RSK (Raitha Samparka Kendra)"
    ),
    GovScheme(
        id = "bhoomi",
        name = "Bhoomi Karnataka",
        nameKn = "ಭೂಮಿ ಕರ್ನಾಟಕ",
        description = "Online land records (RTC) and ownership details portal.",
        descriptionKn = "ಆನ್‌ಲೈನ್ ಭೂ ದಾಖಲೆಗಳು (RTC) ಮತ್ತು ಮಾಲೀಕತ್ವದ ವಿವರಗಳ ಪೋರ್ಟಲ್.",
        url = "https://landrecords.karnataka.gov.in/",
        tag = "Records",
        documents = listOf("Survey Number", "Hissa Number"),
        documentsKn = listOf("ಸರ್ವೆ ನಂಬರ್", "ಹಿಸ್ಸಾ ನಂಬರ್"),
        deadline = "Available 24/7 Online",
        deadlineKn = "ಆನ್‌ಲೈನ್‌ನಲ್ಲಿ 24/7 ಲಭ್ಯವಿದೆ",
        contact = "Bhoomi Monitoring Cell: 080-22113390"
    )
)

val PRODUCTS = listOf(
    Product("1", "NPK 19:19:19", 1250, "Water-soluble fertilizer for balanced crop growth and high yields."),
    Product("2", "Urea (46% Nitrogen)", 450, "Promotes vigorous green foliage and rapid vegetative development."),
    Product("3", "Potash (MOP)", 1100, "Enhances fruit quality, size, and resistance to environmental stress."),
    Product("4", "DAP Fertilizer", 1450, "Phosphorus-rich fertilizer for strong root establishment in young crops."),
    Product("5", "Ammonium Sulfate", 980, "Provides essential nitrogen and sulfur for oilseeds and pulses."),
    Product("6", "Systemic Fungicide", 550, "Prevents and cures fungal infections like leaf spot and root rot."),
    Product("7", "Herbi-Guard Spray", 890, "Selective herbicide to clear weeds without affecting your main crop."),
    Product("8", "Bio-Stimulant Gold", 620, "Improves nutrient uptake and enhances overall plant immunity naturally.")
)
