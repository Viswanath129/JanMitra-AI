package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.entity.*
import com.example.data.repository.AiRepository
import com.example.data.repository.JanMitraRepository
import com.example.data.network.SessionManager
import com.example.data.network.NetworkModule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class JanMitraViewModel(
    application: Application,
    private val repository: JanMitraRepository,
    private val aiRepository: AiRepository
) : AndroidViewModel(application) {

    // --- Core Data Flows ---
    val allReports: StateFlow<List<CitizenReport>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAssets: StateFlow<List<InfrastructureAsset>> = repository.allAssets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVillageStats: StateFlow<List<VillageStatistics>> = repository.allVillageStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<AiChatMessage>> = repository.allChatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Authentication States ---
    private val sessionManager = SessionManager.getInstance(application)
    
    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e("JanMitraViewModel", "Firebase Auth initialization failed: ${e.message}")
            null
        }
    }
    
    var isUserLoggedIn by mutableStateOf(sessionManager.isLoggedIn)
    var currentUserEmail by mutableStateOf(sessionManager.userEmail ?: "")
    var currentUserDisplayName by mutableStateOf(sessionManager.userName ?: "")
    var currentUserPhone by mutableStateOf(sessionManager.userPhone ?: "")
    var currentUserRole by mutableStateOf(sessionManager.userRole ?: "citizen")
    var currentTrackIdFromDeepLink by mutableStateOf<String?>(null)
    
    var isAuthenticating by mutableStateOf(false)
    var authErrorMessage by mutableStateOf<String?>(null)
    
    // For Phone OTP Flow
    var verificationIdToken by mutableStateOf("")
    var otpSentMessage by mutableStateOf<String?>(null)
    var isOtpSent by mutableStateOf(false)

    // --- Seeding and Init ---
    init {
        viewModelScope.launch {
            try {
                repository.preseedDatabaseIfEmpty()
            } catch (e: Exception) {
                Log.e("JanMitraViewModel", "Error preseeding db: ${e.message}")
            }
        }
        restoreSession()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            try {
                val firebaseUser = firebaseAuth?.currentUser
                if (firebaseUser != null && sessionManager.isLoggedIn) {
                    currentUserEmail = firebaseUser.email ?: "anonymous_${firebaseUser.uid}@janmitra.ai"
                    currentUserDisplayName = firebaseUser.displayName ?: "Firebase User"
                    currentUserPhone = firebaseUser.phoneNumber ?: ""
                    isUserLoggedIn = true
                    
                    try {
                        val result = firebaseUser.getIdToken(true).await()
                        result.token?.let { idToken ->
                            exchangeTokenWithBackend(idToken)
                        }
                    } catch (e: Exception) {
                        Log.e("JanMitraViewModel", "Failed to refresh Firebase token, using stored backend token: ${e.message}")
                    }
                } else if (sessionManager.isLoggedIn && sessionManager.refreshToken != null) {
                    refreshBackendSession()
                } else {
                    isUserLoggedIn = false
                }
            } catch (e: Exception) {
                Log.e("JanMitraViewModel", "Error restoring session: ${e.message}")
            }
        }
    }

    private suspend fun exchangeTokenWithBackend(idToken: String) {
        try {
            val response = NetworkModule.backendApiService.exchangeFirebaseToken(
                com.example.data.network.NetworkFirebaseExchangeRequest(idToken, currentUserRole)
            )
            sessionManager.accessToken = response.accessToken
            sessionManager.refreshToken = response.refreshToken
            sessionManager.userEmail = currentUserEmail
            sessionManager.userName = response.fullName
            sessionManager.userRole = response.role
            sessionManager.isLoggedIn = true
            
            currentUserDisplayName = response.fullName
            currentUserRole = response.role
            isUserLoggedIn = true
            authErrorMessage = null
        } catch (e: Exception) {
            Log.e("JanMitraViewModel", "Backend exchange failed: ${e.message}")
            authErrorMessage = "Backend sync failed: ${e.message}. Offline access active."
        }
    }

    private suspend fun refreshBackendSession() {
        val refreshToken = sessionManager.refreshToken ?: return
        try {
            val response = NetworkModule.backendApiService.refreshBackendToken(
                com.example.data.network.NetworkRefreshTokenRequest(refreshToken)
            )
            sessionManager.accessToken = response.accessToken
            sessionManager.refreshToken = response.refreshToken
            sessionManager.userName = response.fullName
            sessionManager.userRole = response.role
            sessionManager.isLoggedIn = true
            
            currentUserDisplayName = response.fullName
            currentUserRole = response.role
            isUserLoggedIn = true
            authErrorMessage = null
        } catch (e: Exception) {
            Log.e("JanMitraViewModel", "Backend token refresh failed: ${e.message}")
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            isAuthenticating = true
            authErrorMessage = null
            try {
                val auth = firebaseAuth
                if (auth != null) {
                    val result = auth.signInAnonymously().await()
                    val user = result.user
                    if (user != null) {
                        currentUserEmail = "anonymous_${user.uid}@janmitra.ai"
                        currentUserDisplayName = "Guest Advocate"
                        currentUserPhone = ""
                        currentUserRole = "citizen"
                        val idTokenResult = user.getIdToken(true).await()
                        idTokenResult.token?.let { idToken ->
                            exchangeTokenWithBackend(idToken)
                        }
                    } else {
                        throw Exception("Anonymous user is null")
                    }
                } else {
                    loginOfflineGuest()
                }
            } catch (e: Exception) {
                Log.e("JanMitraViewModel", "Anonymous sign-in failed: ${e.message}")
                authErrorMessage = e.message ?: "Anonymous sign-in failed"
                loginOfflineGuest()
            } finally {
                isAuthenticating = false
            }
        }
    }

    private fun loginOfflineGuest() {
        val guestUid = java.util.UUID.randomUUID().toString()
        currentUserEmail = "anonymous_$guestUid@janmitra.ai"
        currentUserDisplayName = "Guest Advocate (Local)"
        currentUserPhone = ""
        currentUserRole = "citizen"
        isUserLoggedIn = true
        sessionManager.isLoggedIn = true
        sessionManager.userEmail = currentUserEmail
        sessionManager.userName = currentUserDisplayName
        sessionManager.userRole = currentUserRole
    }

    fun loginWithEmailPassword(email: String, password: String) {
        viewModelScope.launch {
            isAuthenticating = true
            authErrorMessage = null
            try {
                val auth = firebaseAuth
                if (auth != null) {
                    val result = auth.signInWithEmailAndPassword(email, password).await()
                    val user = result.user
                    if (user != null) {
                        currentUserEmail = user.email ?: email
                        currentUserDisplayName = user.displayName ?: email.split("@")[0]
                        currentUserPhone = user.phoneNumber ?: ""
                        val idTokenResult = user.getIdToken(true).await()
                        idTokenResult.token?.let { idToken ->
                            exchangeTokenWithBackend(idToken)
                        }
                    } else {
                        throw Exception("User is null")
                    }
                } else {
                    val response = NetworkModule.backendApiService.exchangeFirebaseToken(
                        com.example.data.network.NetworkFirebaseExchangeRequest(
                            idToken = "mock_token_for_${email}",
                            role = "citizen"
                        )
                    )
                    currentUserEmail = email
                    currentUserDisplayName = response.fullName
                    currentUserRole = response.role
                    isUserLoggedIn = true
                    sessionManager.accessToken = response.accessToken
                    sessionManager.refreshToken = response.refreshToken
                    sessionManager.userEmail = email
                    sessionManager.userName = response.fullName
                    sessionManager.userRole = response.role
                    sessionManager.isLoggedIn = true
                }
            } catch (e: Exception) {
                Log.e("JanMitraViewModel", "Login failed: ${e.message}")
                authErrorMessage = e.message ?: "Authentication failed"
            } finally {
                isAuthenticating = false
            }
        }
    }

    fun registerWithEmailPassword(email: String, password: String, fullName: String) {
        viewModelScope.launch {
            isAuthenticating = true
            authErrorMessage = null
            try {
                val auth = firebaseAuth
                if (auth != null) {
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    val user = result.user
                    if (user != null) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()
                        user.updateProfile(profileUpdates).await()
                        
                        currentUserEmail = email
                        currentUserDisplayName = fullName
                        val tokenResult = user.getIdToken(true).await()
                        tokenResult.token?.let { idToken ->
                            exchangeTokenWithBackend(idToken)
                        }
                    }
                } else {
                    val response = NetworkModule.backendApiService.exchangeFirebaseToken(
                        com.example.data.network.NetworkFirebaseExchangeRequest(
                            idToken = "mock_token_for_${email}",
                            role = "citizen"
                        )
                    )
                    currentUserEmail = email
                    currentUserDisplayName = fullName
                    currentUserRole = response.role
                    isUserLoggedIn = true
                    sessionManager.accessToken = response.accessToken
                    sessionManager.refreshToken = response.refreshToken
                    sessionManager.userEmail = email
                    sessionManager.userName = fullName
                    sessionManager.userRole = response.role
                    sessionManager.isLoggedIn = true
                }
            } catch (e: Exception) {
                Log.e("JanMitraViewModel", "Registration failed: ${e.message}")
                authErrorMessage = e.message ?: "Registration failed"
            } finally {
                isAuthenticating = false
            }
        }
    }

    fun sendPhoneOtp(phoneNumber: String, activity: android.app.Activity) {
        viewModelScope.launch {
            isAuthenticating = true
            authErrorMessage = null
            try {
                val auth = firebaseAuth
                if (auth != null) {
                    val options = com.google.firebase.auth.PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                        .setActivity(activity)
                        .setCallbacks(object : com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                                viewModelScope.launch {
                                    try {
                                        val result = auth.signInWithCredential(credential).await()
                                        val user = result.user
                                        if (user != null) {
                                            currentUserPhone = phoneNumber
                                            currentUserEmail = user.email ?: "phone_${user.uid}@janmitra.ai"
                                            currentUserDisplayName = "Phone Advocate"
                                            val tokenResult = user.getIdToken(true).await()
                                            tokenResult.token?.let { exchangeTokenWithBackend(it) }
                                        }
                                    } catch (e: Exception) {
                                        authErrorMessage = e.message
                                    }
                                }
                            }

                            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                                authErrorMessage = e.message
                                isOtpSent = false
                            }

                            override fun onCodeSent(verificationId: String, token: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken) {
                                verificationIdToken = verificationId
                                isOtpSent = true
                                otpSentMessage = "OTP sent successfully to $phoneNumber"
                            }
                        })
                        .build()
                    com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(options)
                } else {
                    verificationIdToken = "mock_verification_id"
                    isOtpSent = true
                    otpSentMessage = "Demo OTP (use 123456) sent to $phoneNumber"
                }
            } catch (e: Exception) {
                authErrorMessage = e.message ?: "Failed to verify phone"
            } finally {
                isAuthenticating = false
            }
        }
    }

    fun verifyPhoneOtp(code: String) {
        viewModelScope.launch {
            isAuthenticating = true
            authErrorMessage = null
            try {
                val auth = firebaseAuth
                if (auth != null && verificationIdToken != "mock_verification_id") {
                    val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(verificationIdToken, code)
                    val result = auth.signInWithCredential(credential).await()
                    val user = result.user
                    if (user != null) {
                        currentUserPhone = user.phoneNumber ?: ""
                        currentUserEmail = user.email ?: "phone_${user.uid}@janmitra.ai"
                        currentUserDisplayName = "Phone Advocate"
                        val tokenResult = user.getIdToken(true).await()
                        tokenResult.token?.let { exchangeTokenWithBackend(it) }
                    }
                } else {
                    if (code == "123456" || verificationIdToken == "mock_verification_id") {
                        currentUserPhone = "+91 98765 43210"
                        currentUserEmail = "phone_demo@janmitra.ai"
                        currentUserDisplayName = "Phone Advocate (Demo)"
                        isUserLoggedIn = true
                        sessionManager.isLoggedIn = true
                        sessionManager.userEmail = currentUserEmail
                        sessionManager.userName = currentUserDisplayName
                        sessionManager.userPhone = currentUserPhone
                    } else {
                        throw Exception("Invalid OTP verification code")
                    }
                }
            } catch (e: Exception) {
                authErrorMessage = e.message ?: "Verification failed"
            } finally {
                isAuthenticating = false
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            isAuthenticating = true
            authErrorMessage = null
            try {
                val auth = firebaseAuth
                if (auth != null) {
                    val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                    val result = auth.signInWithCredential(credential).await()
                    val user = result.user
                    if (user != null) {
                        currentUserEmail = user.email ?: ""
                        currentUserDisplayName = user.displayName ?: ""
                        val tokenResult = user.getIdToken(true).await()
                        tokenResult.token?.let { exchangeTokenWithBackend(it) }
                    }
                } else {
                    currentUserEmail = "google_demo@gmail.com"
                    currentUserDisplayName = "Kasi Viswanath"
                    currentUserRole = "citizen"
                    isUserLoggedIn = true
                    sessionManager.isLoggedIn = true
                    sessionManager.userEmail = currentUserEmail
                    sessionManager.userName = currentUserDisplayName
                    sessionManager.userRole = currentUserRole
                }
            } catch (e: Exception) {
                authErrorMessage = e.message ?: "Google Sign-In failed"
            } finally {
                isAuthenticating = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                firebaseAuth?.signOut()
            } catch (e: Exception) {
                Log.e("JanMitraViewModel", "Firebase sign out failed: ${e.message}")
            }
            sessionManager.clear()
            isUserLoggedIn = false
            currentUserEmail = ""
            currentUserDisplayName = ""
            currentUserPhone = ""
            isOtpSent = false
            verificationIdToken = ""
            otpSentMessage = null
            authErrorMessage = null
        }
    }

    // --- Citizen Submission Form State ---
    var reportCategory by mutableStateOf("Water")
    var reportDescription by mutableStateOf("")
    var reportVillageName by mutableStateOf("Bhola Village")
    var reportUrgency by mutableStateOf("Medium")
    var isVoiceRecorded by mutableStateOf(false)
    var isPhotoAttached by mutableStateOf(false)
    var isAnalyzingReport by mutableStateOf(false)

    // Advanced features: GPS Location and Photo/Video paths
    var reportLatitude by mutableStateOf(28.6139)
    var reportLongitude by mutableStateOf(77.2090)
    var attachedImageUri by mutableStateOf<String?>(null)
    var attachedVideoUri by mutableStateOf<String?>(null)

    // AI Analysis output fields (Step 3/4)
    var detectedLanguage by mutableStateOf("English")
    var detectedUrgency by mutableStateOf("Medium")
    var aiAnalysisSummary by mutableStateOf("")
    var simulatedPriorityScore by mutableStateOf(50.0)
    var duplicateAlertMessage by mutableStateOf<String?>(null)

    // Form Navigation
    var activeFormStep by mutableStateOf(1)
    var submittedReportId by mutableStateOf<String?>(null)

    fun resetForm() {
        reportCategory = "Water"
        reportDescription = ""
        reportVillageName = "Bhola Village"
        reportUrgency = "Medium"
        isVoiceRecorded = false
        isPhotoAttached = false
        activeFormStep = 1
        submittedReportId = null
        aiAnalysisSummary = ""
        duplicateAlertMessage = null
        isAnalyzingReport = false
        reportLatitude = 28.6139
        reportLongitude = 77.2090
        attachedImageUri = null
        attachedVideoUri = null
    }

    fun saveFormDraft() {
        viewModelScope.launch {
            val stats = allVillageStats.value.firstOrNull { it.villageName == reportVillageName }
            val report = repository.calculatePriorityScore(
                category = reportCategory,
                description = reportDescription,
                villageName = reportVillageName,
                urgency = reportUrgency,
                hasImage = attachedImageUri != null,
                hasVoice = isVoiceRecorded,
                villageStats = stats
            ).copy(
                detectedLanguage = detectedLanguage,
                aiSummary = "DRAFT: " + reportDescription,
                locationLatitude = reportLatitude,
                locationLongitude = reportLongitude,
                imageUri = attachedImageUri,
                voiceFilePath = attachedVideoUri,
                status = "Draft"
            )
            repository.insertReport(report)
            triggerSync()
            resetForm()
        }
    }

    fun loadDraft(report: CitizenReport) {
        reportCategory = report.category
        reportDescription = report.description
        reportVillageName = report.locationName
        reportLatitude = report.locationLatitude
        reportLongitude = report.locationLongitude
        attachedImageUri = report.imageUri
        attachedVideoUri = report.voiceFilePath
        isPhotoAttached = report.imageUri != null
        isVoiceRecorded = report.voiceFilePath != null
        activeFormStep = 2 // Go directly to details
        
        viewModelScope.launch {
            repository.deleteReport(report.id)
        }
    }

    fun analyzeReportWithGemini() {
        isAnalyzingReport = true
        viewModelScope.launch {
            try {
                // Look up village population and index to supply as context
                val stats = allVillageStats.value.firstOrNull { it.villageName == reportVillageName }
                
                val result = aiRepository.analyzeReport(
                    category = reportCategory,
                    description = reportDescription,
                    villageName = reportVillageName,
                    isVoiceRecorded = isVoiceRecorded || attachedVideoUri != null,
                    isPhotoAttached = isPhotoAttached || attachedImageUri != null
                )

                detectedLanguage = result.language
                detectedUrgency = result.urgency
                aiAnalysisSummary = result.summary
                duplicateAlertMessage = result.duplicateAlert

                // Compute priority score simulation
                val computedReport = repository.calculatePriorityScore(
                    category = reportCategory,
                    description = reportDescription,
                    villageName = reportVillageName,
                    urgency = detectedUrgency,
                    hasImage = isPhotoAttached || attachedImageUri != null,
                    hasVoice = isVoiceRecorded || attachedVideoUri != null,
                    villageStats = stats
                )
                simulatedPriorityScore = computedReport.priorityScore
                activeFormStep = 3
            } catch (e: Exception) {
                // Fallback analysis
                detectedLanguage = "English"
                detectedUrgency = reportUrgency
                aiAnalysisSummary = "Urgent attention required for $reportCategory in $reportVillageName. Description: $reportDescription"
                simulatedPriorityScore = 72.0
                duplicateAlertMessage = null
                activeFormStep = 3
            } finally {
                isAnalyzingReport = false
            }
        }
    }

    fun submitFormReport() {
        viewModelScope.launch {
            val stats = allVillageStats.value.firstOrNull { it.villageName == reportVillageName }
            val report = repository.calculatePriorityScore(
                category = reportCategory,
                description = reportDescription,
                villageName = reportVillageName,
                urgency = detectedUrgency,
                hasImage = isPhotoAttached || attachedImageUri != null,
                hasVoice = isVoiceRecorded || attachedVideoUri != null,
                villageStats = stats
            ).copy(
                detectedLanguage = detectedLanguage,
                aiSummary = aiAnalysisSummary,
                locationLatitude = reportLatitude,
                locationLongitude = reportLongitude,
                imageUri = attachedImageUri,
                voiceFilePath = attachedVideoUri,
                status = "Reported"
            )
            repository.insertReport(report)
            triggerSync()
            submittedReportId = report.issueId
            activeFormStep = 4
        }
    }

    // --- AI RAG Chat Assistant ---
    var isSendingChatMessage by mutableStateOf(false)
    var currentChatInput by mutableStateOf("")

    fun sendChatMessage(text: String) {
        if (text.trim().isEmpty()) return
        viewModelScope.launch {
            // Save user message
            repository.insertChatMessage(AiChatMessage(sender = "User", text = text))
            currentChatInput = ""
            isSendingChatMessage = true

            try {
                // Gather contextual state
                val reports = allReports.value
                val assets = allAssets.value
                val stats = allVillageStats.value

                val contextStr = buildRagContext(reports, assets, stats)
                val aiResponse = aiRepository.sendChatMessage(text, contextStr)
                repository.insertChatMessage(AiChatMessage(sender = "JanMitra AI", text = aiResponse))
            } catch (e: Exception) {
                repository.insertChatMessage(
                    AiChatMessage(
                        sender = "JanMitra AI",
                        text = "I encountered an issue analyzing the constituency datasets. Please try asking again. Error: ${e.message}"
                    )
                )
            } finally {
                isSendingChatMessage = false
            }
        }
    }

    private fun buildRagContext(
        reports: List<CitizenReport>,
        assets: List<InfrastructureAsset>,
        stats: List<VillageStatistics>
    ): String {
        val reportsStr = reports.joinToString("\n") { 
            "- [${it.issueId}] ${it.category} in ${it.locationName}: Status: ${it.status}, Urgency: ${it.urgency}, Priority Score: ${String.format("%.1f", it.priorityScore)}/100. Description: ${it.description}"
        }
        val assetsStr = assets.joinToString("\n") { 
            "- ${it.name} (${it.type}) in ${it.villageName}: Status: ${it.status}. Details: ${it.capacityDetail}"
        }
        val statsStr = stats.joinToString("\n") { 
            "- ${it.villageName}: Pop: ${it.population}, Dev Index: ${it.developmentIndex}, Funding: ${it.historicalFundingCr} Cr. Gaps -> Water: ${it.drinkingWaterGap}, Road: ${it.roadConnectivityGap}, School Upgrade: ${it.schoolUpgradeNeed}, Healthcare Gap: ${it.healthcareGap}. Vulnerable Pop: ${it.vulnerablePopulationPct}%"
        }
        return """
            --- LIVE CONSTITUENCY DATASET ---
            
            CITIZEN DEVELOPMENT REPORTS:
            $reportsStr
            
            GOVERNMENT INFRASTRUCTURE ASSETS:
            $assetsStr
            
            VILLAGE METRICS & HISTORICAL DATA:
            $statsStr
        """.trimIndent()
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChat()
            // Add initial welcome back
            repository.insertChatMessage(
                AiChatMessage(
                    sender = "JanMitra AI",
                    text = "Welcome back to JanMitra AI. How can I help you analyze the development requests today?"
                )
            )
        }
    }

    fun approveReport(report: CitizenReport) {
        viewModelScope.launch {
            repository.updateReport(report.copy(status = "Approved"))
        }
    }

    fun triggerSync() {
        try {
            val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.data.SyncReportWorker>()
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                )
                .build()
            androidx.work.WorkManager.getInstance(getApplication()).enqueue(syncRequest)
            Log.d("JanMitraViewModel", "Enqueued OneTimeWorkRequest for SyncReportWorker successfully.")
        } catch (e: Exception) {
            Log.e("JanMitraViewModel", "Error triggering WorkManager sync: ${e.message}")
        }
    }
}

class JanMitraViewModelFactory(
    private val application: Application,
    private val repository: JanMitraRepository,
    private val aiRepository: AiRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JanMitraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JanMitraViewModel(application, repository, aiRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
