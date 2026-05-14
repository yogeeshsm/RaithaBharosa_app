package com.raitha.bharosa.engine

import com.raitha.bharosa.data.*
import com.raitha.bharosa.data.api.WeatherApiService
import com.raitha.bharosa.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

fun analyzeSoil(data: SoilData, crop: CropType): List<Decision> {
    val decisions = mutableListOf<Decision>()

    if (data.moisture > 70) {
        decisions.add(
            Decision(
                type = "Sowing", status = "Warning",
                message = "Wait — soil moisture is too high",
                messageKn = "ಕಾಯಿರಿ — ಮಣ್ಣಿನ ತೇವಾಂಶ ಹೆಚ್ಚಿದೆ",
                reason = "Current moisture is ${data.moisture.toInt()}%, ideal is 30-60%.",
                reasonKn = "ಪ್ರಸ್ತುತ ತೇವಾಂಶ ${data.moisture.toInt()}% ಇದೆ, 30-60% ಇರುವುದು ಸೂಕ್ತ."
            )
        )
    } else if (data.moisture < 20) {
        decisions.add(
            Decision(
                type = "Sowing", status = "Danger",
                message = "Not Recommended — Too Dry",
                messageKn = "ಶಿಫಾರಸು ಮಾಡಲಾಗಿಲ್ಲ — ತುಂಬಾ ಒಣ",
                reason = "Soil is too dry. Irrigate first.",
                reasonKn = "ಮಣ್ಣು ತುಂಬಾ ಒಣಗಿದೆ. ಮೊದಲು ನೀರು ಹಾಯಿಸಿ."
            )
        )
    }

    val fertRecs = getFertilizerCalculations(data, crop)
    fertRecs.forEach { rec ->
        if (rec.quantity > 0) {
            decisions.add(
                Decision(
                    type = "Fertilizer", status = "Warning",
                    message = "Apply ${rec.fertilizer}: ${rec.quantity} kg/ha",
                    messageKn = "${rec.fertilizer} ಪ್ರಯೋಗಿಸಿ: ${rec.quantity} ಕೆಜಿ/ಹೆ",
                    reason = rec.explanation,
                    reasonKn = rec.explanationKn
                )
            )
        }
    }

    if (data.moisture < 40) {
        decisions.add(
            Decision(
                type = "Irrigation", status = "Warning",
                message = "Irrigate Today",
                messageKn = "ಇಂದು ನೀರು ಹಾರಿಸಿ",
                reason = "Moisture level is dropping below optimal.",
                reasonKn = "ತೇವಾಂಶ ಮಟ್ಟ ಕಡಿಮೆಯಾಗುತ್ತಿದೆ."
            )
        )
    }

    if (decisions.isEmpty()) {
        decisions.add(
            Decision(
                type = "Sowing", status = "Safe",
                message = "Conditions Optimal ✓",
                messageKn = "ಸ್ಥಿತಿಯು ಸೂಕ್ತವಾಗಿದೆ ✓",
                reason = "Soil and moisture levels are perfect for sowing.",
                reasonKn = "ಮಣ್ಣು ಮತ್ತು ತೇವಾಂಶದ ಮಟ್ಟಗಳು ವ್ಯವಸ್ಥಿತವಾಗಿವೆ."
            )
        )
    }

    return decisions
}

