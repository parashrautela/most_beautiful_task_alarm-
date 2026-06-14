package com.example.myapplication

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import android.graphics.BlurMaskFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Log
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt

private val DentonFontFamily = FontFamily(
    Font(R.font.denton_test_medium, FontWeight.Medium),
    Font(R.font.denton_condensed_test_bold, FontWeight.Bold)
)

private val SatoshiFontFamily = FontFamily(
    Font(R.font.satoshi_medium, FontWeight.Medium)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(
    onDismiss: () -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    initialTime: LocalTime = LocalTime.now(),
    selectedDate: java.time.LocalDate = java.time.LocalDate.now(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val view = LocalView.current
    
    // For visual display in the sheet
    var currentDisplayTime by remember { mutableStateOf(initialTime) }

    // Capture the current time once to avoid feedback loops when parent state updates
    val baseTime = remember { LocalTime.now() }

    val isDateTimeValid = remember(selectedDate, currentDisplayTime) {
        val selectedDateTime = java.time.LocalDateTime.of(selectedDate, currentDisplayTime)
        selectedDateTime.isAfter(java.time.LocalDateTime.now())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(Color(0xFFE5E7EB), CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. HEADING
            Text(
                text = "Set your time",
                style = TextStyle(
                    fontFamily = SatoshiFontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF3A3A3A),
                    letterSpacing = (-0.96).sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.15f),
                        offset = Offset(0f, 1f),
                        blurRadius = 1.5f
                    )
                ),
                modifier = Modifier.padding(top = 24.dp)
            )

            // 2. SUBTITLE
            Text(
                text = "Scroll to set the time.",
                style = TextStyle(
                    fontFamily = SatoshiFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF3A3A3A),
                    letterSpacing = (-0.36).sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.1f),
                        offset = Offset(0f, 0.5f),
                        blurRadius = 1f
                    )
                ),
                modifier = Modifier.padding(top = 2.dp)
            )

            // 3. TIME DISPLAY
            val timeFormatter = DateTimeFormatter.ofPattern("hh:mm", Locale.US)
            val amPmFormatter = DateTimeFormatter.ofPattern("a", Locale.US)
            
            Column(
                modifier = Modifier.padding(top = 44.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentDisplayTime.format(timeFormatter),
                    style = TextStyle(
                        fontFamily = DentonFontFamily,
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Medium,
                        brush = Brush.verticalGradient(
                            0.0f to Color.White,
                            0.1f to Color.Black,
                            startY = 0f,
                            endY = 120f
                        ),
                        letterSpacing = (-1.44).sp,
                        shadow = Shadow(
                            color = Color(47, 47, 47, (0.34f * 255).toInt()),
                            offset = Offset(0f, 4f),
                            blurRadius = 5f
                        )
                    )
                )
                // Subtext (AM/PM)
                Text(
                    text = currentDisplayTime.format(amPmFormatter).lowercase(),
                    modifier = Modifier.clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        currentDisplayTime = currentDisplayTime.plusHours(12)
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    },
                    style = TextStyle(
                        fontFamily = DentonFontFamily,
                        fontSize = 15.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium,
                        shadow = Shadow(
                            color = Color(47, 47, 47, (0.34f * 255).toInt()),
                            offset = Offset(0f, 1f),
                            blurRadius = 1.5f
                        )
                    )
                )
            }

            // 4. QUOTE TEXT
            Text(
                text = "\u201c time is a created thing. to say \u2018i don\u2019t have time\u2019, is to say, \u2018i don\u2019t want to\u2019\u201d",
                style = TextStyle(
                    fontFamily = SatoshiFontFamily,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.36).sp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF434343), Color(0xFFA9A9A9)),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    ),
                    shadow = Shadow(
                        color = Color(0x40000000),
                        offset = Offset(0f, 1f),
                        blurRadius = 1.2f
                    )
                ),
                modifier = Modifier
                    .padding(top = 16.dp)
                    .width(200.dp)
            )

            // 5. SCROLL PICKER (Waveform)
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                val diff = java.time.temporal.ChronoUnit.MINUTES.between(baseTime, currentDisplayTime).toInt()
                val initialIndex = diff
                WaveformPicker(
                    initialIndex = initialIndex,
                    maxValue = 1440,
                    onIndexChanged = { index ->
                        val newTime = baseTime.plusMinutes(index.toLong())
                        if (newTime != currentDisplayTime) {
                            currentDisplayTime = newTime
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // ── Framing Vectors ──
                Image(
                    painter = painterResource(id = R.drawable.topleft),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 40.dp)
                        .offset(y = 28.dp)
                        .size(width = 81.dp, height = 50.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.top_right),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 40.dp)
                        .offset(y = 28.dp)
                        .size(width = 81.dp, height = 50.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.leftbottom),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 40.dp)
                        .offset(y = (-14).dp)
                        .size(width = 81.dp, height = 50.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.rrightbottom),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 40.dp)
                        .offset(y = (-14).dp)
                        .size(width = 81.dp, height = 50.dp)
                )

                // ── Center Indicator ──
                Canvas(modifier = Modifier
                    .size(width = 14.dp, height = 8.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 16.dp)
                ) {
                    val trianglePath = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2, size.height)
                        close()
                    }
                    drawIntoCanvas { canvas ->
                        val shadowPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            alpha = (0.3f * 255).toInt()
                            maskFilter = BlurMaskFilter(3.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                        }
                        canvas.nativeCanvas.drawPath(trianglePath.asAndroidPath(), shadowPaint)
                    }
                    drawPath(
                        path = trianglePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Black, Color(0xFF4B5563))
                        )
                    )
                }
            }

            // 6. LOCK IT IN BUTTON
            Box(
                modifier = Modifier
                    .padding(top = 40.dp, start = 22.dp, end = 22.dp, bottom = 8.dp)
                    .fillMaxWidth()
                    .height(68.dp)
                    .drawBehind {
                        // 1. Soft wide ambient shadow path & paint
                        val ambientPath = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    rect = Rect(
                                        left = 0f,
                                        top = 6f.dp.toPx(),
                                        right = size.width,
                                        bottom = size.height + 6f.dp.toPx()
                                    ),
                                    cornerRadius = CornerRadius(34f.dp.toPx())
                                )
                            )
                        }
                        val ambientPaint = Paint().apply {
                            color = Color.Black.copy(alpha = 0.18f)
                            asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(
                                12f.dp.toPx(),
                                android.graphics.BlurMaskFilter.Blur.NORMAL
                            )
                        }

                        // 2. Tighter dark shadow near base path & paint
                        val tightPath = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    rect = Rect(
                                        left = 0f,
                                        top = 2f.dp.toPx(),
                                        right = size.width,
                                        bottom = size.height + 2f.dp.toPx()
                                    ),
                                    cornerRadius = CornerRadius(34f.dp.toPx())
                                )
                            )
                        }
                        val tightPaint = Paint().apply {
                            color = Color.Black.copy(alpha = 0.38f)
                            asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(
                                4f.dp.toPx(),
                                android.graphics.BlurMaskFilter.Blur.NORMAL
                            )
                        }

                        // 3. Subtle white outer glow path & paint
                        val glowPath = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    rect = Rect(
                                        left = -1f.dp.toPx(),
                                        top = -1f.dp.toPx(),
                                        right = size.width + 1f.dp.toPx(),
                                        bottom = size.height + 1f.dp.toPx()
                                    ),
                                    cornerRadius = CornerRadius(34f.dp.toPx())
                                )
                            )
                        }
                        val glowPaint = Paint().apply {
                            color = Color.White.copy(alpha = 0.04f)
                            asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(
                                4f.dp.toPx(),
                                android.graphics.BlurMaskFilter.Blur.NORMAL
                            )
                        }
                        
                        if (isDateTimeValid) {
                            drawIntoCanvas { canvas ->
                                canvas.drawPath(ambientPath, ambientPaint)
                                canvas.drawPath(tightPath, tightPaint)
                                canvas.drawPath(glowPath, glowPaint)
                            }
                        }
                    }
                    .background(
                        brush = if (isDateTimeValid) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF3C3C3C), // Smooth top highlight gray
                                    Color(0xFF1E1E1E), // Soft midtone
                                    Color(0xFF080808)  // Pure deep black at bottom
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFF3F4F6),
                                    Color(0xFFE5E7EB)
                                )
                            )
                        },
                        shape = RoundedCornerShape(34.dp)
                    )
                    // Stacked Inset Shadow 1: Subtle top highlight simulating soft overhead lighting
                    .then(
                        if (isDateTimeValid) {
                            Modifier.neomorphicInnerShadow(
                                shape = RoundedCornerShape(34.dp),
                                color = Color.White.copy(alpha = 0.10f),
                                blur = 4.dp,
                                offsetX = 0.dp,
                                offsetY = 3.dp
                            )
                        } else Modifier
                    )
                    // Stacked Inset Shadow 2: Premium 3D diagonal light reflection
                    .then(
                        if (isDateTimeValid) {
                            Modifier.neomorphicInnerShadow(
                                shape = RoundedCornerShape(34.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                blur = 6.8.dp,
                                offsetX = 4.dp,
                                offsetY = 4.dp
                            )
                        } else Modifier
                    )
                    .border(
                        BorderStroke(1.dp, if (isDateTimeValid) Color.Black else Color(0xFFD1D5DB)),
                        shape = RoundedCornerShape(34.dp)
                    )
                    .clip(RoundedCornerShape(34.dp))
                    .clickable(enabled = isDateTimeValid) { 
                        onTimeSelected(currentDisplayTime)
                        onDismiss() 
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isDateTimeValid) "Lock it in" else "Select future time",
                    style = TextStyle(
                        fontFamily = SatoshiFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDateTimeValid) Color.White else Color(0xFF9CA3AF),
                        letterSpacing = (-0.48).sp
                    )
                )
            }
        }
    }
}
