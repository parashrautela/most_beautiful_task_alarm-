package com.example.myapplication

import android.graphics.BlurMaskFilter
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// Context extension to find the Host Activity
private fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun WaveformPicker(
    initialIndex: Int,
    maxValue: Int,
    onIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemSpacingDp: Dp = 14.dp,
    barWidthDp: Dp = 2.dp,
    barGapDp: Dp = 12.dp,
    centerBarMaxHeightDp: Dp = 120.dp,
    minBarHeightDp: Dp = 4.dp
) {
    val density = LocalDensity.current
    val itemSpacing = with(density) { itemSpacingDp.toPx() }
    val barWidth = with(density) { barWidthDp.toPx() }
    val maxBarHeight = with(density) { centerBarMaxHeightDp.toPx() }
    val minBarHeight = with(density) { minBarHeightDp.toPx() }

    // STAGE 1 — ELIMINATE RECOMPOSITION FROM SCROLL STATE
    val scrollOffset = remember { Animatable(-initialIndex * itemSpacing) }
    val scope = rememberCoroutineScope()

    // STAGE 4 — ADD HAPTIC FEEDBACK TIED TO VALUE CHANGES
    val view = LocalView.current
    val previousSelectedValue = remember { mutableIntStateOf(initialIndex) }

    // STAGE 5 — REQUEST 120FPS FROM THE SYSTEM
    DisposableEffect(Unit) {
        val activity = view.context.findActivity()
        val window = activity?.window
        if (window != null) {
            val params = window.attributes
            val previousRefreshRate = params.preferredRefreshRate
            params.preferredRefreshRate = 120f
            window.attributes = params
            onDispose {
                params.preferredRefreshRate = previousRefreshRate
                window.attributes = params
            }
        } else {
            onDispose {}
        }
    }

    // STAGE 6 — LOW PASS FILTER TRACKER
    // Using a floatArrayOf(0f) reference to mutate inside events without causing recompositions
    val smoothedDeltaRef = remember { floatArrayOf(0f) }

    // STAGE 2 — REPLACE GESTURE DETECTOR WITH DIRECT POINTER INPUT
    val velocityTracker = remember { VelocityTracker() }

    // Synchronize parent updates with scrollOffset state
    LaunchedEffect(initialIndex) {
        val targetOffset = -initialIndex * itemSpacing
        if (scrollOffset.value != targetOffset) {
            scrollOffset.snapTo(targetOffset)
            previousSelectedValue.intValue = initialIndex
        }
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    velocityTracker.resetTracking()
                    smoothedDeltaRef[0] = 0f
                    
                    horizontalDrag(down.id) { change ->
                        val dragAmount = change.position.x - change.previousPosition.x
                        
                        // STAGE 6 — LOW PASS FILTER TO PREVENT JITTER
                        smoothedDeltaRef[0] = smoothedDeltaRef[0] * 0.6f + dragAmount * 0.4f
                        val smoothedDelta = smoothedDeltaRef[0]
                        
                        // STAGE 7 — CLAMP AND BOUND THE SCROLL RANGE WITH RUBBER BANDING
                        val minOffset = -(maxValue * itemSpacing)
                        val maxOffset = 0f
                        val proposedOffset = scrollOffset.value + smoothedDelta

                        val clampedOffset = when {
                            proposedOffset < minOffset -> minOffset + (proposedOffset - minOffset) * 0.3f
                            proposedOffset > maxOffset -> maxOffset + (proposedOffset - maxOffset) * 0.3f
                            else -> proposedOffset
                        }

                        scope.launch {
                            scrollOffset.snapTo(clampedOffset)
                        }

                        velocityTracker.addPosition(
                            change.uptimeMillis,
                            change.position
                        )

                        // STAGE 4 — HAPTIC TICK ON VALUE TRANSITIONS
                        val currentIndex = (-clampedOffset / itemSpacing).roundToInt().coerceIn(0, maxValue)
                        if (currentIndex != previousSelectedValue.intValue) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            previousSelectedValue.intValue = currentIndex
                            onIndexChanged(currentIndex)
                        }

                        change.consume()
                    }
                    
                    val velocity = velocityTracker.calculateVelocity().x
                    scope.launch {
                        val minOffset = -(maxValue * itemSpacing)
                        val maxOffset = 0f

                        // Momentum animation: only decay if we are within valid bounds
                        if (scrollOffset.value in minOffset..maxOffset) {
                            try {
                                scrollOffset.animateDecay(
                                    initialVelocity = velocity,
                                    animationSpec = exponentialDecay(
                                        frictionMultiplier = 2.2f,
                                        absVelocityThreshold = 0.1f
                                    )
                                ) {
                                    // Trigger haptics during momentum decay
                                    val currentIndex = (-value / itemSpacing).roundToInt().coerceIn(0, maxValue)
                                    if (currentIndex != previousSelectedValue.intValue) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        previousSelectedValue.intValue = currentIndex
                                        onIndexChanged(currentIndex)
                                    }
                                }
                            } catch (e: Exception) {
                                // Ignore animation cancellation
                            }
                        }
                        
                        // STAGE 7 — SNAP BACK TO NEAREST BOUNDARY/VALUE
                        val rawNearest = (-scrollOffset.value / itemSpacing).roundToInt()
                        val nearestIndex = rawNearest.coerceIn(0, maxValue)
                        val nearestValue = -nearestIndex * itemSpacing

                        scrollOffset.animateTo(
                            targetValue = nearestValue,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessHigh
                            )
                        )
                        
                        // Fire a slightly stronger haptic on snap completion
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        if (nearestIndex != previousSelectedValue.intValue) {
                            previousSelectedValue.intValue = nearestIndex
                            onIndexChanged(nearestIndex)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // STAGE 3 — REBUILD THE CANVAS DRAWING ON THE RENDER THREAD
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val centerY = size.height / 2f + 10.dp.toPx() // Restore original 10.dp shift
            
            // Total bars density
            val totalBars = 52
            val centerIndex = totalBars / 2
            
            // Sub-item shifting offset for infinite rendering effect
            val offsetShift = scrollOffset.value % itemSpacing

            // Draw all bars
            for (barIndex in 0 until totalBars) {
                val barX = (canvasWidth / 2f) + (barIndex - centerIndex) * itemSpacing + offsetShift
                
                // Culling offscreen bars
                if (barX < -barWidth || barX > canvasWidth + barWidth) continue
                
                val distanceFromCenter = abs(barX - (canvasWidth / 2f))
                val maxDistance = (canvasWidth / 2f) * 0.9f // Restore original maxDistance scaling
                val normalizedDistance = (distanceFromCenter / maxDistance).coerceIn(0f, 1f)
                
                // Restore original height factor calculation
                val heightFactor = (1f - normalizedDistance).pow(2.2f)
                val barHeight = minBarHeight + (maxBarHeight - minBarHeight) * heightFactor
                
                // Restore original shadow rendering
                drawIntoCanvas { canvas ->
                    val shadowPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.BLACK
                        alpha = (0.22f * heightFactor * 255).toInt()
                        maskFilter = BlurMaskFilter(
                            if (distanceFromCenter < itemSpacing) 6.dp.toPx() else 4.dp.toPx(),
                            BlurMaskFilter.Blur.NORMAL
                        )
                    }
                    canvas.nativeCanvas.drawRoundRect(
                        barX - barWidth / 2f,
                        centerY - barHeight / 2f + 2.dp.toPx(),
                        barX + barWidth / 2f,
                        centerY + barHeight / 2f + 8.dp.toPx(),
                        barWidth / 2f, barWidth / 2f,
                        shadowPaint
                    )
                }

                // Restore original vertical gradient needle color styling
                val topColor = if (distanceFromCenter < itemSpacing) Color.Black else Color(0xFF212121).copy(alpha = heightFactor)
                
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            topColor,
                            Color(0xFFF3F4F6).copy(alpha = 0.05f * heightFactor)
                        ),
                        startY = centerY - barHeight / 2f,
                        endY = centerY + barHeight / 2f
                    ),
                    topLeft = Offset(barX - barWidth / 2f, centerY - barHeight / 2f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
