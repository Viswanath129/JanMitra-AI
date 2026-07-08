package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "citizen_reports")
data class CitizenReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val issueId: String, // formatted like JM-2026-X84A
    val category: String, // Road, Water, Drainage, Electricity, School, Hospital, Transport, Agriculture, Employment, Internet, Street Lights, Garbage, Women Safety, Other
    val description: String,
    val voiceFilePath: String? = null,
    val imageUri: String? = null,
    val detectedLanguage: String = "English",
    val locationLatitude: Double = 28.6139,
    val locationLongitude: Double = 77.2090,
    val locationName: String = "Rampur Village",
    val urgency: String = "Medium", // Critical, High, Medium, Low
    val status: String = "Reported", // Reported, Analyzed, Approved, In Progress, Completed
    val timestamp: Long = System.currentTimeMillis(),
    val priorityScore: Double = 50.0,
    val aiSummary: String = "",
    val evidenceStrength: String = "Moderate", // Weak, Moderate, Strong
    val citizenSentiment: String = "Neutral", // Frustrated, Concerned, Hopeful, Neutral
    
    // Breakdown components of Priority Score for Explainable AI
    val citizenDemandScore: Double = 10.0,
    val infraGapScore: Double = 10.0,
    val populationImpactScore: Double = 10.0,
    val distanceToServiceScore: Double = 10.0,
    val safetyRiskScore: Double = 5.0,
    val eduHealthScore: Double = 5.0,
    val budgetFeasibilityScore: Double = 5.0,
    val historicalNeglectScore: Double = 5.0,
    val explanationText: String = ""
)

@Entity(tableName = "infrastructure_assets")
data class InfrastructureAsset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "School", "Hospital", "Water Source", "Road", "Power Grid"
    val villageName: String,
    val status: String, // "Functional", "Requires Repair", "Critical Gap"
    val latitude: Double,
    val longitude: Double,
    val capacityDetail: String, // e.g. "Primary School, 120 Students", "Primary Health Subcenter"
    val lastMaintained: String
)

@Entity(tableName = "village_statistics")
data class VillageStatistics(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val villageName: String,
    val district: String = "District East",
    val ward: String = "Ward 12",
    val population: Int,
    val developmentIndex: Double, // 0.0 (poor) to 1.0 (good)
    val historicalFundingCr: Double, // historical funding in Crores
    val drinkingWaterGap: Boolean,
    val roadConnectivityGap: Boolean,
    val schoolUpgradeNeed: Boolean,
    val healthcareGap: Boolean,
    val vulnerablePopulationPct: Double
)

@Entity(tableName = "ai_chat_messages")
data class AiChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "User" or "JanMitra AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
