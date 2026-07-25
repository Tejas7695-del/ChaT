package com.secure.chat.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
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
        val secretKey = Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes)
        return Pair(roomId, secretKey)
    }

    fun generateSecretKey(): String {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256, SecureRandom())
        val secretKey = keyGen.generateKey()
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secretKey.encoded)
    }

    fun generateRoomId(): String {
        val allowedChars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        return (1..8)
            .map { allowedChars[random.nextInt(allowedChars.length)] }
            .joinToString("")
    }

    fun encrypt(plaintext: String, base64Key: String): String {
        val keyBytes = Base64.getUrlDecoder().decode(base64Key)
        val secretKey: SecretKey = SecretKeySpec(keyBytes, "AES")

        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

        val ciphertextBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val ivBase64 = Base64.getEncoder().encodeToString(iv)
        val ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertextBytes)

        return "$ivBase64:$ciphertextBase64"
    }

    fun decrypt(encryptedData: String, base64Key: String): String {
        val parts = encryptedData.split(":")
        if (parts.size != 2) return "[Decryption Error: Invalid Payload]"

        val iv = Base64.getDecoder().decode(parts[0])
        val ciphertextBytes = Base64.getDecoder().decode(parts[1])

        val keyBytes = Base64.getUrlDecoder().decode(base64Key)
        val secretKey: SecretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

        val decryptedBytes = cipher.doFinal(ciphertextBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
