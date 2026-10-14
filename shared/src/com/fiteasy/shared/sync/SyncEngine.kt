// Adapted from Notio-Windows shared/sync/SyncEngine.kt (MIT, Copyright (c) 2026 vibic777); see NOTICE.md. FitEasy moves
// its sections instead of folders, limits a transfer to 64 MiB and holds the Android multicast lock while it listens (D105).
package com.fiteasy.shared.sync

import com.fiteasy.shared.ImportMode
import java.io.File
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** What sync needs from the app's data: the sections, a transfer stream of some sections, an import of such a stream. */
interface SyncData {
    suspend fun folders(): List<FolderInfo>
    suspend fun export(out: OutputStream, folderIds: Set<String>?)
    /** Imports [file]; returns the counts: 0, 0, records added, records updated, records left as they were, 0. */
    suspend fun import(file: File, mode: ImportMode): List<Int>
}

/** A question to the person on this device, answered on the Sync page (D226). */
sealed interface SyncPrompt {
    val answer: CompletableDeferred<Boolean>

    /** Another device typed this device's code: pair with it? */
    data class Pair(val peerName: String, override val answer: CompletableDeferred<Boolean>) : SyncPrompt

    /**
     * A paired device wants to sync. [direction] is seen from HERE: SEND = this device sends, RECEIVE = this device receives.
     * [clockOffset]: the other device's clock minus this one's (ms), shown when it is large (merge goes by time).
     */
    data class Request(
        val peerName: String,
        val direction: SyncDirection,
        val mode: ImportMode,
        val folderNames: List<String>?,
        val clockOffset: Long,
        override val answer: CompletableDeferred<Boolean>,
    ) : SyncPrompt
}

/** The result of a sync started here: what this device received and what the other one reported. */
data class SyncOutcome(val received: List<Int>?, val sent: List<Int>?, val bytesOut: Long, val bytesIn: Long, val clockOffset: Long)

/** One step of Test connection (D226). */
data class SyncCheck(val step: String, val ok: Boolean, val detail: String)

/**
 * Sync between two Notio devices on the same local network (D226). Nothing runs unless the Sync page is open: [startListening]
 * opens a TCP port and announces the device; [stopListening] closes both. Pairing: one device waits with a code
 * ([startPairing]), the other connects with that code ([pair]); both keep the pairing, later syncs reuse it ([sync]).
 * Every connection is encrypted and authenticated ([SyncSession]); every change of data on a device is asked there first.
 */
