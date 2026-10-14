package com.fiteasy.app

import android.app.Activity
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal

/** Adapted from Notio-Android BiometricAuth; system owns biometric data. See NOTICE.md. */
object BiometricAuth {
    fun available(activity:Activity)=runCatching {
        when {
            Build.VERSION.SDK_INT>=30 -> activity.getSystemService(BiometricManager::class.java).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)==BiometricManager.BIOMETRIC_SUCCESS
            Build.VERSION.SDK_INT==29 -> activity.getSystemService(BiometricManager::class.java).canAuthenticate()==BiometricManager.BIOMETRIC_SUCCESS
            else -> activity.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT)
        }
    }.getOrDefault(false)
    fun prompt(activity:Activity,onResult:(Boolean)->Unit) {
        var completed=false
        fun finish(okay:Boolean){if(!completed){completed=true;onResult(okay)}}
        val builder=BiometricPrompt.Builder(activity).setTitle("FitEasy").setNegativeButton("Cancel",activity.mainExecutor){_,_->finish(false)}
        if(Build.VERSION.SDK_INT>=30)builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        builder.build().authenticate(CancellationSignal(),activity.mainExecutor,object:BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult?){finish(true)}
            override fun onAuthenticationError(errorCode:Int,errString:CharSequence?){finish(false)}
        })
    }
}
