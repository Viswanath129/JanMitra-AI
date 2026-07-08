package com.example.data.network

import android.content.Context
import com.example.data.repository.AiRepository
import com.example.data.repository.AiRepositoryImpl
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object NetworkModule {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private val sessionManager: SessionManager?
        get() = appContext?.let { SessionManager.getInstance(it) }

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    val geminiApiService: GeminiApiService by lazy {
        RetrofitClient.service
    }

    val aiDataSource: AiDataSource by lazy {
        GeminiAiDataSource(geminiApiService)
    }

    val aiRepository: AiRepository by lazy {
        AiRepositoryImpl(aiDataSource, moshi)
    }
}
