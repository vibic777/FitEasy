package com.fiteasy.shared

/** Counts active monotonic time, retaining subsecond remainder across pauses and stages. */
class ActiveSecondsClock {
    private var anchor:Long?=null
    private var remainder=0L
    fun resume(now:Long){if(anchor==null)anchor=now}
    fun sample(now:Long):Int {
        val start=anchor ?: return 0
        if(now<start)return 0
        val delta=(now-start).coerceAtMost(86_400_000_000_000L)
        anchor=now
        val total=(remainder+delta).coerceAtMost(86_400_000_000_000L)
        remainder=total%1_000_000_000L
        return (total/1_000_000_000L).toInt()
    }
    fun pause(now:Long?=null){val start=anchor;if(now!=null && start!=null && now>=start)remainder+=(now-start).coerceAtMost(86_400_000_000_000L);anchor=null}
}
