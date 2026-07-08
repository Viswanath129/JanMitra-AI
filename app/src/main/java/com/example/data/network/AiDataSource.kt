package com.example.data.network

interface AiDataSource {
    suspend fun generateContent(prompt: String, systemInstruction: String? = null): String
}

class GeminiAiDataSource(
    private val apiService: GeminiApiService
) : AiDataSource {
    override suspend fun generateContent(prompt: String, systemInstruction: String?): String {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("The API Key is not set or is using the placeholder. Please configure your GEMINI_API_KEY in the Secrets panel in AI Studio.")
        }

        val systemContent = systemInstruction?.let {
            Content(parts = listOf(Part(text = it)))
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = systemContent
        )

        val response = apiService.generateContent(apiKey, request)
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("No insights returned from JanMitra AI. Please try again.")
    }
}
