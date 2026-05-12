package com.example.myapplication

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
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
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Waveform scroll logic with momentum
    val totalOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val velocityTracker = remember { VelocityTracker() }
    
    // Unified step based on barWidth (2dp) + barSpacing (12dp)
    // Each step represents 1 minute
    val timeStepPx = with(LocalDensity.current) { 14.dp.toPx() }
    
    // For visual display in the sheet
    var currentDisplayTime by remember { mutableStateOf(initialTime) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var lastSnappedIndex by remember { mutableStateOf(0) }

    // Capture the initial time once to avoid feedback loops when parent state updates
    val baseTime = remember { initialTime }
    
    // Sync display time and haptics with scroll offset
    LaunchedEffect(Unit) {
        snapshotFlow { totalOffset.value }
            .collect { offset ->
                val currentIndex = (offset / timeStepPx).roundToInt()
                if (currentIndex != lastSnappedIndex) {
                    lastSnappedIndex = currentIndex
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    
                    val newTime = baseTime.plusMinutes((-currentIndex).toLong())
                    if (newTime != currentDisplayTime) {
                        currentDisplayTime = newTime
                        onTimeSelected(newTime)
                    }
                }
            }
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
                        scope.launch {
                            // Toggle AM/PM by shifting the scroll drum by 12 hours (720 minutes)
                            val shift = 720f * timeStepPx
                            totalOffset.animateTo(
                                targetValue = totalOffset.value - shift,
                                animationSpec = spring(stiffness = Spring.StiffnessLow)
                            )
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        }
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
                    .height(160.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { velocityTracker.resetTracking() },
                            onHorizontalDrag = { change, amount ->
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                scope.launch {
                                    totalOffset.snapTo(totalOffset.value + amount)
                                }
                                change.consume()
                            },
                            onDragEnd = {
                                val velocity = velocityTracker.calculateVelocity().x
                                scope.launch {
                                    if (abs(velocity) > 100f) {
                                        totalOffset.animateDecay(velocity, exponentialDecay())
                                    }
                                    val finalSnappedOffset = (totalOffset.value / timeStepPx).roundToInt() * timeStepPx
                                    totalOffset.animateTo(
                                        targetValue = finalSnappedOffset,
                                        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
                                    )
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // ── Waveform Canvas ──
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2 + 10.dp.toPx()
                    val barWidth = 2.dp.toPx()
                    val barSpacing = 12.dp.toPx()
                    val step = barWidth + barSpacing
                    val centerBarHeight = 120.dp.toPx()
                    val edgeBarHeight = 4.dp.toPx()

                    val currentOffset = totalOffset.value
                    val startI = ((-centerX - currentOffset) / step).toInt() - 4
                    val endI = ((size.width - centerX - currentOffset) / step).toInt() + 4
                    
                    for (i in startI..endI) {
                        val x = centerX + (i * step) + currentOffset
                        val distanceFromCenter = abs(x - centerX)
                        val maxDistance = (centerX * 0.9f)
                        
                        val normalizedDistance = (distanceFromCenter / maxDistance).coerceIn(0f, 1f)
                        val heightFactor = (1f - normalizedDistance).pow(2.2f) 
                        val barHeight = edgeBarHeight + (centerBarHeight - edgeBarHeight) * heightFactor

                        if (x > -20f && x < size.width + 20f) {
                            // Shadow
                            drawIntoCanvas { canvas ->
                                val shadowPaint = android.graphics.Paint().apply {
                                    isAntiAlias = true
                                    color = android.graphics.Color.BLACK
                                    alpha = (0.22f * heightFactor * 255).toInt()
                                    maskFilter = BlurMaskFilter(if (distanceFromCenter < 2.dp.toPx()) 6.dp.toPx() else 4.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                                }
                                canvas.nativeCanvas.drawRoundRect(
                                    x - barWidth / 2,
                                    centerY - barHeight / 2 + 2.dp.toPx(),
                                    x + barWidth / 2,
                                    centerY + barHeight / 2 + 8.dp.toPx(),
                                    barWidth / 2, barWidth / 2,
                                    shadowPaint
                                )
                            }

                            // Gradient Needle
                            val topColor = if (distanceFromCenter < step) Color.Black else Color(0xFF212121).copy(alpha = heightFactor)
                            
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        topColor,
                                        Color(0xFFF3F4F6).copy(alpha = 0.05f * heightFactor)
                                    ),
                                    startY = centerY - barHeight / 2,
                                    endY = centerY + barHeight / 2
                                ),
                                topLeft = Offset(x - barWidth / 2, centerY - barHeight / 2),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                            )
                        }
                    }
                }

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
                    .padding(top = 40.dp)
                    .width(358.dp)
                    .height(68.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(34.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.47f),
                        spotColor = Color.Black.copy(alpha = 0.47f)
                    )
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF373737), Color.Black)
                        ),
                        shape = RoundedCornerShape(34.dp)
                    )
                    .border(
                        BorderStroke(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF434343), Color.Black)
                            )
                        ),
                        shape = RoundedCornerShape(34.dp)
                    )
                    .neomorphicInnerShadow(
                        shape = RoundedCornerShape(34.dp),
                        color = Color.White.copy(alpha = 0.29f),
                        blur = 6.8.dp,
                        offsetX = 4.dp,
                        offsetY = 4.dp
                    )
                    .clickable { 
                        onTimeSelected(currentDisplayTime)
                        onDismiss() 
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lock it in",
                    style = TextStyle(
                        fontFamily = SatoshiFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        letterSpacing = (-0.48).sp
                    )
                )
            }
        }
    }
}
