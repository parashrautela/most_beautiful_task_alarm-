package com.example.myapplication

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import java.util.Calendar
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
private val CtaBgColor = Color(0xFFE5E7EB)          // Figma: bg #E5E7EB
private val CtaTextActive = Color(0xFF3A3A3A)       // "LOCK IT IN" text
private val CtaTextInactive = Color(0xFF9CA3AF)     // Figma: text #9CA3AF

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
private val SelectedBorderGreen = Color(0xFF00C896)
private val SelectedBorderRed   = Color(0xFFEF4444)
private val SelectedBorderBlue  = Color(0xFF2563EB)

private data class PriorityItem(
    val label: String,
    val gradient: Brush,
    val borderColor: Color,
    val index: Int
)

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
private val CtaHeight = 64.dp
private val CtaCornerRadius = 40.dp // Increased for a more premium, rounded feel
private val CtaCircleSize = 52.dp
private val CtaCircleOffset = 6.dp

// ─── Composable ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTaskSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Lifted State for Date and Time
    val initialCalendar = remember {
        Calendar.getInstance().apply {
            add(Calendar.MINUTE, 30)
        }
    }

    var selectedFullDate by remember { 
        mutableStateOf(LocalDate.now())
    }
    
    val selectedDay = selectedFullDate.dayOfMonth
    val selectedMonth = selectedFullDate.month.name.take(3).lowercase()
    val selectedYear = selectedFullDate.year

    var selectedFullTime by remember { 
        mutableStateOf(LocalTime.of(
            initialCalendar.get(Calendar.HOUR_OF_DAY),
            initialCalendar.get(Calendar.MINUTE)
        ))
    }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Main Sheet
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBg,
        shape = RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius),
        dragHandle = null,
        tonalElevation = 0.dp,
    ) {
        NewTaskSheetContent(
            selectedDay = selectedDay,
            selectedMonth = selectedMonth,
            selectedYear = selectedYear,
            selectedTime = selectedFullTime,
            onTimeSelected = { selectedFullTime = it },
            onShowDatePicker = { showDatePicker = true },
            onShowTimePicker = { showTimePicker = true },
            onDismiss = onDismiss
        )
    }

    // Date Picker Sheet (Sibling, not nested)
    if (showDatePicker) {
        DatePickerSheet(
            onDismiss = { showDatePicker = false },
            onDateSelected = { newDate ->
                selectedFullDate = newDate
            },
            initialDate = selectedFullDate
        )
    }

    // Time Picker Sheet (Sibling)
    if (showTimePicker) {
        TimePickerSheet(
            onDismiss = { showTimePicker = false },
            onTimeSelected = { newTime ->
                selectedFullTime = newTime
            },
            initialTime = selectedFullTime
        )
    }
}

