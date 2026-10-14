package com.fiteasy.shared

import kotlin.math.*

/** Approximate rhythm detector, never a claim of validated device accuracy. No allocations per sample. */
class SoftwareSteps {
    private var previous=0L
    private var gravity=9.81
    private var smooth=0.0
    private var armed=true
    private var lastPeak=0L
    private var rhythm=0
    private var started=0L
    fun sample(timestampNanos:Long,x:Float,y:Float,z:Float):Int {
        if(timestampNanos<=0 || !x.isFinite() || !y.isFinite() || !z.isFinite())return 0
        val magnitude=sqrt(x.toDouble()*x+y.toDouble()*y+z.toDouble()*z)
        if(previous==0L){previous=timestampNanos;started=timestampNanos;gravity=magnitude;return 0}
        if(timestampNanos<=previous)return 0
        val dt=(timestampNanos-previous)/1e9;previous=timestampNanos
        if(dt>2){gravity=magnitude;smooth=0.0;armed=true;rhythm=0;lastPeak=0;started=timestampNanos;return 0}
        val a=1-exp(-dt/.5);gravity+=a*(magnitude-gravity)
        val linear=magnitude-gravity
        if(abs(linear)>15){rhythm=0;armed=false;smooth=0.0;return 0}
        smooth+=(1-exp(-dt/.08))*(linear-smooth)
        if(smooth<.2)armed=true
        if(timestampNanos-started<1_000_000_000L || !armed || smooth<.9)return 0
        armed=false
        val interval=if(lastPeak==0L)Long.MAX_VALUE else timestampNanos-lastPeak
        if(interval<280_000_000L)return 0
        rhythm=if(interval<=1_800_000_000L)rhythm+1 else 1
        lastPeak=timestampNanos
        return when {rhythm==3->3;rhythm>3->1;else->0}
    }
}
