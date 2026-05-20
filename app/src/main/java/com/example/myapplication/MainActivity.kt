package com.example.myapplication

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.time.LocalDateTime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlin.math.abs

// ─── Custom Fonts ───────────────────────────────────────────────────────────
private val DentonFontFamily = FontFamily(
    Font(R.font.denton_test_medium, FontWeight.Medium)
)

private val SatoshiFontFamily = FontFamily(
    Font(R.font.satoshi_medium, FontWeight.Medium)
)

private val GradBlue   = Color(0xFF2563EB)
private val GradOrange = Color(0xFFF97316)
private val GradRed    = Color(0xFFEF4444)
private val FabGreen   = Color(0xFF00C896)
private val CardBorder = Color(0xFFF2F2F2)       // Figma: border #F2F2F2
private val TextMain   = Color(0xFF000000)        // Figma: text-black
private val TextGray   = Color(0xFF6A7282)        // Figma: #6A7282
private val BarBg      = Color(0xFFFFE6E6)        // Figma: left gradient bar #FFE6E6

// Card gradient (Figma: bg-gradient-to-b from-white to-[#e6e6e6])
private val CardGradientTop = Color(0xFFFFFFFF)
private val CardGradientBottom = Color(0xFFE6E6E6)

// First card gradient (Figma: linear-gradient(177deg, #FFB984, #FFCFDF))
private val Card1GradientTop = Color(0xFFFFB984)
private val Card1GradientBottom = Color(0xFFFFCFDF)

private const val CARD_TILT_INTENSITY = 0.42f
private const val CARD_TILT_MAX_DEGREES = 10f
private const val CARD_TILT_RESPONSE = 0.18f
private val SignatureInkStart = Color(0xFF4DD9E8)
private val SignatureInkEnd = Color(0xFF2BA8B8)
private val CardTouchGlow = SignatureInkStart

class MainActivity : ComponentActivity() {
    private val activeSigningTaskId = mutableStateOf<String?>(null)

    private fun getActiveSigningTaskId(): String? {
        return getSharedPreferences("task_alarms_signing", Context.MODE_PRIVATE)
            .getString("active_signing_task_id", null)
    }

    private fun setActiveSigningTaskId(id: String?) {
        getSharedPreferences("task_alarms_signing", Context.MODE_PRIVATE)
            .edit()
            .putString("active_signing_task_id", id)
            .apply()
        activeSigningTaskId.value = id
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Request Notification Permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }

        handleIntent(intent)
        activeSigningTaskId.value = getActiveSigningTaskId()

