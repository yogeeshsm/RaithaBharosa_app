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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raitha.bharosa.data.CropType
import com.raitha.bharosa.data.FarmerProfile
import com.raitha.bharosa.ui.theme.*
import com.raitha.bharosa.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(viewModel: AppViewModel) {
    val lang by viewModel.lang.collectAsState()

    var name by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var landArea by remember { mutableStateOf("2.5") }
    var selectedCrop by remember { mutableStateOf(CropType.Rice) }
    var showError by remember { mutableStateOf(false) }
    var cropExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Logo / Header
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(BrandDeep, BrandLight))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Grass, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                viewModel.t("appName"),
                style = MaterialTheme.typography.headlineLarge,
                color = OnBackground,
                textAlign = TextAlign.Center
            )
            Text(
                viewModel.t("setupProfile"),
                style = MaterialTheme.typography.labelSmall,
                color = BrandDeep,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
        }

        // Language Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceVariant)
                .padding(4.dp)
        ) {
            listOf("en" to "English", "kn" to "ಕನ್ನಡ").forEach { (code, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (lang == code) Surface else Color.Transparent)
                        .clickable { viewModel.setLang(code) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        color = if (lang == code) BrandDeep else Color.Gray)
                }
            }
        }

        // Error Message
        AnimatedVisibility(visible = showError) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = BrandWarning, modifier = Modifier.size(20.dp))
                    Text(if (lang == "kn") "ನಿಮ್ಮ ಹೆಸರು ಮತ್ತು ಗ್ರಾಮವನ್ನು ನಮೂದಿಸಿ" else "Please fill in your name and village",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF856404))
                }
            }
        }

        // Form Fields
        SetupTextField(
            value = name,
            onValueChange = { name = it; showError = false },
            label = viewModel.t("fullName"),
            placeholder = "Arjun Kumar"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SetupTextField(
                value = village, onValueChange = { village = it; showError = false },
                label = viewModel.t("village"), placeholder = "Village",
                modifier = Modifier.weight(1f)
            )
            SetupTextField(
                value = district, onValueChange = { district = it },
                label = viewModel.t("district"), placeholder = "District",
                modifier = Modifier.weight(1f)
            )
        }

        SetupTextField(
            value = landArea, onValueChange = { landArea = it },
            label = viewModel.t("landArea"), placeholder = "2.5"
        )

        // Crop Selector
        Column {
            Text(if (lang == "kn") "ಪ್ರಾಥಮಿಕ ಬೆಳೆ" else "Primary Crop",
                fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                color = Color.Gray, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            ExposedDropdownMenuBox(
                expanded = cropExpanded,
                onExpandedChange = { cropExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCrop.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cropExpanded) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandDeep),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = cropExpanded,
                    onDismissRequest = { cropExpanded = false }
                ) {
                    CropType.entries.forEach { crop ->
                        DropdownMenuItem(
                            text = { Text(crop.displayName) },
                            onClick = { selectedCrop = crop; cropExpanded = false }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Get Started Button
        Button(
            onClick = {
                if (name.isBlank() || village.isBlank()) {
                    showError = true
                    return@Button
                }
                viewModel.setProfile(
                    FarmerProfile(
                        name = name.trim(), village = village.trim(),
                        district = district.trim(),
                        landArea = landArea.toDoubleOrNull() ?: 2.5,
                        primaryCrop = selectedCrop
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandDeep)
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(viewModel.t("getStarted"), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SetupTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
            color = Color.Gray, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.LightGray) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandDeep,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
