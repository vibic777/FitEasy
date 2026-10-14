package com.fiteasy.shared

/**
 * Writes of the database leave the thread of the interface (D125). The newest state is handed over and the call returns
 * at once; one thread writes, always against what the disk is known to hold, so only changed rows go out. States that
 * arrive while a write runs are joined: only the last of them is written. [flush] waits for everything handed over —
 * before the file is renamed or replaced, before the app leaves the screen or ends — and says whether the disk now
 * holds the last state. A failed write is reported through [failed] with what the disk held before it.
 */
class SaveQueue<T:Any>(private val name:String,private val write:(value:T,persisted:T?)->Unit,private val failed:(value:T,persisted:T?,error:Throwable)->Unit) {
    private val lock=Object()
    private var pending:T?=null
    private var persisted:T?=null
    private var busy=false
    private var lastFailed=false
    private var worker:Thread?=null

    /** What was just read from the disk, or what a replaced file is known to hold; null when that is not known. */
    fun known(value:T?) {synchronized(lock) {if(pending==null && !busy){persisted=value;lastFailed=false}}}

    fun submit(value:T) {synchronized(lock) {
        pending=value
        if(worker==null)worker=Thread(::run,name).apply {isDaemon=true;start()}
        lock.notifyAll()
    }}

    /** Waits until nothing waits to be written; false when the last write failed. */
    fun flush():Boolean = synchronized(lock) {
        while(pending!=null || busy)lock.wait()
        !lastFailed
    }

    private fun run() {
        while(true) {
            val value:T;val base:T?
            synchronized(lock) {
                // The thread waits without waking; after half a minute with nothing to write it ends.
                if(pending==null)lock.wait(IDLE_MILLIS)
                val next=pending
                if(next==null){worker=null;return}
                value=next;pending=null;busy=true;base=persisted
            }
            val error=runCatching {write(value,base)}.exceptionOrNull()
            synchronized(lock) {
                busy=false;lastFailed=error!=null
                // After a failure nothing is assumed about the disk: the next write compares with the file itself.
                persisted=if(error==null)value else null
                lock.notifyAll()
            }
            if(error!=null)runCatching {failed(value,base,error)}
        }
    }

    private companion object {const val IDLE_MILLIS=30_000L}
}
