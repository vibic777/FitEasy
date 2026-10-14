package com.fiteasy.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

/** Quiet margin around a QR code, in modules; scanners need four. */
const val QR_MARGIN=4

/** The dark modules of the QR code of [text], row by row, without the margin (D124). */
fun qrMatrix(text:String):Array<BooleanArray> {
    val matrix=Encoder.encode(text,ErrorCorrectionLevel.M,mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")).matrix
    return Array(matrix.height) {y->BooleanArray(matrix.width) {x->matrix.get(x,y).toInt()==1}}
}

/** A QR code drawn black on white whatever the style, so a camera reads it in the dark theme too. */
@Composable fun QrCode(text:String,label:String,side:Dp=220.dp,modifier:Modifier=Modifier) {
    val matrix=remember(text) {runCatching {qrMatrix(text)}.onFailure {AppJournal.record("qr.encode.failed",JournalLevel.WARN,it)}.getOrNull()} ?: return
    Canvas(modifier.size(side).background(Color.White).semantics {contentDescription=label}) {
        val cell=size.minDimension/(matrix.size+2*QR_MARGIN)
        matrix.forEachIndexed {y,row->row.forEachIndexed {x,dark->
            // A hair of overlap keeps neighbouring modules from showing a white seam at fractional sizes.
            if(dark)drawRect(Color.Black,Offset((x+QR_MARGIN)*cell,(y+QR_MARGIN)*cell),Size(cell+.5f,cell+.5f))
        }}
    }
}
