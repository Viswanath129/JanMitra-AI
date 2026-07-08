package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class JanMitraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase safely with fallback to avoid crashes if google-services.json is missing
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val googleAppIdId = resources.getIdentifier("google_app_id", "string", packageName)
                var initialized = false
                if (googleAppIdId != 0) {
                    try {
                        val appId = getString(googleAppIdId)
                        if (!appId.isNullOrBlank()) {
                            FirebaseApp.initializeApp(this)
                            Log.d("JanMitraApplication", "Firebase initialized successfully with default resource-based config")
                            initialized = true
                        }
                    } catch (e: Exception) {
                        Log.w("JanMitraApplication", "Default FirebaseApp initialization from resources failed: ${e.message}")
                    }
                }
                
                if (!initialized) {
                    Log.d("JanMitraApplication", "Initializing Firebase with custom programmatic fallback options")
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:123456789012:android:0123456789abcdef012345")
                        .setApiKey("AIzaSyDummyApiKeyForFirebaseInitFallback")
                        .setProjectId("janmitra-ai-fallback")
                        .setGcmSenderId("123456789012")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                    Log.d("JanMitraApplication", "Firebase initialized with fallback programmatic options")
                }
            }
        } catch (e: Exception) {
            Log.e("JanMitraApplication", "Fatal error during Firebase initialization: ${e.message}", e)
        }
    }
}