fun getFertilizerCalculations(data: SoilData, crop: CropType): List<FertilizerRec> {
    data class NPK(val n: Double, val p: Double, val k: Double)

    val requirements = mapOf(
        CropType.Rice to NPK(120.0, 60.0, 40.0),
        CropType.Wheat to NPK(100.0, 50.0, 50.0),
        CropType.Maize to NPK(120.0, 60.0, 40.0),
        CropType.Cotton to NPK(100.0, 50.0, 50.0),
        CropType.Groundnut to NPK(25.0, 50.0, 75.0),
        CropType.Chickpea to NPK(20.0, 50.0, 30.0),
        CropType.Mustard to NPK(80.0, 40.0, 40.0),
        CropType.Moong to NPK(20.0, 40.0, 30.0),
        CropType.Watermelon to NPK(100.0, 50.0, 100.0)
    )

    val req = requirements[crop] ?: NPK(120.0, 60.0, 40.0)

    val nDeficit = maxOf(0.0, req.n - data.n)
    val pDeficit = maxOf(0.0, req.p - data.p)
    val kDeficit = maxOf(0.0, req.k - data.k)

    val urea = (nDeficit / 0.46).toInt()
    val dap = (pDeficit / 0.46).toInt()
    val mop = (kDeficit / 0.60).toInt()

    return listOf(
        FertilizerRec(
            type = "Nitrogen", fertilizer = "Urea", quantity = urea,
            explanation = "Nitrogen level (${data.n.toInt()}) is below ICAR standard for ${crop.displayName}. Apply $urea kg/ha Urea.",
            explanationKn = "${crop.displayName} ಗಾಗಿ ಸಾರಜನಕ ಮಟ್ಟ (${data.n.toInt()}) ಐಸಿಎಆರ್ ಕನಿಷ್ಠ ಮಟ್ಟಕ್ಕಿಂತ ಕಡಿಮೆ. $urea ಕೆಜಿ/ಹೆ ಯೂರಿಯಾ ಬಳಸಿ."
        ),
        FertilizerRec(
            type = "Phosphorus", fertilizer = "DAP", quantity = dap,
            explanation = "Phosphorus level (${data.p.toInt()}) is below ICAR standard for ${crop.displayName}. Apply $dap kg/ha DAP.",
            explanationKn = "${crop.displayName} ಗಾಗಿ ರಂಜಕ ಮಟ್ಟ (${data.p.toInt()}) ಐಸಿಎಆರ್ ಕನಿಷ್ಠ ಮಟ್ಟಕ್ಕಿಂತ ಕಡಿಮೆ. $dap ಕೆಜಿ/ಹೆ ಡಿಎಪಿ ಬಳಸಿ."
        ),
        FertilizerRec(
            type = "Potassium", fertilizer = "MOP", quantity = mop,
            explanation = "Potassium level (${data.k.toInt()}) is below ICAR standard for ${crop.displayName}. Apply $mop kg/ha MOP.",
            explanationKn = "${crop.displayName} ಗಾಗಿ ಪೊಟ್ಯಾಸಿಯಮ್ ಮಟ್ಟ (${data.k.toInt()}) ಐಸಿಎಆರ್ ಕನಿಷ್ಠ ಮಟ್ಟಕ್ಕಿಂತ ಕಡಿಮೆ. $mop ಕೆಜಿ/ಹೆ ಎಂಒಪಿ ಬಳಸಿ."
        )
    )
}

