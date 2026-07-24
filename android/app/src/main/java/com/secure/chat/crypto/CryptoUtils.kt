package com.secure.chat.crypto

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12

    fun generate6DigitCode(): String {
        val allowedChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = SecureRandom()
        return (1..6)
            .map { allowedChars[random.nextInt(allowedChars.length)] }
            .joinToString("")
    }

    fun deriveFromCode(code: String): Pair<String, String> {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(code.toByteArray(Charsets.UTF_8))
        val roomId = hashBytes.sliceArray(0 until 3).joinToString("") { "%02x".format(it) }
        val secretKey = Base64.encodeToString(hashBytes, Base64.URL_SAFE or Base64.NO_WRAP)
        return Pair(roomId, secretKey)
    }

    /**
     * Generates a random 256-bit AES secret key formatted in Base64
     */
    fun generateSecretKey(): String {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256, SecureRandom())
        val secretKey = keyGen.generateKey()
        return Base64.encodeToString(secretKey.encoded, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    /**
     * Generates a random 8-character human-readable Room ID
     */
    fun generateRoomId(): String {
        val allowedChars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        return (1..8)
            .map { allowedChars[random.nextInt(allowedChars.length)] }
            .joinToString("")
    }

    /**
     * Encrypts plaintext using AES-256-GCM and the secret key.
     * Returns a string formatted as "IV_BASE64:CIPHERTEXT_BASE64"
     */
    fun encrypt(plaintext: String, base64Key: String): String {
        val keyBytes = Base64.decode(base64Key, Base64.URL_SAFE or Base64.NO_WRAP)
        val secretKey: SecretKey = SecretKeySpec(keyBytes, "AES")

        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

        val ciphertextBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val ciphertextBase64 = Base64.encodeToString(ciphertextBytes, Base64.NO_WRAP)

        return "$ivBase64:$ciphertextBase64"
    }

    /**
     * Decrypts an encrypted payload ("IV_BASE64:CIPHERTEXT_BASE64") using the secret key.
     */
    fun decrypt(encryptedData: String, base64Key: String): String {
        val parts = encryptedData.split(":")
        if (parts.size != 2) return "[Decryption Error: Invalid Payload]"

        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val ciphertextBytes = Base64.decode(parts[1], Base64.NO_WRAP)

        val keyBytes = Base64.decode(base64Key, Base64.URL_SAFE or Base64.NO_WRAP)
        val secretKey: SecretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

        val decryptedBytes = cipher.doFinal(ciphertextBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Parses a Room Address (e.g. "room1234#key5678") into Pair(roomId, key)
     */
    fun parseRoomAddress(address: String): Pair<String, String>? {
        val parts = address.trim().split("#")
        return if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
            Pair(parts[0], parts[1])
        } else {
            null
        }
    }
}
