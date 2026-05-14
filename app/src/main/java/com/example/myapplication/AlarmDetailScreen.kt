package com.example.myapplication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── Font families ────────────────────────────────────────────────────────────
private val DetailDentonMedium  = FontFamily(Font(R.font.denton_test_medium, FontWeight.Medium))
private val DetailSatoshiMedium = FontFamily(Font(R.font.satoshi_medium, FontWeight.Medium))

// ─── Color palette (pixel-perfect from Figma) ─────────────────────────────────
private val DetailBgGradient = listOf(
    Color(0xFF47508C),
    Color(0xFF656E94),
    Color(0xFF8D9BB5),
    Color(0xFFFFECDB),
    Color(0xFFFC8C2A),
    Color(0xFFFC6B01),
)
private val CheckerBg       = Color(0x4DFFFFFF)
private val CheckerBorder   = Color(0xFFE2E2E2)
private val GreenGradTop    = Color(0xFF00E6AC)
private val GreenGradBot    = Color(0xFF005A43)
private val ImportantGray   = Color(0xFF7F7F7F)
private val TitleColor      = Color(0xFF2F2F2F)
private val DescColor       = Color(0xFFA4A4A4)
private val DateColor       = Color(0xFF2F2F2F)
private val DateSmallColor  = Color(0xFF3A3A3A)
private val FreqActiveCol   = Color(0xFF2F2F2F)
private val FreqInactiveCol = Color(0xFFDEDEDE)
private val FreqLabel       = Color(0xFFB4B4B4)
private val SliderBg        = Color(0xFFD0D0D0)
private val SliderText      = Color(0xFF3A3A3A)

