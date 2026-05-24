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
    var currentTask by remember(task.id) { mutableStateOf(task) }

    // Reschedule state
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedRescheduleDate by remember { mutableStateOf<java.time.LocalDate?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val ldt = remember(currentTask.dateTime) {
        try { LocalDateTime.parse(currentTask.dateTime) } catch (e: Exception) { LocalDateTime.now() }
    }
    val dayNum      = ldt.dayOfMonth.toString()
    val monthYear   = ldt.format(DateTimeFormatter.ofPattern("/MMM/yyyy", Locale.US)).lowercase()
    val timeStr     = ldt.format(DateTimeFormatter.ofPattern("H:mm", Locale.US))
    val amPm        = ldt.format(DateTimeFormatter.ofPattern("a", Locale.US))
    val dayOfWeek   = ldt.dayOfWeek.value  // Mon=1 … Sun=7

    // S M T W T F S → index 0=Sun, 1=Mon … 6=Sat
    val dayLetters     = listOf("S", "M", "T", "W", "T", "F", "S")
    val activeDayIndex = dayOfWeek % 7

    val priorityLabel = when (currentTask.priority) {
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
                                    val frameworkPaint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.argb(54, 0, 0, 0) // 21% opacity
                                        maskFilter = android.graphics.BlurMaskFilter(
                                            2.8f * d,
                                            android.graphics.BlurMaskFilter.Blur.NORMAL
                                        )
                                    }
                                    canvas.nativeCanvas.save()
                                    canvas.nativeCanvas.translate(0f, 2f * d) // dx=0, dy=2
                                    canvas.nativeCanvas.drawRect(
                                        0f, 0f, size.width, size.height, frameworkPaint
                                    )
                                    canvas.nativeCanvas.restore()
                                }
                            }
                            .background(CheckerBg)
                            .border(0.4.dp, CheckerBorder)
                    )
                }
            }

            // ── Glassmorphism Back button ──
            Box(
                modifier = Modifier
                    .padding(start = 24.dp, top = 56.dp) // Moved down to clear status bar/be less tight
                    .size(48.dp) // Size requested by user
                    // Glass background fill
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    // Continuous gradient border
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.8f),
                                Color.White.copy(alpha = 0.1f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        ),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBack() },
                contentAlignment = Alignment.Center
            ) {
                // The chevron icon inside
                Image(
                    painter = painterResource(R.drawable.back_button),
                    contentDescription = "Back",
                    // Optically center the chevron by shifting it to the left
                    modifier = Modifier.size(20.dp).offset(x = (-2).dp),
                    contentScale = ContentScale.Fit
                )
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

                    // Reschedule row
                    val remaining = currentTask.reschedulesRemaining ?: 2
                    val canReschedule = remaining > 0
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = canReschedule
                        ) {
                            showDatePicker = true
                        }
                    ) {
                        Image(
                            painter = painterResource(if (canReschedule) R.drawable.reschedule_active else R.drawable.reschedule_inactive),
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            text = "Reschedule",
                            fontFamily = DetailSatoshiMedium,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (canReschedule) Color.Black else Color(0xFF7F7F7F),
                            letterSpacing = (-0.36).sp,
                            style = TextStyle(
                                textDecoration = if (canReschedule) TextDecoration.Underline else TextDecoration.LineThrough
                            )
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
                            text = currentTask.title,
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
                    if (currentTask.description.isNotBlank()) {
                        Text(
                            text = currentTask.description,
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

        // ── 3. Dark "Only winners makes it this Far" Slider ─────────
        com.example.myapplication.SlideToSetButton(
            onSlideComplete = { onComplete() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
            successText = "“Only winners makes it this Far”"
        )
    }

    if (showDatePicker) {
        val currentDateTime = java.time.LocalDateTime.parse(currentTask.dateTime)
        DatePickerSheet(
            initialDate = currentDateTime.toLocalDate(),
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                selectedRescheduleDate = date
                showDatePicker = false
                showTimePicker = true
            }
        )
    }

    if (showTimePicker) {
        val currentDateTime = java.time.LocalDateTime.parse(currentTask.dateTime)
        TimePickerSheet(
            initialTime = currentDateTime.toLocalTime(),
            onDismiss = { showTimePicker = false },
            onTimeSelected = { time ->
                showTimePicker = false
                val newDate = selectedRescheduleDate ?: currentDateTime.toLocalDate()
                val newDateTime = java.time.LocalDateTime.of(newDate, time)
                
                val updatedTask = currentTask.copy(
                    dateTime = newDateTime.toString(),
                    reschedulesRemaining = (currentTask.reschedulesRemaining ?: 2) - 1
                )
                TaskStorage.updateTask(context, updatedTask)
                TaskStorage.logReschedule(context)
                
                currentTask = updatedTask
            }
        )
    }
}
