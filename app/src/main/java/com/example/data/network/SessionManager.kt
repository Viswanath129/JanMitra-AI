package com.example.data.network

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.dataStore by preferencesDataStore(name = "janmitra_preferences")

object CryptoHelper {
    private const val ALIAS = "janmitra_secure_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }
    }

    init {
        try {
            if (!keyStore.containsAlias(ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    KEYSTORE_PROVIDER
                )
                keyGenerator.init(
                    KeyGenParameterSpec.Builder(
                        ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
                )
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            Log.e("CryptoHelper", "Key generation failed: ${e.message}")
        }
    }

    private fun getSecretKey(): SecretKey {
        return (keyStore.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    fun encrypt(text: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
            
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("CryptoHelper", "Encryption failed: ${e.message}")
            text
        }
    }

    fun decrypt(encryptedText: String): String? {
        return try {
            val combined = Base64.decode(encryptedText, Base64.DEFAULT)
            if (combined.size <= 12) return encryptedText // Fallback if plain text was already saved
            
            val iv = ByteArray(12)
            val encryptedBytes = ByteArray(combined.size - 12)
            System.arraycopy(combined, 0, iv, 0, 12)
            System.arraycopy(combined, 12, encryptedBytes, 0, encryptedBytes.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e("CryptoHelper", "Decryption failed: ${e.message}")
            encryptedText // Fallback to raw value
        }
    }
}

class SessionManager private constructor(private val context: Context) {

    companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_PHONE = "user_phone"
        const val KEY_USER_ROLE = "user_role"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_FCM_TOKEN = "fcm_token"
        const val KEY_USER_LANGUAGE = "user_language"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SessionManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private val tokenKey = stringPreferencesKey(KEY_ACCESS_TOKEN)
    private val refreshKey = stringPreferencesKey(KEY_REFRESH_TOKEN)
    private val emailKey = stringPreferencesKey(KEY_USER_EMAIL)
    private val nameKey = stringPreferencesKey(KEY_USER_NAME)
    private val phoneKey = stringPreferencesKey(KEY_USER_PHONE)
    private val roleKey = stringPreferencesKey(KEY_USER_ROLE)
    private val loggedInKey = booleanPreferencesKey(KEY_IS_LOGGED_IN)
    private val fcmKey = stringPreferencesKey(KEY_FCM_TOKEN)
    private val languageKey = stringPreferencesKey(KEY_USER_LANGUAGE)

    // Flow getters
    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[tokenKey]?.let { CryptoHelper.decrypt(it) }
    }
    val refreshTokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[refreshKey]?.let { CryptoHelper.decrypt(it) }
    }
    val userEmailFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[emailKey]?.let { CryptoHelper.decrypt(it) }
    }
    val userNameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[nameKey]?.let { CryptoHelper.decrypt(it) }
    }
    val userPhoneFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[phoneKey]?.let { CryptoHelper.decrypt(it) }
    }
    val userRoleFlow: Flow<String?> = context.dataStore.data.map { it[roleKey] }
    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { it[loggedInKey] ?: false }
    val fcmTokenFlow: Flow<String?> = context.dataStore.data.map { it[fcmKey] }
    val userLanguageFlow: Flow<String> = context.dataStore.data.map { it[languageKey] ?: "English" }

    // Suspending setters
    suspend fun setAccessToken(value: String?) {
        context.dataStore.edit { prefs ->
            if (value != null) {
                prefs[tokenKey] = CryptoHelper.encrypt(value)
            } else {
                prefs.remove(tokenKey)
            }
        }
    }

    suspend fun setRefreshToken(value: String?) {
        context.dataStore.edit { prefs ->
            if (value != null) {
                prefs[refreshKey] = CryptoHelper.encrypt(value)
            } else {
                prefs.remove(refreshKey)
            }
        }
    }

    suspend fun setUserEmail(value: String?) {
        context.dataStore.edit { prefs ->
            if (value != null) {
                prefs[emailKey] = CryptoHelper.encrypt(value)
            } else {
                prefs.remove(emailKey)
            }
        }
    }

    suspend fun setUserName(value: String?) {
        context.dataStore.edit { prefs ->
            if (value != null) {
                prefs[nameKey] = CryptoHelper.encrypt(value)
            } else {
                prefs.remove(nameKey)
            }
        }
    }

    suspend fun setUserPhone(value: String?) {
        context.dataStore.edit { prefs ->
            if (value != null) {
                prefs[phoneKey] = CryptoHelper.encrypt(value)
            } else {
                prefs.remove(phoneKey)
            }
        }
    }

    suspend fun setUserRole(value: String?) {
        context.dataStore.edit { prefs ->
            if (value != null) prefs[roleKey] = value else prefs.remove(roleKey)
        }
    }

    suspend fun setIsLoggedIn(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[loggedInKey] = value
        }
    }

    suspend fun setFcmToken(value: String?) {
        context.dataStore.edit { prefs ->
            if (value != null) prefs[fcmKey] = value else prefs.remove(fcmKey)
        }
    }

    suspend fun setUserLanguage(value: String?) {
        context.dataStore.edit { prefs ->
            if (value != null) prefs[languageKey] = value else prefs.remove(languageKey)
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
