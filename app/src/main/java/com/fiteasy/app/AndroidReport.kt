package com.fiteasy.app

import android.graphics.Paint
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import com.fiteasy.shared.*
import java.io.ByteArrayOutputStream

fun androidReportPdf(report:DiaryReport):ByteArray {
    val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {textSize=11f;color=Color.BLACK}
    val lines=wrapReportLines(reportLines(report),515f,paint::measureText)
    val pages=lines.chunked(47);require(pages.size<=250){"Report exceeds 250 pages"}
    val document=PdfDocument()
    try {
        pages.forEachIndexed {index,rows->
            val page=document.startPage(PdfDocument.PageInfo.Builder(595,842,index+1).create())
            rows.forEachIndexed {line,text->page.canvas.drawText(text,40f,48f+line*16f,paint)}
            page.canvas.drawText("FitEasy · ${index+1}/${pages.size}",40f,810f,paint)
            document.finishPage(page)
        }
        return ByteArrayOutputStream().also(document::writeTo).toByteArray()
    } finally {document.close()}
}