@Composable
private fun NewTaskSheetContent(
    selectedDay: Int,
    selectedMonth: String,
    selectedYear: Int,
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onShowDatePicker: () -> Unit,
    onShowTimePicker: () -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableIntStateOf(0) }  // 0=Important, 1=Critical, 2=Flexible

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
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

        Spacer(modifier = Modifier.height(20.dp))

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

        Spacer(modifier = Modifier.height(28.dp))

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

        Spacer(modifier = Modifier.height(8.dp))

        // ── Title Input (large serif) ────────────────────────────────────
        BasicTextField(
            value = title,
            onValueChange = { if (it.length <= 80) title = it },
            textStyle = TextStyle(
                fontFamily = DentonFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = TitleInputSize,
                brush = Brush.verticalGradient(
                    0.0f to Color.White,
                    0.1f to Color.Black,
                    startY = 0f,
                    endY = 40f
                ),
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color(47, 47, 47, (0.34f * 255).toInt()),
                    offset = androidx.compose.ui.geometry.Offset(0f, 4f),
                    blurRadius = 2.5f
                ),
                letterSpacing = TitleInputTracking,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            decorationBox = { innerTextField ->
                Box {
                    if (title.isEmpty()) {
                        Box {
                            // Dark top-inner shadow
                            Text(
                                text = "Enter title",
                                fontFamily = DentonFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = TitleInputSize,
                                color = Color.Transparent,
                                style = TextStyle(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.25f),
                                        offset = androidx.compose.ui.geometry.Offset(0f, -2f),
                                        blurRadius = 1.5f
                                    )
                                ),
                                letterSpacing = TitleInputTracking,
                            )
                            // Base text
                            Text(
                                text = "Enter title",
                                fontFamily = DentonFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = TitleInputSize,
                                color = Color(0xFFB5B5B5),
                                letterSpacing = TitleInputTracking,
                            )
                        }
                    }
                    innerTextField()
                }
            },
        )

        Spacer(modifier = Modifier.height(32.dp))

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

        Spacer(modifier = Modifier.height(8.dp))

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
                    .background(if (description.isEmpty()) DescBorderEmpty else Color(0xFF3A3A3A), barShape)
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
                    color = Color(0xFF3A3A3A),
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
                                text = "I have to review some of the entries from the recent hiring drive and work accordingly in the dropping review in some of the clients works which the team have done",
                                fontFamily = SatoshiFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = DescriptionSize,
                                color = PlaceholderColor,
                                letterSpacing = DescriptionTracking,
                                lineHeight = 22.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── Select Date / Select Time (side by side, large serif) ────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Date Display
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onShowDatePicker() }
            ) {
                Text(
                    text = selectedDay.toString(),
                    fontFamily = DentonFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 48.sp,
                    color = Color.Black,
                    style = TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color(47, 47, 47, (0.34f * 255).toInt()),
                            offset = androidx.compose.ui.geometry.Offset(0f, 4f),
                            blurRadius = 2.5f
                        )
                    ),
                    letterSpacing = (-1.44).sp,
                    modifier = Modifier.alignByBaseline()
                )
                // Inner Row for Suffix + Icon
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "/$selectedMonth/$selectedYear",
                        fontFamily = DentonFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color(0xFF3A3A3A),
                        style = TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color(47, 47, 47, (0.34f * 255).toInt()),
                                offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                                blurRadius = 0.3f
                            )
                        ),
                        letterSpacing = (-0.42).sp,
                        modifier = Modifier.alignByBaseline()
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    // Figma Exported Edit Icon
                    Icon(
                        painter = painterResource(id = R.drawable.edit_icon),
                        contentDescription = "Edit Date",
                        modifier = Modifier
                            .size(26.dp)
                            .padding(bottom = 6.dp),
                        tint = Color.Unspecified
                    )
                }
            }

            // Time Display
            val timeFormatter = DateTimeFormatter.ofPattern("hh:mm", Locale.US)
            val amPm = selectedTime.format(DateTimeFormatter.ofPattern("a", Locale.US))
            val view = LocalView.current
            
            Row(
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = selectedTime.format(timeFormatter),
                    fontFamily = DentonFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 48.sp,
                    style = TextStyle(
                        brush = Brush.verticalGradient(
                            0.0f to Color.White,
                            0.1f to Color.Black,
                            startY = 0f,
                            endY = 60f
                        ),
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color(47, 47, 47, (0.34f * 255).toInt()),
                            offset = androidx.compose.ui.geometry.Offset(0f, 4f),
                            blurRadius = 2.5f
                        )
                    ),
                    letterSpacing = (-1.44).sp,
                    modifier = Modifier
                        .alignByBaseline()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onShowTimePicker() }
                )
                // Inner Row for Suffix + Icon
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    Text(
                        text = " $amPm".lowercase(),
                        fontFamily = DentonFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = Color.Black,
                        style = TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color(47, 47, 47, (0.34f * 255).toInt()),
                                offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                                blurRadius = 0.3f
                            )
                        ),
                        letterSpacing = (-0.45).sp,
                        modifier = Modifier
                            .alignByBaseline()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                // Toggle AM/PM directly
                                onTimeSelected(selectedTime.plusHours(12))
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    // Figma Exported Edit Icon
                    Icon(
                        painter = painterResource(id = R.drawable.edit_icon),
                        contentDescription = "Edit Time",
                        modifier = Modifier
                            .size(26.dp)
                            .padding(bottom = 6.dp),
                        tint = Color.Unspecified
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

        // ── Priority Swatches ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val priorities = listOf(
                PriorityItem("Important", ImportantGradient, SelectedBorderGreen, 0),
                PriorityItem("Critical", CriticalGradient, SelectedBorderRed, 1),
                PriorityItem("Flexible", FlexibleGradient, SelectedBorderBlue, 2),
            )

            priorities.forEach { item ->
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { selectedPriority = item.index },
                ) {
                    val isSelected = selectedPriority == item.index
                    val swatchShape = RoundedCornerShape(0.dp)
                    
                    // Outer Frame for Selected State
                    Box(
                        modifier = Modifier
                            .size(SwatchSize)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 1.dp,
                                        brush = item.gradient,
                                        shape = swatchShape
                                      )
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // The Swatch (Nested if selected)
                        Box(
                            modifier = Modifier
                                .then(
                                    if (isSelected) Modifier.padding(4.dp).fillMaxSize()
                                    else Modifier.size(SwatchSize)
                                )
                                .background(item.gradient)
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
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = item.label,
                        fontFamily = SatoshiFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = LabelSize,
                        color = PriorityLabelColor,
                        letterSpacing = LabelTracking,
                        modifier = Modifier
                            .offset(x = 2.dp, y = 2.dp)
                            .padding(bottom = 0.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // ── Slide-to-Set CTA ─────────────────────────────────────────────
        val context = androidx.compose.ui.platform.LocalContext.current
        SlideToSetButton(
            onSlideComplete = {
                val alarmDateTime = java.time.LocalDateTime.of(
                    java.time.LocalDate.of(selectedYear, java.time.Month.valueOf(selectedMonth.uppercase()), selectedDay),
                    selectedTime
                )
                val task = TaskAlarm(
                    title = if (title.isEmpty()) "Task Alarm" else title,
                    description = if (description.isEmpty()) "Time to get things done!" else description,
                    dateTime = alarmDateTime.toString(),
                    priority = selectedPriority
                )
                TaskStorage.saveTask(context, task)
                
                AlarmScheduler.scheduleAlarm(
                    context = context,
                    time = alarmDateTime,
                    title = task.title,
                    description = task.description,
                    taskId = task.id
                )
                onDismiss()
            },
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp),
        )
    }
}

