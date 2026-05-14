package com.raitha.bharosa.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raitha.bharosa.data.*
import com.raitha.bharosa.ui.theme.*
import com.raitha.bharosa.viewmodel.LabourUiState
import com.raitha.bharosa.viewmodel.LabourViewModel
import com.raitha.bharosa.viewmodel.SortOption
import kotlinx.coroutines.launch

/**
 * Labourer Search Screen
 * Task 16.1: Create LabourerSearchScreen
 * Requirements: 6, 7, 21
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabourerSearchScreen(
    labourViewModel: LabourViewModel,
    onNavigateBack: () -> Unit,
    onLabourerClick: (String) -> Unit
) {
    val searchResults by labourViewModel.searchResults.collectAsState()
    val uiState by labourViewModel.uiState.collectAsState()

    // Search and filter state
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedSkills by remember { mutableStateOf(setOf<Skill>()) }
    var minWage by remember { mutableStateOf(300) }
    var maxWage by remember { mutableStateOf(2000) }
    var minRating by remember { mutableStateOf(0f) }
    var sortBy by remember { mutableStateOf(SortOption.RATING) }
    var preferredLanguage by remember { mutableStateOf("en") }

    // Auto-load labourers when screen opens
    LaunchedEffect(Unit) {
        labourViewModel.loadInitialLabourers()
    }
    
    // Re-apply search when sort option changes
    LaunchedEffect(sortBy) {
        if (searchResults.isNotEmpty()) {
            android.util.Log.d("LabourerSearch", "Sort changed to: $sortBy, re-applying search")
            labourViewModel.searchLabourers(
                skills = if (selectedSkills.isEmpty()) null else selectedSkills.toList(),
                locationRadius = null,
                farmerLocation = null,
                minWage = minWage,
                maxWage = maxWage,
                minRating = minRating.toDouble(),
                sortBy = sortBy
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
                .padding(horizontal = 16.dp)
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
                        text = if (preferredLanguage == "kn") "ಕಾರ್ಮಿಕರನ್ನು ಹುಡುಕಿ" else "Find Labourers",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (preferredLanguage == "kn") "${searchResults.size} ಫಲಿತಾಂಶಗಳು" 
                        else "${searchResults.size} results",
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Bar with Filter Button
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                if (preferredLanguage == "kn") "ಹೆಸರು ಅಥವಾ ಕೌಶಲ್ಯದಿಂದ ಹುಡುಕಿ"
                                else "Search by name or skill",
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = BrandDeep
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Sorting Options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortChip(
                    label = if (preferredLanguage == "kn") "ರೇಟಿಂಗ್" else "Rating",
                    selected = sortBy == SortOption.RATING,
                    onClick = { 
                        android.util.Log.d("LabourerSearch", "Sort chip clicked: RATING")
                        sortBy = SortOption.RATING 
                    }
                )
                SortChip(
                    label = if (preferredLanguage == "kn") "ವೇತನ" else "Wage",
                    selected = sortBy == SortOption.PRICE_LOW,
                    onClick = { 
                        android.util.Log.d("LabourerSearch", "Sort chip clicked: PRICE_LOW")
                        sortBy = SortOption.PRICE_LOW 
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Results
            if (uiState is LabourUiState.Loading && searchResults.isEmpty()) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = BrandLight,
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = if (preferredLanguage == "kn") "ಕಾರ್ಮಿಕರನ್ನು ಲೋಡ್ ಮಾಡಲಾಗುತ್ತಿದೆ..."
                            else "Loading labourers...",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            } else if (searchResults.isEmpty()) {
                // Empty state with seed button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val scope = rememberCoroutineScope()
                    var isSeeding by remember { mutableStateOf(false) }
                    var seedMessage by remember { mutableStateOf("") }
                    var seedSuccess by remember { mutableStateOf(false) }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = if (preferredLanguage == "kn") "ಯಾವುದೇ ಫಲಿತಾಂಶಗಳು ಕಂಡುಬಂದಿಲ್ಲ"
                            else "No results found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        
                        if (!seedSuccess) {
                            Text(
                                text = if (preferredLanguage == "kn") "ಮಾದರಿ ಡೇಟಾವನ್ನು ಸೇರಿಸಲು ಕೆಳಗಿನ ಬಟನ್ ಕ್ಲಿಕ್ ಮಾಡಿ"
                                else "Click below to add sample data",
                                fontSize = 14.sp,
                                color = Color.Gray.copy(alpha = 0.7f)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Seed Data Button
                            Button(
                                onClick = {
                                    scope.launch {
                                        isSeeding = true
                                        seedMessage = ""
                                        try {
                                            val result = SeedDataHelper.seedLabourData()
                                            result.onSuccess { message ->
                                                seedMessage = message
                                                seedSuccess = true
                                                // Refresh search results
                                                labourViewModel.searchLabourers(
                                                    skills = selectedSkills.toList(),
                                                    locationRadius = null,
                                                    farmerLocation = null,
                                                    minWage = minWage,
                                                    maxWage = maxWage,
                                                    minRating = minRating.toDouble()
                                                )
                                            }.onFailure { error ->
                                                seedMessage = "Error: ${error.message}"
                                            }
                                        } finally {
                                            isSeeding = false
                                        }
                                    }
                                },
                                enabled = !isSeeding,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandDeep,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(56.dp)
                            ) {
                                if (isSeeding) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Seeding...")
                                } else {
                                    Icon(
                                        Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (preferredLanguage == "kn") "12 ಮಾದರಿ ಕಾರ್ಮಿಕರನ್ನು ಸೇರಿಸಿ"
                                        else "Add 12 Sample Labourers",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            if (seedMessage.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (seedMessage.startsWith("Error")) 
                                            Color.Red.copy(alpha = 0.1f) 
                                        else BrandDeep.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text(
                                        text = seedMessage,
                                        fontSize = 13.sp,
                                        color = if (seedMessage.startsWith("Error")) Color.Red else BrandDeep,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        } else {
                            // Success message
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (preferredLanguage == "kn") "ಡೇಟಾ ಯಶಸ್ವಿಯಾಗಿ ಸೇರಿಸಲಾಗಿದೆ!"
                                        else "Data seeded successfully!",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                    Text(
                                        text = if (preferredLanguage == "kn") "ಫಿಲ್ಟರ್‌ಗಳನ್ನು ಬದಲಾಯಿಸಿ ಅಥವಾ ಹುಡುಕಾಟ ಮಾಡಿ"
                                        else "Change filters or search to see results",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Results List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(searchResults) { labourer ->
                        LabourerResultCard(
                            labourer = labourer,
                            preferredLanguage = preferredLanguage,
                            onClick = { onLabourerClick(labourer.userId) }
                        )
                    }
                }
            }
        }
    }
    
    // Filter Dialog
    if (showFilterDialog) {
        FilterDialog(
            selectedSkills = selectedSkills,
            onSkillsChange = { selectedSkills = it },
            minWage = minWage,
            onMinWageChange = { minWage = it },
            maxWage = maxWage,
            onMaxWageChange = { maxWage = it },
            minRating = minRating,
            onMinRatingChange = { minRating = it },
            preferredLanguage = preferredLanguage,
            onDismiss = { showFilterDialog = false },
            onApply = {
                showFilterDialog = false
                android.util.Log.d("LabourerSearch", "Applying filters - Skills: ${selectedSkills.size}, MinWage: $minWage, MaxWage: $maxWage, MinRating: $minRating")
                // Apply filters through ViewModel
                labourViewModel.searchLabourers(
                    skills = if (selectedSkills.isEmpty()) null else selectedSkills.toList(),
                    locationRadius = null,
                    farmerLocation = null,
                    minWage = minWage,
                    maxWage = maxWage,
                    minRating = minRating.toDouble(),
                    sortBy = sortBy
                )
            }
        )
    }
}

/**
 * Labourer Result Card Component
 */
