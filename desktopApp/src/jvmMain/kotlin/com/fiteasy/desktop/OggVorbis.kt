package com.fiteasy.desktop

import com.jcraft.jogg.Packet
import com.jcraft.jogg.Page
import com.jcraft.jogg.StreamState
import com.jcraft.jogg.SyncState
import com.jcraft.jorbis.Block
import com.jcraft.jorbis.Comment
import com.jcraft.jorbis.DspState
import com.jcraft.jorbis.Info
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import javax.sound.sampled.AudioFormat

/**
 * OGG Vorbis into plain 16-bit samples with JOrbis (D123). The javax.sound plug-in for Vorbis drops the end of a short
 * file — of ten seconds it gave four — and a melody of a reminder is often short, so the pages are read here.
 */
internal object OggVorbis {
    private const val CHUNK=4096

    fun isOgg(file:File):Boolean = runCatching {file.inputStream().use {String(it.readNBytes(4),Charsets.ISO_8859_1)=="OggS"}}.getOrDefault(false)

    /** The first [limitMillis] of the sound; fails when the file is not Vorbis (Opus in an OGG file, for one). */
    fun decode(file:File,limitMillis:Long):Pair<AudioFormat,ByteArray> = file.inputStream().buffered().use {input->
        val sync=SyncState();val stream=StreamState();val page=Page();val packet=Packet()
        val info=Info();val comment=Comment()
        sync.init()
        fun feed(source:InputStream):Int {val at=sync.buffer(CHUNK);val read=source.read(sync.data,at,CHUNK).coerceAtLeast(0);sync.wrote(read);return read}
        feed(input)
        require(sync.pageout(page)==1) {"Not an OGG stream"}
        stream.init(page.serialno());info.init();comment.init()
        require(stream.pagein(page)>=0 && stream.packetout(packet)==1 && info.synthesis_headerin(comment,packet)>=0) {"Not a Vorbis stream"}
        // Two more headers: comments and codebooks.
        var headers=0
        while(headers<2) {
            while(headers<2) {
                val result=sync.pageout(page)
                if(result==0)break
                if(result==1) {
                    stream.pagein(page)
                    while(headers<2) {
                        val got=stream.packetout(packet)
                        if(got==0)break
                        require(got==1 && info.synthesis_headerin(comment,packet)>=0) {"Damaged Vorbis header"}
                        headers++
                    }
                }
            }
            if(headers<2)require(feed(input)>0) {"Vorbis headers are cut"}
        }
        require(info.channels in 1..2 && info.rate in 8000..192000) {"Unsupported Vorbis sound"}
        val format=AudioFormat(AudioFormat.Encoding.PCM_SIGNED,info.rate.toFloat(),16,info.channels,info.channels*2,info.rate.toFloat(),false)
        val limit=(info.rate.toLong()*limitMillis/1000*format.frameSize).coerceIn(0,256L*1024*1024)
        val dsp=DspState();dsp.synthesis_init(info)
        val block=Block(dsp)
        val pcm=arrayOfNulls<Array<FloatArray>>(1);val offsets=IntArray(info.channels)
        val out=ByteArrayOutputStream();val frame=ByteArray(CHUNK*format.frameSize)
        var ended=false
        while(!ended && out.size()<limit) {
            while(!ended && out.size()<limit) {
                val result=sync.pageout(page)
                if(result==0)break
                // -1 is a gap in the data: the next page is taken as it comes.
                if(result==1) {
                    stream.pagein(page)
                    while(true) {
                        val got=stream.packetout(packet)
                        if(got==0)break
                        if(got<0)continue
                        if(block.synthesis(packet)==0)dsp.synthesis_blockin(block)
                        while(true) {
                            val available=dsp.synthesis_pcmout(pcm,offsets)
                            if(available<=0)break
                            val count=minOf(available,CHUNK);val samples=pcm[0]!!
                            for(channel in 0 until info.channels) {
                                var at=channel*2;val from=offsets[channel];val data=samples[channel]
                                for(i in 0 until count) {
                                    val value=(data[from+i]*32767f).toInt().coerceIn(-32768,32767)
                                    frame[at]=value.toByte();frame[at+1]=(value shr 8).toByte();at+=format.frameSize
                                }
                            }
                            out.write(frame,0,count*format.frameSize)
                            dsp.synthesis_read(count)
                        }
                    }
                    if(page.eos()!=0)ended=true
                }
            }
            if(!ended && feed(input)==0)ended=true
        }
        val bytes=out.toByteArray()
        format to bytes.copyOf(minOf(bytes.size.toLong(),limit).toInt().let {it-it%format.frameSize})
    }
}
