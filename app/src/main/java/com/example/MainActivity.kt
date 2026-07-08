package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.network.NetworkModule
import com.example.data.repository.JanMitraRepository
import com.example.ui.components.JanMitraHeader
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryText
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.DividerGray
import com.example.viewmodel.JanMitraViewModel
import com.example.viewmodel.JanMitraViewModelFactory
import kotlinx.coroutines.launch
import android.util.Log
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.messaging.FirebaseMessaging
import com.example.data.network.SessionManager
import com.example.data.network.NetworkFcmTokenRegister
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: JanMitraViewModel

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted.")
            fetchAndRegisterFcmToken()
        } else {
            Log.w("MainActivity", "Notification permission denied.")
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            Log.d("MainActivity", "Handling deep link URI: $uri")
            if (uri.scheme == "janmitra" && uri.host == "report") {
                val issueId = uri.getQueryParameter("issue_id")
                if (!issueId.isNullOrEmpty()) {
                    Log.d("MainActivity", "Deep linked report ID: $issueId")
                    viewModel.currentTrackIdFromDeepLink = issueId
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun fetchAndRegisterFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("MainActivity", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result ?: return@addOnCompleteListener
            Log.d("MainActivity", "FCM Token fetched: $token")
            val sessionManager = SessionManager.getInstance(applicationContext)
            sessionManager.fcmToken = token

            // Subscribe to generic alerts topic
            FirebaseMessaging.getInstance().subscribeToTopic("all_alerts")
                .addOnCompleteListener { subTask ->
                    if (subTask.isSuccessful) {
                        Log.d("MainActivity", "Subscribed to 'all_alerts' topic")
                    } else {
                        Log.e("MainActivity", "Failed to subscribe to 'all_alerts' topic")
                    }
                }

            // Retry token registration with backend using exponential backoff
            registerFcmTokenWithRetry(token, sessionManager.userEmail, 1)
        }
    }

    private fun registerFcmTokenWithRetry(token: String, email: String?, attempt: Int) {
        lifecycleScope.launch {
            try {
                val api = NetworkModule.backendApiService
                val response = api.registerFcmToken(NetworkFcmTokenRegister(fcmToken = token, email = email))
                Log.d("MainActivity", "FCM Token registered successfully on attempt $attempt: $response")
            } catch (e: java.lang.Exception) {
                Log.e("MainActivity", "Attempt $attempt to register FCM Token failed: ${e.message}")
                if (attempt < 4) {
                    val delayMs = 2000L * attempt * attempt
                    kotlinx.coroutines.delay(delayMs)
                    registerFcmTokenWithRetry(token, email, attempt + 1)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase safely with fallback to avoid crashes if google-services.json is missing
        try {
            if (FirebaseApp.getApps(applicationContext).isEmpty()) {
                val googleAppIdId = applicationContext.resources.getIdentifier("google_app_id", "string", applicationContext.packageName)
                var initialized = false
                if (googleAppIdId != 0) {
                    try {
                        val appId = applicationContext.getString(googleAppIdId)
                        if (!appId.isNullOrBlank()) {
                            FirebaseApp.initializeApp(applicationContext)
                            Log.d("MainActivity", "Firebase initialized successfully with default resource-based config")
                            initialized = true
                        }
                    } catch (e: Exception) {
                        Log.w("MainActivity", "Default FirebaseApp initialization from resources failed: ${e.message}")
                    }
                }
                
                if (!initialized) {
                    Log.d("MainActivity", "Initializing Firebase with custom programmatic fallback options")
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:123456789012:android:0123456789abcdef012345")
                        .setApiKey("AIzaSyDummyApiKeyForFirebaseInitFallback")
                        .setProjectId("janmitra-ai-fallback")
                        .setGcmSenderId("123456789012")
                        .build()
                    FirebaseApp.initializeApp(applicationContext, options)
                    Log.d("MainActivity", "Firebase initialized with fallback programmatic options")
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Fatal error during Firebase initialization: ${e.message}", e)
        }

        // Initialize NetworkModule
        NetworkModule.initialize(applicationContext)

        // Initialize Room Database & Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = JanMitraRepository(
            database.citizenReportDao(),
            database.infrastructureAssetDao(),
            database.villageStatisticsDao(),
            database.aiChatMessageDao()
        )

        // Pre-seed the database if it is empty to ensure real datasets are loaded
        lifecycleScope.launch {
            repository.preseedDatabaseIfEmpty()
        }

        // Initialize ViewModel using the Custom Factory
        val factory = JanMitraViewModelFactory(application, repository, NetworkModule.aiRepository)
        viewModel = ViewModelProvider(this, factory)[JanMitraViewModel::class.java]

        // Handle initial intent for deep links
        handleIntent(intent)

        // Request POST_NOTIFICATIONS permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                fetchAndRegisterFcmToken()
            }
        } else {
            fetchAndRegisterFcmToken()
        }

        setContent {
            MyApplicationTheme {
                var activeScreen by remember { mutableStateOf("Home") }
                var trackSearchId by remember { mutableStateOf("") }

                LaunchedEffect(viewModel.currentTrackIdFromDeepLink) {
                    viewModel.currentTrackIdFromDeepLink?.let { issueId ->
                        Log.d("MainActivity", "LaunchedEffect received deep linked issueId: $issueId")
                        trackSearchId = issueId
                        activeScreen = "Track"
                        viewModel.currentTrackIdFromDeepLink = null
                    }
                }

                LaunchedEffect(viewModel.isUserLoggedIn, viewModel.currentUserEmail) {
                    if (viewModel.isUserLoggedIn && viewModel.currentUserEmail.isNotEmpty()) {
                        val sessionManager = SessionManager.getInstance(applicationContext)
                        sessionManager.fcmToken?.let { token ->
                            Log.d("MainActivity", "User logged in. Re-registering token for ${viewModel.currentUserEmail}")
                            registerFcmTokenWithRetry(token, viewModel.currentUserEmail, 1)
                        }
                    }
                }

                if (!viewModel.isUserLoggedIn) {
                    LoginScreen(viewModel = viewModel, activity = this@MainActivity)
                } else {

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            JanMitraHeader()
                        },
                        bottomBar = {
                            SleekBottomNavigationBar(
                                activeScreen = activeScreen,
                                onTabSelected = { screen ->
                                    activeScreen = screen
                                    if (screen != "Track") {
                                        trackSearchId = ""
                                    }
                                }
                            )
                        },
                        containerColor = SlateBackground
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (activeScreen) {
                                "Home" -> {
                                    HomeScreen(
                                        viewModel = viewModel,
                                        onNavigateToReport = { voiceFirst ->
                                            if (voiceFirst) {
                                                viewModel.isVoiceRecorded = true
                                                viewModel.activeFormStep = 2
                                            } else {
                                                viewModel.resetForm()
                                            }
                                            activeScreen = "Report"
                                        },
                                        onNavigateToTrack = { reportId ->
                                            trackSearchId = reportId
                                            activeScreen = "Track"
                                        },
                                        onNavigateToMap = {
                                            activeScreen = "Map"
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "Map" -> {
                                    MapScreen(
                                        viewModel = viewModel,
                                        onNavigateToTrack = { reportId ->
                                            trackSearchId = reportId
                                            activeScreen = "Track"
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "Report" -> {
                                    ReportScreen(
                                        viewModel = viewModel,
                                        onNavigateToTrack = { reportId ->
                                            trackSearchId = reportId
                                            activeScreen = "Track"
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "Track" -> {
                                    TrackScreen(
                                        viewModel = viewModel,
                                        initialSearchId = trackSearchId,
                                        onNavigateToMap = { activeScreen = "Map" },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "Profile" -> {
                                    ProfileScreen(
                                        viewModel = viewModel,
                                        onNavigateToChat = {
                                            activeScreen = "Chat"
                                        },
                                        onNavigateToReports = {
                                            activeScreen = "Track"
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                "Chat" -> {
                                    ChatScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = {
                                            activeScreen = "Profile"
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SleekBottomNavigationBar(
    activeScreen: String,
    onTabSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Tab
            BottomNavItem(
                icon = Icons.Rounded.Home,
                label = "Home",
                isSelected = activeScreen == "Home",
                onClick = { onTabSelected("Home") },
                modifier = Modifier.weight(1f)
            )

            // Map Tab
            BottomNavItem(
                icon = Icons.Rounded.Map,
                label = "GIS Map",
                isSelected = activeScreen == "Map",
                onClick = { onTabSelected("Map") },
                modifier = Modifier.weight(1f)
            )

            // Center Elevated Floating Action Button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .offset(y = (-14).dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue)
                        .clickable { onTabSelected("Report") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Speak / Report",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Track Tab
            BottomNavItem(
                icon = Icons.Rounded.Timeline,
                label = "Track Status",
                isSelected = activeScreen == "Track",
                onClick = { onTabSelected("Track") },
                modifier = Modifier.weight(1f)
            )

            // Profile Tab
            BottomNavItem(
                icon = Icons.Rounded.Person,
                label = "Profile",
                isSelected = activeScreen == "Profile",
                onClick = { onTabSelected("Profile") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) PrimaryBlue else SecondaryText,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) PrimaryBlue else SecondaryText
        )
    }
}
