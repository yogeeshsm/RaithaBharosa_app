package com.raitha.bharosa.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.raitha.bharosa.data.*
import com.raitha.bharosa.ui.theme.*
import com.raitha.bharosa.utils.ValidationUtils
import com.raitha.bharosa.viewmodel.LabourUiState
import com.raitha.bharosa.viewmodel.LabourViewModel
import java.io.File

/**
 * Labourer Profile Creation Screen
 * Task 14.1: Create LabourerProfileScreen
 * Requirements: 2, 4, 19, 20
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabourerProfileScreen(
    labourViewModel: LabourViewModel,
    onNavigateBack: () -> Unit,
    onProfileCreated: () -> Unit
) {
    val context = LocalContext.current
    val uiState by labourViewModel.uiState.collectAsState()
    
    // Form state
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var village by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var selectedSkills by remember { mutableStateOf(setOf<Skill>()) }
    var experienceYears by remember { mutableStateOf(mapOf<Skill, Int>()) }
    var pricingType by remember { mutableStateOf(PricingType.DAILY_WAGE) }
    var dailyWage by remember { mutableStateOf("") }
    var hourlyRate by remember { mutableStateOf("") }
    var preferredLanguage by remember { mutableStateOf("en") }
    var profilePhotoUri by remember { mutableStateOf<Uri?>(null) }
    
    // Validation errors
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var ageError by remember { mutableStateOf<String?>(null) }
    var villageError by remember { mutableStateOf<String?>(null) }
    var districtError by remember { mutableStateOf<String?>(null) }
    var skillsError by remember { mutableStateOf<String?>(null) }
    var wageError by remember { mutableStateOf<String?>(null) }
    
    // Photo picker state
    var showPhotoPickerDialog by remember { mutableStateOf(false) }
    
    // Camera/Gallery launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { profilePhotoUri = it }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            profilePhotoUri = null
        }
    }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Create temp file for camera
            val photoFile = File.createTempFile("profile_", ".jpg", context.cacheDir)
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            profilePhotoUri = photoUri
            cameraLauncher.launch(photoUri)
        }
    }
    
    // Handle UI state changes
    LaunchedEffect(uiState) {
        if (uiState is LabourUiState.Success) {
            onProfileCreated()
        }
    }
    
    // Validation function
    fun validateForm(): Boolean {
        var isValid = true
        
        // Validate name
        val nameValidation = ValidationUtils.validateName(name)
        nameError = nameValidation.getErrorMessage(preferredLanguage)
        if (!nameValidation.isValid) isValid = false
        
        // Validate phone
        val phoneValidation = ValidationUtils.validatePhoneNumber(phoneNumber)
        phoneError = phoneValidation.getErrorMessage(preferredLanguage)
        if (!phoneValidation.isValid) isValid = false
        
        // Validate age
        val ageInt = age.toIntOrNull() ?: -1
        val ageValidation = ValidationUtils.validateAge(ageInt)
        ageError = ageValidation.getErrorMessage(preferredLanguage)
        if (!ageValidation.isValid) isValid = false
        
        // Validate village
        if (village.trim().isEmpty()) {
            villageError = if (preferredLanguage == "kn") "ಗ್ರಾಮ ಅಗತ್ಯವಿದೆ" else "Village is required"
            isValid = false
        } else {
            villageError = null
        }
        
        // Validate district
        if (district.trim().isEmpty()) {
            districtError = if (preferredLanguage == "kn") "ಜಿಲ್ಲೆ ಅಗತ್ಯವಿದೆ" else "District is required"
            isValid = false
        } else {
            districtError = null
        }
        
        // Validate skills
        val skillsValidation = ValidationUtils.validateSkills(selectedSkills.toList())
        skillsError = skillsValidation.getErrorMessage(preferredLanguage)
        if (!skillsValidation.isValid) isValid = false
        
        // Validate wage/rate
        if (pricingType == PricingType.DAILY_WAGE) {
            val wageInt = dailyWage.toIntOrNull() ?: -1
            val wageValidation = ValidationUtils.validateDailyWage(wageInt)
            wageError = wageValidation.getErrorMessage(preferredLanguage)
            if (!wageValidation.isValid) isValid = false
        } else {
            val rateInt = hourlyRate.toIntOrNull() ?: -1
            val rateValidation = ValidationUtils.validateHourlyRate(rateInt)
            wageError = rateValidation.getErrorMessage(preferredLanguage)
            if (!rateValidation.isValid) isValid = false
        }
        
        return isValid
    }
    
    // Submit function
    fun submitProfile() {
        if (validateForm()) {
            labourViewModel.createLabourerProfile(
                name = name.trim(),
                phoneNumber = phoneNumber.trim(),
                age = age.toInt(),
                gender = gender,
                village = village.trim(),
                district = district.trim(),
                latitude = 0.0, // TODO: Get actual location
                longitude = 0.0,
                skills = selectedSkills.toList(),
                experienceYears = experienceYears,
                pricingType = pricingType,
                dailyWage = if (pricingType == PricingType.DAILY_WAGE) dailyWage.toIntOrNull() else null,
                hourlyRate = if (pricingType == PricingType.HOURLY_RATE) hourlyRate.toIntOrNull() else null,
                preferredLanguage = preferredLanguage
            )
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D2B1A),
                        Color(0xFF1A4731),
                        Background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding()
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (preferredLanguage == "kn") "ಕಾರ್ಮಿಕ ಪ್ರೊಫೈಲ್" else "Labourer Profile",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (preferredLanguage == "kn") "ನಿಮ್ಮ ವಿವರಗಳನ್ನು ನಮೂದಿಸಿ" else "Enter your details",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Language toggle
                IconButton(
                    onClick = { preferredLanguage = if (preferredLanguage == "en") "kn" else "en" }
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = "Change Language",
                        tint = BrandLight
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Error Banner
            AnimatedVisibility(visible = uiState is LabourUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            (uiState as? LabourUiState.Error)?.message ?: "",
                            fontSize = 12.sp,
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Form Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Surface.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Photo
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(BrandDeep.copy(alpha = 0.1f))
                                .clickable { showPhotoPickerDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profilePhotoUri != null) {
                                // TODO: Display image from URI
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = BrandDeep,
                                    modifier = Modifier.size(40.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    tint = BrandDeep,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (preferredLanguage == "kn") "ಫೋಟೋ ಸೇರಿಸಿ" else "Add Photo",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    
                    // Name
                    ProfileTextField(
                        value = name,
                        onValueChange = { name = it; nameError = null },
                        label = if (preferredLanguage == "kn") "ಹೆಸರು" else "Full Name",
                        leadingIcon = Icons.Default.Person,
                        error = nameError
                    )
                    
                    // Phone Number
                    ProfileTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it; phoneError = null },
                        label = if (preferredLanguage == "kn") "ದೂರವಾಣಿ ಸಂಖ್ಯೆ" else "Phone Number",
                        leadingIcon = Icons.Default.Phone,
                        keyboardType = KeyboardType.Phone,
                        error = phoneError
                    )
                    
                    // Age
                    ProfileTextField(
                        value = age,
                        onValueChange = { age = it; ageError = null },
                        label = if (preferredLanguage == "kn") "ವಯಸ್ಸು" else "Age",
                        leadingIcon = Icons.Default.CalendarToday,
                        keyboardType = KeyboardType.Number,
                        error = ageError
                    )
                    
                    // Gender
                    Column {
                        Text(
                            text = if (preferredLanguage == "kn") "ಲಿಂಗ" else "Gender",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GenderChip(
                                label = if (preferredLanguage == "kn") "ಪುರುಷ" else "Male",
                                selected = gender == "Male",
                                onClick = { gender = "Male" },
                                modifier = Modifier.weight(1f)
                            )
                            GenderChip(
                                label = if (preferredLanguage == "kn") "ಮಹಿಳೆ" else "Female",
                                selected = gender == "Female",
                                onClick = { gender = "Female" },
                                modifier = Modifier.weight(1f)
                            )
                            GenderChip(
                                label = if (preferredLanguage == "kn") "ಇತರೆ" else "Other",
                                selected = gender == "Other",
                                onClick = { gender = "Other" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // Village
                    ProfileTextField(
                        value = village,
                        onValueChange = { village = it; villageError = null },
                        label = if (preferredLanguage == "kn") "ಗ್ರಾಮ" else "Village",
                        leadingIcon = Icons.Default.LocationOn,
                        error = villageError
                    )
                    
                    // District
                    ProfileTextField(
                        value = district,
                        onValueChange = { district = it; districtError = null },
                        label = if (preferredLanguage == "kn") "ಜಿಲ್ಲೆ" else "District",
                        leadingIcon = Icons.Default.Map,
                        error = districtError
                    )
                    
                    // Skills Section
                    Column {
                        Text(
                            text = if (preferredLanguage == "kn") "ಕೌಶಲ್ಯಗಳು" else "Skills",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurface
                        )
                        if (skillsError != null) {
                            Text(
                                text = skillsError!!,
                                fontSize = 12.sp,
                                color = Color(0xFFC62828)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Skill.values().forEach { skill ->
                            val isSelected = selectedSkills.contains(skill)
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedSkills = if (isSelected) {
                                            selectedSkills - skill
                                        } else {
                                            selectedSkills + skill
                                        }
                                        skillsError = null
                                    }
                                    .background(
                                        if (isSelected) BrandDeep.copy(alpha = 0.1f)
                                        else Color.Transparent
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = BrandDeep
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = skill.getDisplayName(preferredLanguage),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = OnSurface
                                    )
                                    
                                    // Experience input for selected skills
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = experienceYears[skill]?.toString() ?: "",
                                            onValueChange = { value ->
                                                val years = value.toIntOrNull()
                                                if (years != null && years >= 0) {
                                                    experienceYears = experienceYears + (skill to years)
                                                } else if (value.isEmpty()) {
                                                    experienceYears = experienceYears - skill
                                                }
                                            },
                                            label = {
                                                Text(
                                                    if (preferredLanguage == "kn") "ಅನುಭವ (ವರ್ಷಗಳು)"
                                                    else "Experience (years)",
                                                    fontSize = 12.sp
                                                )
                                            },
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = BrandDeep,
                                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                                            ),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    // Pricing Type
                    Column {
                        Text(
                            text = if (preferredLanguage == "kn") "ಬೆಲೆ ಪ್ರಕಾರ" else "Pricing Type",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PricingTypeChip(
                                label = if (preferredLanguage == "kn") "ದೈನಂದಿನ ವೇತನ" else "Daily Wage",
                                selected = pricingType == PricingType.DAILY_WAGE,
                                onClick = { pricingType = PricingType.DAILY_WAGE; wageError = null },
                                modifier = Modifier.weight(1f)
                            )
                            PricingTypeChip(
                                label = if (preferredLanguage == "kn") "ಗಂಟೆಯ ದರ" else "Hourly Rate",
                                selected = pricingType == PricingType.HOURLY_RATE,
                                onClick = { pricingType = PricingType.HOURLY_RATE; wageError = null },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // Wage/Rate Input
                    if (pricingType == PricingType.DAILY_WAGE) {
                        ProfileTextField(
                            value = dailyWage,
                            onValueChange = { dailyWage = it; wageError = null },
                            label = if (preferredLanguage == "kn") "ದೈನಂದಿನ ವೇತನ (₹300-₹2000)"
                            else "Daily Wage (₹300-₹2000)",
                            leadingIcon = Icons.Default.CurrencyRupee,
                            keyboardType = KeyboardType.Number,
                            error = wageError
                        )
                    } else {
                        ProfileTextField(
                            value = hourlyRate,
                            onValueChange = { hourlyRate = it; wageError = null },
                            label = if (preferredLanguage == "kn") "ಗಂಟೆಯ ದರ (₹40-₹300)"
                            else "Hourly Rate (₹40-₹300)",
                            leadingIcon = Icons.Default.CurrencyRupee,
                            keyboardType = KeyboardType.Number,
                            error = wageError
                        )
                    }
                    
                    // Submit Button
                    Button(
                        onClick = { submitProfile() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandDeep),
                        enabled = uiState !is LabourUiState.Loading
                    ) {
                        if (uiState is LabourUiState.Loading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (preferredLanguage == "kn") "ಪ್ರೊಫೈಲ್ ರಚಿಸಿ"
                                else "Create Profile",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Photo Picker Dialog
    if (showPhotoPickerDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoPickerDialog = false },
            title = {
                Text(
                    text = if (preferredLanguage == "kn") "ಫೋಟೋ ಆಯ್ಕೆಮಾಡಿ" else "Choose Photo",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showPhotoPickerDialog = false
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (preferredLanguage == "kn") "ಕ್ಯಾಮೆರಾ" else "Camera")
                    }
                    TextButton(
                        onClick = {
                            showPhotoPickerDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (preferredLanguage == "kn") "ಗ್ಯಾಲರಿ" else "Gallery")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoPickerDialog = false }) {
                    Text(if (preferredLanguage == "kn") "ರದ್ದುಮಾಡಿ" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = {
                Icon(leadingIcon, contentDescription = null, tint = BrandDeep)
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandDeep,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                errorBorderColor = Color(0xFFC62828)
            ),
            isError = error != null,
            singleLine = true
        )
        if (error != null) {
            Text(
                text = error,
                fontSize = 12.sp,
                color = Color(0xFFC62828),
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun GenderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) BrandDeep else Color.Gray.copy(alpha = 0.1f)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color.Gray
        )
    }
}

@Composable
private fun PricingTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) BrandDeep else Color.Gray.copy(alpha = 0.1f)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color.Gray
        )
    }
}