// ─── Laurel Wreath Painter ──────────────────────────────────────────────────

@Composable
private fun LaurelWreath(isLeft: Boolean) {
    Canvas(modifier = Modifier.size(width = 20.dp, height = 32.dp)) {
        val color = Color(0xFFD4AF37) // Gold
        val strokeWidth = 1.2.dp.toPx()
        
        // Main stem
        val stemPath = Path().apply {
            if (isLeft) {
                moveTo(size.width * 0.9f, size.height * 0.9f)
                quadraticTo(
                    size.width * 0.1f, size.height * 0.5f,
                    size.width * 0.9f, size.height * 0.1f
                )
            } else {
                moveTo(size.width * 0.1f, size.height * 0.9f)
                quadraticTo(
                    size.width * 0.9f, size.height * 0.5f,
                    size.width * 0.1f, size.height * 0.1f
                )
            }
        }
        drawPath(stemPath, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
        
        // Leaf paths
        val leafCount = 6
        for (i in 0 until leafCount) {
            val fraction = i.toFloat() / (leafCount - 1)
            val y = size.height * (0.85f - fraction * 0.75f)
            
            // Calculate point on stem curve
            val t = 0.9f - fraction * 0.8f
            val stemX = if (isLeft) {
                // Quadratic bezier: (1-t)^2*P0 + 2(1-t)t*P1 + t^2*P2
                (1-t)*(1-t)*(size.width * 0.9f) + 2*(1-t)*t*(size.width * 0.1f) + t*t*(size.width * 0.9f)
            } else {
                (1-t)*(1-t)*(size.width * 0.1f) + 2*(1-t)*t*(size.width * 0.9f) + t*t*(size.width * 0.1f)
            }
            
            val leafWidth = 6.dp.toPx()
            val leafHeight = 3.dp.toPx()
            val angle = if (isLeft) -30f else 30f
            
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.translate(stemX, y)
                canvas.rotate(angle)
                
                val leafPath = Path().apply {
                    moveTo(0f, 0f)
                    quadraticTo(leafWidth/2, -leafHeight, leafWidth, 0f)
                    quadraticTo(leafWidth/2, leafHeight, 0f, 0f)
                }
                canvas.drawPath(leafPath, Paint().apply { 
                    this.color = color 
                    this.isAntiAlias = true
                })
                canvas.restore()
            }
        }
    }
}

// ─── Slide-to-Set Button ────────────────────────────────────────────────────

