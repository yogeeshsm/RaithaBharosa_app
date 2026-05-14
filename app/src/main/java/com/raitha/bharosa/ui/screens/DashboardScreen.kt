package com.raitha.bharosa.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raitha.bharosa.data.CropType
import com.raitha.bharosa.data.WeatherDay
import com.raitha.bharosa.engine.*
import com.raitha.bharosa.utils.generateAndSaveFarmReport
import com.raitha.bharosa.utils.generateAndSaveSoilHealthCard
import com.raitha.bharosa.ui.theme.*
import com.raitha.bharosa.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: AppViewModel) {
    val lang by viewModel.lang.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val soilHistory by viewModel.soilHistory.collectAsState()

    val latestSoil = soilHistory.firstOrNull()
        ?: com.raitha.bharosa.data.SoilData(
            dbId = 0,
            n = 45.0,
            p = 30.0,
            k = 50.0,
            moisture = 42.0,
            temperature = 26.0,
            pH = 7.0,
            organicMatter = 0.6,
            timestamp = ""
        )
    val score = calculateSoilScore(latestSoil)
    val decisions = analyzeSoil(latestSoil, profile?.primaryCrop ?: CropType.Rice)
    val healthCard = getSoilHealthCard(latestSoil)
    
    var forecast by remember { mutableStateOf<List<WeatherDay>>(emptyList()) }
    
    LaunchedEffect(lang) {
        forecast = getWeatherForecast(lang, profile?.district ?: "Bangalore")
    }

    var showReport by remember { mutableStateOf(false) }
    var showHealthCard by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // --- Modals ---
    if (showReport) {
        AlertDialog(
            onDismissRequest = { showReport = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Assessment, contentDescription = null, tint = BrandDeep, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(viewModel.t("performanceReport"), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReportRow(viewModel.t("soilHealth"), "$score/100",
                        color = if (score > 70) Color(0xFF22C55E) else if (score > 40) BrandWarning else BrandDanger)
                    ReportRow(viewModel.t("crops"), profile?.primaryCrop?.displayName ?: "Rice")
                    ReportRow(viewModel.t("efficiency"), "94%", color = Color(0xFF8B5CF6))
                    ReportRow(viewModel.t("lastReading"), SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()))

                    Card(colors = CardDefaults.cardColors(containerColor = BrandBg),
                        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(viewModel.t("criticalInsights"), fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold, color = BrandDeep, letterSpacing = 1.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("• Nitrogen levels are in optimal range for current stage.",
                                fontSize = 11.sp, color = Color.Gray)
                            Text("• Pest risk is low. Monitor moisture closely.",
                                fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            generateAndSaveFarmReport(context, profile, score, decisions)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandDeep),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(viewModel.t("downloadPDF"), fontWeight = FontWeight.Bold)
                    }
                    // WhatsApp Share
                    Button(
                        onClick = {
                            val shareText = "🌱 Raitha-Bharosa Soil Score: $score/100 | ${profile?.primaryCrop?.displayName} | ${decisions.firstOrNull()?.message}"
                            try {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    setPackage("com.whatsapp")
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://wa.me/?text=${Uri.encode(shareText)}"))
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("WhatsApp", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showReport = false }, modifier = Modifier.fillMaxWidth()) {
                        Text(viewModel.t("close"), color = Color.Gray)
                    }
                }
            },
            dismissButton = null,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showHealthCard) {
        AlertDialog(
            onDismissRequest = { showHealthCard = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                            .border(6.dp, BrandBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$score", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = BrandDeep)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(viewModel.t("healthCertificate"), fontWeight = FontWeight.ExtraBold)
                    Text(healthCard.grade.name.uppercase(), fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold, color = BrandDeep, letterSpacing = 2.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(viewModel.t("nutrientBreakdown"), fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold, color = Color.Gray, letterSpacing = 1.sp)
                    healthCard.analysis.forEach { item ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                            shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(if (lang == "kn") item.nutrientKn else item.nutrient,
                                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("💡 ${if (lang == "kn") item.recommendationKn else item.recommendation}",
                                        fontSize = 10.sp, color = Color.Gray)
                                }
                                Surface(
                                    color = when (item.status) {
                                        "Low" -> Color(0xFFFEE2E2); "High" -> Color(0xFFDEEBFF); else -> Color(0xFFDCFCE7)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        if (lang == "kn") item.statusKn else item.status,
                                        fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                                        color = when (item.status) {
                                            "Low" -> Color(0xFFDC2626); "High" -> Color(0xFF2563EB); else -> Color(0xFF16A34A)
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { generateAndSaveSoilHealthCard(context, profile, score, healthCard) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandDeep),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(viewModel.t("downloadHealthCard"), fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showHealthCard = false }, modifier = Modifier.fillMaxWidth()) {
                        Text(viewModel.t("close"), color = Color.Gray)
                    }
                }
            },
            dismissButton = null,
            shape = RoundedCornerShape(24.dp)
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(viewModel.t("appName"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(viewModel.t("tagline"), fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = {}) {
                Box {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Gray)
                    Box(Modifier.size(8.dp).clip(CircleShape).background(BrandDanger).align(Alignment.TopEnd))
                }
            }
        }

        // Warning Banner
        val warningDecision = decisions.firstOrNull { it.status != "Safe" }
        AnimatedVisibility(visible = warningDecision != null, enter = fadeIn(), exit = fadeOut()) {
            warningDecision?.let { decision ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFFED7AA))
                ) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFDE68A)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Warning, contentDescription = null, tint = BrandWarning, modifier = Modifier.size(20.dp)) }
                        Column {
                            Text(
                                if (lang == "kn") decision.messageKn else decision.message,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F)
                            )
                            Text(
                                if (lang == "kn") decision.reasonKn else decision.reason,
                                fontSize = 11.sp, color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }
        }

        // Stats Grid
        val statsItems = listOf(
            Triple(viewModel.t("sowingIndex"), "82%", Icons.Default.Grass) to Color(0xFF22C55E),
            Triple(viewModel.t("soilHealth"), "$score", Icons.Default.Science) to Color(0xFF3B82F6),
            Triple(viewModel.t("weather"), "28°C", Icons.Default.WbSunny) to Color(0xFFF59E0B),
            Triple(viewModel.t("efficiency"), "94", Icons.Default.TrendingUp) to Color(0xFF8B5CF6)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            statsItems.forEach { (triple, color) ->
                val (label, value, icon) = triple
                Card(
                    modifier = Modifier.weight(1f).height(110.dp)
                        .clickable(enabled = label == viewModel.t("soilHealth")) { showHealthCard = true },
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(14.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                .background(color.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp)) }
                        Column {
                            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = OnBackground)
                            Text(label, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }
        }

        // Quick Actions
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {},
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandDeep),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Grass, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(viewModel.t("sowNow"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = { showReport = true },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, BrandDeep)
            ) {
                Icon(Icons.Default.Assessment, contentDescription = null, tint = BrandDeep, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(viewModel.t("viewReport"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandDeep)
            }
        }

        // Decisions
        Text(viewModel.t("summary"), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
            color = Color.Gray, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp))
        decisions.forEach { decision ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    val (bgColor, iconColor, icon) = when (decision.status) {
                        "Safe" -> Triple(Color(0xFFDCFCE7), StatusSafe, Icons.Default.CheckCircle)
                        "Danger" -> Triple(Color(0xFFFEE2E2), StatusDanger, Icons.Default.Cancel)
                        else -> Triple(Color(0xFFFEF3C7), StatusWarning, Icons.Default.Warning)
                    }
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(bgColor),
                        contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(if (lang == "kn") decision.messageKn else decision.message,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(decision.type.uppercase(), fontSize = 9.sp, color = Color.Gray,
                            letterSpacing = 1.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                }
            }
        }

        // Weather Forecast
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(viewModel.t("weatherOutlook"), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                color = Color.Gray, letterSpacing = 1.sp)
            Surface(color = BrandBg, shape = RoundedCornerShape(8.dp)) {
                Text(viewModel.t("forecast7Day"), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                    color = BrandDeep, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(forecast) { day ->
                WeatherCard(day = day, lang = lang)
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun ReportRow(label: String, value: String, color: Color = OnBackground) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFF9FAFB)).padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
private fun WeatherCard(day: WeatherDay, lang: String) {
    Card(
        modifier = Modifier.width(160.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(day.day, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray)
                val (icon, tint) = when (day.condition) {
                    "Sunny" -> Icons.Default.WbSunny to Color(0xFFFBBF24)
                    "Rainy" -> Icons.Default.Umbrella to Color(0xFF60A5FA)
                    "Stormy" -> Icons.Default.Thunderstorm to Color(0xFFA78BFA)
                    else -> Icons.Default.Cloud to Color(0xFF9CA3AF)
                }
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${day.temp}°", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text("C", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp, start = 2.dp))
                }
                Text(day.condition, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
            Divider(color = Color(0xFFF3F4F6))
            Text(if (lang == "kn") day.adviceKn else day.advice,
                fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = BrandDeep,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
