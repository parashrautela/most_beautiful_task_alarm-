package com.example.myapplication

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

// ─── Custom Inner Shadow Modifier ───────────────────────────────────────────
fun Modifier.innerShadow(
    shape: androidx.compose.ui.graphics.Shape,
    color: Color,
    blur: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
    offsetX: androidx.compose.ui.unit.Dp,
    spread: androidx.compose.ui.unit.Dp = 0.dp
) = this.drawWithContent {
    drawContent()
    val size = this.size
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply {
        when (outline) {
            is androidx.compose.ui.graphics.Outline.Rectangle -> addRect(outline.rect)
            is androidx.compose.ui.graphics.Outline.Rounded -> addRoundRect(outline.roundRect)
            is androidx.compose.ui.graphics.Outline.Generic -> addPath(outline.path)
        }
    }
    
    clipPath(path) {
        val paint = Paint()
        paint.color = color
        val frameworkPaint = paint.asFrameworkPaint()
        if (blur.toPx() > 0f) {
            frameworkPaint.maskFilter = android.graphics.BlurMaskFilter(
                blur.toPx(), 
                android.graphics.BlurMaskFilter.Blur.NORMAL
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

// ─── Custom Fonts ───────────────────────────────────────────────────────────
private val DentonFontFamily = FontFamily(
    Font(R.font.denton_test_medium, FontWeight.Medium)
)

private val SatoshiFontFamily = FontFamily(
    Font(R.font.satoshi_medium, FontWeight.Medium)
)

// ─── Figma Design Tokens ────────────────────────────────────────────────────

// Colors (exact hex from Figma)
private val SheetBg = Color.White
private val HeadingColor = Color(0xFF3A3A3A)       // "New Task" heading
private val LabelColor = Color.Black               // TITLE, DESCRIPTION, PRIORITY labels
private val PlaceholderColor = Color(0xFFB5B5B5)    // "Enter title", description, date/time
private val DescBorderFilled = Color(0xFF777777)    // Left border when description has text
private val DescBorderEmpty = Color(0xFFD9D9D9)     // Left border when description is empty
private val PriorityLabelColor = Color(0xFF3A3A3A)  // Important, Critical, Flexible text
private val CtaBgColor = Color(0xFFD0D0D0)          // Slide-to-set pill background
private val CtaTextActive = Color(0xFF3A3A3A)       // "LOCK IT IN" text
private val CtaTextInactive = Color(0xFFB4B4B4)     // "SLIDE TO SET" text

// Priority swatch gradients (from Figma: gradient top → bottom)
private val ImportantGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF00E6AC), Color(0xFF005A43)),
    startY = 0f,
    endY = Float.POSITIVE_INFINITY,
)
private val CriticalGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFEA5457), Color(0xFF440708)),
    startY = 0f,
    endY = Float.POSITIVE_INFINITY,
)
private val FlexibleGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF00A9E6), Color(0xFF00105A)),
    startY = 0f,
    endY = Float.POSITIVE_INFINITY,
)

// Selection border
private val SelectedBorder = Color(0xFF00E6AC)
private val SwatchBorder = Color(0xFF01634A)

// ─── Typography tokens ──────────────────────────────────────────────────────
// Figma uses "Satoshi Variable Medium" → closest system sans-serif
// Figma uses "Denton Test Medium" for title input → serif italic-like

// Font sizes from Figma
private val HeadingSize = 32.sp           // "New Task"
private val LabelSize = 12.sp            // TITLE, DESCRIPTION, PRIORITY, swatch labels
private val TitleInputSize = 48.sp       // "Enter title" placeholder
private val DescriptionSize = 16.sp      // Description text
private val DateTimeSize = 32.sp         // "Select date" / "Select Time"
private val CtaTextSize = 12.sp          // "LOCK IT IN" / "SLIDE TO SET"

// Letter spacing from Figma
private val HeadingTracking = (-0.96).sp
private val LabelTracking = (-0.36).sp
private val TitleInputTracking = (-1.44).sp
private val DescriptionTracking = (-0.48).sp

// ─── Dimension tokens ───────────────────────────────────────────────────────
// Corner radius
private val SheetCornerRadius = 24.dp

// Drag handle
private val DragHandleWidth = 48.dp
private val DragHandleHeight = 4.dp

// Description left bar
private val DescBarWidth = 4.dp
private val DescBarHeight = 100.dp

// Priority swatch size
private val SwatchSize = 44.dp

// CTA pill
private val CtaHeight = 68.dp
private val CtaCornerRadius = 32.dp
private val CtaCircleSize = 60.dp
private val CtaCircleOffset = 4.dp

// ─── Composable ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTaskSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBg,
        shape = RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius),
        dragHandle = null,      // we draw our own
        tonalElevation = 0.dp,
    ) {
        NewTaskSheetContent(onDismiss = onDismiss)
    }
}

