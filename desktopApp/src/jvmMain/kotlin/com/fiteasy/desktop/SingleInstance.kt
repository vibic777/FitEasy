package com.fiteasy.desktop

import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.awt.EventQueue

/** Independent implementation: one process per local data directory, before opening the database. */
class SingleInstance(private val root:File):AutoCloseable {
    private val file=RandomAccessFile(File(root.apply {mkdirs()},"instance.lock"),"rw")
    private val lock=file.channel.tryLock()
    private var server:ServerSocket?=null
    @Volatile var onShow:()->Unit={}
    @Volatile var onQuit:()->Unit={}
    @Volatile private var pendingShow=false
    val claimed get()=lock!=null
    init {
        if(claimed){
            val listener=ServerSocket(0,4,InetAddress.getByName("127.0.0.1"));server=listener
            val token=Base64.getEncoder().encodeToString(ByteArray(32).also {SecureRandom().nextBytes(it)})
            File(root,"instance.port").writeText("${listener.localPort}\n$token\n")
            Thread({while(!listener.isClosed){
                val client=runCatching {listener.accept()}.getOrNull() ?: break
                runCatching {client.use {c->
                    c.soTimeout=1500
                    val input=c.getInputStream()
                    fun line():String {val bytes=ArrayList<Byte>();while(bytes.size<128){val n=input.read();if(n<0 || n==10)return bytes.toByteArray().toString(Charsets.US_ASCII);bytes.add(n.toByte())};error("Command too long")}
                    val command=line();val supplied=line()
                    if(!MessageDigest.isEqual(token.toByteArray(),supplied.toByteArray()))return@use
                    when(command){
                        "show"->{pendingShow=true;EventQueue.invokeLater {onShow()}}
                        "quit"->{c.getOutputStream().write("ok\n".toByteArray());EventQueue.invokeLater {onQuit()}}
                    }
                }}
            }},"FitEasy-instance").apply {isDaemon=true;start()}
        }else {
            val lines=File(root,"instance.port").takeIf {it.exists()}?.readLines().orEmpty()
            if(lines.size>=2)runCatching {Socket().use {s->s.connect(InetSocketAddress("127.0.0.1",lines[0].toInt()),1500);s.getOutputStream().write("show\n${lines[1]}\n".toByteArray())}}
            file.close()
        }
    }
    fun ready(show:()->Unit,quit:()->Unit){onShow=show;onQuit=quit;if(pendingShow){pendingShow=false;EventQueue.invokeLater(show)}}
    override fun close(){if(claimed){runCatching {server?.close()};File(root,"instance.port").delete();runCatching {lock?.release()};runCatching {file.close()}}}
}
