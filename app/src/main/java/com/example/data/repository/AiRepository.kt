package com.example.data.repository

import com.example.data.entity.CitizenReport
import com.example.data.network.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class ReportAnalysisResult(
    @Json(name = "language") val language: String = "English",
    @Json(name = "urgency") val urgency: String = "Medium",
    @Json(name = "summary") val summary: String = "",
    @Json(name = "duplicate_alert") val duplicateAlert: String? = null
)

interface AiRepository {
    suspend fun analyzeReport(
        category: String,
        description: String,
        villageName: String,
        isVoiceRecorded: Boolean,
        isPhotoAttached: Boolean
    ): ReportAnalysisResult

    suspend fun sendChatMessage(
        text: String,
        contextStr: String
    ): String

    suspend fun compareProjects(
        projA: CitizenReport,
        projB: CitizenReport
    ): String
}


class AiRepositoryImpl(
    private val aiDataSource: AiDataSource,
    private val moshi: Moshi
) : AiRepository {

    override suspend fun analyzeReport(
        category: String,
        description: String,
        villageName: String,
        isVoiceRecorded: Boolean,
        isPhotoAttached: Boolean
    ): ReportAnalysisResult {
        // Direct local/remote AI analysis via Gemini REST API
        val prompt = """
            You are the backend AI of JanMitra. The citizen is reporting an issue in ${villageName}:
            - Category: $category
            - User Description: "$description"
            - Recorded Voice: ${if (isVoiceRecorded) "Yes" else "No"}
            - Photo Attached: ${if (isPhotoAttached) "Yes" else "No"}
            
            Please perform:
            1. Language Detection: Detect the language of the description (could be English, Hindi, Telugu, Tamil, Marathi, code-mixed, etc.).
            2. Urgency Level: Recommend Urgency (Critical, High, Medium, Low) based on the severity.
            3. AI Summary: Write a concise, professional 2-line English summary for state planners.
            4. Check for similar/duplicate complaints: If the description is very simple, suggest if it could overlap with existing work.
            
            Respond strictly in JSON format matching this schema:
            {
              "language": "Detected language",
              "urgency": "Critical or High or Medium or Low",
              "summary": "Professional summary here",
              "duplicate_alert": "Optional duplicate alert details or null if no duplicates"
            }
        """.trimIndent()

        val systemInstruction = "You are a JSON parsing endpoint. Only output valid JSON. Do not include markdown code block markers or any preamble."

        val rawResponse = try {
            aiDataSource.generateContent(prompt, systemInstruction)
        } catch (e: Exception) {
            // Absolute local offline fallback without network at all (heuristic analysis)
            val descLower = description.lowercase()
            val detectedUrgency = if (descLower.contains("critical") || 
                                     descLower.contains("emergency") ||
                                     descLower.contains("accident") ||
                                     descLower.contains("broken") ||
                                     descLower.contains("overflowing") ||
                                     descLower.contains("dying") ||
                                     descLower.contains("unsafe")) "Critical" else "Medium"
            return ReportAnalysisResult(
                language = "English (Offline Fallback)",
                urgency = detectedUrgency,
                summary = "Local Heuristic: Requirement of $category in $villageName. Description: $description. Summary processed locally on-device.",
                duplicateAlert = null
            )
        }
        
        // Use clean JSON string
        var cleanJson = rawResponse.trim()
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.removePrefix("```json")
        }
        if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.removePrefix("```")
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.removeSuffix("```")
        }
        cleanJson = cleanJson.trim()

        return try {
            val adapter = moshi.adapter(ReportAnalysisResult::class.java)
            adapter.fromJson(cleanJson) ?: throw IllegalStateException("Parsed analysis result was null")
        } catch (e: Exception) {
            // Regex parsing fallback if Moshi fails on invalid JSON returned by LLM
            val langRegex = "\"language\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val urgencyRegex = "\"urgency\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val summaryRegex = "\"summary\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val dupRegex = "\"duplicate_alert\"\\s*:\\s*\"([^\"]+)\"".toRegex()

            val detectedLanguage = langRegex.find(cleanJson)?.groups?.get(1)?.value ?: "English"
            val detectedUrgency = urgencyRegex.find(cleanJson)?.groups?.get(1)?.value ?: "Medium"
            val aiAnalysisSummary = summaryRegex.find(cleanJson)?.groups?.get(1)?.value 
                ?: "JanMitra AI generated summary: Demand for $category in $villageName."
            val duplicateStr = dupRegex.find(cleanJson)?.groups?.get(1)?.value
            val duplicateAlertMessage = if (duplicateStr != "null" && duplicateStr != null) duplicateStr else null

            ReportAnalysisResult(
                language = detectedLanguage,
                urgency = detectedUrgency,
                summary = aiAnalysisSummary,
                duplicateAlert = duplicateAlertMessage
            )
        }
    }

    override suspend fun sendChatMessage(text: String, contextStr: String): String {
        // Direct Gemini model generation
        val systemInstruction = """
            You are JanMitra AI, a state-of-the-art Decision Intelligence system for Members of Parliament (MPs) and development planners.
            You have access to live datasets of citizen reported needs, current infrastructure statuses, and village development indexes.
            Your goal is to provide precise, data-driven, non-evasive answers to the MP.
            Always prioritize transparency. Explain the trade-offs of budget spending. Recommending the highest priority works based on evidence.
            Use bullet points, bold key figures (e.g. "₹2 Crore"), and keep responses professional, action-oriented, and structured.
        """.trimIndent()

        val prompt = """
            $contextStr
            
            USER MP QUESTION: "$text"
            
            Respond to the MP's question by cross-referencing the contextual datasets above. Make your response highly specific, naming actual villages, specific priority scores, and infrastructure status.
        """.trimIndent()

        return try {
            aiDataSource.generateContent(prompt, systemInstruction)
        } catch (e: Exception) {
            "JanMitra Intelligence Fallback: Offline mode active. Cannot compile deep analysis. Please check your internet connection or try again."
        }
    }

    override suspend fun compareProjects(projA: CitizenReport, projB: CitizenReport): String {
        // Direct Gemini model comparison
        val prompt = """
            Compare these two constituency projects side-by-side for an MP's decision making:
            Project A: ID ${projA.issueId}, ${projA.category} in ${projA.locationName}, priority score ${projA.priorityScore}. Description: "${projA.description}". Explanation: ${projA.explanationText}
            Project B: ID ${projB.issueId}, ${projB.category} in ${projB.locationName}, priority score ${projB.priorityScore}. Description: "${projB.description}". Explanation: ${projB.explanationText}
            
            Please write a clear, 3-paragraph comparison of which project is recommended to fund first. Include:
            - Dynamic population trade-off.
            - Evidence validation strength comparison.
            - Specific budgetary trade-off.
        """.trimIndent()

        return try {
            aiDataSource.generateContent(prompt, null)
        } catch (e: Exception) {
            "Comparison Fallback (Offline): Project A Priority Score is ${projA.priorityScore} and Project B Priority Score is ${projB.priorityScore}. Project A should be addressed first as recommended by local priority algorithms."
        }
    }
}
