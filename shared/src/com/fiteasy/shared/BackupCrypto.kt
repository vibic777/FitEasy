package com.fiteasy.shared

import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
    private val magic = "FITEASY1".toByteArray()
    fun encode(bytes: ByteArray, password: String): ByteArray {
        if (password.isEmpty()) return bytes
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val nonce = ByteArray(12).also(random::nextBytes)
        val header = magic + salt + nonce
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, nonce))
        cipher.updateAAD(header)
        return header + cipher.doFinal(bytes)
    }
    /** A copy protected with a password starts with the FitEasy mark; an open one is plain JSON. */
    fun isProtected(bytes: ByteArray) = bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(magic)
    fun decode(bytes: ByteArray, password: String): ByteArray {
        require(bytes.size <= 64 * 1024 * 1024) { "Backup too large" }
        if (!bytes.take(8).toByteArray().contentEquals(magic)) return bytes
        require(bytes.size >= 52 && password.isNotEmpty()) { "Password required" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(password, bytes.copyOfRange(8, 24)), GCMParameterSpec(128, bytes.copyOfRange(24, 36)))
        cipher.updateAAD(bytes.copyOfRange(0, 36))
        return cipher.doFinal(bytes, 36, bytes.size - 36)
    }
    private fun key(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, 600_000, 256)
        try { return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES") } finally { spec.clearPassword() }
    }
}
