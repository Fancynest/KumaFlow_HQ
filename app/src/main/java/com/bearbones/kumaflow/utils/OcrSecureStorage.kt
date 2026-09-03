package com.bearbones.kumaflow.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class OcrSecureStorage(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            "ocr_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback in case of keystore migration/corruption
        context.getSharedPreferences("ocr_secure_prefs_fallback", Context.MODE_PRIVATE)
    }

    fun saveApiKey(key: String) {
        sharedPreferences.edit()
            .putString("anthropic_api_key", key.trim())
            .apply()
    }

    fun getApiKey(): String? {
        val key = sharedPreferences.getString("anthropic_api_key", null)
        return if (key.isNullOrBlank()) null else key.trim()
    }

    fun hasApiKey(): Boolean {
        return !getApiKey().isNullOrBlank()
    }

    fun deleteApiKey() {
        sharedPreferences.edit()
            .remove("anthropic_api_key")
            .apply()
    }
}
