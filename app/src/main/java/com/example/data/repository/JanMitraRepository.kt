package com.example.data.repository

import android.util.Log
import com.example.data.AppDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class JanMitraRepository(
    private val database: AppDatabase,
    private val citizenReportDao: CitizenReportDao,
    private val infrastructureAssetDao: InfrastructureAssetDao,
    private val villageStatisticsDao: VillageStatisticsDao,
    private val aiChatMessageDao: AiChatMessageDao
) {
    val allReports: Flow<List<CitizenReport>> = citizenReportDao.getAllReports().map { list ->
        list.map { decryptReport(it) }
    }
    val allAssets: Flow<List<InfrastructureAsset>> = infrastructureAssetDao.getAllAssets()
    val allVillageStats: Flow<List<VillageStatistics>> = villageStatisticsDao.getAllVillageStats()
    val allChatMessages: Flow<List<AiChatMessage>> = aiChatMessageDao.getAllMessages()

    suspend fun insertReport(report: CitizenReport) = withContext(Dispatchers.IO) {
        citizenReportDao.insertReport(encryptReport(report))
    }

    suspend fun updateReport(report: CitizenReport) = withContext(Dispatchers.IO) {
        citizenReportDao.updateReport(encryptReport(report))
    }

    suspend fun deleteReport(id: Int) = withContext(Dispatchers.IO) {
        citizenReportDao.deleteReportById(id)
    }

    suspend fun insertChatMessage(message: AiChatMessage) = withContext(Dispatchers.IO) {
        aiChatMessageDao.insertMessage(message)
    }

    suspend fun clearChat() = withContext(Dispatchers.IO) {
        aiChatMessageDao.clearAllMessages()
    }

    suspend fun cleanupOrphanedMedia(context: android.content.Context) = withContext(Dispatchers.IO) {
        try {
            val reportsList = citizenReportDao.getAllReports().firstOrNull() ?: return@withContext
            val referencedPaths = HashSet<String>()
            for (report in reportsList) {
                val decrypted = decryptReport(report)
                decrypted.imageUri?.let { referencedPaths.add(it) }
                decrypted.voiceFilePath?.let { referencedPaths.add(it) }
            }
            
            val filesDir = context.filesDir
            val files = filesDir.listFiles() ?: return@withContext
            for (file in files) {
                val absPath = file.absolutePath
                val name = file.name
                if (name.startsWith("attached_img") || name.startsWith("attached_vid")) {
                    if (!referencedPaths.contains(absPath)) {
                        val deleted = file.delete()
                        Log.d("JanMitraRepository", "Deleted orphaned media file: $name (success=$deleted)")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("JanMitraRepository", "Error cleaning up orphaned media: ${e.message}")
        }
    }

    suspend fun preseedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        database.withTransaction {
            val existingStats = villageStatisticsDao.getAllVillageStats().firstOrNull()
            if (existingStats.isNullOrEmpty()) {
                Log.d("JanMitraRepository", "Pre-seeding database with mock datasets...")

                // 1. Seed Village Statistics
            val stats = listOf(
                VillageStatistics(
                    villageName = "Bhola Village",
                    district = "District East",
                    ward = "Ward 12",
                    population = 1800,
                    developmentIndex = 0.32,
                    historicalFundingCr = 0.5,
                    drinkingWaterGap = true,
                    roadConnectivityGap = true,
                    schoolUpgradeNeed = false,
                    healthcareGap = true,
                    vulnerablePopulationPct = 55.0
                ),
                VillageStatistics(
                    villageName = "Rampur Village",
                    district = "District East",
                    ward = "Ward 14",
                    population = 4500,
                    developmentIndex = 0.45,
                    historicalFundingCr = 1.2,
                    drinkingWaterGap = true,
                    roadConnectivityGap = false,
                    schoolUpgradeNeed = true,
                    healthcareGap = true,
                    vulnerablePopulationPct = 35.0
                ),
                VillageStatistics(
                    villageName = "Seva Village",
                    district = "District West",
                    ward = "Ward 03",
                    population = 6200,
                    developmentIndex = 0.65,
                    historicalFundingCr = 2.8,
                    drinkingWaterGap = false,
                    roadConnectivityGap = false,
                    schoolUpgradeNeed = true,
                    healthcareGap = false,
                    vulnerablePopulationPct = 20.0
                ),
                VillageStatistics(
                    villageName = "Kalyanpur Village",
                    district = "District North",
                    ward = "Ward 08",
                    population = 3200,
                    developmentIndex = 0.52,
                    historicalFundingCr = 1.5,
                    drinkingWaterGap = false,
                    roadConnectivityGap = true,
                    schoolUpgradeNeed = false,
                    healthcareGap = false,
                    vulnerablePopulationPct = 40.0
                )
            )

            for (stat in stats) {
                villageStatisticsDao.insertVillageStat(stat)
            }

            // 2. Seed Infrastructure Assets
            val assets = listOf(
                InfrastructureAsset(
                    name = "Bhola Government Primary School",
                    type = "School",
                    villageName = "Bhola Village",
                    status = "Requires Repair",
                    latitude = 28.6145,
                    longitude = 77.2095,
                    capacityDetail = "Classes 1-5, 85 Students, leaking roof",
                    lastMaintained = "2023-04-12"
                ),
                InfrastructureAsset(
                    name = "Rampur Community Tube-well",
                    type = "Water Source",
                    villageName = "Rampur Village",
                    status = "Critical Gap",
                    latitude = 28.6250,
                    longitude = 77.2150,
                    capacityDetail = "Provides drinking water to 400 households, currently dry",
                    lastMaintained = "2024-01-20"
                ),
                InfrastructureAsset(
                    name = "Kalyanpur Main Village Access Link Road",
                    type = "Road",
                    villageName = "Kalyanpur Village",
                    status = "Critical Gap",
                    latitude = 28.6010,
                    longitude = 77.1950,
                    capacityDetail = "Single lane dirt track, washed out during monsoon",
                    lastMaintained = "2022-11-10"
                ),
                InfrastructureAsset(
                    name = "Seva Primary Health Sub-center",
                    type = "Hospital",
                    villageName = "Seva Village",
                    status = "Functional",
                    latitude = 28.6320,
                    longitude = 77.2280,
                    capacityDetail = "Outpatient subcenter, 1 Nurse, limited diagnostics",
                    lastMaintained = "2025-05-15"
                )
            )

            for (asset in assets) {
                infrastructureAssetDao.insertAsset(asset)
            }

            // 3. Seed Initial Citizen Reports using Priority Score calculations
            val vStatsList = villageStatisticsDao.getAllVillageStats().firstOrNull() ?: emptyList()
            val bholaStats = vStatsList.firstOrNull { it.villageName == "Bhola Village" }
            val rampurStats = vStatsList.firstOrNull { it.villageName == "Rampur Village" }
            val kalyanpurStats = vStatsList.firstOrNull { it.villageName == "Kalyanpur Village" }
            val sevaStats = vStatsList.firstOrNull { it.villageName == "Seva Village" }

            val reports = listOf(
                calculatePriorityScore(
                    category = "Water",
                    description = "Main community tube-well is completely dry. Women have to walk 3.5 kilometers to fetch contaminated river water.",
                    villageName = "Bhola Village",
                    urgency = "Critical",
                    hasImage = true,
                    hasVoice = true,
                    villageStats = bholaStats
                ).copy(status = "In Review"),

                calculatePriorityScore(
                    category = "School",
                    description = "School roof tiles fell during storms. Children study in rain under leaking sheet. Demanding urgent classroom rehabilitation.",
                    villageName = "Rampur Village",
                    urgency = "High",
                    hasImage = true,
                    hasVoice = false,
                    villageStats = rampurStats
                ).copy(status = "Approved"),

                calculatePriorityScore(
                    category = "Road",
                    description = "Connecting access road is broken. Patients can't reach hospital in emergencies. Auto-rickshaws refuse to enter village.",
                    villageName = "Kalyanpur Village",
                    urgency = "High",
                    hasImage = true,
                    hasVoice = true,
                    villageStats = kalyanpurStats
                ).copy(status = "In Progress"),

                calculatePriorityScore(
                    category = "Women Safety",
                    description = "Main road leading to Girls High School lacks street lights. Girls feel extremely unsafe returning home from evening classes.",
                    villageName = "Seva Village",
                    urgency = "Critical",
                    hasImage = false,
                    hasVoice = true,
                    villageStats = sevaStats
                ).copy(status = "Analyzed")
            )

            for (report in reports) {
                citizenReportDao.insertReport(encryptReport(report))
            }

            // 4. Seed initial AI assistant welcome messages
            aiChatMessageDao.insertMessage(
                AiChatMessage(
                    sender = "JanMitra AI",
                    text = "Welcome to the JanMitra AI Decision Intelligence assistant. Ask me questions about priority development needs, budgets, or village metrics."
                )
            )
        }
    }
}

    fun calculatePriorityScore(
        category: String,
        description: String,
        villageName: String,
        urgency: String,
        hasImage: Boolean,
        hasVoice: Boolean,
        villageStats: VillageStatistics?
    ): CitizenReport {
        val citizenDemandScore = when (urgency) {
            "Critical" -> 15.0
            "High" -> 12.0
            "Medium" -> 8.0
            else -> 4.0
        }

        val infraGapScore = if (villageStats != null) {
            val hasGap = when (category) {
                "Water" -> villageStats.drinkingWaterGap
                "Road" -> villageStats.roadConnectivityGap
                "School" -> villageStats.schoolUpgradeNeed
                "Hospital" -> villageStats.healthcareGap
                else -> false
            }
            if (hasGap) 15.0 else 5.0
        } else 8.0

        val populationImpactScore = if (villageStats != null) {
            val pop = villageStats.population
            when {
                pop > 5000 -> 15.0
                pop > 2000 -> 10.0
                pop > 1000 -> 7.0
                else -> 4.0
            }
        } else 8.0

        val distanceToServiceScore = when (category) {
            "Water", "Hospital", "Transport" -> 12.0
            "School", "Road" -> 8.0
            else -> 5.0
        }

        val safetyRiskScore = when {
            category == "Women Safety" -> 15.0
            category == "Electricity" || category == "Street Lights" -> 10.0
            category == "Drainage" || category == "Road" -> 8.0
            else -> 4.0
        }

        val eduHealthScore = when (category) {
            "School" -> 12.0
            "Hospital" -> 12.0
            "Water" -> 10.0
            else -> 4.0
        }

        val budgetFeasibilityScore = when {
            category == "Street Lights" || category == "Garbage" || category == "Internet" -> 12.0
            category == "Water" || category == "Drainage" -> 8.0
            else -> 5.0
        }

        val historicalNeglectScore = if (villageStats != null) {
            (1.0 - villageStats.developmentIndex) * 15.0
        } else 7.0

        val vulnerableWeight = if (villageStats != null) {
            villageStats.vulnerablePopulationPct * 15.0 / 100.0
        } else 5.0

        val evidenceStrengthScore = when {
            hasImage && hasVoice -> 10.0
            hasImage || hasVoice -> 7.0
            else -> 4.0
        }

        val totalScore = citizenDemandScore + infraGapScore + populationImpactScore + 
                         distanceToServiceScore + safetyRiskScore + eduHealthScore + 
                         budgetFeasibilityScore + historicalNeglectScore + vulnerableWeight + 
                         evidenceStrengthScore

        val truncatedScore = Math.min(100.0, totalScore)

        val explanation = "This project ranks highly (${String.format("%.1f", truncatedScore)}/100) because " +
                (if (historicalNeglectScore > 9) "the village has experienced a low development index (${villageStats?.developmentIndex ?: 0.4}) and low funding. " else "") +
                (if (infraGapScore > 10) "there is a verified government infrastructure gap in the ${category} department. " else "") +
                "It affects ${villageStats?.population ?: "1800+"} citizens with a ${String.format("%.0f", villageStats?.vulnerablePopulationPct ?: 45.0)}% vulnerable population weight. " +
                "The submission provides ${if (evidenceStrengthScore > 8) "Strong" else "Moderate"} evidence including media attachments."

        val randomIdSuffix = (1000..9999).random()
        val issueId = "JM-2026-$randomIdSuffix"

        return CitizenReport(
            issueId = issueId,
            category = category,
            description = description,
            voiceFilePath = if (hasVoice) "voice_clip.mp3" else null,
            imageUri = if (hasImage) "content://mock_image" else null,
            detectedLanguage = "English",
            locationLatitude = 28.6139 + (Math.random() - 0.5) * 0.05,
            locationLongitude = 77.2090 + (Math.random() - 0.5) * 0.05,
            locationName = villageName,
            urgency = urgency,
            status = "Reported",
            timestamp = System.currentTimeMillis(),
            priorityScore = truncatedScore,
            aiSummary = "Requirement of ${category} support in ${villageName}. Description: ${description}",
            evidenceStrength = if (evidenceStrengthScore > 8) "Strong" else "Moderate",
            citizenSentiment = if (urgency == "Critical") "Frustrated" else "Concerned",
            citizenDemandScore = citizenDemandScore,
            infraGapScore = infraGapScore,
            populationImpactScore = populationImpactScore,
            distanceToServiceScore = distanceToServiceScore,
            safetyRiskScore = safetyRiskScore,
            eduHealthScore = eduHealthScore,
            budgetFeasibilityScore = budgetFeasibilityScore,
            historicalNeglectScore = historicalNeglectScore,
            explanationText = explanation
        )
    }

    companion object {
        fun encryptReport(report: CitizenReport): CitizenReport {
            return report.copy(
                description = com.example.data.network.CryptoHelper.encrypt(report.description),
                voiceFilePath = report.voiceFilePath?.let { com.example.data.network.CryptoHelper.encrypt(it) },
                imageUri = report.imageUri?.let { com.example.data.network.CryptoHelper.encrypt(it) },
                locationName = com.example.data.network.CryptoHelper.encrypt(report.locationName),
                aiSummary = com.example.data.network.CryptoHelper.encrypt(report.aiSummary),
                explanationText = com.example.data.network.CryptoHelper.encrypt(report.explanationText)
            )
        }

        fun decryptReport(report: CitizenReport): CitizenReport {
            return report.copy(
                description = com.example.data.network.CryptoHelper.decrypt(report.description) ?: report.description,
                voiceFilePath = report.voiceFilePath?.let { com.example.data.network.CryptoHelper.decrypt(it) ?: it },
                imageUri = report.imageUri?.let { com.example.data.network.CryptoHelper.decrypt(it) ?: it },
                locationName = com.example.data.network.CryptoHelper.decrypt(report.locationName) ?: report.locationName,
                aiSummary = com.example.data.network.CryptoHelper.decrypt(report.aiSummary) ?: report.aiSummary,
                explanationText = com.example.data.network.CryptoHelper.decrypt(report.explanationText) ?: report.explanationText
            )
        }
    }
}
