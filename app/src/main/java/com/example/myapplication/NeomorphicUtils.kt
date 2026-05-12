package com.example.myapplication

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.graphics.BlurMaskFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

/**
 * A premium neomorphic inner shadow effect for rounded shapes.
 * Used across TaskAlarm to create "carved" or "pressed" UI elements.
 */
fun Modifier.neomorphicInnerShadow(
    shape: Shape,
    color: Color,
    blur: Dp = 4.dp,
    offsetY: Dp = 2.dp,
    offsetX: Dp = 2.dp,
    spread: Dp = 0.dp
) = drawWithContent {
    drawContent()

    val rect = Rect(Offset.Zero, size)
    val paint = Paint().apply {
        this.color = color
        this.isAntiAlias = true
    }

    val shadowLeft = rect.left + offsetX.toPx()
    val shadowTop = rect.top + offsetY.toPx()
    val shadowRight = rect.right + offsetX.toPx()
    val shadowBottom = rect.bottom + offsetY.toPx()

    drawIntoCanvas { canvas ->
        canvas.saveLayer(rect, paint)
        canvas.drawOutline(shape.createOutline(size, layoutDirection, this), paint)

        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)

        if (blur.toPx() > 0) {
            frameworkPaint.maskFilter = BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL)
        }

        canvas.translate(offsetX.toPx(), offsetY.toPx())
        canvas.drawOutline(shape.createOutline(size, layoutDirection, this), paint)
        canvas.restore()
    }
}