fun getSoilHealthCard(data: SoilData): SoilHealthCard {
    val analysis = listOf(
        SoilAnalysisItem(
            nutrient = "Nitrogen (N)", nutrientKn = "ಸಾರಜನಕ (N)",
            status = if (data.n < 280) "Low" else if (data.n > 560) "High" else "Medium",
            statusKn = if (data.n < 280) "ಕಡಿಮೆ" else if (data.n > 560) "ಹೆಚ್ಚು" else "ಮಧ್ಯಮ",
            recommendation = when {
                data.n < 280 -> "High deficit. Increase Urea application."
                data.n > 560 -> "Optimal levels. No Nitrogen needed."
                else -> "Maintain levels with moderate Urea."
            },
            recommendationKn = when {
                data.n < 280 -> "ಹೆಚ್ಚಿನ ಕೊರತೆ. ಯೂರಿಯಾ ಬಳಕೆ ಹೆಚ್ಚಿಸಿ."
                data.n > 560 -> "ಸೂಕ್ತ ಮಟ್ಟ. ಸಾರಜನಕದ ಅಗತ್ಯವಿಲ್ಲ."
                else -> "ಮಧ್ಯಮ ಯೂರಿಯಾದೊಂದಿಗೆ ಮಟ್ಟ ಕಾಪಾಡಿಕೊಳ್ಳಿ."
            },
            fertilizer = "Urea"
        ),
        SoilAnalysisItem(
            nutrient = "Phosphorus (P)", nutrientKn = "ರಂಜಕ (P)",
            status = if (data.p < 10) "Low" else if (data.p > 25) "High" else "Medium",
            statusKn = if (data.p < 10) "ಕಡಿಮೆ" else if (data.p > 25) "ಹೆಚ್ಚು" else "ಮಧ್ಯಮ",
            recommendation = when {
                data.p < 10 -> "Significant P deficiency. Add DAP."
                data.p > 25 -> "Rich in Phosphorus. Skip DAP."
                else -> "Apply balanced DAP."
            },
            recommendationKn = when {
                data.p < 10 -> "ಗಣನೀಯ ರಂಜಕ ಕೊರತೆ. DAP ಸೇರಿಸಿ."
                data.p > 25 -> "ರಂಜಕ ಸಮೃದ್ಧ. DAP ಬಿಟ್ಟುಬಿಡಿ."
                else -> "ಸಮತೋಲಿತ DAP ಬಳಸಿ."
            },
            fertilizer = "DAP"
        ),
        SoilAnalysisItem(
            nutrient = "Potassium (K)", nutrientKn = "ಪೊಟ್ಯಾಶ್ (K)",
            status = if (data.k < 110) "Low" else if (data.k > 280) "High" else "Medium",
            statusKn = if (data.k < 110) "ಕಡಿಮೆ" else if (data.k > 280) "ಹೆಚ್ಚು" else "ಮಧ್ಯಮ",
            recommendation = when {
                data.k < 110 -> "Potash levels low. Use MOP."
                data.k > 280 -> "Excess Potash. No action needed."
                else -> "Standard MOP application."
            },
            recommendationKn = when {
                data.k < 110 -> "ಪೊಟ್ಯಾಶ್ ಮಟ್ಟ ಕಡಿಮೆ. MOP ಬಳಸಿ."
                data.k > 280 -> "ಹೆಚ್ಚುವರಿ ಪೊಟ್ಯಾಶ್. ಅಗತ್ಯವಿಲ್ಲ."
                else -> "ಸಾಮಾನ್ಯ MOP ಬಳಕೆ."
            },
            fertilizer = "MOP"
        ),
        SoilAnalysisItem(
            nutrient = "pH Level", nutrientKn = "ಮಣ್ಣಿನ pH ಮಟ್ಟ",
            status = if (data.pH < 6.5) "Low" else if (data.pH > 7.5) "High" else "Medium",
            statusKn = if (data.pH < 6.5) "ಕಡಿಮೆ" else if (data.pH > 7.5) "ಹೆಚ್ಚು" else "ಮಧ್ಯಮ",
            recommendation = when {
                data.pH < 6.5 -> "Soil is Acidic. Apply lime."
                data.pH > 7.5 -> "Soil is Alkaline. Use Gypsum."
                else -> "pH is Optimal."
            },
            recommendationKn = when {
                data.pH < 6.5 -> "ಮಣ್ಣು ಆಮ್ಲೀಯ. ಸುಣ್ಣದ ಪುಡಿ ಬಳಸಿ."
                data.pH > 7.5 -> "ಮಣ್ಣು ಕ್ಷಾರೀಯ. ಜಿಪ್ಸಮ್ ಬಳಸಿ."
                else -> "pH ಮಟ್ಟ ಸೂಕ್ತ."
            },
            fertilizer = "Soil Amendment"
        ),
        SoilAnalysisItem(
            nutrient = "Organic Matter (%)", nutrientKn = "ಸಾವಯವ ಸಮೃದ್ಧಿ (%)",
            status = if (data.organicMatter < 0.5) "Low" else if (data.organicMatter > 0.75) "High" else "Medium",
            statusKn = if (data.organicMatter < 0.5) "ಕಡಿಮೆ" else if (data.organicMatter > 0.75) "ಹೆಚ್ಚು" else "ಮಧ್ಯಮ",
            recommendation = when {
                data.organicMatter < 0.5 -> "Very Low. Add manure."
                data.organicMatter > 0.75 -> "Excellent organic levels."
                else -> "Maintain with compost."
            },
            recommendationKn = when {
                data.organicMatter < 0.5 -> "ತುಂಬಾ ಕಡಿಮೆ. ಗೊಬ್ಬರ ಸೇರಿಸಿ."
                data.organicMatter > 0.75 -> "ಅತ್ಯುತ್ತಮ ಸಾವಯವ ಮಟ್ಟ."
                else -> "ಕಾಂಪೋಸ್ಟ್‌ನೊಂದಿಗೆ ಕಾಪಾಡಿಕೊಳ್ಳಿ."
            },
            fertilizer = "Manure/Compost"
        )
    )

    val score = calculateSoilScore(data)
    val grade = when {
        score > 85 -> SoilStatus.Excellent
        score > 65 -> SoilStatus.Good
        score < 40 -> SoilStatus.Poor
        else -> SoilStatus.Fair
    }

    return SoilHealthCard(score, grade, analysis)
}

