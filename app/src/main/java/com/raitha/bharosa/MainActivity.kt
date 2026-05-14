package com.raitha.bharosa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.raitha.bharosa.data.UserRole
import com.raitha.bharosa.ui.screens.*
import com.raitha.bharosa.ui.theme.RaithaBharosaTheme
import com.raitha.bharosa.viewmodel.AppViewModel
import com.raitha.bharosa.viewmodel.AuthViewModel
import com.raitha.bharosa.viewmodel.LabourViewModel
import kotlinx.coroutines.tasks.await

sealed class Screen(val route: String, val labelEn: String, val labelKn: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", "ಮನೆ", Icons.Default.Home)
    object Inputs : Screen("inputs", "Inputs", "ಮಣ್ಣು", Icons.Default.Science)
    object Crops : Screen("crops", "Crops", "ಬೆಳೆ", Icons.Default.Grass)
    object Labour : Screen("labour", "Labour", "ಕಾರ್ಮಿಕರು", Icons.Default.Work)
    object Profile : Screen("profile", "Profile", "ಪ್ರೊಫೈಲ್", Icons.Default.Person)
    
    // Deprecated - kept for reference but not in navigation
    object Store : Screen("store", "Store", "ಅಂಗಡಿ", Icons.Default.ShoppingCart)
}

val NAV_ITEMS = listOf(Screen.Home, Screen.Inputs, Screen.Crops, Screen.Labour, Screen.Profile)

