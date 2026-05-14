package com.raitha.bharosa.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.raitha.bharosa.data.GOV_SCHEMES
import com.raitha.bharosa.data.GovScheme
import com.raitha.bharosa.engine.getSoilHealthCard
import com.raitha.bharosa.engine.calculateSoilScore
import com.raitha.bharosa.data.CropType
import com.raitha.bharosa.data.SoilData
import com.raitha.bharosa.utils.generateAndSaveSoilHealthCard
import com.raitha.bharosa.ui.theme.*
import com.raitha.bharosa.viewmodel.AppViewModel

@Composable
fun ProfileScreen(
    viewModel: AppViewModel,
    onCommunity: () -> Unit,
    onSignOut: () -> Unit = {}
) {
    val lang by viewModel.lang.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val soilHistory by viewModel.soilHistory.collectAsState()
    val firebaseEmail = FirebaseAuth.getInstance().currentUser?.email

    val latestSoil = soilHistory.firstOrNull()
        ?: SoilData(
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
    val healthCard = getSoilHealthCard(latestSoil)

    var showEligibility by remember { mutableStateOf(false) }
    var showSchemes by remember { mutableStateOf(false) }
    var showHealthCard by remember { mutableStateOf(false) }
    var showCommunity by remember { mutableStateOf(false) }

    // Eligibility states
    var ownsLand by remember { mutableStateOf(false) }
    var milletFarmer by remember { mutableStateOf(false) }
    var isTaxPayer by remember { mutableStateOf(false) }
    var lessThan2Hectare by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (showCommunity) {
        CommunityScreen(viewModel = viewModel, onBack = { showCommunity = false })
        return
    }

    // Eligibility Dialog
    if (showEligibility) {
        val eligibleSchemes = GOV_SCHEMES.filter { scheme ->
            when (scheme.id) {
                "pmkisan" -> ownsLand && !isTaxPayer
                "pmfby" -> ownsLand && (milletFarmer || true)
                "shc" -> ownsLand
                "bhoomi" -> ownsLand && lessThan2Hectare
                else -> false
            }
        }
        AlertDialog(
            onDismissRequest = { showEligibility = false },
            title = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = BrandDeep, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(viewModel.t("eligibility"), fontWeight = FontWeight.ExtraBold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Check your eligibility criteria:", fontSize = 12.sp, color = Color.Gray)
                    listOf(
                        Triple("ownsLand", "Owns agricultural land", ownsLand) to { ownsLand = !ownsLand },
                        Triple("millet", "Cultivates millet/food crops", milletFarmer) to { milletFarmer = !milletFarmer },
                        Triple("tax", "Is NOT an income tax payer", isTaxPayer) to { isTaxPayer = !isTaxPayer },
                        Triple("hectare", "Land < 2 hectares", lessThan2Hectare) to { lessThan2Hectare = !lessThan2Hectare }
                    ).forEach { (data, action) ->
                        val (_, label, checked) = data
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceVariant)
                                .clickable { action() }.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null, tint = if (checked) BrandDeep else Color.Gray
                            )
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    if (eligibleSchemes.isNotEmpty()) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)), shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("✅ Eligible Schemes:", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16A34A))
                                Spacer(Modifier.height(6.dp))
                                eligibleSchemes.forEach { scheme ->
                                    Text("• ${scheme.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                }
                            }
                        }
                    } else {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)), shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = BrandWarning, modifier = Modifier.size(16.dp))
                                Text("Check the criteria above to see eligible schemes.", fontSize = 11.sp, color = Color(0xFF78350F))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showEligibility = false }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandDeep), shape = RoundedCornerShape(14.dp)) {
                    Text(viewModel.t("close"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = null,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Health Card Dialog
    if (showHealthCard) {
        AlertDialog(
            onDismissRequest = { showHealthCard = false },
            title = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(72.dp).clip(CircleShape).border(6.dp, BrandBg, CircleShape), contentAlignment = Alignment.Center) {
                        Text("$score", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = BrandDeep)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(viewModel.t("healthCertificate"), fontWeight = FontWeight.ExtraBold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    healthCard.analysis.forEach { item ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)), shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(if (lang == "kn") item.nutrientKn else item.nutrient, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("💡 ${if (lang == "kn") item.recommendationKn else item.recommendation}", fontSize = 10.sp, color = Color.Gray)
                                }
                                Surface(
                                    color = when (item.status) { "Low" -> Color(0xFFFEE2E2); "High" -> Color(0xFFDEEBFF); else -> Color(0xFFDCFCE7) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (lang == "kn") item.statusKn else item.status, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                                        color = when (item.status) { "Low" -> Color(0xFFDC2626); "High" -> Color(0xFF2563EB); else -> Color(0xFF16A34A) },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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

    // Gov Schemes Dialog
    if (showSchemes) {
        var schemeSearch by remember { mutableStateOf("") }
        var selectedScheme by remember { mutableStateOf<GovScheme?>(null) }
        val filteredSchemes = GOV_SCHEMES.filter {
            schemeSearch.isBlank() || it.name.contains(schemeSearch, ignoreCase = true) || it.description.contains(schemeSearch, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = { showSchemes = false; selectedScheme = null },
            title = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = BrandDeep)
                    Text(viewModel.t("govSchemes"), fontWeight = FontWeight.ExtraBold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                    if (selectedScheme == null) {
                        OutlinedTextField(
                            value = schemeSearch, onValueChange = { schemeSearch = it },
                            placeholder = { Text(viewModel.t("search")) }, singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandDeep),
                            modifier = Modifier.fillMaxWidth()
                        )
                        filteredSchemes.forEach { scheme ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { selectedScheme = scheme },
                                colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(scheme.name, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = BrandDeep)
                                    Text(scheme.ministry, fontSize = 10.sp, color = Color.Gray)
                                    Text(scheme.description, fontSize = 11.sp, color = OnBackground, maxLines = 2)
                                }
                            }
                        }
                    } else {
                        val scheme = selectedScheme!!
                        IconButton(onClick = { selectedScheme = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                        Text(scheme.name, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BrandDeep)
                        Text(scheme.ministry, fontSize = 11.sp, color = Color.Gray)
                        Spacer(Modifier.height(4.dp))
                        Text(scheme.description, fontSize = 12.sp)
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceVariant), shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Documents Required:", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray, letterSpacing = 1.sp)
                                scheme.documents.forEach { doc -> Text("• $doc", fontSize = 11.sp) }
                                Spacer(Modifier.height(4.dp))
                                Text("Deadline: ${scheme.deadline}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandDanger)
                                Text("Contact: ${scheme.contact}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scheme.portal))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandDeep),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(viewModel.t("visitPortal"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSchemes = false; selectedScheme = null }, modifier = Modifier.fillMaxWidth()) {
                    Text(viewModel.t("close"), color = Color.Gray)
                }
            },
            dismissButton = null,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Main Profile Content
    Column(
        Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header
        Box(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(BrandDeep, Color(0xFF43A047)))).padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.size(80.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).border(3.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text(
                        profile?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "F",
                        fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(profile?.name ?: "Farmer", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("${profile?.village ?: "Village"}, ${profile?.district ?: "District"}",
                    fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                if (firebaseEmail != null) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(12.dp))
                            Text(firebaseEmail, fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                        Text("🌾 ${profile?.primaryCrop?.displayName ?: "Rice"}", fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                    Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                        Text("${profile?.landArea ?: 2.5} ha", fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                }
            }
        }

        // Menu Items
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Gov Schemes Portal
            ProfileMenuCard(
                icon = Icons.Default.AccountBalance, label = viewModel.t("govSchemes"),
                subtitle = viewModel.t("checkEligibility"), tint = Color(0xFF2563EB),
                bgColor = Color(0xFFEFF6FF)
            ) { showEligibility = true }

            // Community Hub
            ProfileMenuCard(
                icon = Icons.Default.Group, label = viewModel.t("community"),
                subtitle = "Connect with farmers", tint = Color(0xFF7C3AED),
                bgColor = Color(0xFFF5F3FF)
            ) { showCommunity = true }

            // Quick Links to Gov Schemes
            ProfileMenuCard(
                icon = Icons.Default.Link, label = viewModel.t("govLinks"),
                subtitle = "View all government schemes", tint = Color(0xFF0891B2),
                bgColor = Color(0xFFECFEFF)
            ) { showSchemes = true }

            // Soil Health Card
            ProfileMenuCard(
                icon = Icons.Default.HealthAndSafety, label = viewModel.t("healthCertificate"),
                subtitle = "Score: $score/100", tint = Color(0xFF16A34A),
                bgColor = Color(0xFFF0FDF4)
            ) { showHealthCard = true }

            // Language Toggle
            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFEF3C7)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = BrandWarning, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text(viewModel.t("language"), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(if (lang == "kn") "ಕನ್ನಡ ಆಯ್ಕೆ ಮಾಡಲಾಗಿದೆ" else "English selected", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceVariant).padding(4.dp)
                    ) {
                        listOf("en" to "EN", "kn" to "ಕನ್ನಡ").forEach { (code, label) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (lang == code) BrandDeep else Color.Transparent)
                                    .clickable { viewModel.setLang(code) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    color = if (lang == code) Color.White else Color.Gray)
                            }
                        }
                    }
                }
            }

            // Order History
            ProfileMenuCard(
                icon = Icons.Default.History, label = viewModel.t("orderHistory"),
                subtitle = "View past orders", tint = Color.Gray,
                bgColor = SurfaceVariant
            ) { }

            // Logout
            var showLogoutConfirm by remember { mutableStateOf(false) }
            if (showLogoutConfirm) {
                AlertDialog(
                    onDismissRequest = { showLogoutConfirm = false },
                    icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = BrandDanger, modifier = Modifier.size(32.dp)) },
                    title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
                    text = { Text("Are you sure you want to sign out of your account?", fontSize = 13.sp) },
                    confirmButton = {
                        Button(
                            onClick = { showLogoutConfirm = false; onSignOut() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandDanger),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Sign Out", fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
                    },
                    shape = RoundedCornerShape(24.dp)
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showLogoutConfirm = true },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFEE2E2)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = BrandDanger, modifier = Modifier.size(24.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(viewModel.t("logout"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandDanger)
                        Text(firebaseEmail ?: "", fontSize = 11.sp, color = BrandDanger.copy(alpha = 0.6f))
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandDanger)
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ProfileMenuCard(
    icon: ImageVector, label: String, subtitle: String,
    tint: Color, bgColor: Color, onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(bgColor), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 11.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}
