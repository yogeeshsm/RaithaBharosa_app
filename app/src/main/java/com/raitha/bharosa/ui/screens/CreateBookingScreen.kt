package com.raitha.bharosa.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raitha.bharosa.data.*
import com.raitha.bharosa.ui.theme.*
import com.raitha.bharosa.viewmodel.LabourUiState
import com.raitha.bharosa.viewmodel.LabourViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Create Booking Screen
 * Task 18.1: Create CreateBookingScreen
 * Requirements: 8, 9
 * 
 * Simple and functional MVP implementation for booking creation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBookingScreen(
    labourViewModel: LabourViewModel,
    labourerProfile: LabourerProfile,
    onNavigateBack: () -> Unit,
    onBookingCreated: () -> Unit
) {
    val uiState by labourViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Form state
    var workDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var estimatedHours by remember { mutableStateOf("") }
    var selectedWorkType by remember { mutableStateOf<Skill?>(null) }
    var specialInstructions by remember { mutableStateOf("") }
    var isEmergency by remember { mutableStateOf(false) }
    var preferredLanguage by remember { mutableStateOf("en") }
    
    // Validation errors
    var dateError by remember { mutableStateOf<String?>(null) }
    var timeError by remember { mutableStateOf<String?>(null) }
    var hoursError by remember { mutableStateOf<String?>(null) }
    var workTypeError by remember { mutableStateOf<String?>(null) }
    
    // Confirmation dialog
    var showConfirmDialog by remember { mutableStateOf(false) }
    
    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Calculate estimated payment
    val estimatedPayment = remember(estimatedHours, selectedWorkType) {
        val hours = estimatedHours.toIntOrNull() ?: 0
        when (labourerProfile.pricingType) {
            PricingType.DAILY_WAGE -> {
                if (hours >= 8) labourerProfile.dailyWage ?: 0
                else ((labourerProfile.dailyWage ?: 0) * hours / 8)
            }
            PricingType.HOURLY_RATE -> {
                (labourerProfile.hourlyRate ?: 0) * hours
            }
        }
    }
    
    // Handle UI state changes
    LaunchedEffect(uiState) {
        if (uiState is LabourUiState.Success) {
            onBookingCreated()
        }
    }
    
    // Validation function
    fun validateForm(): Boolean {
        var isValid = true
        
        // Validate work date
        if (workDate.trim().isEmpty()) {
            dateError = if (preferredLanguage == "kn") "ದಿನಾಂಕ ಅಗತ್ಯವಿದೆ" else "Date is required"
            isValid = false
        } else {
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val selectedDate = sdf.parse(workDate)
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                
                if (selectedDate != null && selectedDate.before(today)) {
                    dateError = if (preferredLanguage == "kn") 
                        "ದಿನಾಂಕ ಭೂತಕಾಲದಲ್ಲಿರಬಾರದು" 
                        else "Date cannot be in the past"
                    isValid = false
                } else {
                    dateError = null
                }
            } catch (e: Exception) {
                dateError = if (preferredLanguage == "kn") 
                    "ಅಮಾನ್ಯ ದಿನಾಂಕ ಸ್ವರೂಪ" 
                    else "Invalid date format"
                isValid = false
            }
        }
        
        // Validate start time
        if (startTime.trim().isEmpty()) {
            timeError = if (preferredLanguage == "kn") "ಸಮಯ ಅಗತ್ಯವಿದೆ" else "Time is required"
            isValid = false
        } else {
            timeError = null
        }
        
        // Validate estimated hours
        val hours = estimatedHours.toIntOrNull()
        if (hours == null || hours <= 0 || hours > 24) {
            hoursError = if (preferredLanguage == "kn") 
                "1-24 ಗಂಟೆಗಳನ್ನು ನಮೂದಿಸಿ" 
                else "Enter 1-24 hours"
            isValid = false
        } else {
            hoursError = null
        }
        
        // Validate work type
        if (selectedWorkType == null) {
            workTypeError = if (preferredLanguage == "kn") 
                "ಕೆಲಸದ ಪ್ರಕಾರವನ್ನು ಆಯ್ಕೆಮಾಡಿ" 
                else "Select work type"
            isValid = false
        } else {
            workTypeError = null
        }
        
        return isValid
    }
    
    // Submit function
    fun submitBooking() {
        if (validateForm()) {
            showConfirmDialog = true
        }
    }
    
    fun confirmBooking() {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = sdf.parse(workDate)
            
            labourViewModel.createBooking(
                labourerId = labourerProfile.userId,
                labourerName = labourerProfile.name,
                labourerPhone = labourerProfile.phoneNumber,
                workDate = date?.time ?: System.currentTimeMillis(),
                startTime = startTime,
                estimatedHours = estimatedHours.toInt(),
                workType = selectedWorkType!!,
                specialInstructions = specialInstructions.ifBlank { null },
                isEmergency = isEmergency,
                estimatedPayment = estimatedPayment,
                farmerName = "Farmer", // TODO: Get from farmer profile
                farmerPhone = "1234567890", // TODO: Get from farmer profile
                farmerLocation = "${labourerProfile.village}, ${labourerProfile.district}"
            )
        } catch (e: Exception) {
            // Handle error
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (preferredLanguage == "kn") "ಬುಕಿಂಗ್ ರಚಿಸಿ" else "Create Booking",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (preferredLanguage == "kn") 
                            "${labourerProfile.name} ಅವರೊಂದಿಗೆ" 
                            else "with ${labourerProfile.name}",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
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
                    // Work Date
                    BookingTextField(
                        value = workDate,
                        onValueChange = { workDate = it; dateError = null },
                        label = if (preferredLanguage == "kn") "ಕೆಲಸದ ದಿನಾಂಕ (DD/MM/YYYY)" 
                                else "Work Date (DD/MM/YYYY)",
                        leadingIcon = Icons.Default.CalendarToday,
                        error = dateError,
                        placeholder = "01/01/2024"
                    )
                    
                    // Start Time
                    BookingTextField(
                        value = startTime,
                        onValueChange = { startTime = it; timeError = null },
                        label = if (preferredLanguage == "kn") "ಪ್ರಾರಂಭ ಸಮಯ (HH:MM)" 
                                else "Start Time (HH:MM)",
                        leadingIcon = Icons.Default.AccessTime,
                        error = timeError,
                        placeholder = "09:00"
                    )
                    
                    // Estimated Hours
                    BookingTextField(
                        value = estimatedHours,
                        onValueChange = { estimatedHours = it; hoursError = null },
                        label = if (preferredLanguage == "kn") "ಅಂದಾಜು ಗಂಟೆಗಳು" 
                                else "Estimated Hours",
                        leadingIcon = Icons.Default.Timer,
                        keyboardType = KeyboardType.Number,
                        error = hoursError,
                        placeholder = "8"
                    )
                    
                    // Work Type Selection
                    Column {
                        Text(
                            text = if (preferredLanguage == "kn") "ಕೆಲಸದ ಪ್ರಕಾರ" else "Work Type",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurface
                        )
                        if (workTypeError != null) {
                            Text(
                                text = workTypeError!!,
                                fontSize = 12.sp,
                                color = Color(0xFFC62828)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Show only labourer's skills
                        labourerProfile.skills.forEach { skill ->
                            val isSelected = selectedWorkType == skill
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedWorkType = skill
                                        workTypeError = null
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) 
                                        BrandDeep.copy(alpha = 0.15f) 
                                        else Color.Transparent
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) BrandDeep else Color.Gray.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = BrandDeep
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = skill.getDisplayName(preferredLanguage),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = OnSurface
                                        )
                                        val experience = labourerProfile.experienceYears[skill]
                                        if (experience != null && experience > 0) {
                                            Text(
                                                text = if (preferredLanguage == "kn") 
                                                    "$experience ವರ್ಷಗಳ ಅನುಭವ" 
                                                    else "$experience years experience",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    // Special Instructions
                    OutlinedTextField(
                        value = specialInstructions,
                        onValueChange = { specialInstructions = it },
                        label = {
                            Text(
                                if (preferredLanguage == "kn") "ವಿಶೇಷ ಸೂಚನೆಗಳು (ಐಚ್ಛಿಕ)" 
                                else "Special Instructions (Optional)"
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Notes, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandDeep,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        maxLines = 4
                    )
                    
                    // Emergency Hiring Checkbox
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEmergency) 
                                Color(0xFFFFEBEE) 
                                else Color.Transparent
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isEmergency) Color(0xFFC62828) else Color.Gray.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isEmergency = !isEmergency }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isEmergency,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFFC62828)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFC62828),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (preferredLanguage == "kn") 
                                            "ತುರ್ತು ನೇಮಕಾತಿ" 
                                            else "Emergency Hiring",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEmergency) Color(0xFFC62828) else OnSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (preferredLanguage == "kn") 
                                        "ತುರ್ತು ಅಧಿಸೂಚನೆಗಳೊಂದಿಗೆ ಆದ್ಯತೆಯ ಬುಕಿಂಗ್" 
                                        else "Priority booking with urgent notifications",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    
                    Divider(color = Color.Gray.copy(alpha = 0.2f))
                    
                    // Estimated Payment Display
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = BrandDeep.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (preferredLanguage == "kn") 
                                        "ಅಂದಾಜು ಪಾವತಿ" 
                                        else "Estimated Payment",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CurrencyRupee,
                                        contentDescription = null,
                                        tint = BrandDeep,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = estimatedPayment.toString(),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandDeep
                                    )
                                }
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = when (labourerProfile.pricingType) {
                                        PricingType.DAILY_WAGE -> if (preferredLanguage == "kn") 
                                            "₹${labourerProfile.dailyWage}/ದಿನ" 
                                            else "₹${labourerProfile.dailyWage}/day"
                                        PricingType.HOURLY_RATE -> if (preferredLanguage == "kn") 
                                            "₹${labourerProfile.hourlyRate}/ಗಂಟೆ" 
                                            else "₹${labourerProfile.hourlyRate}/hr"
                                    },
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = if (preferredLanguage == "kn") 
                                        "${estimatedHours.toIntOrNull() ?: 0} ಗಂಟೆಗಳು" 
                                        else "${estimatedHours.toIntOrNull() ?: 0} hours",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    
                    // Submit Button
                    Button(
                        onClick = { submitBooking() },
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
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (preferredLanguage == "kn") "ಬುಕಿಂಗ್ ರಚಿಸಿ" 
                                        else "Create Booking",
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
    
    // Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = BrandDeep,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = if (preferredLanguage == "kn") "ಬುಕಿಂಗ್ ದೃಢೀಕರಿಸಿ" 
                            else "Confirm Booking",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isEmergency) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (preferredLanguage == "kn") 
                                        "ತುರ್ತು ಬುಕಿಂಗ್" 
                                        else "URGENT BOOKING",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    BookingDetailRow(
                        label = if (preferredLanguage == "kn") "ಕಾರ್ಮಿಕ" else "Labourer",
                        value = labourerProfile.name
                    )
                    BookingDetailRow(
                        label = if (preferredLanguage == "kn") "ದಿನಾಂಕ" else "Date",
                        value = workDate
                    )
                    BookingDetailRow(
                        label = if (preferredLanguage == "kn") "ಸಮಯ" else "Time",
                        value = startTime
                    )
                    BookingDetailRow(
                        label = if (preferredLanguage == "kn") "ಗಂಟೆಗಳು" else "Hours",
                        value = "$estimatedHours ${if (preferredLanguage == "kn") "ಗಂಟೆಗಳು" else "hours"}"
                    )
                    BookingDetailRow(
                        label = if (preferredLanguage == "kn") "ಕೆಲಸದ ಪ್ರಕಾರ" else "Work Type",
                        value = selectedWorkType?.getDisplayName(preferredLanguage) ?: ""
                    )
                    BookingDetailRow(
                        label = if (preferredLanguage == "kn") "ಅಂದಾಜು ಪಾವತಿ" else "Estimated Payment",
                        value = "₹$estimatedPayment",
                        valueColor = BrandDeep,
                        valueBold = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        confirmBooking()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandDeep)
                ) {
                    Text(if (preferredLanguage == "kn") "ದೃಢೀಕರಿಸಿ" else "Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(if (preferredLanguage == "kn") "ರದ್ದುಮಾಡಿ" else "Cancel")
                }
            }
        )
    }
}

/**
 * Booking Text Field Component
 */
@Composable
private fun BookingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null,
    placeholder: String = ""
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = {
                Icon(leadingIcon, contentDescription = null)
            },
            placeholder = { Text(placeholder, color = Color.Gray.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandDeep,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                errorBorderColor = Color(0xFFC62828)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            isError = error != null
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

/**
 * Booking Detail Row Component for Confirmation Dialog
 */
@Composable
private fun BookingDetailRow(
    label: String,
    value: String,
    valueColor: Color = OnSurface,
    valueBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = valueColor,
            fontWeight = if (valueBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}
