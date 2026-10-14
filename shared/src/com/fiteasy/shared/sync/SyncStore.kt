package com.fiteasy.shared.sync

import com.fiteasy.shared.Platform
import com.fiteasy.shared.deviceIdentifier
import java.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Which way data goes, seen from the device that starts the sync. */
enum class SyncDirection { SEND, RECEIVE, BOTH }

/**
 * A device this one is paired with, kept until the person removes it (adapted from Notio's SyncStore, D105). [secret] is
 * the pairing key; the whole store is one device secret, sealed by Android Keystore or Windows DPAPI, and is never part
 * of a snapshot, a backup or a transfer. The last choices of the sync dialog are kept per pairing.
 */
@Serializable
data class Pairing(
    val id: String,
    val peerDeviceId: String,
    val peerName: String,
    val secret: String,
    val createdAt: Long,
    val lastSyncAt: Long = 0,
    val lastResult: String? = null,
    val lastHost: String? = null,
    val lastPort: Int = 0,
    val direction: SyncDirection = SyncDirection.BOTH,
    /** `ImportMode` name: MERGE, COPIES or REPLACE. */
    val mode: String = "MERGE",
    val allFolders: Boolean = true,
    /** The sections chosen last time when not all of them. */
    val folderIds: List<String> = emptyList(),
    /**
     * Whether Synchronize asks for the direction, the way and the sections each time (D109). When one is not asked, the value
     * kept above is used as set beforehand; when none is asked, Synchronize starts at once.
     */
    val askDirection: Boolean = true,
    val askMode: Boolean = true,
    val askFolders: Boolean = true,
)

/**
 * The owner's rule (D111, D116): with direction, way and sections all set beforehand a sync starts at once and the other
 * device accepts it without a question; with any one of the three still chosen at the sync, the other device asks.
 */
val Pairing.allSetBeforehand:Boolean get()=!askDirection && !askMode && !askFolders

/** The pairings, this device's name and the sync log. A restored or wiped device is paired again. */
class SyncStore(private val platform: Platform) {
    @Serializable
    private data class Stored(val deviceName: String? = null, val port: Int = 0, val pairings: List<Pairing> = emptyList(), val log: List<String> = emptyList())

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private var stored: Stored = runCatching { platform.readSecret(SECRET)?.let { json.decodeFromString(Stored.serializer(), it) } }.getOrNull() ?: Stored()

    private val _pairings = MutableStateFlow(stored.pairings)
    val pairings: StateFlow<List<Pairing>> = _pairings.asStateFlow()

    private val _log = MutableStateFlow(stored.log)
    val log: StateFlow<List<String>> = _log.asStateFlow()

    @Synchronized
    private fun write(change: (Stored) -> Stored) {
        stored = change(stored)
        // The lists on the screen follow at once; a failed write of the secret must not break a running sync.
        _pairings.value = stored.pairings
        _log.value = stored.log
        runCatching { platform.writeSecret(SECRET, json.encodeToString(Stored.serializer(), stored)) }
    }

    /** A random id of this installation; it tells paired devices apart, it names nobody. */
    val deviceId: String by lazy { deviceIdentifier(platform) }

    var deviceName: String
        get() = stored.deviceName?.takeIf { it.isNotBlank() } ?: platform.deviceName().trim().take(40).ifBlank { "FitEasy" }
        set(value) = write { it.copy(deviceName = value.trim().take(40)) }

    /** The port this device waits on (D132): 0 = automatic (the usual one, or a free one when it is taken), else the one set by hand. */
    var port: Int
        get() = stored.port.takeIf { SyncEngine.validPort(it) } ?: 0
        set(value) = write { it.copy(port = if (SyncEngine.validPort(value)) value else 0) }

    fun get(id: String): Pairing? = _pairings.value.firstOrNull { it.id == id }

    /** The pairing key (32 bytes), or `null` when it is damaged: pair again then. */
    fun secretOf(pairing: Pairing): ByteArray? = runCatching { Base64.getDecoder().decode(pairing.secret) }.getOrNull()?.takeIf { it.size == 32 }

    @Synchronized
    fun add(id: String, peerDeviceId: String, peerName: String, secret: ByteArray, now: Long) {
        // Pairing the same device again replaces the old pairing with it.
        write { s -> s.copy(pairings = s.pairings.filter { it.peerDeviceId != peerDeviceId && it.id != id } + Pairing(id, peerDeviceId, peerName, Base64.getEncoder().encodeToString(secret), now)) }
    }

    @Synchronized
    fun update(id: String, change: (Pairing) -> Pairing) = write { s -> s.copy(pairings = s.pairings.map { if (it.id == id) change(it) else it }) }

    @Synchronized
    fun remove(id: String) = write { s -> s.copy(pairings = s.pairings.filter { it.id != id }) }

    /** One line of the sync log (never a secret or a code); the newest [LOG_LINES] are kept. */
    @Synchronized
    fun log(line: String, now: Long = System.currentTimeMillis()) {
        val stamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date(now))
        write { s -> s.copy(log = (listOf("$stamp  $line") + s.log).take(LOG_LINES)) }
    }

    @Synchronized
    fun clearLog() = write { it.copy(log = emptyList()) }

    private companion object {
        const val LOG_LINES = 200
        const val SECRET = "sync-store"
    }
}

/** An address of a paired device typed by hand: only where to knock changes, the pairing key stays (after Notio's SyncAddress). */
object SyncAddress {
    /** The host as typed, or `null` when it is not an IPv4 address or a plain host name. */
    fun host(text: String): String? {
        val h = text.trim()
        if (h.isEmpty() || h.length > 253) return null
        val parts = h.split('.')
        if (parts.size == 4 && parts.all { p -> p.isNotEmpty() && p.all(Char::isDigit) }) return h.takeIf { parts.all { p -> p.length <= 3 && p.toInt() in 0..255 } }
        val nameOk = parts.all { p -> p.isNotEmpty() && p.length <= 63 && p.all { it.isLetterOrDigit() || it == '-' } && !p.startsWith('-') && !p.endsWith('-') }
        return h.takeIf { nameOk && h.any { it.isLetter() } }
    }

    /** The port as typed, or `null` when it is not 1 - 65535. */
    fun port(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 1..65535 }
}
