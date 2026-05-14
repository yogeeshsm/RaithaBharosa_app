package com.raitha.bharosa.ui.screens

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
import com.raitha.bharosa.data.CropType
import com.raitha.bharosa.engine.getRotationAdvice
import com.raitha.bharosa.engine.getRotationAdviceForSeason
import com.raitha.bharosa.engine.getSeasonAdvice
import com.raitha.bharosa.ui.theme.*
import com.raitha.bharosa.viewmodel.AppViewModel

@Composable
fun CropsScreen(viewModel: AppViewModel) {
    val lang by viewModel.lang.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val tasks by viewModel.tasks.collectAsState()

    val seasons = listOf("Kharif", "Rabi", "Zaid")
    var selectedSeason by remember { mutableStateOf("Kharif") }

    val seasonAdvice = remember(selectedSeason, lang) { getSeasonAdvice(selectedSeason, lang) }
    val rotationAdvice = remember(profile?.primaryCrop, selectedSeason, lang) {
        getRotationAdviceForSeason(profile?.primaryCrop ?: CropType.Rice, selectedSeason)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(viewModel.t("crops"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(viewModel.t("tagline"), fontSize = 12.sp, color = Color.Gray)
        }

        // Season Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceVariant)
                .padding(4.dp)
        ) {
            seasons.forEach { season ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedSeason == season) Surface else Color.Transparent)
                        .clickable { selectedSeason = season }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(season, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        color = if (selectedSeason == season) BrandDeep else Color.Gray)
                }
            }
        }

        // Season Advice Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(BrandBg),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = BrandDeep, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(selectedSeason, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Text(viewModel.t("summary"), fontSize = 10.sp, color = Color.Gray, letterSpacing = 1.sp)
                    }
                }

                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BrandBg).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = BrandDeep, modifier = Modifier.size(18.dp))
                    Column {
                        Text(viewModel.t("sowingWindow"), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                            color = Color.Gray, letterSpacing = 1.sp)
                        Text(seasonAdvice.window, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OnBackground)
                    }
                }

                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF7ED)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = BrandWarning, modifier = Modifier.size(18.dp))
                    Text(seasonAdvice.tip, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(viewModel.t("recommendedCrops"), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                        color = Color.Gray, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        seasonAdvice.crops.forEach { crop ->
                            Surface(
                                color = BrandDeep,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Grass, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Text(crop, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Crop Rotation Advisor
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(BrandDeep),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Autorenew, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(viewModel.t("cropRotation"), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(viewModel.t("current") + ": ${profile?.primaryCrop?.displayName ?: "Rice"}",
                            fontSize = 11.sp, color = Color.Gray)
                    }
                }
                rotationAdvice.suggestions.forEachIndexed { i, suggestion ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(BrandBg),
                            contentAlignment = Alignment.Center) {
                            Text("${i + 1}", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = BrandDeep)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(suggestion.crop, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(suggestion.benefit, fontSize = 11.sp, color = Color.Gray)
                        }
                        Surface(color = BrandBg, shape = RoundedCornerShape(8.dp)) {
                            Text(suggestion.season, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                                color = BrandDeep, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        }

        // Growth Progress
        Card(
            colors = CardDefaults.cardColors(containerColor = Surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = BrandDeep, modifier = Modifier.size(24.dp))
                    Column {
                        Text(viewModel.t("growthProgress"), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${profile?.primaryCrop?.displayName ?: "Rice"} — ${viewModel.t("current")}",
                            fontSize = 11.sp, color = Color.Gray)
                    }
                }
                LinearProgressIndicator(
                    progress = { 0.45f },
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                    color = BrandDeep,
                    trackColor = BrandBg
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("45%", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BrandDeep)
                        Text(viewModel.t("progress"), fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Mar 20 → Jun 15", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnBackground)
                        Text(viewModel.t("harvest"), fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Daily Tasks
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(viewModel.t("dailyTasks"), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                color = Color.Gray, letterSpacing = 1.sp)
            Surface(color = BrandBg, shape = RoundedCornerShape(8.dp)) {
                Text("${tasks.count { it.done }}/${tasks.size}", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    color = BrandDeep, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }

        tasks.forEach { task ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (task.done) Color(0xFFF0FDF4) else Surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.clickable { viewModel.toggleTask(task.id) }
            ) {
                Row(Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (task.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (task.done) Color(0xFF16A34A) else Color.LightGray,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(if (lang == "kn") task.titleKn else task.title,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = if (task.done) Color(0xFF16A34A) else OnBackground)
                        Text(task.time, fontSize = 10.sp, color = Color.Gray)
                    }
                    Surface(
                        color = when (task.priority) {
                            "High" -> Color(0xFFFEE2E2); "Medium" -> Color(0xFFFEF3C7); else -> Color(0xFFF0FDF4)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(task.priority, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                            color = when (task.priority) {
                                "High" -> BrandDanger; "Medium" -> BrandWarning; else -> Color(0xFF16A34A)
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}