@Composable
fun SlideToSetButton(
    onSlideComplete: () -> Unit,
    modifier: Modifier = Modifier,
    idleText: String = "SLIDE TO SET",
    successText: String = "\u201c Lets make it count \u201d",
    useSoftDepth: Boolean = false,
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

    var isComplete by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var lastMilestone by remember { mutableIntStateOf(0) }
    var travelSinceLastBuzz by remember { mutableFloatStateOf(0f) }

    val offsetAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Handle dismissal delay after completion
    LaunchedEffect(isComplete) {
        if (isComplete) {
            kotlinx.coroutines.delay(800)
            onSlideComplete()
        }
    }

    val draggableState = rememberDraggableState { delta ->
        if (maxOffset > 0f && !isComplete) {
            val oldOffset = offsetAnim.value
            val newOffset = (oldOffset + delta).coerceIn(0f, maxOffset)
            scope.launch { offsetAnim.snapTo(newOffset) }
            val moved = kotlin.math.abs(newOffset - oldOffset)

            travelSinceLastBuzz += moved
            if (travelSinceLastBuzz >= 8f) {
                travelSinceLastBuzz = 0f
                vibrate(5L)
            }

            val milestone = ((newOffset / maxOffset) * 4).toInt().coerceIn(0, 4)
            if (milestone > lastMilestone) {
                lastMilestone = milestone
                vibrate(30L)
            }
            if (milestone < lastMilestone) {
                lastMilestone = milestone
            }
        }
    }

    // ── Background Gradient ──
    val defaultBg = Brush.verticalGradient(listOf(CtaBgColor, CtaBgColor))
    val successBg = Color.Black // Pure black for "fully black" finish

    val ctaShape = RoundedCornerShape(CtaCornerRadius)
    val depthModifier = if (useSoftDepth) {
        Modifier
            .shadow(
                elevation = 15.dp,
                shape = ctaShape,
                ambientColor = Color(0xFF717171).copy(alpha = 0.36f),
                spotColor = Color(0xFF717171).copy(alpha = 0.36f)
            )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CtaHeight)
            .onGloballyPositioned { pillWidthPx = it.size.width.toFloat() }
            .then(depthModifier)
            .clip(ctaShape)
            .background(CtaBgColor)
            .then(
                if (useSoftDepth) {
                    Modifier.innerShadow(
                        shape = ctaShape,
                        color = Color.White.copy(alpha = 0.16f),
                        blur = 4.dp,
                        offsetX = (-2).dp,
                        offsetY = (-2).dp
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // 1. Default "SLIDE TO SET" Text (Centered in full pill)
        Text(
            text = idleText,
            modifier = Modifier.align(Alignment.Center),
            fontFamily = SatoshiFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = CtaTextSize,
            letterSpacing = 2.sp,
            color = CtaTextInactive
        )

        // 2. Success Layer (Refined: No evidence at start + Interactive Blur)
        val revealProgress = (offsetAnim.value / (maxOffset.coerceAtLeast(1f))).coerceIn(0f, 1f)
        val thumbCenterX = offsetAnim.value + circleTotalPx / 2
        // Feather width tightened for more precision
        val featherWidth = with(density) { (20 + 20 * revealProgress).dp.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Overall alpha fades in to ensure "no evidence" at the absolute start
                    alpha = (revealProgress / 0.1f).coerceIn(0f, 1f)
                }
                .drawWithContent {
                    // 1:1 Physical Reveal with a subtle "Final Push"
                    // We maintain 1:1 speed for 92% of the drag, then gently accelerate 
                    // at the very end to swallow any feathered "white space" artifacts.
                    val baseRadius = thumbCenterX + circleTotalPx / 2
                    val finalRadius = if (revealProgress > 0.92f) {
                        val endPush = (revealProgress - 0.92f) / 0.08f
                        androidx.compose.ui.util.lerp(baseRadius, size.width * 1.4f, endPush)
                    } else {
                        baseRadius
                    }.coerceAtLeast(1f)

                    val maskBrush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.90f to Color.Black,
                            1.0f to Color.Transparent
                        ),
                        center = Offset(0f, size.height / 2),
                        radius = finalRadius
                    )

                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply { this.isAntiAlias = true }
                        val rect = androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height)
                        canvas.saveLayer(rect, paint)
                        drawContent()
                        // Use DstIn to mask the content with our curved expansion
                        drawRect(maskBrush, blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
                        canvas.restore()
                    }
                }
        ) {
            // Dark Background with Inner Shadow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(successBg)
                    .innerShadow(
                        shape = RoundedCornerShape(CtaCornerRadius),
                        color = Color(182, 182, 182).copy(alpha = 0.31f),
                        blur = 4.dp,
                        offsetX = 4.dp,
                    offsetY = 4.dp
                    )
            )

            // "Lets make it count" Text & Gold Laurel Wreaths
            // The blur here is now "interactive" - it softens as you move
            val interactiveBlur = (1.5 * (1f - revealProgress)).dp 
            
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.leaf_left),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 28.dp, height = 48.dp)
                        .blur(interactiveBlur.coerceAtLeast(0.1.dp)),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = successText,
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = CtaTextSize,
                    color = Color.White,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.blur(interactiveBlur.coerceAtLeast(0.1.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.leaf_right),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 28.dp, height = 48.dp)
                        .blur(interactiveBlur.coerceAtLeast(0.1.dp)),
                    tint = Color.Unspecified
                )
            }
        }

        // 3. Visual Thumb + Draggable Logic
        // Gradual color transition based on revealProgress
        // We start the transition after 20% drag and finish at 90% for a smooth, natural feel
        val transitionProgress = ((revealProgress - 0.2f) / 0.7f).coerceIn(0f, 1f)
        
        // ── Thumb Styling ──
        // Background Gradient: Interpolate between white and a premium dark gradient
        val thumbStartColor = androidx.compose.ui.graphics.lerp(Color.White, Color(0xFF3E3E3E), transitionProgress)
        val thumbEndColor = androidx.compose.ui.graphics.lerp(Color.White, Color(0xFF1B1B1B), transitionProgress)
        val thumbBrush = Brush.verticalGradient(listOf(thumbStartColor, thumbEndColor))

        // Border: A subtle highlight stroke that appears as we reach success
        val borderAlpha = transitionProgress
        val borderBrush = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.3f * borderAlpha),
                Color.Black.copy(alpha = 0.1f * borderAlpha)
            )
        )
        
        val thumbIconColor = androidx.compose.ui.graphics.lerp(
            Color.Black, 
            Color.White, 
            transitionProgress
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetAnim.value.roundToInt(), 0) }
                .padding(start = CtaCircleOffset, top = CtaCircleOffset, bottom = CtaCircleOffset)
                .size(CtaCircleSize)
                // Sharper shadow to prevent "weird blur" look
                .shadow(
                    elevation = 4.dp, 
                    shape = CircleShape,
                    spotColor = Color.Black.copy(alpha = 0.4f)
                )
                .background(thumbBrush, CircleShape)
                .border(BorderStroke(1.dp, borderBrush), CircleShape)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = draggableState,
                    onDragStarted = {
                        isDragging = true
                        travelSinceLastBuzz = 0f
                    },
                    onDragStopped = {
                        isDragging = false
                        if (maxOffset > 0f && offsetAnim.value > maxOffset * 0.8f) {
                            vibrate(100L)
                            isComplete = true
                            scope.launch { offsetAnim.animateTo(maxOffset, tween(200)) }
                        } else {
                            scope.launch { 
                                isComplete = false
                                offsetAnim.animateTo(0f, tween(300)) 
                            }
                            lastMilestone = 0
                            travelSinceLastBuzz = 0f
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Gradual Icon Transition (Interpolated Alpha)
            // Starts fading chevron at 70% and finishes tick by 90%
            // ── Transform Animation (Next -> Tick) ──
            // We use scale and rotation to make it feel like a transformation
            val nextIconAlpha = (1f - (revealProgress - 0.7f) / 0.15f).coerceIn(0f, 1f)
            val tickIconAlpha = ((revealProgress - 0.75f) / 0.2f).coerceIn(0f, 1f)
            
            val nextScale = if (revealProgress < 0.7f) 1f else (1f - (revealProgress - 0.7f) * 2f).coerceIn(0.4f, 1f)
            val nextRotate = if (revealProgress < 0.7f) 0f else (revealProgress - 0.7f) * 90f
            
            val tickScale = if (revealProgress < 0.75f) 0.4f else ((revealProgress - 0.75f) * 4f + 0.4f).coerceIn(0.4f, 1f)
            val tickRotate = if (revealProgress < 0.75f) -45f else (1f - (revealProgress - 0.75f) * 4f) * -45f

            if (nextIconAlpha > 0f) {
                Icon(
                    painter = painterResource(id = R.drawable.next_icon),
                    contentDescription = "Slide to Set",
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { 
                            alpha = nextIconAlpha 
                            scaleX = nextScale
                            scaleY = nextScale
                            rotationZ = nextRotate
                        },
                    tint = thumbIconColor
                )
            }
            
            if (tickIconAlpha > 0f) {
                Icon(
                    painter = painterResource(id = R.drawable.tick_icon), 
                    contentDescription = "Complete",
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer { 
                            alpha = tickIconAlpha 
                            scaleX = tickScale
                            scaleY = tickScale
                            rotationZ = tickRotate
                        },
                    tint = thumbIconColor
                )
            }
        }
    }
}