fun calculateSoilScore(data: SoilData): Int {
    fun getNScore(n: Double) = when {
        n < 280 -> (n / 280) * 40
        n <= 560 -> 40 + ((n - 280) / 280) * 40
        else -> 80 + minOf(20.0, ((n - 560) / 100) * 20)
    }
    fun getPScore(p: Double) = when {
        p < 10 -> (p / 10) * 40
        p <= 25 -> 40 + ((p - 10) / 15) * 40
        else -> 80 + minOf(20.0, ((p - 25) / 10) * 20)
    }
    fun getKScore(k: Double) = when {
        k < 110 -> (k / 110) * 40
        k <= 280 -> 40 + ((k - 110) / 170) * 40
        else -> 80 + minOf(20.0, ((k - 280) / 100) * 20)
    }
    fun getPHScore(ph: Double) = when {
        ph < 6.5 -> maxOf(0.0, (ph / 6.5) * 60)
        ph <= 7.5 -> 100.0
        else -> maxOf(0.0, 100 - ((ph - 7.5) / 2.5) * 40)
    }
    fun getOMScore(om: Double) = when {
        om < 0.5 -> (om / 0.5) * 40
        om <= 0.75 -> 40 + ((om - 0.5) / 0.25) * 60
        else -> 100.0
    }
    return ((getNScore(data.n) + getPScore(data.p) + getKScore(data.k) + getPHScore(data.pH) + getOMScore(data.organicMatter)) / 5).toInt()
}

fun getCurrentSeason(): Season {
    val month = Calendar.getInstance().get(Calendar.MONTH) + 1
    return when {
        month in 6..10 -> Season.Kharif
        month == 11 || month <= 3 -> Season.Rabi
        else -> Season.Zaid
    }
}

fun getSeasonAdvice(season: Season): SeasonAdvice = when (season) {
    Season.Kharif -> SeasonAdvice(
        crops = listOf(CropType.Rice, CropType.Maize, CropType.Cotton),
        window = "June - July",
        tip = "Best time for high-water crops due to monsoon rainfall."
    )
    Season.Rabi -> SeasonAdvice(
        crops = listOf(CropType.Wheat, CropType.Mustard, CropType.Chickpea),
        window = "Nov - Dec",
        tip = "Ideal for cool-weather crops using residual soil moisture."
    )
    Season.Zaid -> SeasonAdvice(
        crops = listOf(CropType.Watermelon, CropType.Moong),
        window = "Mar - Apr",
        tip = "Requires consistent irrigation during summer months."
    )
}

// Overload accepting string season name and lang (used by CropsScreen)
fun getSeasonAdvice(seasonName: String, lang: String): SeasonAdviceDisplay {
    val base = when (seasonName) {
        "Rabi" -> getSeasonAdvice(Season.Rabi)
        "Zaid" -> getSeasonAdvice(Season.Zaid)
        else -> getSeasonAdvice(Season.Kharif)
    }
    return SeasonAdviceDisplay(
        crops = base.crops.map { it.displayName },
        window = base.window,
        tip = base.tip
    )
}

data class SeasonAdviceDisplay(val crops: List<String>, val window: String, val tip: String)

fun getRotationAdvice(prevCrop: CropType): RotationAdvice = when (prevCrop) {
    CropType.Rice -> buildRotationAdvice(listOf(CropType.Chickpea, CropType.Moong), "Rice depletes nitrogen; legumes restore fertility and structure.", "Kharif", "Nitrogen Fixation")
    CropType.Wheat -> buildRotationAdvice(listOf(CropType.Maize, CropType.Groundnut), "Breaking the cereal cycle prevents pest buildup.", "Kharif", "Pest Control")
    CropType.Cotton -> buildRotationAdvice(listOf(CropType.Rice, CropType.Wheat), "Helps in managing soil-borne pathogens.", "Kharif/Rabi", "Pathogen Control")
    else -> buildRotationAdvice(listOf(CropType.Rice, CropType.Wheat), "Maintain a balanced cycle between cereals and legumes.", "Kharif/Rabi", "Balance")
}

// Season-aware rotation advice (used by CropsScreen with season selection)
fun getRotationAdviceForSeason(prevCrop: CropType, selectedSeason: String): RotationAdvice {
    return when (selectedSeason) {
        "Kharif" -> getKharifRotationAdvice(prevCrop)
        "Rabi" -> getRabiRotationAdvice(prevCrop)
        "Zaid" -> getZaidRotationAdvice(prevCrop)
        else -> getRotationAdvice(prevCrop)
    }
}