/** Auth navigation routes */
private enum class AuthRoute { LOGIN, SIGNUP, FORGOT_PASSWORD }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RaithaBharosaTheme {
                RaithaBharosaApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaithaBharosaApp() {
    val authViewModel: AuthViewModel = viewModel()
    val appViewModel: AppViewModel = viewModel()
    val labourViewModel: LabourViewModel = viewModel()

    val currentUser by authViewModel.currentUser.collectAsState()
    val profile by appViewModel.profile.collectAsState()
    val labourerProfile by labourViewModel.labourerProfile.collectAsState()

    // Auth screen route state
    var authRoute by remember { mutableStateOf(AuthRoute.LOGIN) }
    
    // User role state
    var userRole by remember { mutableStateOf<UserRole?>(null) }
    var isCheckingRole by remember { mutableStateOf(false) }
    var userDocumentReady by remember { mutableStateOf(false) }
    var setupError by remember { mutableStateOf<String?>(null) }

    // Check user role and ensure user document exists when logged in
    LaunchedEffect(currentUser) {
        if (currentUser != null && userRole == null && !isCheckingRole) {
            isCheckingRole = true
            setupError = null
            userDocumentReady = false
            
            val userId = currentUser!!.uid
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            
            // Try to create/verify user document with retry logic
            var retryCount = 0
            val maxRetries = 3
            var success = false
            
            while (retryCount < maxRetries && !success) {
                try {
                    // Check if user document exists
                    val doc = firestore.collection("users").document(userId).get().await()
                    
                    if (doc.exists() && doc.getString("role") != null) {
                        // User document exists with role
                        userRole = UserRole.valueOf(doc.getString("role")!!)
                        userDocumentReady = true
                        success = true
                    } else {
                        // User document doesn't exist or has no role - create it
                        firestore.collection("users").document(userId)
                            .set(
                                mapOf("role" to UserRole.FARMER.name),
                                com.google.firebase.firestore.SetOptions.merge()
                            )
                            .await()
                        
                        // Verify it was created
                        val verifyDoc = firestore.collection("users").document(userId).get().await()
                        if (verifyDoc.exists() && verifyDoc.getString("role") != null) {
                            userRole = UserRole.FARMER
                            userDocumentReady = true
                            success = true
                        } else {
                            throw Exception("User document verification failed")
                        }
                    }
                } catch (e: Exception) {
                    retryCount++
                    if (retryCount < maxRetries) {
                        // Exponential backoff: wait 1s, 2s, 4s
                        kotlinx.coroutines.delay(1000L * (1 shl (retryCount - 1)))
                    } else {
                        // All retries failed
                        setupError = "Unable to complete setup. Please check your network connection and try again."
                        // Default to FARMER role locally to allow offline usage
                        userRole = UserRole.FARMER
                        userDocumentReady = false
                    }
                }
            }
            
            isCheckingRole = false
        }
    }

    when {
        // ── Not logged in → Show auth screens ──────────────────────────────
        currentUser == null -> {
            when (authRoute) {
                AuthRoute.LOGIN -> LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToSignUp = { authRoute = AuthRoute.SIGNUP },
                    onNavigateToForgotPassword = { authRoute = AuthRoute.FORGOT_PASSWORD },
                    onLoginSuccess = { /* currentUser state update drives navigation */ }
                )
                AuthRoute.SIGNUP -> SignUpScreen(
                    authViewModel = authViewModel,
                    onNavigateToLogin = { authRoute = AuthRoute.LOGIN },
                    onSignUpSuccess = { /* currentUser state update drives navigation */ }
                )
                AuthRoute.FORGOT_PASSWORD -> ForgotPasswordScreen(
                    authViewModel = authViewModel,
                    onBack = { authRoute = AuthRoute.LOGIN }
                )
            }
        }

        // ── Logged in but checking role/creating user document → Show loading ───────────
        isCheckingRole -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Setting up your account...", style = MaterialTheme.typography.bodyLarge)
                    
                    if (setupError != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    setupError!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        // Retry setup
                                        isCheckingRole = false
                                        userRole = null
                                        setupError = null
                                    }
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Logged in but no role selected → Skip role selection, default to FARMER ───────────
        // Role selection screen removed - automatically defaults to FARMER in LaunchedEffect above

        // ── Farmer role but no profile → Skip profile setup ─────────
        // Profile setup skipped - go directly to main app

        // ── Labourer role but no profile → Skip profile setup ─────
        // Profile setup skipped - go directly to main app

        // ── Fully authenticated → Main app (no profile required) ───────────────────
        else -> {
            MainAppScaffold(
                appViewModel = appViewModel,
                authViewModel = authViewModel,
                labourViewModel = labourViewModel,
                userRole = userRole
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppScaffold(
    appViewModel: AppViewModel,
    authViewModel: AuthViewModel,
    labourViewModel: LabourViewModel,
    userRole: UserRole?
) {
    val lang by appViewModel.lang.collectAsState()
    var selectedTab by remember { mutableStateOf(Screen.Home.route) }
    val cart by appViewModel.cart.collectAsState()
    val cartCount = cart.sumOf { it.quantity }
    
    // Navigation state for labour screens
    var labourNavRoute by remember { mutableStateOf<String?>(null) }
    var selectedLabourerId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NAV_ITEMS.forEach { screen ->
                    NavigationBarItem(
                        selected = selectedTab == screen.route,
                        onClick = { 
                            selectedTab = screen.route
                            labourNavRoute = null // Reset labour navigation
                        },
                        icon = {
                            if (screen == Screen.Store && cartCount > 0) {
                                BadgedBox(badge = {
                                    Badge { Text("$cartCount", fontSize = 9.sp) }
                                }) {
                                    Icon(screen.icon, contentDescription = null)
                                }
                            } else {
                                Icon(screen.icon, contentDescription = null)
                            }
                        },
                        label = {
                            Text(
                                if (lang == "kn") screen.labelKn else screen.labelEn,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            when (selectedTab) {
                Screen.Home.route -> DashboardScreen(viewModel = appViewModel)
                Screen.Inputs.route -> InputsScreen(viewModel = appViewModel)
                Screen.Crops.route -> CropsScreen(viewModel = appViewModel)
                Screen.Labour.route -> {
                    // Labour feature navigation
                    when (labourNavRoute) {
                        "create_booking" -> {
                            // Get selected labourer profile
                            val searchResults by labourViewModel.searchResults.collectAsState()
                            val selectedProfile = searchResults.find { it.userId == selectedLabourerId }
                            
                            if (selectedProfile != null) {
                                CreateBookingScreen(
                                    labourViewModel = labourViewModel,
                                    labourerProfile = selectedProfile,
                                    onNavigateBack = { labourNavRoute = null },
                                    onBookingCreated = { 
                                        labourNavRoute = null
                                        // Show success message
                                    }
                                )
                            } else {
                                // Fallback to search if profile not found
                                labourNavRoute = null
                            }
                        }
                        else -> {
                            // Default labour screen based on user role
                            if (userRole == UserRole.FARMER) {
                                LabourerSearchScreen(
                                    labourViewModel = labourViewModel,
                                    onNavigateBack = { /* Can't go back from main tab */ },
                                    onLabourerClick = { labourerId ->
                                        selectedLabourerId = labourerId
                                        labourNavRoute = "create_booking"
                                    }
                                )
                            } else {
                                // For labourers, show their profile or dashboard
                                // For now, show search screen (can be updated later)
                                LabourerSearchScreen(
                                    labourViewModel = labourViewModel,
                                    onNavigateBack = { /* Can't go back from main tab */ },
                                    onLabourerClick = { labourerId ->
                                        selectedLabourerId = labourerId
                                        labourNavRoute = "create_booking"
                                    }
                                )
                            }
                        }
                    }
                }
                Screen.Profile.route -> ProfileScreen(
                    viewModel = appViewModel,
                    onCommunity = { selectedTab = "community" },
                    onSignOut = {
                        appViewModel.logout()
                        authViewModel.signOut()
                    }
                )
                "community" -> CommunityScreen(
                    viewModel = appViewModel,
                    onBack = { selectedTab = Screen.Profile.route }
                )
            }
        }
    }
}