        setContent {
            MyApplicationTheme {
                val currentTaskId = activeSigningTaskId.value
                if (currentTaskId != null) {
                    val taskList = TaskStorage.getTasks(this)
                    val task = taskList.find { it.id == currentTaskId }
                    
                    if (task != null) {
                        SigningScreen(
                            task = task,
                            onProceed = {
                                TaskStorage.deleteTask(this, task.id)
                                setActiveSigningTaskId(null)
                            }
                        )
                    } else {
                        // Fallback in case task was deleted but SharedPreferences still has ID
                        val fallbackTask = TaskAlarm(
                            id = currentTaskId,
                            title = intent?.getStringExtra("TASK_TITLE") ?: "Task Alarm",
                            description = intent?.getStringExtra("TASK_DESC") ?: "",
                            dateTime = LocalDateTime.now().toString(),
                            priority = 0
                        )
                        SigningScreen(
                            task = fallbackTask,
                            onProceed = {
                                TaskStorage.deleteTask(this, fallbackTask.id)
                                setActiveSigningTaskId(null)
                            }
                        )
                    }
                } else {
                    TaskAlarmHomeScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent != null && intent.action == "DISMISS_ALARM") {
            // Stop the alarm service
            stopService(Intent(this, AlarmService::class.java))
            
            val taskId = intent.getStringExtra("TASK_ID")
            if (!taskId.isNullOrEmpty()) {
                setActiveSigningTaskId(taskId)
            }
        }
    }
}

@Composable
fun TaskAlarmHomeScreen() {
    var showSheet by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<TaskAlarm?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var tasks by remember { mutableStateOf(TaskStorage.getTasks(context)) }

    // Refresh tasks when sheet is dismissed
    LaunchedEffect(showSheet) {
        if (!showSheet) {
            tasks = TaskStorage.getTasks(context)
        }
    }

    // Show detail screen when a task is tapped
    selectedTask?.let { task ->
        AlarmDetailScreen(
            task = task,
            onBack = { selectedTask = null },
            onComplete = {
                TaskStorage.deleteTask(context, task.id)
                tasks = TaskStorage.getTasks(context)
                selectedTask = null
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ── Left gradient bar (separate from cards, Figma: x=32, 28×244, #FFE6E6) ──
        Box(
            modifier = Modifier
                .padding(start = 32.dp, top = 142.dp)
                .width(28.dp)
                .height(244.dp)
                .background(BarBg, RoundedCornerShape(8.dp))
                .align(Alignment.TopStart)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp, end = 0.dp, top = 56.dp)
        ) {
            // ── Header ──
            Text(
                text = "Task Alarm",
                fontFamily = DentonFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                color = TextMain,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your tasks, your deadlines.",
                fontFamily = SatoshiFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = TextGray,
            )
            Spacer(modifier = Modifier.height(36.dp))

            // ── Cards (offset to the right, Figma: cards at x=84) ──
            Column(
                modifier = Modifier
                    .padding(start = 62.dp, end = 24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (tasks.isEmpty()) {
                    TaskCard(
                        title = "No Tasks",
                        subtitle = "Tap + to add your first alarm",
                        cardBrush = Brush.verticalGradient(
                            listOf(Card1GradientTop, Card1GradientBottom),
                        ),
                    )
                } else {
                    tasks.sortedBy { it.dateTime }.forEachIndexed { index, task ->
                        val ldt = java.time.LocalDateTime.parse(task.dateTime)
                        val formatter = java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", java.util.Locale.US)
                        val subtitle = ldt.format(formatter)

                        TaskCard(
                            title = task.title,
                            subtitle = subtitle,
                            cardBrush = if (index == 0) {
                                Brush.verticalGradient(listOf(Card1GradientTop, Card1GradientBottom))
                            } else {
                                Brush.verticalGradient(listOf(CardGradientTop, CardGradientBottom))
                            },
                            onClick = { selectedTask = task }
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(60.dp)
                // 1. Exact outer drop shadow from Figma: dy=4, stdDev=1.95, color=rgba(124,124,124,0.17)
                .drawBehind {
                    val d = this.density
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.TRANSPARENT
                            setShadowLayer(
                                1.95f * d, // blur
                                0f * d,    // dx
                                4f * d,    // dy
                                android.graphics.Color.argb((0.17f * 255).toInt(), 124, 124, 124)
                            )
                        }
                        canvas.nativeCanvas.drawCircle(size.width / 2, size.height / 2, size.width / 2, paint)
                    }
                }
                // 2. Background fill: white to #F1F2F3
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color(0xFFF1F2F3))
                    ),
                    shape = CircleShape
                )
                // 3. Border stroke: 0.8 width, linear gradient
                .border(
                    width = 0.8.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF969696), Color(0xFFF8F8F8).copy(alpha = 0.69f)),
                        start = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                        end = androidx.compose.ui.geometry.Offset(0f, 0f)
                    ),
                    shape = CircleShape
                )
                // 4. Inner shadow on the background: dx=4, dy=4, blur=2.5, spread=3, color=rgba(145,145,145,0.25)
                .innerShadow(
                    shape = CircleShape,
                    color = Color(145, 145, 145, (0.25f * 255).toInt()),
                    blur = 2.5.dp,
                    offsetX = 4.dp,
                    offsetY = 4.dp,
                    spread = 3.dp
                )
                .clip(CircleShape)
                .clickable { showSheet = true },
            contentAlignment = Alignment.Center
        ) {
            // 5. The exact Plus icon stroke
            androidx.compose.foundation.Canvas(modifier = Modifier.size(60.dp)) {
                val strokeWidth = 2.dp.toPx()
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(21f.dp.toPx(), 30f.dp.toPx())
                    lineTo(39f.dp.toPx(), 30f.dp.toPx())
                    moveTo(30f.dp.toPx(), 21f.dp.toPx())
                    lineTo(30f.dp.toPx(), 39f.dp.toPx())
                }
                
                // Base black stroke
                drawPath(
                    path = path,
                    color = Color.Black,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )
                // Tiny white inner shadow highlight (dx=0.4, dy=0.4, blur=0.25, white 0.43)
                val highlightOffset = 0.4f.dp.toPx()
                withTransform({
                    translate(highlightOffset, highlightOffset)
                }) {
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.43f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth * 0.5f,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )
                }
            }
        }
        if (showSheet) {
            NewTaskSheet(
                onDismiss = { showSheet = false }
            )
        }
    }
}