private fun getKharifRotationAdvice(prevCrop: CropType): RotationAdvice = when (prevCrop) {
    CropType.Rice -> buildRotationAdvice(
        listOf(CropType.Maize, CropType.Cotton),
        "After rice, rotate to maize or cotton to break pest cycles.",
        "Kharif",
        "Pest Control"
    )
    CropType.Wheat -> buildRotationAdvice(
        listOf(CropType.Rice, CropType.Maize, CropType.Cotton),
        "After wheat, plant Kharif cereals or cotton.",
        "Kharif",
        "Nutrient Balance"
    )
    CropType.Cotton -> buildRotationAdvice(
        listOf(CropType.Rice, CropType.Maize),
        "After cotton, rotate to rice or maize for soil health.",
        "Kharif",
        "Soil Health"
    )
    CropType.Chickpea, CropType.Mustard -> buildRotationAdvice(
        listOf(CropType.Rice, CropType.Maize, CropType.Cotton),
        "After Rabi legumes/oilseeds, plant Kharif cereals.",
        "Kharif",
        "Nitrogen Utilization"
    )
    else -> buildRotationAdvice(
        listOf(CropType.Rice, CropType.Maize, CropType.Cotton),
        "Recommended Kharif crops for monsoon season.",
        "Kharif",
        "Monsoon Crops"
    )
}

private fun getRabiRotationAdvice(prevCrop: CropType): RotationAdvice = when (prevCrop) {
    CropType.Rice -> buildRotationAdvice(
        listOf(CropType.Wheat, CropType.Chickpea, CropType.Mustard),
        "After Kharif rice, plant Rabi wheat or legumes to restore nitrogen.",
        "Rabi",
        "Nitrogen Fixation"
    )
    CropType.Maize -> buildRotationAdvice(
        listOf(CropType.Wheat, CropType.Chickpea),
        "After maize, rotate to wheat or chickpea for balanced nutrition.",
        "Rabi",
        "Nutrient Balance"
    )
    CropType.Cotton -> buildRotationAdvice(
        listOf(CropType.Wheat, CropType.Chickpea, CropType.Mustard),
        "After cotton, plant Rabi cereals or legumes.",
        "Rabi",
        "Soil Recovery"
    )
    CropType.Wheat -> buildRotationAdvice(
        listOf(CropType.Chickpea, CropType.Mustard),
        "After wheat, rotate to legumes or oilseeds.",
        "Rabi",
        "Pest Control"
    )
    else -> buildRotationAdvice(
        listOf(CropType.Wheat, CropType.Chickpea, CropType.Mustard),
        "Recommended Rabi crops for winter season.",
        "Rabi",
        "Winter Crops"
    )
}

private fun getZaidRotationAdvice(prevCrop: CropType): RotationAdvice = when (prevCrop) {
    CropType.Wheat, CropType.Chickpea, CropType.Mustard -> buildRotationAdvice(
        listOf(CropType.Watermelon, CropType.Moong),
        "After Rabi crops, plant Zaid summer crops with irrigation.",
        "Zaid",
        "Summer Utilization"
    )
    CropType.Rice -> buildRotationAdvice(
        listOf(CropType.Moong, CropType.Watermelon),
        "After rice, plant short-duration Zaid crops.",
        "Zaid",
        "Quick Harvest"
    )
    CropType.Maize -> buildRotationAdvice(
        listOf(CropType.Moong, CropType.Watermelon),
        "After maize, rotate to Zaid legumes or melons.",
        "Zaid",
        "Soil Enrichment"
    )
    else -> buildRotationAdvice(
        listOf(CropType.Watermelon, CropType.Moong),
        "Recommended Zaid crops for summer season with irrigation.",
        "Zaid",
        "Summer Crops"
    )
}

private fun buildRotationAdvice(next: List<CropType>, reason: String, season: String, benefit: String): RotationAdvice {
    return RotationAdvice(
        next = next,
        reason = reason,
        suggestions = next.map { com.raitha.bharosa.data.RotationSuggestion(it.displayName, benefit, season) }
    )
}

