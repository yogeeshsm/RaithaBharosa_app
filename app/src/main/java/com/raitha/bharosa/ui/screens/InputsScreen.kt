package com.raitha.bharosa.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raitha.bharosa.data.*
import com.raitha.bharosa.engine.*
import com.raitha.bharosa.ui.theme.*
import com.raitha.bharosa.viewmodel.AppViewModel

data class SoilField(
    val id: String, 
    val label: String, 
    val min: Float, 
    val max: Float, 
    val step: Float = 1f, 
    val unit: String = "",
    val description: String = ""
)

@Composable
fun InputsScreen(viewModel: AppViewModel) {
    val lang by viewModel.lang.collectAsState()
    val soilHistory by viewModel.soilHistory.collectAsState()
    val profile by viewModel.profile.collectAsState()

    val initial = soilHistory.firstOrNull()
    var n by remember { mutableFloatStateOf(initial?.n?.toFloat() ?: 45f) }
    var p by remember { mutableFloatStateOf(initial?.p?.toFloat() ?: 30f) }
    var k by remember { mutableFloatStateOf(initial?.k?.toFloat() ?: 50f) }
    var pH by remember { mutableFloatStateOf(initial?.pH?.toFloat() ?: 7.0f) }
    var organicMatter by remember { mutableFloatStateOf(initial?.organicMatter?.toFloat() ?: 0.6f) }
    var moisture by remember { mutableFloatStateOf(initial?.moisture?.toFloat() ?: 42f) }
    var temperature by remember { mutableFloatStateOf(initial?.temperature?.toFloat() ?: 26f) }

    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var showToast by remember { mutableStateOf(false) }

    val fields = listOf(
        // Nitrogen (N) - ICAR Standard: kg/ha (Available Nitrogen)
        // Low: <280, Medium: 280-560, High: >560 kg/ha
        SoilField(
            "n", 
            if (lang == "kn") "ನೈಟ್ರೋಜನ್ (N)" else "Nitrogen (N)", 
            0f, 800f, 10f, 
            "kg/ha",
            if (lang == "kn") "ಲಭ್ಯವಿರುವ ನೈಟ್ರೋಜನ್" else "Available Nitrogen"
        ),
        // Phosphorus (P₂O₅) - ICAR Standard: kg/ha (Available Phosphorus)
        // Low: <11, Medium: 11-25, High: >25 kg/ha
        SoilField(
            "p", 
            if (lang == "kn") "ರಂಜಕ (P₂O₅)" else "Phosphorus (P₂O₅)", 
            0f, 100f, 1f, 
            "kg/ha",
            if (lang == "kn") "ಲಭ್ಯವಿರುವ ರಂಜಕ" else "Available Phosphorus"
        ),
        // Potassium (K₂O) - ICAR Standard: kg/ha (Available Potassium)
        // Low: <110, Medium: 110-280, High: >280 kg/ha
        SoilField(
            "k", 
            if (lang == "kn") "ಪೊಟ್ಯಾಸಿಯಮ್ (K₂O)" else "Potassium (K₂O)", 
            0f, 600f, 10f, 
            "kg/ha",
            if (lang == "kn") "ಲಭ್ಯವಿರುವ ಪೊಟ್ಯಾಸಿಯಮ್" else "Available Potassium"
        ),
        // pH - ICAR Standard: pH scale (0-14)
        // Acidic: <6.5, Neutral: 6.5-7.5, Alkaline: >7.5
        SoilField(
            "pH", 
            if (lang == "kn") "pH ಮಟ್ಟ" else "pH Level", 
            4.0f, 9.0f, 0.1f, 
            "",
            if (lang == "kn") "ಮಣ್ಣಿನ ಆಮ್ಲತೆ/ಕ್ಷಾರತೆ" else "Soil Acidity/Alkalinity"
        ),
        // Organic Carbon - ICAR Standard: % (Percentage)
        // Low: <0.5%, Medium: 0.5-0.75%, High: >0.75%
        SoilField(
            "organicMatter", 
            if (lang == "kn") "ಸಾವಯವ ಇಂಗಾಲ (OC)" else "Organic Carbon (OC)", 
            0f, 2.0f, 0.05f, 
            "%",
            if (lang == "kn") "ಮಣ್ಣಿನ ಸಾವಯವ ಇಂಗಾಲ" else "Soil Organic Carbon"
        ),
        // Soil Moisture - Field Capacity: % (Percentage)
        // Varies by soil type: Sandy (10-20%), Loamy (25-35%), Clay (35-45%)
        SoilField(
            "moisture", 
            if (lang == "kn") "ಮಣ್ಣಿನ ತೇವಾಂಶ" else "Soil Moisture", 
            0f, 100f, 1f, 
            "%",
            if (lang == "kn") "ಮಣ್ಣಿನ ನೀರಿನ ಅಂಶ" else "Soil Water Content"
        ),
        // Soil Temperature - ICAR Standard: °C (Celsius)
        // Optimal for most crops: 20-30°C
        SoilField(
            "temperature", 
            if (lang == "kn") "ಮಣ್ಣಿನ ತಾಪಮಾನ" else "Soil Temperature", 
            10f, 45f, 1f, 
            "°C",
            if (lang == "kn") "ಮಣ್ಣಿನ ಉಷ್ಣತೆ" else "Soil Heat"
        )
    )

    fun getValue(id: String) = when (id) {
        "n" -> n; "p" -> p; "k" -> k; "pH" -> pH
        "organicMatter" -> organicMatter; "moisture" -> moisture; else -> temperature
    }

    fun setValue(id: String, v: Float) = when (id) {
        "n" -> n = v; "p" -> p = v; "k" -> k = v; "pH" -> pH = v
        "organicMatter" -> organicMatter = v; "moisture" -> moisture = v; else -> temperature = v
    }

    // Analyzing overlay
    if (isAnalyzing) {
        Box(
            Modifier.fillMaxSize().background(BrandDeep.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(56.dp), strokeWidth = 4.dp)
                Text(viewModel.t("analyzingSoil"), fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp, color = Color.White)
                Text("Running Raitha-Bharosa Hub Diagnostic Engine",
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1500)
            val soilData = SoilData(
                dbId = 0,
                n = n.toDouble(),
                p = p.toDouble(),
                k = k.toDouble(),
                moisture = moisture.toDouble(),
                temperature = temperature.toDouble(),
                pH = pH.toDouble(),
                organicMatter = organicMatter.toDouble(),
                timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).format(java.util.Date())
            )
            viewModel.addSoilReading(soilData)
            val crop = profile?.primaryCrop ?: CropType.Rice
            val score = calculateSoilScore(soilData)
            val decisions = analyzeSoil(soilData, crop)
            val healthCard = getSoilHealthCard(soilData)
            val fertRecs = getFertilizerCalculations(soilData, crop)
            analysisResult = AnalysisResult(score, decisions, healthCard, fertRecs)
            isAnalyzing = false
            showToast = true
        }
        return
    }

    // Analysis Result Dialog
    val currentResult = analysisResult
    if (currentResult != null) { val result = currentResult
        AlertDialog(
            onDismissRequest = { analysisResult = null },
            title = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).border(6.dp, BrandBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${result.score}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = BrandDeep)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(viewModel.t("soilHealthReport"), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text("${viewModel.t("summary")}: ${result.healthCard.grade.name}", fontSize = 10.sp,
                        color = Color.Gray, letterSpacing = 1.sp)
                }
            },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Nutrient Status
                    Text(viewModel.t("nutrientStatus"), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                        color = Color.Gray, letterSpacing = 1.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        result.healthCard.analysis.take(3).forEach { item ->
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(item.nutrient.split(" ").first(), fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold, color = Color.Gray)
                                    Text(
                                        if (lang == "kn") item.statusKn else item.status,
                                        fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                                        color = when (item.status) {
                                            "Low" -> NutrientLow; "High" -> NutrientHigh; else -> NutrientMedium
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Fertilizer Recs
                    Text(viewModel.t("fertilizerCalc"), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                        color = Color.Gray, letterSpacing = 1.sp)
                    val neededRecs = result.fertilizerRecs.filter { it.quantity > 0 }
                    if (neededRecs.isEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                            shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                                Text("All nutrient levels are optimal. No fertilizers needed!",
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                            }
                        }
                    } else {
                        neededRecs.forEach { rec ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                shape = RoundedCornerShape(14.dp)) {
                                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White),
                                        contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Eco, contentDescription = null, tint = BrandDanger, modifier = Modifier.size(20.dp))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text("${rec.fertilizer}: ${rec.quantity} kg/ha",
                                            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                        Text(if (lang == "kn") rec.explanationKn else rec.explanation,
                                            fontSize = 9.sp, color = Color.Gray, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }

                    // Verdict
                    Card(colors = CardDefaults.cardColors(containerColor = BrandBg),
                        shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(viewModel.t("finalVerdict"), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                                color = BrandDeep, letterSpacing = 1.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                when {
                                    result.score > 70 -> if (lang == "kn") "ನಿಮ್ಮ ಮಣ್ಣು ಅತ್ಯುತ್ತಮ ಸ್ಥಿತಿ. ಕನಿಷ್ಠ ಗೊಬ್ಬರ ಸಾಕು." else "Your soil is in excellent condition. Minimal fertilizer required."
                                    result.score > 40 -> if (lang == "kn") "ಮಣ್ಣಿನ ಆರೋಗ್ಯ ಸಾಧಾರಣ. ಶಿಫಾರಸು ಗೊಬ್ಬರ ಹಾಕಿ." else "Soil health is moderate. Follow the recommended fertilizer plan."
                                    else -> if (lang == "kn") "ಗಂಭೀರ ಕೊರತೆ. ತಕ್ಷಣ ಮಣ್ಣಿನ ಸುಧಾರಣೆ ಅಗತ್ಯ." else "Critical deficiencies detected. Immediate soil correction required."
                                },
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.addPost(
                                authorName = profile?.name ?: "Farmer",
                                authorCrop = profile?.primaryCrop ?: CropType.Rice,
                                message = "I just analyzed my soil for ${profile?.primaryCrop?.displayName ?: "Rice"}. Soil Health Score: ${result.score}/100. Grade: ${result.healthCard.grade.name}.",
                                category = "Update", topic = "Soil"
                            )
                            analysisResult = null
                            showToast = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandDeep),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(viewModel.t("shareHub"), fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { analysisResult = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(viewModel.t("close")) }
                }
            },
            dismissButton = null,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Toast
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    if (lang == "kn") "ಮಣ್ಣಿನ ಆರೋಗ್ಯ ಡೇಟಾ" else "Soil Health Data", 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    if (lang == "kn") "ICAR ಮಾನದಂಡಗಳ ಆಧಾರದ ಮೇಲೆ ಮಣ್ಣಿನ ಪರೀಕ್ಷೆ" 
                    else "Soil Testing Based on ICAR Standards", 
                    fontSize = 12.sp, 
                    color = Color.Gray
                )
                Spacer(Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFDCFCE7)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            if (lang == "kn") "ಭಾರತೀಯ ಕೃಷಿ ಸಂಶೋಧನಾ ಮಂಡಳಿ (ICAR) ಪ್ರಮಾಣಿತ"
                            else "Indian Council of Agricultural Research (ICAR) Certified",
                            fontSize = 10.sp,
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            fields.forEach { field ->
                val value = getValue(field.id)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Header with icon and label
                        Row(
                            Modifier.fillMaxWidth(), 
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp), 
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Science, 
                                        contentDescription = null, 
                                        tint = BrandDeep, 
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        field.label, 
                                        fontSize = 15.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface
                                    )
                                }
                                if (field.description.isNotEmpty()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        field.description,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(start = 28.dp)
                                    )
                                }
                            }
                            // Current value display
                            Surface(
                                color = BrandBg, 
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    "${if (field.step >= 1f) value.toInt() else String.format("%.2f", value)} ${field.unit}",
                                    fontSize = 14.sp, 
                                    fontWeight = FontWeight.ExtraBold, 
                                    color = BrandDeep,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        
                        // Slider
                        Slider(
                            value = value,
                            onValueChange = { setValue(field.id, it) },
                            valueRange = field.min..field.max,
                            steps = if (field.step >= 1f) 0 else ((field.max - field.min) / field.step).toInt() - 1,
                            colors = SliderDefaults.colors(
                                thumbColor = BrandDeep, 
                                activeTrackColor = BrandDeep,
                                inactiveTrackColor = Color.Gray.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Min/Max range labels
                        Row(
                            Modifier.fillMaxWidth(), 
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${if (field.step >= 1f) field.min.toInt() else field.min} ${field.unit}",
                                fontSize = 10.sp, 
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${if (field.step >= 1f) field.max.toInt() else field.max} ${field.unit}",
                                fontSize = 10.sp, 
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // ICAR Standard Reference
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF0F9FF)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    when (field.id) {
                                        "n" -> if (lang == "kn") "ICAR ಮಾನದಂಡ: ಕಡಿಮೆ <280, ಮಧ್ಯಮ 280-560, ಹೆಚ್ಚು >560" 
                                               else "ICAR Standard: Low <280, Medium 280-560, High >560"
                                        "p" -> if (lang == "kn") "ICAR ಮಾನದಂಡ: ಕಡಿಮೆ <11, ಮಧ್ಯಮ 11-25, ಹೆಚ್ಚು >25"
                                               else "ICAR Standard: Low <11, Medium 11-25, High >25"
                                        "k" -> if (lang == "kn") "ICAR ಮಾನದಂಡ: ಕಡಿಮೆ <110, ಮಧ್ಯಮ 110-280, ಹೆಚ್ಚು >280"
                                               else "ICAR Standard: Low <110, Medium 110-280, High >280"
                                        "pH" -> if (lang == "kn") "ಆದರ್ಶ ವ್ಯಾಪ್ತಿ: 6.5-7.5 (ತಟಸ್ಥ)"
                                                else "Optimal Range: 6.5-7.5 (Neutral)"
                                        "organicMatter" -> if (lang == "kn") "ICAR ಮಾನದಂಡ: ಕಡಿಮೆ <0.5%, ಮಧ್ಯಮ 0.5-0.75%, ಹೆಚ್ಚು >0.75%"
                                                           else "ICAR Standard: Low <0.5%, Medium 0.5-0.75%, High >0.75%"
                                        "moisture" -> if (lang == "kn") "ಮರಳು 10-20%, ಮಣ್ಣು 25-35%, ಜೇಡಿಮಣ್ಣು 35-45%"
                                                      else "Sandy 10-20%, Loamy 25-35%, Clay 35-45%"
                                        "temperature" -> if (lang == "kn") "ಆದರ್ಶ ವ್ಯಾಪ್ತಿ: 20-30°C"
                                                         else "Optimal Range: 20-30°C"
                                        else -> ""
                                    },
                                    fontSize = 9.sp,
                                    color = Color(0xFF0369A1),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { isAnalyzing = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandDeep),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(viewModel.t("saveAnalyze"), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(80.dp))
        }

        // Toast
        AnimatedVisibility(
            visible = showToast,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
        ) {
            LaunchedEffect(showToast) {
                if (showToast) { kotlinx.coroutines.delay(2500); showToast = false }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandDeep),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Text(viewModel.t("saveSuccess"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
