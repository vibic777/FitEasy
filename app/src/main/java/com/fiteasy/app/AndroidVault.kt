package com.fiteasy.app

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec

object AndroidVault {
    // The key handle is looked up once per process, not on every sealed value (D115).
    private var held:SecretKey?=null
    @Synchronized private fun key():SecretKey {
        held?.let {return it}
        val store=KeyStore.getInstance("AndroidKeyStore").apply {load(null)}
        (store.getKey("fiteasy-secrets",null) as? SecretKey)?.let {held=it;return it}
        return KeyGenerator.getInstance("AES","AndroidKeyStore").apply {init(KeyGenParameterSpec.Builder("fiteasy-secrets",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())}.generateKey().also {held=it}
    }
    fun seal(text:String):String {val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());return Base64.getEncoder().encodeToString(c.iv+c.doFinal(text.toByteArray()))}
    fun unseal(text:String):String {val bytes=Base64.getDecoder().decode(text);require(bytes.size>=28);val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes,0,12));return String(c.doFinal(bytes,12,bytes.size-12))}
}
