package com.example.data.network

import android.content.Context
import com.example.data.repository.AiRepository
import com.example.data.repository.AiRepositoryImpl
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

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

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                sessionManager?.accessToken?.let { token ->
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }
            .build()
    }

    val backendApiService: BackendApiService by lazy {
        val baseUrl = try {
            val configUrl = com.example.BuildConfig.BACKEND_URL
            if (configUrl.isNotEmpty() && configUrl != "MY_BACKEND_URL") configUrl else "http://10.0.2.2:8000/"
        } catch (e: Exception) {
            "http://10.0.2.2:8000/"
        }

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BackendApiService::class.java)
    }

    val aiRepository: AiRepository by lazy {
        AiRepositoryImpl(aiDataSource, backendApiService, moshi)
    }
}

