package com.fiteasy.shared.lock

import com.fiteasy.shared.Platform
import com.fiteasy.shared.codec
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Serializable data class LockSettings(
    val enabled: Boolean = false,
    val salt: String = "", val hash: String = "",
    val question: String = "", val answerSalt: String = "", val answerHash: String = "",
    val totp: String = "", val smtp: SmtpSettings = SmtpSettings(),
    val failures: Int = 0, val blockedUntil: Long = 0,
    val freeAttempts: Int = 5, val delaySeconds: Int = 10,
    val maxAttempts: Int = 0, val wipeAfterLimit: Boolean = false,
    val recoveryOnly: Boolean = false,
    val biometric:Boolean=false,
    val pattern:Boolean=false,
    /** password, pin, login or pattern; older settings carry only the pattern flag. */
    val kind:String="password",
    /** Length of the PIN, kept so the pad can submit at the last digit; 0 for other ways and for PINs set before 0.4.19. */
    val pinLength:Int=0,
    /** Seconds the app may stay in the background before it asks again (D099): 0 at once, -1 only when the app starts. */
    val timeoutSeconds:Int=0,
)
const val LOCK_NEVER=-1
const val LOCK_MAX_SECONDS=24*3600-1
val lockTimeouts=listOf(0,30,60,300,900,LOCK_NEVER)
fun validLockTimeout(seconds:Int)=seconds==LOCK_NEVER || seconds in 0..LOCK_MAX_SECONDS
/** Whether coming back after [awayMillis] in the background asks for the secret. A clock moved back counts as a long absence. */
fun lockAfterBackground(timeoutSeconds:Int,awayMillis:Long):Boolean = when {
    timeoutSeconds==LOCK_NEVER->false
    timeoutSeconds<=0 || awayMillis<0->true
    else->awayMillis>=timeoutSeconds*1000L
}
val LockSettings.mode get()=if(pattern)"pattern" else kind
const val LOGIN_SEPARATOR="\u0000"
/** What the person typed for the chosen way to sign in, checked before hashing. Login and password travel as login␀password. */
fun validCredential(mode:String,value:String):Boolean = when(mode){
    "pin"->value.length in 4..12 && value.all {it in '0'..'9'}
    "login"->value.substringBefore(LOGIN_SEPARATOR,"").isNotBlank() && value.substringAfter(LOGIN_SEPARATOR,"").length>=4
    "pattern"->value.split(',').mapNotNull {it.toIntOrNull()}.let(CredentialHasher::isValidPattern)
    else->value.length>=4
}

/** Device-local configuration: never part of Snapshot, backups or network data. */
class DeviceLock(private val platform: Platform) {
    private val hasher = CredentialHasher()
    var settings = platform.readSecret("app-lock")?.let {codec.decodeFromString<LockSettings>(it)} ?: LockSettings()
        private set
    private var mailCode: String? = null
    private var mailDeadline = 0L
    private var mailAttempts = 0
    private var lastMail = 0L
    fun configure(value: LockSettings) { settings=value;platform.writeSecret("app-lock",codec.encodeToString(value)) }
    fun biometricAvailable()=platform.biometricAvailable()
    /** What the QR code of a TOTP key carries (D124): the authenticator app lists it as FitEasy with the name of this device. */
    fun totpUri(secret:String)=Totp.uri("FitEasy",platform.deviceName(),secret)
    fun biometric(onResult:(Boolean)->Unit) {
        require(settings.biometric && !settings.recoveryOnly && System.currentTimeMillis()>=settings.blockedUntil)
        platform.authenticateBiometric {okay->if(okay)configure(settings.copy(failures=0,blockedUntil=0));onResult(okay)}
    }
    fun password(value:String,pattern:Boolean=false,kind:String=if(pattern)"pattern" else "password"): LockSettings { require(kind in setOf("password","pin","login","pattern") && (kind=="pattern")==pattern && validCredential(kind,value)){"Invalid credential"};val h=if(pattern){val cells=value.split(',').map {it.toInt()};require(CredentialHasher.isValidPattern(cells));hasher.hashPattern(cells)}else hasher.hash(value.toCharArray());return settings.copy(enabled=true,salt=h.salt,hash=h.hash,failures=0,blockedUntil=0,recoveryOnly=false,pattern=pattern,kind=if(pattern)"password" else kind,pinLength=if(kind=="pin")value.length else 0) }
    fun question(question:String,answer:String):LockSettings {require(question.isNotBlank() && answer.isNotBlank());val h=hasher.hash(answer.trim().lowercase().toCharArray());return settings.copy(question=question,answerSalt=h.salt,answerHash=h.hash)}
    fun unlock(value:String,now:Long=System.currentTimeMillis()):Boolean {
        require(!settings.recoveryOnly) {"Требуется восстановление доступа"}
        require(now>=settings.blockedUntil) {"Повторите через ${(settings.blockedUntil-now+999)/1000} с"}
        val okay=if(settings.pattern){val cells=value.split(',').mapNotNull {it.toIntOrNull()};CredentialHasher.isValidPattern(cells) && hasher.verifyPattern(cells,settings.salt,settings.hash)}else hasher.verify(value.toCharArray(),settings.salt,settings.hash)
        if(okay){configure(settings.copy(failures=0,blockedUntil=0));return true}
        failed(now);return false
    }
    private fun failed(now:Long) {
        val failures=(settings.failures+1).coerceAtMost(1000)
        val extra=(failures-settings.freeAttempts).coerceAtLeast(0)
        val delay=if(extra==0)0L else (settings.delaySeconds.toLong()*1000*(1L shl (extra-1).coerceAtMost(20))).coerceAtMost(900_000)
        val limit=settings.maxAttempts>0 && failures>=settings.maxAttempts
        configure(settings.copy(failures=failures,blockedUntil=now+delay,recoveryOnly=limit))
        if(limit && settings.wipeAfterLimit){platform.wipeLocal();configure(LockSettings());error("Локальные данные удалены после настроенного лимита ошибок. Внешние копии и другие устройства сохранены.")}
    }
    fun recover(kind:String,value:String,now:Long=System.currentTimeMillis()):Boolean {
        require(now>=settings.blockedUntil) {"Подождите до следующей попытки"}
        val okay=when(kind){
            "question" -> settings.answerHash.isNotBlank() && hasher.verify(value.trim().lowercase().toCharArray(),settings.answerSalt,settings.answerHash)
            "totp" -> settings.totp.isNotBlank() && Totp.verify(settings.totp,value,now)
            "email" -> mailCode!=null && now<=mailDeadline && mailAttempts++<5 && java.security.MessageDigest.isEqual(value.toByteArray(),mailCode!!.toByteArray())
            else -> false
        }
        if(okay){mailCode=null;configure(settings.copy(enabled=false,failures=0,blockedUntil=0,recoveryOnly=false))}else failed(now)
        return okay
    }
    suspend fun sendRecovery(): MailResult {
        val now=System.currentTimeMillis();require(now-lastMail>=60_000) {"Повторная отправка доступна через минуту"}
        require(settings.smtp.isComplete) {"Настройте SMTP"}
        lastMail=now;mailCode=null
        val code=java.security.SecureRandom().nextInt(1_000_000).toString().padStart(6,'0')
        val result=SmtpClient().send(settings.smtp,"FitEasy — восстановление доступа","Код восстановления: $code\nДействует 10 минут.")
        if(result is MailResult.Sent){mailCode=code;mailDeadline=System.currentTimeMillis()+600_000;mailAttempts=0}
        return result
    }
}
