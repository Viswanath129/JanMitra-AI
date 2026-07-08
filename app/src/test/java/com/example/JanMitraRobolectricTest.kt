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
    private lateinit var moshi: Moshi
    private lateinit var aiRepository: AiRepositoryImpl

    @Before
    fun setUp() {
        fakeAiDataSource = FakeAiDataSource()
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        aiRepository = AiRepositoryImpl(fakeAiDataSource, moshi)
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
        assertEquals("English (Offline Fallback)", result.language)
        assertEquals("Medium", result.urgency)
        assertEquals("Local Heuristic: Requirement of Water in Bhola Village. Description: पानी की समस्या है. Summary processed locally on-device.", result.summary)
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
}