@Composable
private fun TaskCard(
    title: String,
    subtitle: String,
    cardBrush: Brush = Brush.verticalGradient(
        listOf(CardGradientTop, CardGradientBottom),
    ),
    onClick: () -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),                              // Figma: rounded-[24px]
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, CardBorder),                        // Figma: border #F2F2F2
        elevation = CardDefaults.cardElevation(0.dp),                   // NO elevation / shadow
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush, RoundedCornerShape(24.dp)),      // gradient fills card
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),     // Figma: text at x=24, y=28
            ) {
                Text(
                    text = title,
                    fontFamily = DentonFontFamily,                      // Figma: Denton Test Medium
                    fontWeight = FontWeight.Medium,
                    fontSize = 36.sp,                                   // Figma: text-[36px]
                    color = TextMain,                                   // Figma: text-black
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = subtitle,
                    fontFamily = FontFamily.SansSerif,                  // Figma: Satoshi Variable
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,                                   // Figma: text-[12px]
                    color = TextGray,                                   // Figma: #6A7282
                    lineHeight = 18.sp,                                 // Figma: leading-[18px]
                )
            }
        }
    }
}

// Data class for completed signature strokes
data class ColoredStroke(
    val points: List<Offset>
)

private fun android.graphics.Path.addSmoothedStroke(points: List<Offset>) {
    if (points.isEmpty()) return

    moveTo(points.first().x, points.first().y)
    if (points.size == 1) return
    if (points.size == 2) {
        lineTo(points.last().x, points.last().y)
        return
    }

    for (index in 1 until points.lastIndex) {
        val current = points[index]
        val next = points[index + 1]
        quadTo(
            current.x,
            current.y,
            (current.x + next.x) / 2f,
            (current.y + next.y) / 2f
        )
    }
    lineTo(points.last().x, points.last().y)
}

private fun signaturePaint(
    strokeWidthPx: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    density: Float
): android.graphics.Paint {
    return android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        shader = android.graphics.LinearGradient(
            0f,
            0f,
            canvasWidth,
            canvasHeight,
            intArrayOf(SignatureInkStart.toArgb(), SignatureInkEnd.toArgb()),
            null,
            android.graphics.Shader.TileMode.CLAMP
        )
        setShadowLayer(
            2.3f * density,
            0f,
            4f * density,
            android.graphics.Color.argb((0.33f * 255).toInt(), 0, 124, 140)
        )
    }
}

