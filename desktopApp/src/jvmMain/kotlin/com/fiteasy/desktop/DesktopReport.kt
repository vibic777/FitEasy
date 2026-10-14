package com.fiteasy.desktop

import com.fiteasy.shared.*
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/** JPEG pages in a standard PDF container; Unicode is rendered by the OS logical font. */
fun desktopReportPdf(report:DiaryReport):ByteArray {
    val image=BufferedImage(1190,1684,BufferedImage.TYPE_INT_RGB)
    val graphics=image.createGraphics().apply {font=Font(Font.SANS_SERIF,Font.PLAIN,22);setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON)}
    try {
        val lines=wrapReportLines(reportLines(report),1030f){graphics.fontMetrics.stringWidth(it).toFloat()}
        val pages=lines.chunked(47);require(pages.size<=250){"Report exceeds 250 pages"}
        val output=ByteArrayOutputStream();val offsets=mutableListOf(0)
        fun raw(text:String){output.write(text.toByteArray(Charsets.US_ASCII))}
        fun objectStart(number:Int){check(offsets.size==number);offsets+=output.size();raw("$number 0 obj\n")}
        raw("%PDF-1.4\n")
        objectStart(1);raw("<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
        objectStart(2);raw("<< /Type /Pages /Count ${pages.size} /Kids [${pages.indices.joinToString(" "){"${3+it*3} 0 R"}}] >>\nendobj\n")
        pages.forEachIndexed {index,rows->
            graphics.color=Color.WHITE;graphics.fillRect(0,0,image.width,image.height);graphics.color=Color.BLACK
            rows.forEachIndexed {line,text->graphics.drawString(text,80,96+line*32)}
            graphics.drawString("FitEasy · ${index+1}/${pages.size}",80,1620)
            val jpeg=ByteArrayOutputStream().also {check(ImageIO.write(image,"jpeg",it))}.toByteArray()
            val page=3+index*3
            objectStart(page);raw("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /XObject << /Im0 ${page+1} 0 R >> >> /Contents ${page+2} 0 R >>\nendobj\n")
            objectStart(page+1);raw("<< /Type /XObject /Subtype /Image /Width 1190 /Height 1684 /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpeg.size} >>\nstream\n");output.write(jpeg);raw("\nendstream\nendobj\n")
            val content="q 595 0 0 842 0 0 cm /Im0 Do Q\n"
            objectStart(page+2);raw("<< /Length ${content.length} >>\nstream\n$content"+"endstream\nendobj\n")
        }
        val xref=output.size();raw("xref\n0 ${offsets.size}\n0000000000 65535 f \n")
        offsets.drop(1).forEach {raw(it.toString().padStart(10,'0')+" 00000 n \n")}
        raw("trailer\n<< /Size ${offsets.size} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return output.toByteArray()
    } finally {graphics.dispose()}
}
