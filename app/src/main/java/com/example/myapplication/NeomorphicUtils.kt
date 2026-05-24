package com.example.myapplication

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.graphics.BlurMaskFilter

/**
 * A highly reliable and premium neomorphic inner shadow effect for rounded shapes.
 * Uses a hardware-acceleration-safe stroke-clipping technique to create gorgeous
 * "carved" or "pressed" 3D inset lighting without offscreen buffer/DST_OUT glitches.
 */
fun Modifier.neomorphicInnerShadow(
    shape: Shape,
    color: Color,
    blur: Dp = 4.dp,
    offsetY: Dp = 2.dp,
    offsetX: Dp = 2.dp,
    spread: Dp = 0.dp
) = this.drawWithContent {
    drawContent()
    
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply {
        when (outline) {
            is Outline.Rectangle -> addRect(outline.rect)
            is Outline.Rounded -> addRoundRect(outline.roundRect)
            is Outline.Generic -> addPath(outline.path)
        }
    }
    
    clipPath(path) {
        val paint = Paint()
        paint.color = color
        val frameworkPaint = paint.asFrameworkPaint()
        if (blur.toPx() > 0f) {
            frameworkPaint.maskFilter = BlurMaskFilter(
                blur.toPx(), 
                BlurMaskFilter.Blur.NORMAL
            )
        }
        frameworkPaint.style = android.graphics.Paint.Style.STROKE
        val strokeWidth = if (spread.toPx() > 0) spread.toPx() * 2f + blur.toPx() else blur.toPx() * 2f
        frameworkPaint.strokeWidth = strokeWidth.coerceAtLeast(1f)
        
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(offsetX.toPx(), offsetY.toPx())
            canvas.drawPath(path, paint)
            canvas.restore()
        }
    }
}