@Composable
fun AlarmDetailScreen(
    task: TaskAlarm,
    onBack: () -> Unit,
    onComplete: () -> Unit = {}
) {
    val ldt = remember(task.dateTime) {
        try { LocalDateTime.parse(task.dateTime) } catch (e: Exception) { LocalDateTime.now() }
    }
    val dayNum      = ldt.dayOfMonth.toString()
    val monthYear   = ldt.format(DateTimeFormatter.ofPattern("/MMM/yyyy", Locale.US)).lowercase()
    val timeStr     = ldt.format(DateTimeFormatter.ofPattern("H:mm", Locale.US))
    val amPm        = ldt.format(DateTimeFormatter.ofPattern("a", Locale.US))
    val dayOfWeek   = ldt.dayOfWeek.value  // Mon=1 … Sun=7

    // S M T W T F S → index 0=Sun, 1=Mon … 6=Sat
    val dayLetters     = listOf("S", "M", "T", "W", "T", "F", "S")
    val activeDayIndex = dayOfWeek % 7

    val priorityLabel = when (task.priority) {
        0    -> "IMPORTANT"
        1    -> "CRITICAL"
        else -> "FLEXIBLE"
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        // ── 1. Hero gradient (Figma Frame 13: full-width × 420dp) ────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .background(Brush.verticalGradient(colors = DetailBgGradient))
        ) {
            // Checker squares (Figma: x=22, y=332, 12×12, gap=6, ×4)
            Column(
                modifier = Modifier
                    .padding(start = 22.dp, top = 332.dp)
                    .width(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            // Figma: box-shadow 0px 2px 2.8px 0px rgba(0,0,0,0.21)
                            .drawBehind {
                                val d = density
                                drawIntoCanvas { canvas ->
                                    val p = android.graphics.Paint().apply {
                                        color = android.graphics.Color.TRANSPARENT
                                        // blur=2.8, dx=0, dy=2, alpha=0.21*255≈54
                                        setShadowLayer(
                                            2.8f * d,
                                            0f,
                                            2f * d,
                                            android.graphics.Color.argb(54, 0, 0, 0)
                                        )
                                    }
                                    canvas.nativeCanvas.drawRect(
                                        0f, 0f, size.width, size.height, p
                                    )
                                }
                            }
                            .background(CheckerBg)
                            .border(0.4.dp, CheckerBorder)
                    )
                }
            }

            // ── Back button (Figma: x=24, y=26, 48×48)
            // Built from the 4 corner arc PNGs forming a circle + chevron
            Box(
                modifier = Modifier
                    .padding(start = 24.dp, top = 26.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))        // semi-transparent white fill
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBack() },
                contentAlignment = Alignment.Center
            ) {
                // Corner arc overlays — 4 quadrant curves composing the circle border
                Image(
                    painter = painterResource(R.drawable.topleft),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.TopStart),
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(R.drawable.top_right),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.TopEnd),
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(R.drawable.leftbottom),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.BottomStart),
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(R.drawable.rrightbottom),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.BottomEnd),
                    contentScale = ContentScale.Fit
                )
                // Chevron left drawn via Canvas
                Canvas(modifier = Modifier.size(20.dp)) {
                    val strokeW = 1.6.dp.toPx()
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val arm = 5.dp.toPx()
                    val path = Path().apply {
                        moveTo(cx + arm * 0.5f, cy - arm)
                        lineTo(cx - arm * 0.5f, cy)
                        lineTo(cx + arm * 0.5f, cy + arm)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF4A4A4A),
                        style = Stroke(
                            width = strokeW,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        // ── 2. Content area (Figma Frame 174: y=448, left=22) ────────────────
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(380)) + slideInVertically(tween(380)) { it / 5 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 448.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {

                // ── Row: IMPORTANT badge + Reschedule ────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // IMPORTANT badge (Figma 322:331 — 91×20)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Outer 20×20 border-only box (Figma 322:332)
                        Box(modifier = Modifier.size(20.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(0.46.dp, GreenGradTop)
                            )
                            // Inner 16×16 at offset (2,2)
                            // Figma: inset 0px -0.909px 1.818px rgba(0,0,0,0.25)  ← dark bottom edge
                            //        inset 0.455px 0.455px 0.864px rgba(255,255,255,0.8) ← white top-left
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .offset(x = 2.dp, y = 2.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colorStops = arrayOf(
                                                0.028f to GreenGradTop,
                                                1.315f to GreenGradBot
                                            )
                                        )
                                    )
                                    // dark bottom inset: dy=-0.909, blur=1.818, rgba(0,0,0,0.25) → alpha=64
                                    .neomorphicInnerShadow(
                                        shape = RectangleShape,
                                        color = Color(0, 0, 0, 64),
                                        blur = 1.818.dp,
                                        offsetX = 0.dp,
                                        offsetY = (-0.909).dp
                                    )
                                    // white top-left inset: dx=0.455, dy=0.455, blur=0.864, rgba(255,255,255,0.8) → alpha=204
                                    .neomorphicInnerShadow(
                                        shape = RectangleShape,
                                        color = Color(255, 255, 255, 204),
                                        blur = 0.864.dp,
                                        offsetX = 0.455.dp,
                                        offsetY = 0.455.dp
                                    )
                            )
                        }
                        // "IMPORTANT" label (Figma 322:333)
                        Text(
                            text = priorityLabel,
                            fontFamily = DetailSatoshiMedium,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = ImportantGray,
                            letterSpacing = (-0.36).sp
                        )
                    }

                    // Reschedule row (Figma Frame 176: 78×13)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* TODO */ }
                    ) {
                        // Clock icon — reschedule_active.png (Figma 329:20, 13×13)
                        Image(
                            painter = painterResource(R.drawable.reschedule_active),
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            contentScale = ContentScale.Fit
                        )
                        // "Reschedule" underlined text (Figma 322:337)
                        Text(
                            text = "Reschedule",
                            fontFamily = DetailSatoshiMedium,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color.Black,
                            letterSpacing = (-0.36).sp,
                            style = TextStyle(textDecoration = TextDecoration.Underline)
                        )
                    }
                }

                // ── Title + description (Figma Frame 168) ────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {

                    // Title row (Figma Frame 167)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(30.dp)
                    ) {
                        // Title — Denton 48sp #2F2F2F
                        // Figma text-shadow: 0px 1px 0.8px rgba(47,47,47,0.28)
                        Text(
                            text = task.title,
                            fontFamily = DetailDentonMedium,
                            fontWeight = FontWeight.Medium,
                            fontSize = 48.sp,
                            lineHeight = 36.sp,
                            style = TextStyle(
                                color = TitleColor,
                                shadow = Shadow(
                                    color = Color(0x4A2F2F2F), // rgba(47,47,47,0.28) → alpha≈71
                                    offset = Offset(0f, 1f),
                                    blurRadius = 0.8f
                                )
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        // Day starburst badge — icon_day.png (increased size)
                        Image(
                            painter = painterResource(R.drawable.icon_day),
                            contentDescription = null,
                            modifier = Modifier
                                .size(width = 64.dp, height = 32.dp)
                                .rotate(-1.06f),
                            contentScale = ContentScale.FillBounds
                        )
                    }

                    // Description (Figma 322:362 — w=237, 12sp, #A4A4A4)
                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            fontFamily = DetailSatoshiMedium,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = DescColor,
                            letterSpacing = (-0.36).sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.widthIn(max = 237.dp)
                        )
                    }
                }

                // ── Date + Time (Figma Frame 171) ─────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(50.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Date (Figma Frame 169)
                    Row(verticalAlignment = Alignment.Bottom) {
                        // "20" – Denton 48sp #2F2F2F tracking=-1.44
                        Text(
                            text = dayNum,
                            fontFamily = DetailDentonMedium,
                            fontWeight = FontWeight.Medium,
                            fontSize = 48.sp,
                            color = DateColor,
                            letterSpacing = (-1.44).sp
                        )
                        // "/may/2026" – 14sp #3A3A3A tracking=-0.42
                        Text(
                            text = monthYear,
                            fontFamily = DetailDentonMedium,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = DateSmallColor,
                            letterSpacing = (-0.42).sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Time (Figma Frame 170)
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // "12:10" – Denton 48sp #2F2F2F tracking=-1.44
                        Text(
                            text = timeStr,
                            fontFamily = DetailDentonMedium,
                            fontWeight = FontWeight.Medium,
                            fontSize = 48.sp,
                            color = DateColor,
                            letterSpacing = (-1.44).sp
                        )
                        // "PM" – 14sp #3A3A3A center tracking=-0.42
                        Text(
                            text = amPm.uppercase(Locale.US),
                            fontFamily = DetailDentonMedium,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = DateSmallColor,
                            letterSpacing = (-0.42).sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                // ── Frequency (Figma Frame 165: w=317) ───────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    // "FREQUENCY" label – Satoshi 12sp #B4B4B4
                    Text(
                        text = "FREQUENCY",
                        fontFamily = DetailSatoshiMedium,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = FreqLabel,
                        letterSpacing = (-0.36).sp
                    )
                    // S M T W T F S row — Denton 36sp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        dayLetters.forEachIndexed { index, letter ->
                            val isActive = (index == activeDayIndex)
                            Text(
                                text = letter,
                                fontFamily = DetailDentonMedium,
                                fontWeight = FontWeight.Medium,
                                fontSize = 36.sp,
                                color = if (isActive) FreqActiveCol else FreqInactiveCol,
                                letterSpacing = (-1.08).sp
                            )
                        }
                    }
                }
            }
        }

        // ── 3. "TASK COMPLETED" slider (Figma: y=784, x=22, 358×68) ─────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 22.dp, end = 22.dp, bottom = 22.dp)
                .fillMaxWidth()
                .height(68.dp)
                // outer drop-shadow: 2px 2px 4px rgba(90,90,90,0.25)
                .drawBehind {
                    val d = density
                    drawIntoCanvas { canvas ->
                        val shadowPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.TRANSPARENT
                            setShadowLayer(
                                4f * d, 2f * d, 2f * d,
                                android.graphics.Color.argb(64, 90, 90, 90)
                            )
                        }
                        canvas.nativeCanvas.drawRoundRect(
                            0f, 0f, size.width, size.height,
                            32f * d, 32f * d, shadowPaint
                        )
                    }
                }
                .background(SliderBg, RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp))
        ) {
            // Inset shadow overlay: inset 1px 1px 4px rgba(48,48,48,0.25)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .neomorphicInnerShadow(
                        shape = RoundedCornerShape(32.dp),
                        color = Color(48, 48, 48, 64),
                        blur = 4.dp,
                        offsetX = 1.dp,
                        offsetY = 1.dp
                    )
            )

            // Slider thumb — neomorphic circle (Figma: left=4, top=4, 60×60)
            Box(
                modifier = Modifier
                    .padding(start = 4.dp, top = 4.dp)
                    .size(60.dp)
                    // outer drop-shadow + gradient fill (matching Figma slider thumb)
                    .drawBehind {
                        val d = density
                        drawIntoCanvas { canvas ->
                            val p = android.graphics.Paint().apply {
                                color = android.graphics.Color.TRANSPARENT
                                setShadowLayer(
                                    1.95f * d, 0f, 4f * d,
                                    android.graphics.Color.argb(43, 124, 124, 124)
                                )
                            }
                            canvas.nativeCanvas.drawCircle(
                                size.width / 2f, size.height / 2f, size.width / 2f, p
                            )
                        }
                    }
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White, Color(0xFFF1F2F3))
                        ),
                        CircleShape
                    )
                    .border(
                        0.8.dp,
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF969696), Color(0xB0F8F8F8)),
                            start = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                            end = Offset(0f, 0f)
                        ),
                        CircleShape
                    )
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onComplete() },
                contentAlignment = Alignment.Center
            ) {
                // next_icon.png — the `>` chevron (Figma: black chevron inside the circle)
                Image(
                    painter = painterResource(R.drawable.next_icon),
                    contentDescription = "Complete task",
                    modifier = Modifier.size(18.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // "TASK COMPLETED" label (Figma: left=154, top=29, Satoshi 12sp #3A3A3A)
            Text(
                text = "TASK COMPLETED",
                fontFamily = DetailSatoshiMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = SliderText,
                letterSpacing = (-0.36).sp,
                modifier = Modifier
                    .absoluteOffset(x = 154.dp, y = 29.dp)
            )
        }
    }
}
