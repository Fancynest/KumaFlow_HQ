package com.bearbones.kumaflow.duo

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class DuoSecureStorage(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "duo_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun savePairingSecret(pairingId: String, secret: String) {
        sharedPreferences.edit()
            .putString("secret_$pairingId", secret)
            .apply()
    }

    fun getPairingSecret(pairingId: String): String? {
        return sharedPreferences.getString("secret_$pairingId", null)
    }

    fun deletePairingSecret(pairingId: String) {
        sharedPreferences.edit()
            .remove("secret_$pairingId")
            .apply()
    }
}