@Composable
private fun NewTaskSheetContent(onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableIntStateOf(0) }  // 0=Important, 1=Critical, 2=Flexible

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 22.dp),
    ) {
        // ── Drag Handle ──────────────────────────────────────────────────
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(DragHandleWidth)
                .height(DragHandleHeight)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFD0D0D0)),
        )

        Spacer(modifier = Modifier.height(27.dp))

        // ── "New Task" Heading ───────────────────────────────────────────
        Text(
            text = "New Task",
            fontFamily = SatoshiFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = HeadingSize,
            color = HeadingColor,
            letterSpacing = HeadingTracking,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        Spacer(modifier = Modifier.height(50.dp))

        // ── TITLE Label ──────────────────────────────────────────────────
        Text(
            text = "TITLE",
            fontFamily = SatoshiFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = LabelSize,
            color = LabelColor,
            letterSpacing = LabelTracking,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Title Input (large serif) ────────────────────────────────────
        BasicTextField(
            value = title,
            onValueChange = { if (it.length <= 80) title = it },
            textStyle = TextStyle(
                fontFamily = DentonFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = TitleInputSize,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF555555), Color(0xFF999999))
                ),
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.White,
                    offset = androidx.compose.ui.geometry.Offset(0f, 3f),
                    blurRadius = 1f
                ),
                letterSpacing = TitleInputTracking,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box {
                    if (title.isEmpty()) {
                        // Figma: #b5b5b5 with inner shadow inset letterpress trick
                        Text(
                            text = "Enter title",
                            fontFamily = DentonFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = TitleInputSize,
                            style = TextStyle(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF888888), Color(0xFFCCCCCC))
                                ),
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color.White,
                                    offset = androidx.compose.ui.geometry.Offset(0f, 4f),
                                    blurRadius = 1f
                                )
                            ),
                            letterSpacing = TitleInputTracking,
                        )
                    }
                    innerTextField()
                }
            },
        )

        Spacer(modifier = Modifier.height(40.dp))

        // ── DESCRIPTION Label ────────────────────────────────────────────
        Text(
            text = "DESCRIPTION",
            fontFamily = SatoshiFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = LabelSize,
            color = LabelColor,
            letterSpacing = LabelTracking,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Description Input with left accent bar ───────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
        ) {
            val barShape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 0.dp, bottomEnd = 0.dp)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(100.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = barShape,
                        spotColor = Color.Black.copy(alpha = 0.25f),
                        clip = false,
                    )
                    .background(Color(0xFF777777), barShape)
                    .innerShadow(
                        shape = barShape,
                        color = Color.White.copy(alpha = 0.45f),
                        blur = 1.1.dp,
                        offsetX = 1.dp,
                        offsetY = 1.dp
                    )
            )

            Spacer(modifier = Modifier.width(13.dp))

            BasicTextField(
                value = description,
                onValueChange = { if (it.length <= 200) description = it },
                textStyle = TextStyle(
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = DescriptionSize,
                    color = PlaceholderColor,
                    letterSpacing = DescriptionTracking,
                    lineHeight = 22.sp,
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = DescBarHeight),
                decorationBox = { innerTextField ->
                    Box {
                        if (description.isEmpty()) {
                            Text(
                                text = "Description (optional)",
                                fontFamily = SatoshiFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = DescriptionSize,
                                color = PlaceholderColor,
                                letterSpacing = DescriptionTracking,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // ── Select Date / Select Time (side by side, large serif) ────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
        ) {
            Text(
                text = "Select date",
                fontFamily = SatoshiFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = DateTimeSize,
                color = PlaceholderColor,
                letterSpacing = HeadingTracking,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Select Time",
                fontFamily = SatoshiFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = DateTimeSize,
                color = PlaceholderColor,
                letterSpacing = HeadingTracking,
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // ── PRIORITY Label ───────────────────────────────────────────────
        Text(
            text = "PRIORITY",
            fontFamily = SatoshiFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = LabelSize,
            color = LabelColor,
            letterSpacing = LabelTracking,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Priority Swatches ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val priorities = listOf(
                Triple("Important", ImportantGradient, 0),
                Triple("Critical", CriticalGradient, 1),
                Triple("Flexible", FlexibleGradient, 2),
            )

            priorities.forEach { (label, gradient, index) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(80.dp)
                        .clickable { selectedPriority = index },
                ) {
                    val isSelected = selectedPriority == index
                    val swatchShape = RoundedCornerShape(0.dp)
                    Box(
                        modifier = Modifier
                            .size(SwatchSize)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = SelectedBorder,
                                        shape = swatchShape,
                                    )
                                } else {
                                    Modifier.border(
                                        width = 0.2.dp,
                                        color = SwatchBorder,
                                        shape = swatchShape,
                                    )
                                }
                            )
                            .background(gradient)
                            .innerShadow(
                                shape = swatchShape,
                                color = Color.Black.copy(alpha = 0.25f),
                                blur = 4.dp,
                                offsetX = 0.dp,
                                offsetY = (-2).dp
                            )
                            .innerShadow(
                                shape = swatchShape,
                                color = Color.White.copy(alpha = 0.8f),
                                blur = 1.9.dp,
                                offsetX = 1.dp,
                                offsetY = 1.dp
                            )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = label,
                        fontFamily = SatoshiFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = LabelSize,
                        color = PriorityLabelColor,
                        letterSpacing = LabelTracking,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // ── Slide-to-Set CTA ─────────────────────────────────────────────
        SlideToSetButton(
            onSlideComplete = onDismiss,
            modifier = Modifier
                .padding(horizontal = 22.dp),
        )
    }
}

// ─── Slide-to-Set Button ────────────────────────────────────────────────────


@Composable
private fun SlideToSetButton(
    onSlideComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val context = view.context
    val density = LocalDensity.current

    // Vibrator for guaranteed haptic motor activation
    val vibrator = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE)
                    as android.os.VibratorManager
            mgr.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE)
                    as android.os.Vibrator
        }
    }

    // Helper – vibrate with a given duration (ms)
    val vibrate: (Long) -> Unit = remember(vibrator) {
        { durationMs ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    android.os.VibrationEffect.createOneShot(
                        durationMs,
                        255, // Max amplitude
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    }

    // Calculate max offset dynamically from measured pill width
    val circleTotalDp = CtaCircleSize + CtaCircleOffset * 2  // 60 + 4*2 = 68dp
    val circleTotalPx = with(density) { circleTotalDp.toPx() }

    var pillWidthPx by remember { mutableFloatStateOf(0f) }
    val maxOffset by remember { derivedStateOf { (pillWidthPx - circleTotalPx).coerceAtLeast(0f) } }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var isComplete by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var lastMilestone by remember { mutableIntStateOf(0) }
    // Track cumulative travel for continuous buzz throttling
    var travelSinceLastBuzz by remember { mutableFloatStateOf(0f) }

    // Animate: during drag → instant follow; on release → spring back or snap to end
    val animatedOffset by animateFloatAsState(
        targetValue = when {
            isDragging -> offsetX        // follow finger instantly
            isComplete -> maxOffset      // snap to end
            else -> 0f                   // spring back to start
        },
        animationSpec = if (isDragging) {
            tween(durationMillis = 0)
        } else {
            tween(durationMillis = 300)
        },
        label = "slideOffset",
    )

    val draggableState = rememberDraggableState { delta ->
        if (maxOffset > 0f) {
            val oldOffset = offsetX
            offsetX = (offsetX + delta).coerceIn(0f, maxOffset)
            val moved = kotlin.math.abs(offsetX - oldOffset)

            // ── Continuous micro-vibration every ~8px of travel ──
            travelSinceLastBuzz += moved
            if (travelSinceLastBuzz >= 8f) {
                travelSinceLastBuzz = 0f
                vibrate(5L)   // very short 5ms buzz — feels like texture
            }

            // ── Stronger tick at each 25% milestone ──
            val milestone = ((offsetX / maxOffset) * 4).toInt().coerceIn(0, 4)
            if (milestone > lastMilestone) {
                lastMilestone = milestone
                vibrate(30L)   // noticeable 30ms tick at milestones
            }
            if (milestone < lastMilestone) {
                lastMilestone = milestone
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CtaHeight)
            .onGloballyPositioned { coordinates ->
                pillWidthPx = coordinates.size.width.toFloat()
            }
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(CtaCornerRadius),
                spotColor = Color(0xFF5A5A5A).copy(alpha = 0.25f),
                clip = false,
            )
            .background(CtaBgColor, RoundedCornerShape(CtaCornerRadius))
            .innerShadow(
                shape = RoundedCornerShape(CtaCornerRadius),
                color = Color(0xFF303030).copy(alpha = 0.25f),
                blur = 4.dp,
                offsetX = 1.dp,
                offsetY = 1.dp
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        // CTA Text centered
        Text(
            text = if (isComplete) "LOCK IT IN" else "SLIDE TO SET",
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = CtaTextSize,
            color = if (isComplete) CtaTextActive else CtaTextInactive,
            letterSpacing = LabelTracking,
            modifier = Modifier.align(Alignment.Center),
        )

        // Draggable circle with haptic feedback
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .padding(start = CtaCircleOffset, top = CtaCircleOffset, bottom = CtaCircleOffset)
                .size(CtaCircleSize)
                .shadow(elevation = 4.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = draggableState,
                    onDragStarted = {
                        isDragging = true
                        travelSinceLastBuzz = 0f
                    },
                    onDragStopped = {
                        isDragging = false
                        if (maxOffset > 0f && offsetX > maxOffset * 0.8f) {
                            // ── Slide complete ──
                            vibrate(100L)   // strong 100ms confirmation buzz
                            isComplete = true
                            offsetX = maxOffset
                            onSlideComplete()
                        } else {
                            // ── Snap back to start ──
                            offsetX = 0f
                            lastMilestone = 0
                            travelSinceLastBuzz = 0f
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "›",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = HeadingColor,
            )
        }
    }
}