@Composable
private fun LabourerResultCard(
    labourer: LabourerProfile,
    preferredLanguage: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile Photo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BrandDeep.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = BrandDeep,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            // Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Name and Availability
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = labourer.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    AvailabilityBadge(
                        status = labourer.availabilityStatus,
                        preferredLanguage = preferredLanguage
                    )
                }
                
                // Skills
                Text(
                    text = labourer.skills.take(3).joinToString(", ") { 
                        it.getDisplayName(preferredLanguage) 
                    },
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
                
                // Rating and Completed Bookings
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = String.format("%.1f", labourer.averageRating),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurface
                    )
                    Text(
                        text = "(${labourer.totalRatings})",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BrandDeep,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${labourer.completedBookings} ${
                            if (preferredLanguage == "kn") "ಕೆಲಸಗಳು" else "jobs"
                        }",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                // Pricing and Distance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CurrencyRupee,
                            contentDescription = null,
                            tint = BrandDeep,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when (labourer.pricingType) {
                                PricingType.DAILY_WAGE -> "₹${labourer.dailyWage}/${
                                    if (preferredLanguage == "kn") "ದಿನ" else "day"
                                }"
                                PricingType.HOURLY_RATE -> "₹${labourer.hourlyRate}/${
                                    if (preferredLanguage == "kn") "ಗಂಟೆ" else "hr"
                                }"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandDeep
                        )
                    }
                }
            }
        }
    }
}

