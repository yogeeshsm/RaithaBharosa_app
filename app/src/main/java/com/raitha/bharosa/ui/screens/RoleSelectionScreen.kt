package com.raitha.bharosa.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.raitha.bharosa.data.UserRole
import com.raitha.bharosa.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Role Selection Screen for Agricultural Labour Booking System
 * 
 * Allows users to select their role as either Farmer or Labourer after authentication.
 * Stores the selected role in Firestore and navigates to the appropriate dashboard.
 * 
 * Requirement 1: User Role Management
 * - Presents role selection screen with Farmer and Labourer options
 * - Stores selected UserRole in Firestore
 * - Navigates to farmer dashboard or labourer profile creation based on selection
 * - Prevents role changes after initial selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    onRoleSelected: (UserRole) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Check if user already has a role selected
    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                val doc = firestore.collection("users").document(userId).get().await()
                val role = doc.getString("role")
                if (role != null) {
                    // User already has a role, navigate directly
                    onRoleSelected(UserRole.valueOf(role))
                }
            } catch (e: Exception) {
                // No role set yet, continue with selection
            }
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
        // Decorative circles
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-100).dp, y = (-80).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(BrandDeep.copy(alpha = 0.2f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(50)
                )
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 80.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(BrandLight.copy(alpha = 0.15f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(50)
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // App Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(listOf(BrandDeep, BrandLight))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Grass,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(45.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title
            Text(
                "Welcome to Labour Booking",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                "ಕಾರ್ಮಿಕ ಬುಕಿಂಗ್‌ಗೆ ಸ್ವಾಗತ",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Select your role to continue",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Text(
                "ಮುಂದುವರಿಯಲು ನಿಮ್ಮ ಪಾತ್ರವನ್ನು ಆಯ್ಕೆಮಾಡಿ",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Error Message
            AnimatedVisibility(visible = errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
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
                            errorMessage ?: "",
                            fontSize = 12.sp,
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Role Cards
            RoleCard(
                title = "I am a Farmer",
                titleKn = "ನಾನು ರೈತ",
                description = "Search and book labourers for agricultural work",
                descriptionKn = "ಕೃಷಿ ಕೆಲಸಕ್ಕಾಗಿ ಕಾರ್ಮಿಕರನ್ನು ಹುಡುಕಿ ಮತ್ತು ಬುಕ್ ಮಾಡಿ",
                icon = Icons.Default.Agriculture,
                isSelected = selectedRole == UserRole.FARMER,
                onClick = { selectedRole = UserRole.FARMER }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            RoleCard(
                title = "I am a Labourer",
                titleKn = "ನಾನು ಕಾರ್ಮಿಕ",
                description = "Create profile and receive work opportunities",
                descriptionKn = "ಪ್ರೊಫೈಲ್ ರಚಿಸಿ ಮತ್ತು ಕೆಲಸದ ಅವಕಾಶಗಳನ್ನು ಪಡೆಯಿರಿ",
                icon = Icons.Default.Person,
                isSelected = selectedRole == UserRole.LABOURER,
                onClick = { selectedRole = UserRole.LABOURER }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Continue Button
            Button(
                onClick = {
                    if (selectedRole != null) {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            
                            try {
                                val userId = auth.currentUser?.uid
                                if (userId != null) {
                                    // Store role in Firestore
                                    firestore.collection("users")
                                        .document(userId)
                                        .set(mapOf(
                                            "role" to selectedRole!!.name,
                                            "createdAt" to System.currentTimeMillis(),
                                            "updatedAt" to System.currentTimeMillis()
                                        ))
                                        .await()
                                    
                                    // Navigate based on role
                                    onRoleSelected(selectedRole!!)
                                } else {
                                    errorMessage = "User not authenticated"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Failed to save role: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandDeep,
                    disabledContainerColor = Color.Gray
                ),
                enabled = selectedRole != null && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Continue",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Info Text
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "You can only select your role once. Choose carefully.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/**
 * Role selection card component
 */
@Composable
private fun RoleCard(
    title: String,
    titleKn: String,
    description: String,
    descriptionKn: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                BrandDeep.copy(alpha = 0.95f)
            } else {
                Surface.copy(alpha = 0.95f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 12.dp else 6.dp
        ),
        border = if (isSelected) {
            BorderStroke(3.dp, BrandLight)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) {
                            Color.White.copy(alpha = 0.2f)
                        } else {
                            BrandDeep.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else BrandDeep,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            // Text Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else OnBackground
                )
                Text(
                    titleKn,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) {
                        Color.White.copy(alpha = 0.8f)
                    } else {
                        Color.Gray
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    fontSize = 12.sp,
                    color = if (isSelected) {
                        Color.White.copy(alpha = 0.9f)
                    } else {
                        Color.Gray
                    },
                    lineHeight = 16.sp
                )
            }
            
            // Selection Indicator
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = BrandLight,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
