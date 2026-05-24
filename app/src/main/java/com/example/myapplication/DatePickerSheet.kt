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
import java.time.LocalDate
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
fun DatePickerSheet(
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    initialDate: LocalDate = LocalDate.now(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Waveform scroll logic with momentum
    val totalOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val velocityTracker = remember { VelocityTracker() }
    
    // Unified step based on barWidth (2dp) + barSpacing (12dp)
    val dayStepPx = with(LocalDensity.current) { 14.dp.toPx() }
    
    // For visual display in the sheet
    var currentDisplayDate by remember { mutableStateOf(initialDate) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var lastSnappedIndex by remember { mutableStateOf(0) }

    // Capture the initial date once to avoid feedback loops
    val baseDate = remember { initialDate }
    
    // Sync display date and haptics with scroll offset
    LaunchedEffect(Unit) {
        snapshotFlow { totalOffset.value }
            .collect { offset ->
                val currentIndex = (offset / dayStepPx).roundToInt()
                if (currentIndex != lastSnappedIndex) {
                    lastSnappedIndex = currentIndex
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    
                    val newDate = baseDate.plusDays((-currentIndex).toLong())
                    if (newDate != currentDisplayDate) {
                        currentDisplayDate = newDate
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
                text = "Set your date",
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
                text = "Scroll to set the date.",
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

            // 3. DATE DISPLAY
            Column(
                modifier = Modifier.padding(top = 44.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentDisplayDate.dayOfMonth.toString(),
                    style = TextStyle(
                        fontFamily = DentonFontFamily,
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        letterSpacing = (-1.44).sp,
                        shadow = Shadow(
                            color = Color(47, 47, 47, (0.34f * 255).toInt()),
                            offset = Offset(0f, 4f),
                            blurRadius = 5f
                        )
                    )
                )
                // Subtext with shadow - moved closer
                Text(
                    text = "${currentDisplayDate.month.name.take(3).lowercase()}/${currentDisplayDate.year}",
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
                text = "\u201c this is going to be the best day lets make it count\u201d",
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
                    .width(163.dp)
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
                                        // Fling with decay
                                        totalOffset.animateDecay(velocity, exponentialDecay())
                                    }
                                    // Always snap to the nearest pillar after movement
                                    val finalSnappedOffset = (totalOffset.value / dayStepPx).roundToInt() * dayStepPx
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
                // ── Tension Lines & Needles ──
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2 + 10.dp.toPx()
                    val barWidth = 2.dp.toPx()
                    val barSpacing = 12.dp.toPx() // Tighter spacing for "drum" density
                    val step = barWidth + barSpacing
                    val centerBarHeight = 120.dp.toPx()
                    val edgeBarHeight = 4.dp.toPx() // Even shorter edges

                    // 1. Draw Corner Vectors (Using exported PNGs)
                    // (Corner images are now placed as Composables in the Box below for better alignment)

                    // 2. Draw Needles
                    val currentOffset = totalOffset.value
                    val startI = ((-centerX - currentOffset) / step).toInt() - 4
                    val endI = ((size.width - centerX - currentOffset) / step).toInt() + 4
                    
                    for (i in startI..endI) {
                        val x = centerX + (i * step) + currentOffset
                        val distanceFromCenter = abs(x - centerX)
                        val maxDistance = (centerX * 0.9f) // Reveal more across the drum width
                        
                        val normalizedDistance = (distanceFromCenter / maxDistance).coerceIn(0f, 1f)
                        // Slightly wider falloff for the "drum" reveal
                        val heightFactor = (1f - normalizedDistance).pow(2.2f) 
                        val barHeight = edgeBarHeight + (centerBarHeight - edgeBarHeight) * heightFactor

                        if (x > -20f && x < size.width + 20f) {
                            // 2a. Dynamic Needle Shadow (Stronger at center)
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

                            // 2b. Dynamic Needle Gradient (Black center, Gray edges)
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

                // ── Framing Vectors (Corner & Side Ornaments from Figma) ──
                // ── Framing Vectors (Corner Ornaments precisely aligned to drum) ──
                // Top Left
                Image(
                    painter = painterResource(id = R.drawable.topleft),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 40.dp)
                        .offset(y = 28.dp)
                        .size(width = 81.dp, height = 50.dp)
                )
                // Top Right
                Image(
                    painter = painterResource(id = R.drawable.top_right),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 40.dp)
                        .offset(y = 28.dp)
                        .size(width = 81.dp, height = 50.dp)
                )
                // Bottom Left
                Image(
                    painter = painterResource(id = R.drawable.leftbottom),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 40.dp)
                        .offset(y = (-14).dp)
                        .size(width = 81.dp, height = 50.dp)
                )
                // Bottom Right
                Image(
                    painter = painterResource(id = R.drawable.rrightbottom),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 40.dp)
                        .offset(y = (-14).dp)
                        .size(width = 81.dp, height = 50.dp)
                )

                // ── Center Indicator Triangle (Premium Lock with Shadow) ──
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
                    
                    // Triangle Shadow
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
                        
                        drawIntoCanvas { canvas ->
                            canvas.drawPath(ambientPath, ambientPaint)
                            canvas.drawPath(tightPath, tightPaint)
                            canvas.drawPath(glowPath, glowPaint)
                        }
                    }
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF3C3C3C), // Smooth top highlight gray
                                Color(0xFF1E1E1E), // Soft midtone
                                Color(0xFF080808)  // Pure deep black at bottom
                            )
                        ),
                        shape = RoundedCornerShape(34.dp)
                    )
                    // Stacked Inset Shadow 1: Subtle top highlight simulating soft overhead lighting
                    .neomorphicInnerShadow(
                        shape = RoundedCornerShape(34.dp),
                        color = Color.White.copy(alpha = 0.10f),
                        blur = 4.dp,
                        offsetX = 0.dp,
                        offsetY = 3.dp
                    )
                    // Stacked Inset Shadow 2: Premium 3D diagonal light reflection
                    .neomorphicInnerShadow(
                        shape = RoundedCornerShape(34.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        blur = 6.8.dp,
                        offsetX = 4.dp,
                        offsetY = 4.dp
                    )
                    .border(
                        BorderStroke(1.dp, Color.Black),
                        shape = RoundedCornerShape(34.dp)
                    )
                    .clip(RoundedCornerShape(34.dp))
                    .clickable { 
                        onDateSelected(currentDisplayDate)
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
