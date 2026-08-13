package com.bearbones.kumaflow.duo

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec

object DuoCrypto {
    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val HMAC_ALGO = "HmacSHA256"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12

    fun generatePairingSecret(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32) // 256 bits
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun getSecretKeySpec(secretBase64: String): SecretKeySpec {
        val keyBytes = Base64.decode(secretBase64, Base64.NO_WRAP)
        // Ensure 256-bit key by hashing it if it's not exactly 32 bytes
        val digest = MessageDigest.getInstance("SHA-256")
        val finalKey = digest.digest(keyBytes)
        return SecretKeySpec(finalKey, "AES")
    }

    fun encrypt(plaintext: String, secretBase64: String): String {
        val keySpec = getSecretKeySpec(secretBase64)
        val cipher = Cipher.getInstance(AES_MODE)
        
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        
        // Combine IV and encrypted bytes
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
        
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encryptedBase64: String, secretBase64: String): String {
        val keySpec = getSecretKeySpec(secretBase64)
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        
        if (combined.size < IV_LENGTH) {
            throw IllegalArgumentException("Invalid encrypted payload size")
        }
        
        val iv = ByteArray(IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH)
        
        val encryptedBytes = ByteArray(combined.size - IV_LENGTH)
        System.arraycopy(combined, IV_LENGTH, encryptedBytes, 0, encryptedBytes.size)
        
        val cipher = Cipher.getInstance(AES_MODE)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun generateHmacSignature(payload: String, secretBase64: String): String {
        val keyBytes = Base64.decode(secretBase64, Base64.NO_WRAP)
        val mac = Mac.getInstance(HMAC_ALGO)
        val keySpec = SecretKeySpec(keyBytes, HMAC_ALGO)
        mac.init(keySpec)
        
        val signatureBytes = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
    }

    fun verifyHmacSignature(payload: String, signatureBase64: String, secretBase64: String): Boolean {
        val expectedSignature = generateHmacSignature(payload, secretBase64)
        // Time-constant comparison to prevent timing attacks
        return MessageDigest.isEqual(
            expectedSignature.toByteArray(Charsets.UTF_8),
            signatureBase64.toByteArray(Charsets.UTF_8)
        )
    }
}
