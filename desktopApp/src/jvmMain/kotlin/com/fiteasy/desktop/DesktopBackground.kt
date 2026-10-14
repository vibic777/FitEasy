package com.fiteasy.desktop

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.awt.image.BufferedImage
import java.awt.RenderingHints
import javax.imageio.ImageIO

fun desktopBackground(bytes:ByteArray):ByteArray {
    ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use {input->
        val readers=ImageIO.getImageReaders(input);require(readers.hasNext()){ "Image format unsupported" }
        val reader=readers.next()
        try {
            reader.input=input;val width=reader.getWidth(0);val height=reader.getHeight(0)
            require(width>0 && height>0 && width.toLong()*height<=40_000_000){"Image dimensions unsupported"}
            val sample=((maxOf(width,height)+1079)/1080).coerceAtLeast(1)
            val decoded=reader.read(0,reader.defaultReadParam.apply {setSourceSubsampling(sample,sample,0,0)})
            val image=BufferedImage(decoded.width,decoded.height,BufferedImage.TYPE_INT_RGB)
            image.createGraphics().apply {color=java.awt.Color.WHITE;fillRect(0,0,image.width,image.height);setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);drawImage(decoded,0,0,null);dispose()}
            return ByteArrayOutputStream().also {check(ImageIO.write(image,"jpeg",it))}.toByteArray().also {require(it.size<=1048576)}
        }finally {reader.dispose()}
    }
}
