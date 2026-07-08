package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

@JsonClass(generateAdapter = true)
data class NetworkReportCreate(
    @Json(name = "issue_id") val issueId: String,
    @Json(name = "category") val category: String,
    @Json(name = "description") val description: String,
    @Json(name = "voice_file_path") val voiceFilePath: String?,
    @Json(name = "image_uri") val imageUri: String?,
    @Json(name = "location_latitude") val locationLatitude: Double,
    @Json(name = "location_longitude") val locationLongitude: Double,
    @Json(name = "location_name") val locationName: String,
    @Json(name = "urgency") val urgency: String,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "priority_score") val priorityScore: Double,
    @Json(name = "ai_summary") val aiSummary: String?,
    @Json(name = "evidence_strength") val evidenceStrength: String?,
    @Json(name = "citizen_sentiment") val citizenSentiment: String?,
    @Json(name = "explanation_text") val explanationText: String?
)

@JsonClass(generateAdapter = true)
data class NetworkChatQuery(
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class NetworkChatResponse(
    @Json(name = "status") val status: String,
    @Json(name = "response") val response: String
)

@JsonClass(generateAdapter = true)
data class NetworkCompareRequest(
    @Json(name = "project_a_id") val projectAId: String,
    @Json(name = "project_b_id") val projectBId: String
)

@JsonClass(generateAdapter = true)
data class NetworkCompareResponse(
    @Json(name = "status") val status: String,
    @Json(name = "comparison") val comparison: String
)

@JsonClass(generateAdapter = true)
data class NetworkAnalyticsResponse(
    @Json(name = "total_reports") val totalReports: Int,
    @Json(name = "urgency_distribution") val urgencyDistribution: Map<String, Int>,
    @Json(name = "category_distribution") val categoryDistribution: Map<String, Int>,
    @Json(name = "status_distribution") val statusDistribution: Map<String, Int>
)

@JsonClass(generateAdapter = true)
data class NetworkMediaResponse(
    @Json(name = "status") val status: String,
    @Json(name = "url") val url: String,
    @Json(name = "filename") val filename: String
)

@JsonClass(generateAdapter = true)
data class NetworkFirebaseExchangeRequest(
    @Json(name = "id_token") val idToken: String,
    @Json(name = "role") val role: String = "citizen"
)

@JsonClass(generateAdapter = true)
data class NetworkRefreshTokenRequest(
    @Json(name = "refresh_token") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class NetworkFcmTokenRegister(
    @Json(name = "fcm_token") val fcmToken: String,
    @Json(name = "email") val email: String? = null
)

@JsonClass(generateAdapter = true)
data class NetworkTestNotificationRequest(
    @Json(name = "fcm_token") val fcmToken: String,
    @Json(name = "title") val title: String,
    @Json(name = "message") val message: String,
    @Json(name = "issue_id") val issueId: String? = null
)

@JsonClass(generateAdapter = true)
data class NetworkTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "role") val role: String,
    @Json(name = "full_name") val fullName: String
)

interface BackendApiService {
    @POST("api/auth/firebase_exchange")
    suspend fun exchangeFirebaseToken(
        @Body request: NetworkFirebaseExchangeRequest
    ): NetworkTokenResponse

    @POST("api/auth/refresh")
    suspend fun refreshBackendToken(
        @Body request: NetworkRefreshTokenRequest
    ): NetworkTokenResponse

    @POST("api/reports/submit")
    suspend fun submitReport(
        @Body report: NetworkReportCreate
    ): Map<String, Any>

    @POST("api/auth/register_fcm_token")
    suspend fun registerFcmToken(
        @Body request: NetworkFcmTokenRegister
    ): Map<String, Any>

    @POST("api/auth/send_test_notification")
    suspend fun sendTestNotification(
        @Body request: NetworkTestNotificationRequest
    ): Map<String, Any>

    @POST("api/ai/chat")
    suspend fun sendChatMessage(
        @Body query: NetworkChatQuery
    ): NetworkChatResponse

    @POST("api/ai/compare")
    suspend fun compareProjects(
        @Body request: NetworkCompareRequest
    ): NetworkCompareResponse

    @GET("api/analytics/dashboard")
    suspend fun getAnalyticsDashboard(): NetworkAnalyticsResponse

    @Multipart
    @POST("api/media/upload")
    suspend fun uploadMedia(
        @retrofit2.http.Part file: MultipartBody.Part,
        @retrofit2.http.Part("media_type") mediaType: RequestBody,
        @retrofit2.http.Part("report_id") reportId: RequestBody?
    ): NetworkMediaResponse
}