/**
 * Availability Badge Component
 */
@Composable
private fun AvailabilityBadge(
    status: AvailabilityStatus,
    preferredLanguage: String
) {
    val (text, color) = when (status) {
        AvailabilityStatus.AVAILABLE -> {
            (if (preferredLanguage == "kn") "ಲಭ್ಯ" else "Available") to Color(0xFF4CAF50)
        }
        AvailabilityStatus.UNAVAILABLE -> {
            (if (preferredLanguage == "kn") "ಅಲಭ್ಯ" else "Unavailable") to Color(0xFFFF5722)
        }
        AvailabilityStatus.BOOKED -> {
            (if (preferredLanguage == "kn") "ಬುಕ್ ಆಗಿದೆ" else "Booked") to Color(0xFFFFC107)
        }
    }
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Sort Chip Component
 */
@Composable
private fun SortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) BrandDeep else Surface.copy(alpha = 0.95f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else OnSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/**
 * Filter Dialog Component
 */
@Composable
private fun FilterDialog(
    selectedSkills: Set<Skill>,
    onSkillsChange: (Set<Skill>) -> Unit,
    minWage: Int,
    onMinWageChange: (Int) -> Unit,
    maxWage: Int,
    onMaxWageChange: (Int) -> Unit,
    minRating: Float,
    onMinRatingChange: (Float) -> Unit,
    preferredLanguage: String,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (preferredLanguage == "kn") "ಫಿಲ್ಟರ್‌ಗಳು" else "Filters",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Skills Filter
                Text(
                    text = if (preferredLanguage == "kn") "ಕೌಶಲ್ಯಗಳು" else "Skills",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Skill.values().forEach { skill ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSkillsChange(
                                    if (selectedSkills.contains(skill)) {
                                        selectedSkills - skill
                                    } else {
                                        selectedSkills + skill
                                    }
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedSkills.contains(skill),
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = BrandDeep)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = skill.getDisplayName(preferredLanguage),
                            fontSize = 13.sp
                        )
                    }
                }
                
                Divider()
                
                // Wage Range Filter
                Text(
                    text = if (preferredLanguage == "kn") "ವೇತನ ವ್ಯಾಪ್ತಿ" else "Wage Range",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minWage.toString(),
                        onValueChange = { onMinWageChange(it.toIntOrNull() ?: 300) },
                        label = { Text(if (preferredLanguage == "kn") "ಕನಿಷ್ಠ" else "Min", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxWage.toString(),
                        onValueChange = { onMaxWageChange(it.toIntOrNull() ?: 2000) },
                        label = { Text(if (preferredLanguage == "kn") "ಗರಿಷ್ಠ" else "Max", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                Divider()
                
                // Rating Filter
                Text(
                    text = if (preferredLanguage == "kn") "ಕನಿಷ್ಠ ರೇಟಿಂಗ್: ${minRating.toInt()}"
                    else "Minimum Rating: ${minRating.toInt()}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = minRating,
                    onValueChange = onMinRatingChange,
                    valueRange = 0f..5f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = BrandDeep,
                        activeTrackColor = BrandDeep
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(containerColor = BrandDeep)
            ) {
                Text(if (preferredLanguage == "kn") "ಅನ್ವಯಿಸಿ" else "Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (preferredLanguage == "kn") "ರದ್ದುಮಾಡಿ" else "Cancel")
            }
        }
    )
}