suspend fun getWeatherForecast(lang: String, city: String = "Bangalore"): List<WeatherDay> {
    return try {
        withContext(Dispatchers.IO) {
            val apiService = WeatherApiService.create()
            val apiKey = BuildConfig.WEATHER_API_KEY
            
            if (apiKey.isBlank()) {
                // Fallback to mock data if no API key
                return@withContext getMockWeatherForecast(lang)
            }
            
            val response = apiService.getForecast(city, apiKey, count = 7)
            
            val days = if (lang == "kn")
                listOf("ಸೋಮ", "ಮಂಗಳ", "ಬುಧ", "ಗುರು", "ಶುಕ್ರ", "ಶನಿ", "ಭಾನು")
            else
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            
            response.list.take(7).mapIndexed { i, forecast ->
                val condition = forecast.weather.firstOrNull()?.main ?: "Clear"
                val advice = getWeatherAdvice(condition, forecast.main.temp, lang)
                
                WeatherDay(
                    day = days[i % days.size],
                    temp = forecast.main.temp.toInt(),
                    condition = condition,
                    humidity = forecast.main.humidity,
                    advice = advice.first,
                    adviceKn = advice.second
                )
            }
        }
    } catch (e: Exception) {
        // Fallback to mock data on error
        getMockWeatherForecast(lang)
    }
}

private fun getWeatherAdvice(condition: String, temp: Double, lang: String): Pair<String, String> {
    return when {
        condition.contains("Rain", ignoreCase = true) -> 
            "Rain likely. Avoid Fert." to "ಮಳೆ ಸಾಧ್ಯತೆ. ಗೊಬ್ಬರ ಹಾಕಬೇಡಿ."
        condition.contains("Cloud", ignoreCase = true) -> 
            "High Humidity detected." to "ಹೆಚ್ಚಿನ ತೇವಾಂಶ ಕಂಡುಬಂದಿದೆ."
        temp > 30 -> 
            "Hot day. Increase water." to "ಬಿಸಿಲು ಜಾಸ್ತಿ. ನೀರು ಹೆಚ್ಚಿಸಿ."
        temp < 20 -> 
            "Cool weather. Good for crops." to "ತಂಪಾದ ಹವಾಮಾನ. ಬೆಳೆಗಳಿಗೆ ಒಳ್ಳೆಯದು."
        else -> 
            "Optimal for Sowing." to "ಬಿತ್ತನೆಗೆ ಸೂಕ್ತ ಸಮಯ."
    }
}

private fun getMockWeatherForecast(lang: String): List<WeatherDay> {
    val days = if (lang == "kn")
        listOf("ಸೋಮ", "ಮಂಗಳ", "ಬುಧ", "ಗುರು", "ಶುಕ್ರ", "ಶನಿ", "ಭಾನು")
    else
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val conditions = listOf("Sunny", "Rainy", "Cloudy", "Sunny", "Rainy", "Cloudy", "Sunny")
    val adviceList = listOf(
        "Optimal for Sowing." to "ಬಿತ್ತನೆಗೆ ಸೂಕ್ತ ಸಮಯ.",
        "Rain likely. Avoid Fert." to "ಮಳೆ ಸಾಧ್ಯತೆ. ಗೊಬ್ಬರ ಹಾಕಬೇಡಿ.",
        "High Humidity detected." to "ಹೆಚ್ಚಿನ ತೇವಾಂಶ ಕಂಡುಬಂದಿದೆ.",
        "Clearing skies expected." to "ಸ್ಪಷ್ಟ ಹವಾಮಾನ ನಿರೀಕ್ಷಿಸಲಾಗಿದೆ.",
        "Perfect for harvesting." to "ಕೊಯಿಲಿಗೆ ಸೂಕ್ತ ಸಮಯ.",
        "Light winds expected." to "ಹಗುರವಾದ ಗಾಳಿ ನಿರೀಕ್ಷಿಸಲಾಗಿದೆ.",
        "Hot day. Increase water." to "ಬಿಸಿಲು ಜಾಸ್ತಿ. ನೀರು ಹೆಚ್ಚಿಸಿ."
    )
    val temps = listOf(29, 26, 27, 30, 28, 27, 32)
    return days.mapIndexed { i, day ->
        WeatherDay(
            day = day,
            temp = temps[i % temps.size],
            condition = conditions[i % conditions.size],
            humidity = 60 + (i * 5 % 20),
            advice = adviceList[i % adviceList.size].first,
            adviceKn = adviceList[i % adviceList.size].second
        )
    }
}