@Composable
fun SigningScreen(
    task: TaskAlarm,
    onProceed: () -> Unit
) {
    BackHandler(enabled = true) {
        // Do nothing - user must sign to unlock the app
    }

    val context = LocalContext.current
    var totalDrawingLength by remember { mutableStateOf(0f) }
    val currentStroke = remember { mutableStateOf<List<Offset>>(emptyList()) }
    val strokes = remember { mutableStateListOf<ColoredStroke>() }
    val localDensity = LocalDensity.current
    var isCardFlipped by remember { mutableStateOf(false) }

    val isThresholdMet = totalDrawingLength > 1000f

    // ─── Signature touch response ──────────────────────────────────────────
    val isCardTouched = currentStroke.value.isNotEmpty()
    val isSignatureComplete = isThresholdMet && !isCardTouched
    val cardTouchGlowProgress by animateFloatAsState(
        targetValue = if (isCardTouched) 1f else 0f,
        animationSpec = tween(durationMillis = if (isCardTouched) 140 else 420),
        label = "cardTouchGlow"
    )
    val cardFlipRotation by animateFloatAsState(
        targetValue = if (isCardFlipped) 180f else 0f,
        animationSpec = tween(
            durationMillis = 720,
            easing = FastOutSlowInEasing
        ),
        label = "cardFlipRotation"
    )
    val cardFlipDepth = 1f - (abs(cardFlipRotation - 90f) / 90f).coerceIn(0f, 1f)
    val isCardBackVisible = cardFlipRotation >= 90f

    // ─── Gyroscope parallax ────────────────────────────────────────────────
    var rotationX by remember { mutableStateOf(0f) }
    var rotationY by remember { mutableStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(
            Context.SENSOR_SERVICE
        ) as SensorManager

        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor == null) {
            return@DisposableEffect onDispose {}
        }

        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientationAngles = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)

                val pitchDegrees = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                val rollDegrees = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                val targetRotationX = (pitchDegrees * CARD_TILT_INTENSITY)
                    .coerceIn(-CARD_TILT_MAX_DEGREES, CARD_TILT_MAX_DEGREES)
                val targetRotationY = (-rollDegrees * CARD_TILT_INTENSITY)
                    .coerceIn(-CARD_TILT_MAX_DEGREES, CARD_TILT_MAX_DEGREES)

                rotationX += (targetRotationX - rotationX) * CARD_TILT_RESPONSE
                rotationY += (targetRotationY - rotationY) * CARD_TILT_RESPONSE
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_GAME
        )

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val animatedRotationX by animateFloatAsState(
        targetValue = rotationX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotationX"
    )
    val animatedRotationY by animateFloatAsState(
        targetValue = rotationY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotationY"
    )
    val signedStackProgress by animateFloatAsState(
        targetValue = if (isSignatureComplete) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "signedStackProgress"
    )
    val cardTiltX = animatedRotationX * (1f - signedStackProgress)
    val cardTiltY = animatedRotationY * (1f - signedStackProgress)
    val isActionEnabled = !isCardFlipped || isSignatureComplete

    // Animate button colors
    val buttonBgColor by animateColorAsState(
        targetValue = if (isActionEnabled) Color.Black else Color(0xFFE2E2E2),
        animationSpec = tween(durationMillis = 300),
        label = "buttonBgColor"
    )
    val buttonTextColor by animateColorAsState(
        targetValue = if (isActionEnabled) Color.White else Color(0xFF888888),
        animationSpec = tween(durationMillis = 300),
        label = "buttonTextColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(top = 60.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Task Title — left-aligned with padding
        Text(
            text = task.title,
            fontFamily = DentonFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 36.sp,
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // White signing card — centered, with flip, depth, and gyroscope parallax
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(456.dp)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            if (signedStackProgress > 0.01f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            translationX = 14.dp.toPx() * signedStackProgress
                            translationY = 18.dp.toPx() * signedStackProgress
                            scaleX = 0.96f + (0.02f * signedStackProgress)
                            scaleY = 0.96f + (0.02f * signedStackProgress)
                            rotationZ = 2.5f * signedStackProgress
                            alpha = 0.34f * signedStackProgress
                            cameraDistance = 12f * density
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFE9F8F8), RoundedCornerShape(24.dp))
                        .border(
                            BorderStroke(1.dp, CardTouchGlow.copy(alpha = 0.22f)),
                            RoundedCornerShape(24.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            translationX = 28.dp.toPx() * signedStackProgress
                            translationY = 32.dp.toPx() * signedStackProgress
                            scaleX = 0.92f + (0.02f * signedStackProgress)
                            scaleY = 0.92f + (0.02f * signedStackProgress)
                            rotationZ = 5f * signedStackProgress
                            alpha = 0.18f * signedStackProgress
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFF0F0F0), RoundedCornerShape(24.dp))
                        .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(24.dp))
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                // Parallax tilt from gyroscope
                .graphicsLayer {
                    this.rotationX = cardTiltX - (cardFlipDepth * 3f)
                    this.rotationY = cardTiltY + if (isCardBackVisible) {
                        cardFlipRotation - 180f
                    } else {
                        cardFlipRotation
                    }
                    val depthScale = 1f - (cardFlipDepth * 0.035f)
                    scaleX = depthScale + (cardTouchGlowProgress * 0.008f)
                    scaleY = depthScale + (cardTouchGlowProgress * 0.008f)
                    translationY = cardFlipDepth * 10.dp.toPx()
                    cameraDistance = 12f * density
                }
                // Figma shadow: 0px 0px 15.7px 0px rgba(112,112,112,0.25)
                .drawBehind {
                    val d = this.density
                    drawIntoCanvas { canvas ->
                        if (cardTouchGlowProgress > 0.01f) {
                            val glowPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                setShadowLayer(
                                    (18f + 18f * cardTouchGlowProgress) * d,
                                    0f,
                                    0f,
                                    android.graphics.Color.argb(
                                        (0.42f * cardTouchGlowProgress * 255).toInt(),
                                        77,
                                        217,
                                        232
                                    )
                                )
                            }
                            val glowRect = android.graphics.RectF(0f, 0f, size.width, size.height)
                            canvas.nativeCanvas.drawRoundRect(glowRect, 24.dp.toPx(), 24.dp.toPx(), glowPaint)
                        }

                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            setShadowLayer(
                                15.7f * d,
                                0f * d,
                                0f * d,
                                android.graphics.Color.argb((0.25f * 255).toInt(), 112, 112, 112)
                            )
                        }
                        val rect = android.graphics.RectF(0f, 0f, size.width, size.height)
                        canvas.nativeCanvas.drawRoundRect(rect, 24.dp.toPx(), 24.dp.toPx(), paint)
                    }
                }
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White, RoundedCornerShape(24.dp))
                .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(24.dp))
            ) {
                if (!isCardBackVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(enabled = !isThresholdMet) { isCardFlipped = true }
                            .pointerInput(Unit) {
                                var totalDragX = 0f
                                detectDragGestures(
                                    onDragStart = {
                                        totalDragX = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        totalDragX += dragAmount.x
                                        if (!isThresholdMet && abs(totalDragX) > 24f) {
                                            isCardFlipped = true
                                        }
                                        change.consume()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isThresholdMet) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidthPx = 8.dp.toPx()
                                drawIntoCanvas { canvas ->
                                    val nativeCanvas = canvas.nativeCanvas
                                    for (coloredStroke in strokes) {
                                        if (coloredStroke.points.size > 1) {
                                            val paint = signaturePaint(
                                                strokeWidthPx,
                                                size.width,
                                                size.height,
                                                density
                                            )
                                            val path = android.graphics.Path().apply {
                                                addSmoothedStroke(coloredStroke.points)
                                            }
                                            nativeCanvas.drawPath(path, paint)
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "Autograph saved",
                                fontFamily = DentonFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 34.sp,
                                color = Color.Black,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = 24.dp, top = 26.dp)
                            )
                        } else {
                            Text(
                                text = "Give us your autograph if you're serious about a business analyst career.",
                                fontFamily = DentonFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 30.sp,
                                lineHeight = 34.sp,
                                color = Color.Black,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 30.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Drawing canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentStroke.value = listOf(offset)
                                    },
                                    onDragEnd = {
                                        if (currentStroke.value.isNotEmpty()) {
                                            strokes.add(ColoredStroke(currentStroke.value))
                                            currentStroke.value = emptyList()
                                        }
                                    },
                                    onDragCancel = {
                                        if (currentStroke.value.isNotEmpty()) {
                                            strokes.add(ColoredStroke(currentStroke.value))
                                            currentStroke.value = emptyList()
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val nextPoint = change.position
                                        val lastPoint = currentStroke.value.lastOrNull()
                                        if (lastPoint != null) {
                                            totalDrawingLength += (nextPoint - lastPoint).getDistance()
                                        }
                                        currentStroke.value = currentStroke.value + nextPoint
                                    }
                                )
                            }
                    ) {
                        val strokeWidthPx = 8.dp.toPx()

                        drawIntoCanvas { canvas ->
                            val nativeCanvas = canvas.nativeCanvas

                            // Draw completed strokes with one consistent signature gradient
                            for (coloredStroke in strokes) {
                                if (coloredStroke.points.size > 1) {
                                    val paint = signaturePaint(
                                        strokeWidthPx,
                                        size.width,
                                        size.height,
                                        density
                                    )
                                    val path = android.graphics.Path().apply {
                                        addSmoothedStroke(coloredStroke.points)
                                    }
                                    nativeCanvas.drawPath(path, paint)
                                }
                            }

                            // Draw current stroke
                            val current = currentStroke.value
                            if (current.size > 1) {
                                val paint = signaturePaint(
                                    strokeWidthPx,
                                    size.width,
                                    size.height,
                                    density
                                )
                                val path = android.graphics.Path().apply {
                                    addSmoothedStroke(current)
                                }
                                nativeCanvas.drawPath(path, paint)
                            }
                        }
                    }
                }

                if (cardTouchGlowProgress > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .border(
                                width = (1.2f + 1.4f * cardTouchGlowProgress).dp,
                                color = CardTouchGlow.copy(alpha = 0.18f + 0.42f * cardTouchGlowProgress),
                                shape = RoundedCornerShape(24.dp)
                            )
                    )
                }

                // Tilde watermark — bottom-end of card
                Text(
                    text = "~${task.title}",
                    fontFamily = DentonFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 14.sp,
                    color = Color(0xFF2C2C2C),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    style = LocalTextStyle.current.copy(
                        shadow = Shadow(
                            color = Color(0, 0, 0, (0.43f * 255).toInt()),
                            offset = Offset(0f, 1.2f * localDensity.density),
                            blurRadius = 1.3f * localDensity.density
                        )
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 16.dp)
                )

                // Glare / shine overlay — moves opposite to tilt
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .drawBehind {
                            // Spotlight position shifts opposite to tilt
                            val spotX = size.width * (0.5f - cardTiltY / 20f)
                            val spotY = size.height * (0.3f - cardTiltX / 40f)

                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.08f),
                                        Color.Transparent
                                    ),
                                    center = Offset(spotX, spotY),
                                    radius = size.width * 0.7f
                                ),
                                size = size
                            )
                        }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom CTA Button
        Button(
            onClick = {
                if (!isCardFlipped) {
                    if (isThresholdMet) {
                        onProceed()
                    } else {
                        isCardFlipped = true
                    }
                } else if (isSignatureComplete) {
                    isCardFlipped = false
                } else if (isThresholdMet) {
                    onProceed()
                }
            },
            enabled = isActionEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonBgColor,
                contentColor = buttonTextColor,
                disabledContainerColor = buttonBgColor,
                disabledContentColor = buttonTextColor
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                disabledElevation = 0.dp
            )
        ) {
            Text(
                text = when {
                    !isCardFlipped && isThresholdMet -> "I own this"
                    !isCardFlipped -> "Flip card"
                    isSignatureComplete -> "Flip to front"
                    else -> "Sign to proceed"
                },
                fontFamily = SatoshiFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
