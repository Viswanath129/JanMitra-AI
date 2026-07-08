package com.example

import com.example.data.entity.CitizenReport
import com.example.data.network.*
import com.example.data.repository.AiRepositoryImpl
import com.example.data.repository.ReportAnalysisResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JanMitraRobolectricTest {

    private lateinit var fakeAiDataSource: FakeAiDataSource
    private lateinit var fakeBackendApiService: FakeBackendApiService
    private lateinit var moshi: Moshi
    private lateinit var aiRepository: AiRepositoryImpl

    @Before
    fun setUp() {
        fakeAiDataSource = FakeAiDataSource()
        fakeBackendApiService = FakeBackendApiService()
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        aiRepository = AiRepositoryImpl(fakeAiDataSource, fakeBackendApiService, moshi)
    }

    @Test
    fun `analyzeReport with successful JSON parsing matches schema`() = runTest {
        val mockJson = """
            {
              "language": "Hindi",
              "urgency": "Critical",
              "summary": "Severe water crisis observed in Bhola village.",
              "duplicate_alert": "JM-2026-X84B is similar"
            }
        """.trimIndent()

        fakeAiDataSource.resultText = mockJson
        fakeAiDataSource.shouldFail = false

        val result = aiRepository.analyzeReport(
            category = "Water",
            description = "पानी की समस्या है",
            villageName = "Bhola Village",
            isVoiceRecorded = false,
            isPhotoAttached = false
        )

        assertNotNull(result)
        assertEquals("Hindi", result.language)
        assertEquals("Critical", result.urgency)
        assertEquals("Severe water crisis observed in Bhola village.", result.summary)
        assertEquals("JM-2026-X84B is similar", result.duplicateAlert)
    }

    @Test
    fun `analyzeReport with offline or failing network fallback is robust`() = runTest {
        fakeAiDataSource.shouldFail = true

        val result = aiRepository.analyzeReport(
            category = "Water",
            description = "पानी की समस्या है",
            villageName = "Bhola Village",
            isVoiceRecorded = false,
            isPhotoAttached = false
        )

        assertNotNull(result)
        assertEquals("English (Offline)", result.language)
        assertEquals("Medium", result.urgency)
        assertEquals("Requirement of Water in Bhola Village. Summary processed locally on-device.", result.summary)
    }

    // --- Pure Kotlin Fakes for Testing ---

    class FakeAiDataSource : AiDataSource {
        var resultText: String = ""
        var shouldFail: Boolean = false

        override suspend fun generateContent(prompt: String, systemInstruction: String?): String {
            if (shouldFail) {
                throw RuntimeException("Network disconnected")
            }
            return resultText
        }
    }

    class FakeBackendApiService : BackendApiService {
        override suspend fun submitReport(report: NetworkReportCreate): Map<String, Any> {
            return mapOf("status" to "success")
        }

        override suspend fun sendChatMessage(query: NetworkChatQuery): NetworkChatResponse {
            return NetworkChatResponse("success", "RAG Response")
        }

        override suspend fun compareProjects(request: NetworkCompareRequest): NetworkCompareResponse {
            return NetworkCompareResponse("success", "Comparison Response")
        }

        override suspend fun getAnalyticsDashboard(): NetworkAnalyticsResponse {
            return NetworkAnalyticsResponse(0, emptyMap(), emptyMap(), emptyMap())
        }

        override suspend fun uploadMedia(
            file: okhttp3.MultipartBody.Part,
            mediaType: okhttp3.RequestBody,
            reportId: okhttp3.RequestBody?
        ): NetworkMediaResponse {
            return NetworkMediaResponse("success", "http://test.url", "file.png")
        }
    }
}