class SyncEngine(
    private val store: SyncStore,
    private val discovery: Discovery?,
    private val data: SyncData,
    private val tempDir: File,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
    /** Android needs a multicast lock to hear announcements; told true while listening. */
    private val multicast: (Boolean) -> Unit = {},
) {
    data class Listening(val port: Int, val addresses: List<String>)

    private val _listening = MutableStateFlow<Listening?>(null)
    val listening: StateFlow<Listening?> = _listening.asStateFlow()

    private val _code = MutableStateFlow<String?>(null)
    /** The pairing code shown while this device waits to be paired; `null` when it does not wait. */
    val pairingCode: StateFlow<String?> = _code.asStateFlow()

    private val _prompt = MutableStateFlow<SyncPrompt?>(null)
    val prompt: StateFlow<SyncPrompt?> = _prompt.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    /** What an incoming connection is doing now (a key of [SyncStatus]), for the page. */
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _nearby = MutableStateFlow<List<FoundDevice>>(emptyList())
    val nearby: StateFlow<List<FoundDevice>> = _nearby.asStateFlow()

    /** A pairing made by the other side while this one waited (for the page to say "paired with …"). */
    private val _pairedEvent = MutableStateFlow<String?>(null)
    val pairedEvent: StateFlow<String?> = _pairedEvent.asStateFlow()

    private val _portBusy = MutableStateFlow<Int?>(null)
    /**
     * The port found taken by another program when listening last started (D132), or `null`. With the port chosen automatically
     * the device then listens on a free one; with a port set by hand it does not listen at all, and the page says so.
     */
    val portBusy: StateFlow<Int?> = _portBusy.asStateFlow()

    @Volatile private var server: ServerSocket? = null
    private var serverJob: Job? = null
    private var discoveryJob: Job? = null
    private var unadvertise: () -> Unit = {}
    private var wrongCodes = 0
    private val clientLock = Mutex()

    // ---- listening (only while the Sync page is open) ----

    /**
     * The mDNS work of this computer (JmDNS: making the responders, announcing, withdrawing, closing) blocks for seconds; it runs
     * here, one step after the other in the order asked, never on the UI thread (the desktop's change to the phone's engine, whose
     * NsdManager does not block: the Sync page froze while it opened, closed and started a pairing).
     */
    private val mdns = Dispatchers.IO.limitedParallelism(1)

    @Synchronized
    fun startListening() {
        if (server != null) return
        // The port set by hand is the only one tried: the person opened exactly it in the firewall (D132).
        val manual = store.port
        val wanted = if (manual > 0) manual else DEFAULT_PORT
        val first = runCatching { ServerSocket().let { s -> try { s.reuseAddress = true; s.bind(InetSocketAddress(wanted)); s } catch (e: Exception) { runCatching { s.close() }; throw e } } }
        _portBusy.value = if (first.isFailure) wanted else null
        if (first.isFailure) store.log("listening: port $wanted is taken by another program" + if (manual > 0) "" else ", a free one is used")
        val socket = first.getOrNull() ?: (if (manual > 0) null else runCatching { ServerSocket(0) }.getOrNull()) ?: return
        server = socket
        _listening.value = Listening(socket.localPort, emptyList())
        scope.launch(mdns) { runCatching { multicast(true) } }
        // The addresses of this computer are read off the UI thread too (the network adapters can take a moment).
        scope.launch(mdns) {
            val addresses = discovery?.ownAddresses().orEmpty()
            if (server === socket) _listening.value = Listening(socket.localPort, addresses)
        }
        advertise()
        serverJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                // Only the local network (D231): on mobile data the phone may have a public IPv6 address.
                if (!isLocal(client.inetAddress)) {
                    store.log("incoming: refused " + client.inetAddress.hostAddress + " (not on the local network)")
                    runCatching { client.close() }
                    continue
                }
                runCatching { handle(client) }.onFailure { e -> store.log("incoming: " + describe(e)) }
                runCatching { client.close() }
                _status.value = null
            }
        }
        discoveryJob = discovery?.let { d -> scope.launch(mdns) { d.devices(store.deviceId).collect { _nearby.value = it } } }
    }

    /** Leaving the Sync page: the port closes, the announcement is withdrawn, the discovery stops. */
    @Synchronized
    fun stopListening() {
        runCatching { server?.close() }
        server = null
        serverJob?.cancel(); serverJob = null
        discoveryJob?.cancel(); discoveryJob = null
        scope.launch(mdns) {
            unadvertise(); unadvertise = {}
            discovery?.close()
            runCatching { multicast(false) }
        }
        _listening.value = null
        _nearby.value = emptyList()
        _code.value = null
        _prompt.value?.answer?.complete(false)
        _prompt.value = null
    }

    /** Whether the port is open now (the Sync page is on the screen). */
    val isListening: Boolean get() = server != null

    private fun advertise() {
        val port = server?.localPort
        val waiting = _code.value != null
        val id = store.deviceId
        val name = store.deviceName
        scope.launch(mdns) {
            unadvertise(); unadvertise = {}
            if (port != null && server?.localPort == port) unadvertise = discovery?.advertise(port, id, name, waiting) ?: {}
        }
    }

    fun startPairing() {
        wrongCodes = 0
        _pairedEvent.value = null
        _code.value = SyncCrypto.newCode()
        advertise()
    }

    fun stopPairing() {
        _code.value = null
        advertise()
    }

    fun clearPairedEvent() { _pairedEvent.value = null }

    private fun hello(purpose: String, pairingId: String? = null) = Hello(purpose = purpose, deviceId = store.deviceId, name = store.deviceName, pairingId = pairingId)

    private suspend fun ask(prompt: SyncPrompt): Boolean {
        _prompt.value = prompt
        val ok = withTimeoutOrNull(PROMPT_MILLIS) { prompt.answer.await() } ?: false
        if (_prompt.value === prompt) _prompt.value = null
        return ok
    }

    // ---- the answering side ----

    private suspend fun handle(socket: Socket) {
        // Connections are answered one at a time: one that says nothing must not hold the port for long (the code check of 2026-10-01).
        socket.soTimeout = HANDSHAKE_TIMEOUT
        var pairing: Pairing? = null
        val session = try {
            SyncSession.accept(socket.getInputStream(), socket.getOutputStream(), hello("sync"), keyFor = { h, salt ->
                runCatching {
                    when (h.purpose) {
                        "pair" -> SyncCrypto.stretchCode(_code.value ?: throw SyncException(SyncReasons.NOT_WAITING), salt)
                        else -> {
                            val p = store.pairings.value.firstOrNull { it.id == h.pairingId && it.peerDeviceId == h.deviceId }
                                ?: throw SyncException(SyncReasons.UNKNOWN_PAIRING)
                            pairing = p
                            store.secretOf(p) ?: throw SyncException(SyncReasons.UNKNOWN_PAIRING)
                        }
                    }
                }
            }, now = now)
        } catch (e: SyncException) {
            if (e.reason == SyncReasons.WRONG_CODE && _code.value != null && ++wrongCodes >= 3) {
                // Three wrong codes: a new one, so guessing gains nothing.
                store.log("pairing: 3 wrong codes, a new code was made")
                startPairing()
            }
            throw e
        }
        socket.soTimeout = IO_TIMEOUT
        if (session.peer.purpose == "pair") return answerPairing(session)
        val p = pairing ?: return
        store.update(p.id) { it.copy(lastHost = socket.inetAddress.hostAddress, peerName = session.peer.name.ifBlank { it.peerName }) }
        while (true) {
            val message = try { session.receive() } catch (e: SyncException) { return }
            when (message.t) {
                "ping" -> session.send(SyncMessage("pong", time = now()))
                "list-folders" -> session.send(SyncMessage("folders", folderList = data.folders()))
                "unpair" -> {
                    store.remove(p.id)
                    store.log("${p.peerName}: removed the pairing")
                    session.send(SyncMessage("ok"))
                    return
                }
                "request" -> answerRequest(session, p, message)
                "bye" -> return
                else -> session.send(SyncMessage("error", reason = SyncReasons.BROKEN))
            }
        }
    }

    private suspend fun answerPairing(session: SyncSession) {
        val name = session.peer.name.ifBlank { "?" }
        _status.value = SyncStatus.PAIRING
        if (!ask(SyncPrompt.Pair(name, CompletableDeferred()))) {
            session.send(SyncMessage("error", reason = SyncReasons.DENIED))
            store.log("pairing with $name: refused here")
            return
        }
        val id = UUID.randomUUID().toString()
        val secret = SyncCrypto.randomBytes(32)
        session.send(SyncMessage("pair-ok", pairingId = id, secret = java.util.Base64.getEncoder().encodeToString(secret)))
        store.add(id, session.peer.deviceId, name, secret, now())
        store.log("paired with $name")
        _code.value = null
        advertise()
        _pairedEvent.value = name
    }

    private suspend fun answerRequest(session: SyncSession, p: Pairing, message: SyncMessage) {
        val theirDirection = runCatching { SyncDirection.valueOf(message.direction.orEmpty()) }.getOrNull()
        val mode = runCatching { ImportMode.valueOf(message.mode.orEmpty()) }.getOrNull()
        if (theirDirection == null || mode == null || !allowed(theirDirection, mode, message.folders)) {
            session.send(SyncMessage("decision", ok = false, reason = SyncReasons.BROKEN)); return
        }
        val here = when (theirDirection) { SyncDirection.SEND -> SyncDirection.RECEIVE; SyncDirection.RECEIVE -> SyncDirection.SEND; SyncDirection.BOTH -> SyncDirection.BOTH }
        val ids = message.folders?.toSet()
        val names = ids?.let { set -> data.folders().filter { it.id in set }.map { it.name } }
        // The owner's rule (D111, made the only one by D116): no question here only when the device that starts says all
        // three choices of the connection were set beforehand; while one of them is still chosen at the sync, this
        // device always asks — whatever is set for the connection here.
        val allowedBeforehand = message.preset == true
        if (allowedBeforehand) store.log("${p.peerName}: not asked, the choices of the connection were set beforehand")
        val ok = allowedBeforehand || ask(SyncPrompt.Request(p.peerName, here, mode, names, session.clockOffset, CompletableDeferred()))
        session.send(SyncMessage("decision", ok = ok, reason = if (ok) null else SyncReasons.DENIED))
        if (!ok) { store.log("${p.peerName}: sync refused here"); return }
        store.log("${p.peerName}: sync started (${here.name.lowercase()} here, ${mode.name.lowercase()})")
        var received: List<Int>? = null
        if (theirDirection != SyncDirection.RECEIVE) {
            _status.value = SyncStatus.RECEIVING
            received = receiveAndImport(session, mode)
            session.send(SyncMessage("result", ok = true, text = received.joinToString(",")))
        }
        if (theirDirection != SyncDirection.SEND) {
            _status.value = SyncStatus.SENDING
            session.sendData { out -> data.export(out, ids) }
            session.receive() // their result
        }
        store.update(p.id) { it.copy(lastSyncAt = now(), lastResult = received?.joinToString(",")) }
        store.log("${p.peerName}: sync done")
    }

    private suspend fun receiveAndImport(session: SyncSession, mode: ImportMode): List<Int> {
        tempDir.mkdirs()
        val file = File(tempDir, "incoming-" + UUID.randomUUID() + ".zip")
        try {
            session.receiveData(file, MAX_TRANSFER)
            return data.import(file, mode)
        } finally {
            file.delete()
        }
    }

    // ---- the connecting side ----

    /** Where [pairing]'s device is now: found on the network by its id, else the address it had last time. */
    fun locate(pairing: Pairing): Pair<String, Int>? {
        _nearby.value.firstOrNull { it.deviceId == pairing.peerDeviceId }?.let { return it.host to it.port }
        return pairing.lastHost?.let { it to (pairing.lastPort.takeIf { p -> p > 0 } ?: DEFAULT_PORT) }
    }

    private fun open(host: String, port: Int): Socket = Socket().apply {
        connect(InetSocketAddress(host, port), CONNECT_TIMEOUT)
        soTimeout = IO_TIMEOUT
    }

    /** Client role of pairing: connects to [host]:[port] with the [code] shown there; the pairing is kept on both sides. */
    suspend fun pair(host: String, port: Int, code: String): Result<Pairing> = clientLock.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                open(host, port).use { socket ->
                    val session = SyncSession.connect(socket.getInputStream(), socket.getOutputStream(), hello("pair"), { salt -> SyncCrypto.stretchCode(code, salt) }, now)
                    socket.soTimeout = (PROMPT_MILLIS + IO_TIMEOUT).toInt() // the person there is asked first
                    val answer = session.receive()
                    if (answer.t != "pair-ok") throw SyncException(answer.reason ?: SyncReasons.BROKEN)
                    val id = answer.pairingId ?: throw SyncException(SyncReasons.BROKEN)
                    val secret = java.util.Base64.getDecoder().decode(answer.secret ?: throw SyncException(SyncReasons.BROKEN))
                    store.add(id, session.peer.deviceId, session.peer.name.ifBlank { host }, secret, now())
                    store.update(id) { it.copy(lastHost = host, lastPort = port) }
                    store.log("paired with ${session.peer.name}")
                    store.get(id) ?: throw SyncException(SyncReasons.BROKEN)
                }
            }.onFailure { store.log("pairing with $host: " + describe(it)) }
        }
    }

    private fun connectPaired(pairing: Pairing): Pair<Socket, SyncSession> {
        val (host, port) = locate(pairing) ?: throw SyncException(NOT_FOUND)
        val socket = try { open(host, port) } catch (e: java.io.IOException) { throw SyncException(NOT_FOUND, e.message ?: NOT_FOUND, e) }
        try {
            val secret = store.secretOf(pairing) ?: throw SyncException(SyncReasons.UNKNOWN_PAIRING)
            val session = SyncSession.connect(socket.getInputStream(), socket.getOutputStream(), hello("sync", pairing.id), { secret }, now)
            store.update(pairing.id) { it.copy(lastHost = host, lastPort = port, peerName = session.peer.name.ifBlank { it.peerName }) }
            return socket to session
        } catch (e: Exception) {
            runCatching { socket.close() }
            throw e
        }
    }

    /**
     * Syncs with a paired device: [direction] seen from here, [mode] as in an import, [folderIds] `null` = all folders.
     * The other device asks its person first. The choices are remembered in the pairing.
     */
    suspend fun sync(pairingId: String, direction: SyncDirection, mode: ImportMode, folderIds: Set<String>?, preset: Boolean = false): Result<SyncOutcome> = clientLock.withLock {
        withContext(Dispatchers.IO) {
            val pairing = store.get(pairingId)
            runCatching {
                pairing ?: throw SyncException(SyncReasons.UNKNOWN_PAIRING)
                if (!allowed(direction, mode, folderIds?.toList())) throw SyncException(SyncReasons.BROKEN, "replace needs one direction")
                store.update(pairingId) { it.copy(direction = direction, mode = mode.name, allFolders = folderIds == null, folderIds = folderIds?.toList() ?: it.folderIds) }
                val (socket, session) = connectPaired(pairing)
                socket.use {
                    socket.soTimeout = (PROMPT_MILLIS + IO_TIMEOUT).toInt()
                    session.send(SyncMessage("request", direction = direction.name, mode = mode.name, folders = folderIds?.toList(), preset = if (preset) true else null))
                    val decision = session.receive()
                    if (decision.ok != true) throw SyncException(decision.reason ?: SyncReasons.DENIED)
                    socket.soTimeout = IO_TIMEOUT
                    store.log("${pairing.peerName}: sync started (${direction.name.lowercase()}, ${mode.name.lowercase()})")
                    var sent: List<Int>? = null
                    var received: List<Int>? = null
                    var bytesOut = 0L
                    var bytesIn = 0L
                    if (direction != SyncDirection.RECEIVE) {
                        bytesOut = session.sendData { out -> data.export(out, folderIds) }
                        sent = session.receive().text?.split(",")?.mapNotNull { it.toIntOrNull() }
                    }
                    if (direction != SyncDirection.SEND) {
                        tempDir.mkdirs()
                        val file = File(tempDir, "incoming-" + UUID.randomUUID() + ".zip")
                        try {
                            bytesIn = session.receiveData(file, MAX_TRANSFER)
                            received = data.import(file, mode)
                        } finally {
                            file.delete()
                        }
                        session.send(SyncMessage("result", ok = true, text = received.joinToString(",")))
                    }
                    runCatching { session.send(SyncMessage("bye")) }
                    store.update(pairingId) { it.copy(lastSyncAt = now(), lastResult = (received ?: sent)?.joinToString(",")) }
                    store.log("${pairing.peerName}: sync done")
                    SyncOutcome(received, sent, bytesOut, bytesIn, session.clockOffset)
                }
            }.onFailure { store.log((pairing?.peerName ?: "?") + ": sync failed: " + describe(it)) }
        }
    }

    /** Removes the pairing here and, when the other device is reachable now, there too. */
    suspend fun unpair(pairingId: String): Boolean = clientLock.withLock {
        withContext(Dispatchers.IO) {
            val pairing = store.get(pairingId) ?: return@withContext false
            val told = runCatching {
                val (socket, session) = connectPaired(pairing)
                socket.use { session.send(SyncMessage("unpair")); session.receive() }
            }.isSuccess
            store.remove(pairingId)
            store.log("${pairing.peerName}: pairing removed" + if (told) " on both devices" else " here (the other device was not reachable)")
            told
        }
    }

    /** Test connection (D226): each step with its result; nothing is transferred. */
    suspend fun test(pairingId: String): List<SyncCheck> = clientLock.withLock {
        withContext(Dispatchers.IO) {
            val out = ArrayList<SyncCheck>()
            val pairing = store.get(pairingId) ?: return@withContext listOf(SyncCheck("pairing", false, "not found"))
            val local = discovery?.onLocalNetwork() ?: true
            val addresses = discovery?.ownAddresses().orEmpty()
            out += SyncCheck("network", local, if (local) addresses.joinToString(", ").ifBlank { "?" } else "no Wi-Fi / Ethernet")
            out += SyncCheck("listening", _listening.value != null, _listening.value?.let { "port " + it.port } ?: "off")
            val seen = _nearby.value.firstOrNull { it.deviceId == pairing.peerDeviceId }
            val target = locate(pairing)
            out += SyncCheck("find", target != null, when {
                seen != null -> "found: ${seen.host}:${seen.port}"
                target != null -> "not announced now; last address ${target.first}:${target.second}"
                else -> "not found (open Settings -> Sync on the other device)"
            })
            if (target == null) return@withContext out
            val started = System.nanoTime()
            val socket = try { open(target.first, target.second) } catch (e: Exception) {
                out += SyncCheck("connect", false, describe(e)); return@withContext out
            }
            out += SyncCheck("connect", true, "${(System.nanoTime() - started) / 1_000_000} ms")
            socket.use {
                val session = try {
                    val secret = store.secretOf(pairing) ?: throw SyncException(SyncReasons.UNKNOWN_PAIRING)
                    SyncSession.connect(socket.getInputStream(), socket.getOutputStream(), hello("sync", pairing.id), { secret }, now)
                } catch (e: Exception) {
                    out += SyncCheck("handshake", false, describe(e)); return@withContext out
                }
                out += SyncCheck("handshake", true, "AES-256-GCM, ECDH P-256; ${session.peer.name}")
                val offset = session.clockOffset
                out += SyncCheck("clock", kotlin.math.abs(offset) < CLOCK_TOLERANCE, "${offset / 1000} s")
                val pingAt = System.nanoTime()
                val reply = runCatching { session.send(SyncMessage("ping")); session.receive() }
                out += SyncCheck("reply", reply.isSuccess, reply.fold({ "${(System.nanoTime() - pingAt) / 1_000_000} ms" }, { describe(it) }))
                runCatching { session.send(SyncMessage("bye")) }
            }
            store.log("${pairing.peerName}: test " + if (out.all { it.ok }) "ok" else "failed at " + out.first { !it.ok }.step)
            out
        }
    }

    companion object {
        const val DEFAULT_PORT = 47821
        const val NOT_FOUND = "not-found"
        private const val CONNECT_TIMEOUT = 5_000
        /** Long enough for the other side to import a big backup before it answers. */
        private const val IO_TIMEOUT = 300_000
        /** The hello and the proofs: both sides send them at once, without asking anybody. */
        private const val HANDSHAKE_TIMEOUT = 15_000
        private const val PROMPT_MILLIS = 90_000L
        /** Merge compares times of change: clocks further apart than this are shown as a problem. */
        const val CLOCK_TOLERANCE = 120_000L
        /** The largest transfer accepted, as for a backup file. */
        const val MAX_TRANSFER = 64L * 1024 * 1024

        /**
         * Replace wipes the chosen sections of the receiving side, so only in one direction; an empty choice moves nothing.
         * Chosen sections may be replaced on their own (D113): a device before 0.4.29 refuses such a request.
         */
        /** A port a person may set by hand: above the ones the system keeps for itself, which a phone cannot open. */
        fun validPort(port: Int): Boolean = port in 1024..65535

        /** Whether [port] can be opened now: nobody else waits on it. The port this app itself waits on is taken too. */
        fun portFree(port: Int): Boolean = validPort(port) && runCatching { ServerSocket().use { it.reuseAddress = true; it.bind(InetSocketAddress(port)) } }.isSuccess

        fun allowed(direction: SyncDirection, mode: ImportMode, folders: List<String>?): Boolean =
            folders?.isEmpty() != true && (mode != ImportMode.REPLACE || direction != SyncDirection.BOTH)

        /**
         * An address of the local network: loopback, link-local, private IPv4 (10/8, 172.16/12, 192.168/16), IPv6 unique local
         * (fc00::/7), or the shared carrier range 100.64/10 some hotspots use.
         */
        fun isLocal(address: java.net.InetAddress): Boolean {
            if (address.isLoopbackAddress || address.isLinkLocalAddress || address.isSiteLocalAddress) return true
            val b = address.address
            return when (b.size) {
                4 -> (b[0].toInt() and 0xFF) == 100 && (b[1].toInt() and 0xC0) == 64
                16 -> (b[0].toInt() and 0xFE) == 0xFC
                else -> false
            }
        }

        fun describe(e: Throwable): String = when (e) {
            is SyncException -> e.reason + if (e.message != null && e.message != e.reason) " (" + e.message + ")" else ""
            is java.net.SocketTimeoutException -> "timeout"
            is java.net.ConnectException -> "refused: " + (e.message ?: "")
            else -> (e.message ?: e.javaClass.simpleName).take(200)
        }
    }
}

/** What an incoming connection is doing (shown on the page). */
object SyncStatus {
    const val PAIRING = "pairing"
    const val RECEIVING = "receiving"
    const val SENDING = "sending"
}
