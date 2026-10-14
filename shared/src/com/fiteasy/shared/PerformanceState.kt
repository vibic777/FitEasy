package com.fiteasy.shared

/** Cache is published only after the writer returns successfully. Caller owns its transaction lock. */
class CommittedState<T>(initial:T?=null) {
    var value:T?=initial
        private set
    fun invalidate(){value=null}
    fun read(reader:()->T):T=value ?: reader().also {value=it}
    fun commit(next:T,writer:(T)->Unit):Boolean {
        if(value==next)return false
        writer(next);value=next;return true
    }
}

/** Hardware step sensors may hold events this long before waking the phone (D080). */
const val STEP_BATCH_MICROS=60_000_000
/** Software pedometer: no detected steps for this long parks the accelerometer until significant motion. */
const val SOFTWARE_IDLE_MILLIS=180_000L
/** Delay before a step batch is written. Screen on follows the UI; screen off writes a hardware batch at once and spaces out software writes. */
/** Battery saver (D086): longer hardware batches, quicker parking of the software pedometer, rarer notification updates. */
const val STEP_BATCH_ECONOMY_MICROS=300_000_000
const val SOFTWARE_IDLE_ECONOMY_MILLIS=60_000L
fun stepNotificationInterval(economy:Boolean)=if(economy)60_000L else 15_000L
fun stepFlushDelay(interactive:Boolean,software:Boolean)=when {interactive->2_000L;software->30_000L;else->500L}

data class CounterReading(val count:Long,val elapsed:Long,val date:String,val boot:Int)
fun recordDetectedSteps(snapshot:Snapshot,date:String,deviceId:String,count:Long,now:Long=System.currentTimeMillis()):Snapshot {
    require(count in 1..100000)
    val stableId=java.util.UUID.nameUUIDFromBytes("fiteasy:phone:$deviceId:$date".toByteArray()).toString()
    val old=snapshot.shared.entries.filter {it.date==date && it.source=="phone:$deviceId" && it.type=="steps" && it.deleted==null}.maxByOrNull {it.amount}
    val id=old?.id ?: if(snapshot.shared.entries.any {it.id==stableId})newId()else stableId
    val entry=Entry(id=id,date=date,type="steps",name="Телефон",amount=(old?.amount ?: 0.0)+count,unit="steps",source="phone:$deviceId",updated=now)
    return snapshot.copy(shared=snapshot.shared.copy(entries=snapshot.shared.entries.filterNot {it.id==id}+entry),local=snapshot.local.copy(phoneCounter=null))
}
fun counterReading(count:Float,elapsed:Long,date:String,boot:Int):CounterReading? =
    if(!count.isFinite() || count<0 || elapsed<0 || boot<0)null else CounterReading(count.toLong(),elapsed,date,boot)
fun recordCounter(snapshot:Snapshot,reading:CounterReading,deviceId:String,now:Long=System.currentTimeMillis()):Snapshot {
    val state=snapshot.local.phoneCounter
    if(state!=null && state.count==reading.count && state.boot==reading.boot && state.date==reading.date && reading.elapsed>=state.elapsed)return snapshot
    val reboot=state!=null && (reading.boot!=state.boot || reading.elapsed<state.elapsed || reading.count<state.count)
    val measured=when {state==null->0L;reboot->reading.count;else->reading.count-state.count}.coerceAtLeast(0)
    val ambiguous=state!=null && state.date!=reading.date && measured>0
    val stableId=java.util.UUID.nameUUIDFromBytes("fiteasy:phone:$deviceId:${reading.date}".toByteArray()).toString()
    val old=snapshot.shared.entries.filter {it.date==reading.date && it.source=="phone:$deviceId" && it.type=="steps" && it.deleted==null}.maxByOrNull {it.amount}
    val entryId=old?.id ?: if(snapshot.shared.entries.any {it.id==stableId})newId()else stableId
    val entry=Entry(id=entryId,date=reading.date,type="steps",name="Телефон",amount=(old?.amount ?: 0.0)+if(ambiguous)0 else measured,unit="steps",source="phone:$deviceId",updated=now)
    val pending=if(ambiguous)listOf(Entry(date=reading.date,type="steps",name="${state!!.date} → ${reading.date}",amount=measured.toDouble(),unit="steps",source="pending:$deviceId",updated=now))else emptyList()
    return snapshot.copy(shared=snapshot.shared.copy(entries=snapshot.shared.entries.filterNot {it.id==entryId}+entry+pending),local=snapshot.local.copy(phoneCounter=PhoneCounter(reading.date,reading.count,reading.elapsed,reading.boot)))
}
