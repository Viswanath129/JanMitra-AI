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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
    
    var isUserLoggedIn by mutableStateOf(false)
    var currentUserEmail by mutableStateOf("")
    var currentUserDisplayName by mutableStateOf("")
    var currentUserPhone by mutableStateOf("")
    var currentUserRole by mutableStateOf("citizen")
    var currentTrackIdFromDeepLink by mutableStateOf<String?>(null)
    var editingDraftId by mutableStateOf<Int?>(null)
    
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
                val isLoggedIn = sessionManager.isLoggedInFlow.first()
                userLanguage = sessionManager.userLanguageFlow.first()
                if (isLoggedIn) {
                    currentUserEmail = sessionManager.userEmailFlow.first() ?: ""
                    currentUserDisplayName = sessionManager.userNameFlow.first() ?: ""
                    currentUserPhone = sessionManager.userPhoneFlow.first() ?: ""
                    currentUserRole = sessionManager.userRoleFlow.first() ?: "citizen"
                    isUserLoggedIn = true
                } else {
                    isUserLoggedIn = false
                }
            } catch (e: Exception) {
                Log.e("JanMitraViewModel", "Error restoring session: ${e.message}")
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            isAuthenticating = true
            authErrorMessage = null
            try {
                loginOfflineGuest()
            } catch (e: Exception) {
                Log.e("JanMitraViewModel", "Anonymous sign-in failed: ${e.message}")
                authErrorMessage = e.message ?: "Anonymous sign-in failed"
            } finally {
                isAuthenticating = false
            }
        }
    }

    private suspend fun loginOfflineGuest() {
        val guestUid = java.util.UUID.randomUUID().toString().take(6)
        currentUserEmail = "guest_$guestUid@janmitra.ai"
        currentUserDisplayName = "Guest Advocate"
        currentUserPhone = ""
        currentUserRole = "citizen"
        isUserLoggedIn = true
        sessionManager.setIsLoggedIn(true)
        sessionManager.setUserEmail(currentUserEmail)
        sessionManager.setUserName(currentUserDisplayName)
        sessionManager.setUserPhone(currentUserPhone)
        sessionManager.setUserRole(currentUserRole)
    }

    fun loginWithEmailPassword(email: String, password: String) {
        viewModelScope.launch {
            isAuthenticating = true
            authErrorMessage = null
            try {
                currentUserEmail = email
                currentUserDisplayName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                currentUserPhone = ""
                currentUserRole = when {
                    email.contains("admin", ignoreCase = true) -> "admin"
                    email.contains("officer", ignoreCase = true) || email.contains("mp", ignoreCase = true) -> "officer"
                    else -> "citizen"
                }
                isUserLoggedIn = true
                sessionManager.setIsLoggedIn(true)
                sessionManager.setUserEmail(currentUserEmail)
                sessionManager.setUserName(currentUserDisplayName)
                sessionManager.setUserPhone(currentUserPhone)
                sessionManager.setUserRole(currentUserRole)
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
                currentUserEmail = email
                currentUserDisplayName = fullName
                currentUserPhone = ""
                currentUserRole = "citizen"
                isUserLoggedIn = true
                sessionManager.setIsLoggedIn(true)
                sessionManager.setUserEmail(email)
                sessionManager.setUserName(fullName)
                sessionManager.setUserPhone(currentUserPhone)
                sessionManager.setUserRole(currentUserRole)
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
                verificationIdToken = "mock_verification_id"
                isOtpSent = true
                otpSentMessage = "Demo OTP (use 123456) sent to $phoneNumber"
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
                if (code == "123456") {
                    currentUserPhone = "+91 98765 43210"
                    currentUserEmail = "phone_demo@janmitra.ai"
                    currentUserDisplayName = "Phone Advocate (Demo)"
                    currentUserRole = "citizen"
                    isUserLoggedIn = true
                    sessionManager.setIsLoggedIn(true)
                    sessionManager.setUserEmail(currentUserEmail)
                    sessionManager.setUserName(currentUserDisplayName)
                    sessionManager.setUserPhone(currentUserPhone)
                    sessionManager.setUserRole(currentUserRole)
                } else {
                    throw Exception("Invalid OTP verification code. Please use 123456.")
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
                currentUserEmail = "google_demo@gmail.com"
                currentUserDisplayName = "Kasi Viswanath"
                currentUserRole = "citizen"
                isUserLoggedIn = true
                sessionManager.setIsLoggedIn(true)
                sessionManager.setUserEmail(currentUserEmail)
                sessionManager.setUserName(currentUserDisplayName)
                sessionManager.setUserRole(currentUserRole)
            } catch (e: Exception) {
                authErrorMessage = e.message ?: "Google Sign-In failed"
            } finally {
                isAuthenticating = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
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

    fun deleteReport(id: Int) {
        viewModelScope.launch {
            repository.deleteReport(id)
        }
    }

    fun updateReport(report: CitizenReport) {
        viewModelScope.launch {
            repository.updateReport(report)
        }
    }

    // --- Citizen Submission Form State ---
    var userLanguage by mutableStateOf("English")

    fun updateUserLanguage(lang: String) {
        userLanguage = lang
        viewModelScope.launch {
            sessionManager.setUserLanguage(lang)
        }
    }

    var reportCategory by mutableStateOf("")
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
        reportCategory = ""
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
        editingDraftId = null
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
                id = editingDraftId ?: 0,
                detectedLanguage = detectedLanguage,
                aiSummary = "DRAFT: " + reportDescription,
                locationLatitude = reportLatitude,
                locationLongitude = reportLongitude,
                imageUri = attachedImageUri,
                voiceFilePath = attachedVideoUri,
                status = "Draft"
            )
            val newId = repository.insertReport(report)
            if (editingDraftId == null) {
                editingDraftId = newId.toInt()
            }
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
        editingDraftId = report.id
    }

    suspend fun copyUriToLocal(context: android.content.Context, uri: android.net.Uri, prefix: String): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        return@withContext try {
            val contentResolver = context.contentResolver
            val type = contentResolver.getType(uri)
            
            // 1. Format validation: Only allow images, videos, and audio
            if (type != null && !type.contains("image") && !type.contains("video") && !type.contains("audio")) {
                Log.e("JanMitraViewModel", "Unsupported format: $type")
                null
            } else {
                // 2. Pre-copy size validation using OpenableColumns
                var size = -1L
                try {
                    contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                            if (sizeIndex != -1) {
                                size = cursor.getLong(sizeIndex)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("JanMitraViewModel", "Could not query size beforehand: ${e.message}")
                }

                if (size > 10 * 1024 * 1024) {
                    Log.e("JanMitraViewModel", "File exceeds 10MB limit (pre-checked): $size bytes")
                    null
                } else {
                    val ext = when {
                        type?.contains("video") == true -> "mp4"
                        type?.contains("audio") == true -> "3gp"
                        else -> "jpg"
                    }
                    val fileName = "${prefix}_${System.currentTimeMillis()}.$ext"
                    val localFile = java.io.File(context.filesDir, fileName)
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        java.io.FileOutputStream(localFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    // Fallback post-copy check
                    if (localFile.length() > 10 * 1024 * 1024) {
                        Log.e("JanMitraViewModel", "File exceeds 10MB limit (post-checked): ${localFile.length()} bytes")
                        localFile.delete()
                        null
                    } else {
                        localFile.absolutePath
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("JanMitraViewModel", "Error copying URI to local file: ${e.message}")
            null
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
                id = editingDraftId ?: 0,
                detectedLanguage = detectedLanguage,
                aiSummary = aiAnalysisSummary,
                locationLatitude = reportLatitude,
                locationLongitude = reportLongitude,
                imageUri = attachedImageUri,
                voiceFilePath = attachedVideoUri,
                status = "PendingSubmission"
            )
            repository.insertReport(report)
            triggerSync()
            submittedReportId = report.issueId
            activeFormStep = 4
            editingDraftId = null
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
