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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import android.os.Build
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

enum class AppScreen { Home, Patterns, Streak }

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

private val RainbowColors = listOf(
    Color(0xFFCCE9FF),
    Color(0xFFBDFFEB),
    Color(0xFFD0FFCA),
    Color(0xFFF4FDCF),
    Color(0xFFFFD0CD),
    Color(0xFFFFBAFB),
    Color(0xFFB4B4FF)
)

// Card gradient (Figma: bg-gradient-to-b from-white to-[#e6e6e6])
private val CardGradientTop = Color(0xFFFFFFFF)
private val CardGradientBottom = Color(0xFFE6E6E6)

// First card gradient (Figma: linear-gradient(177deg, #FFB984, #FFCFDF))
private val Card1GradientTop = Color(0xFFFFB984)
private val Card1GradientBottom = Color(0xFFFFCFDF)

private const val CARD_TILT_INTENSITY = 0.42f
private const val CARD_TILT_MAX_DEGREES = 10f
private const val CARD_TILT_LOW_PASS_KEEP = 0.7f
private const val CARD_TILT_LOW_PASS_NEW = 0.3f
private const val CARD_TILT_UPDATE_INTERVAL_NANOS = 16_000_000L
private val SignatureInkHighlight = Color(0xFFE8FDFF)
private val SignatureInkStart = Color(0xFF55E7F5)
private val SignatureInkMid = Color(0xFF16BCE3)
private val SignatureInkEnd = Color(0xFF168DA3)
private val CardTouchGlow = SignatureInkStart
private val SignatureShadowColor = Color(0xFF0B8798)
private const val SignatureLiveStrokeDp = 4f
private const val SignatureSavedStrokeDp = 4.2f
private const val SignatureLiveShadowAlpha = 0.22f
private const val SignatureSavedShadowAlpha = 0.28f
private const val SignatureLiveShadowBlur = 2.2f
private const val SignatureSavedShadowBlur = 3.8f
private const val SignatureLiveShadowOffsetY = 2.8f
private const val SignatureSavedShadowOffsetY = 5.2f

// Safe blur helper: blur() uses RenderEffect which requires API 31+.
// On older devices, skip the blur to avoid rendering crashes.
private fun Modifier.safeBlur(
    radius: androidx.compose.ui.unit.Dp,
    edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Rectangle
): Modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    this.blur(radius, edgeTreatment)
} else {
    this // graceful no-op on older devices
}

private fun Modifier.scrollFadeBlur(
    screenY: androidx.compose.ui.unit.Dp
): Modifier {
    val startFadeY = 359f
    val endFadeY = 312f
    val yVal = screenY.value
    
    val fraction = when {
        yVal >= startFadeY -> 1f
        yVal <= endFadeY -> 0f
        else -> (yVal - endFadeY) / (startFadeY - endFadeY)
    }
    
    val alphaVal = fraction
    val blurRadius = (1f - fraction) * 16.7f
    
    return this
        .graphicsLayer {
            alpha = alphaVal
        }
        .let {
            if (blurRadius > 0.1f) {
                it.safeBlur(blurRadius.dp)
            } else {
                it
            }
        }
}

class MainActivity : ComponentActivity() {
    private val activeSigningTaskId = mutableStateOf<String?>(null)
    private val activeSigningTaskTitle = mutableStateOf<String?>(null)
    private val activeSigningTaskDesc = mutableStateOf<String?>(null)

    private fun getActiveSigningTaskId(): String? {
        return getSharedPreferences("task_alarms_signing", Context.MODE_PRIVATE)
            .getString("active_signing_task_id", null)
    }

    private fun getActiveSigningTaskTitle(): String? {
        return getSharedPreferences("task_alarms_signing", Context.MODE_PRIVATE)
            .getString("active_signing_task_title", null)
    }

    private fun getActiveSigningTaskDesc(): String? {
        return getSharedPreferences("task_alarms_signing", Context.MODE_PRIVATE)
            .getString("active_signing_task_desc", null)
    }

    private fun setActiveSigningTaskData(id: String?, title: String?, desc: String?) {
        getSharedPreferences("task_alarms_signing", Context.MODE_PRIVATE)
            .edit()
            .putString("active_signing_task_id", id)
            .putString("active_signing_task_title", title)
            .putString("active_signing_task_desc", desc)
            .apply()
        activeSigningTaskId.value = id
        activeSigningTaskTitle.value = title
        activeSigningTaskDesc.value = desc
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
        activeSigningTaskTitle.value = getActiveSigningTaskTitle()
        activeSigningTaskDesc.value = getActiveSigningTaskDesc()

        setContent {
            MyApplicationTheme {
                val currentTaskId = activeSigningTaskId.value
                val currentTaskTitle = activeSigningTaskTitle.value ?: "Task Alarm"
                val currentTaskDesc = activeSigningTaskDesc.value ?: "Time to get things done!"

                if (currentTaskId != null) {
                    val taskList = TaskStorage.getTasks(this)
                    val task = taskList.find { it.id == currentTaskId }
                    
                    if (task != null) {
                        SigningScreen(
                            task = task,
                            onProceed = {
                                TaskStorage.logDrop(this@MainActivity, task.title)
                                TaskStorage.deleteTask(this@MainActivity, task.id)
                                setActiveSigningTaskData(null, null, null)
                                stopService(Intent(this@MainActivity, AlarmService::class.java))
                            },
                            onSnooze = { snoozedTask ->
                                TaskStorage.updateTask(this@MainActivity, snoozedTask)
                                val snoozeTime = java.time.LocalDateTime.parse(snoozedTask.dateTime)
                                AlarmScheduler.scheduleAlarm(
                                    this@MainActivity,
                                    snoozeTime,
                                    snoozedTask.title,
                                    snoozedTask.description,
                                    snoozedTask.id
                                )
                                setActiveSigningTaskData(null, null, null)
                                stopService(Intent(this@MainActivity, AlarmService::class.java))
                            }
                        )
                    } else {
                        // Fallback in case task was deleted but SharedPreferences still has ID
                        val fallbackTask = TaskAlarm(
                            id = currentTaskId,
                            title = currentTaskTitle,
                            description = currentTaskDesc,
                            dateTime = LocalDateTime.now().toString(),
                            priority = 0
                        )
                        SigningScreen(
                            task = fallbackTask,
                            onProceed = {
                                TaskStorage.logDrop(this@MainActivity, fallbackTask.title)
                                TaskStorage.deleteTask(this@MainActivity, fallbackTask.id)
                                setActiveSigningTaskData(null, null, null)
                                stopService(Intent(this@MainActivity, AlarmService::class.java))
                            },
                            onSnooze = { snoozedTask ->
                                val snoozeTime = java.time.LocalDateTime.parse(snoozedTask.dateTime)
                                AlarmScheduler.scheduleAlarm(
                                    this@MainActivity,
                                    snoozeTime,
                                    snoozedTask.title,
                                    snoozedTask.description,
                                    snoozedTask.id
                                )
                                setActiveSigningTaskData(null, null, null)
                                stopService(Intent(this@MainActivity, AlarmService::class.java))
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
            // Keep the alarm ringing until the user interacts with the card
            
            
            val taskId = intent.getStringExtra("TASK_ID")
            val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Task Alarm"
            val taskDesc = intent.getStringExtra("TASK_DESC") ?: "Time to get things done!"
            if (!taskId.isNullOrEmpty()) {
                setActiveSigningTaskData(taskId, taskTitle, taskDesc)
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
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }

    // Refresh tasks when sheet is dismissed or screen changes
    LaunchedEffect(showSheet, currentScreen) {
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
                TaskStorage.logCompletion(context)
                TaskStorage.deleteTask(context, task.id)
                tasks = TaskStorage.getTasks(context)
                selectedTask = null
            }
        )
        return
    }

    if (currentScreen == AppScreen.Patterns) {
        PatternsScreen(onBack = { currentScreen = AppScreen.Home })
        return
    }

    if (currentScreen == AppScreen.Streak) {
        StreakScreen(onBack = { currentScreen = AppScreen.Home })
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Red line and Vertical Gradient Pill
        Column(
            modifier = Modifier
                .padding(start = 32.dp, top = 90.dp)
                .width(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.width(8.dp).height(52.dp).background(Color(0xFFFFE6E6)))
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(244.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF69A0CB),
                                Color(0xDBFB740E),
                                Color(0xFFEB8638),
                                Color(0xFFF3682C),
                                Color(0xFFFF5A39)
                            )
                        )
                    )
            )
        }

        // Cards Scrollable Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 150.dp, start = 84.dp, end = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (tasks.isEmpty()) {
                TaskCard(
                    title = "No Tasks",
                    subtitle = "Tap + to add your first alarm",
                    isFirst = true
                )
            } else {
                tasks.sortedBy { it.dateTime }.forEachIndexed { index, task ->
                    val ldt = java.time.LocalDateTime.parse(task.dateTime)
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", java.util.Locale.US)
                    val subtitle = ldt.format(formatter)
                    
                    TaskCard(
                        title = task.title,
                        subtitle = subtitle,
                        isFirst = index == 0,
                        onClick = { selectedTask = task }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
                Spacer(modifier = Modifier.height(80.dp)) // padding for bottom FAB
            }
        }

        // Top Gradient Fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(111.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White, Color.Transparent)
                    )
                )
        )

        // Header Texts
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, top = 35.dp)
        ) {
            Text(
                text = "Task Alarm",
                fontFamily = DentonFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 36.sp,
                color = Color(0xFF303030),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your tasks, your deadlines.",
                fontFamily = SatoshiFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Color(0xFF6C6C6C),
            )
        }

        // Header Icons
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 22.dp, top = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stats Icon
            Image(
                painter = painterResource(id = R.drawable.stats_icon),
                contentDescription = "Stats",
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { currentScreen = AppScreen.Streak }
            )
            // Notification Icon
            Image(
                painter = painterResource(id = R.drawable.notification_icon),
                contentDescription = "Notifications",
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { /* TODO: notification screen */ }
            )
        }

        // FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 32.dp, bottom = 48.dp)
                .size(74.dp)
                .drawBehind {
                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas
                        val center = size.width / 2f
                        val radius = size.minDimension / 2f - 9.dp.toPx()

                        val shadowPaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = android.graphics.Color.argb(1, 0, 0, 0)
                            setShadowLayer(
                                10.dp.toPx(),
                                0f,
                                7.dp.toPx(),
                                android.graphics.Color.argb((0.34f * 255).toInt(), 104, 104, 104)
                            )
                        }
                        nativeCanvas.drawCircle(center, center, radius, shadowPaint)

                        val facePaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            shader = android.graphics.LinearGradient(
                                0f,
                                center - radius,
                                0f,
                                center + radius,
                                intArrayOf(
                                    android.graphics.Color.WHITE,
                                    android.graphics.Color.rgb(246, 247, 248),
                                    android.graphics.Color.rgb(224, 224, 224)
                                ),
                                floatArrayOf(0f, 0.54f, 1f),
                                android.graphics.Shader.TileMode.CLAMP
                            )
                        }
                        nativeCanvas.drawCircle(center, center, radius, facePaint)

                        val rimPaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 1.2.dp.toPx()
                            shader = android.graphics.LinearGradient(
                                0f,
                                center - radius,
                                0f,
                                center + radius,
                                intArrayOf(
                                    android.graphics.Color.argb((0.96f * 255).toInt(), 255, 255, 255),
                                    android.graphics.Color.argb((0.62f * 255).toInt(), 180, 180, 180)
                                ),
                                null,
                                android.graphics.Shader.TileMode.CLAMP
                            )
                        }
                        nativeCanvas.drawCircle(center, center, radius - 0.6.dp.toPx(), rimPaint)

                        val lowerInsetPaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 4.dp.toPx()
                            color = android.graphics.Color.argb((0.14f * 255).toInt(), 124, 124, 124)
                        }
                        nativeCanvas.drawArc(
                            center - radius + 2.dp.toPx(),
                            center - radius + 2.dp.toPx(),
                            center + radius - 2.dp.toPx(),
                            center + radius - 2.dp.toPx(),
                            32f,
                            116f,
                            false,
                            lowerInsetPaint
                        )

                        val topHighlightPaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 3.dp.toPx()
                            color = android.graphics.Color.argb((0.92f * 255).toInt(), 255, 255, 255)
                        }
                        nativeCanvas.drawArc(
                            center - radius + 4.dp.toPx(),
                            center - radius + 4.dp.toPx(),
                            center + radius - 4.dp.toPx(),
                            center + radius - 4.dp.toPx(),
                            214f,
                            118f,
                            false,
                            topHighlightPaint
                        )
                    }
                }
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { showSheet = true },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(22.dp)) {
                val strokeW = 2.dp.toPx()
                drawLine(Color.Black, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(Color.Black, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = strokeW, cap = StrokeCap.Round)
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
    isFirst: Boolean = false,
    onClick: () -> Unit = {}
) {
    val gradientBrush = if (isFirst) {
        Brush.linearGradient(
            colors = listOf(Color(0xFFFFB984), Color(0xFFFFCFDF)),
            start = Offset(0f, 0f),
            end = Offset(0f, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color.White, Color(0xFFE6E6E6))
        )
    }
    
    val borderModifier = if (!isFirst) {
        Modifier.border(1.dp, Color(0xFFF2F2F2), RoundedCornerShape(24.dp))
    } else Modifier
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .then(borderModifier)
            .clip(RoundedCornerShape(24.dp))
            .background(gradientBrush)
            .clickable(onClick = onClick)
    ) {
        // Inner shadow for first card
        if (isFirst) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 4.dp,
                        color = Color.Black.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .blur(8.dp)
            )
        }

        Column(
            modifier = Modifier.padding(start = 24.dp, top = 28.dp)
        ) {
            Text(
                text = title,
                fontFamily = DentonFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 36.sp,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = SatoshiFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Color(0xFF6A7282),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Next Icon
        Image(
            painter = painterResource(id = R.drawable.next_icon),
            contentDescription = "Details",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
                .size(24.dp)
        )
    }
}

@Composable
fun PatternsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val stats = remember { TaskStorage.getStats(context) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val strokeW = 2.dp.toPx()
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
                        color = Color.White,
                        style = Stroke(
                            width = strokeW,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Patterns",
                fontFamily = DentonFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 28.sp,
                letterSpacing = 0.3828.sp,
                color = Color.White
            )
        }
        
        // 1. Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    label = "Completed",
                    value = stats.completedCount.toString(),
                    barColor = Color(0xFF00C896)
                )
                StatCard(
                    label = "Dropped",
                    value = stats.droppedCount.toString(),
                    barColor = Color(0xFFFF4D4D)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    label = "Rescheduled",
                    value = stats.rescheduledCount.toString(),
                    barColor = Color(0xFFF97316)
                )
                StatCard(
                    label = "Avg. Delay",
                    value = "${String.format(java.util.Locale.US, "%.1f", stats.avgDelayHours)}h",
                    barColor = Color(0xFF2563EB)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 2. Active Hours Bar Chart
        Text(
            text = "ACTIVE HOURS",
            fontFamily = SatoshiFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Color(0xFF6A7282),
            letterSpacing = (-0.36).sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(211.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1A1A))
                .padding(16.dp)
        ) {
            val maxVal = stats.activeHours.maxOrNull() ?: 1
            val labels = listOf("6A", "8A", "10A", "12P", "2P", "4P", "6P", "8P", "10P")
            
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                stats.activeHours.forEachIndexed { idx, value ->
                    val isPeak = value == maxVal && maxVal > 0
                    val barCol = if (isPeak) Color(0xFF00D9A6) else Color(0xFF00C896)
                    val barHeightFraction = if (maxVal > 0) value.toFloat() / maxVal else 0f
                    val heightDp = (barHeightFraction * 130).dp
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (value > 0) value.toString() else "",
                            fontFamily = SatoshiFontFamily,
                            fontSize = 10.sp,
                            color = if (isPeak) Color(0xFF00D9A6) else Color(0xFF6A7282),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(heightDp.coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barCol)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = labels[idx],
                            fontFamily = SatoshiFontFamily,
                            fontSize = 10.sp,
                            color = Color(0xFF6A7282),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 3. Most Dropped Tasks
        Text(
            text = "MOST DROPPED TASKS",
            fontFamily = SatoshiFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Color(0xFF6A7282),
            letterSpacing = (-0.36).sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1A1A))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val droppedList = stats.droppedTasks.take(3)
                if (droppedList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No dropped tasks recorded",
                            fontFamily = SatoshiFontFamily,
                            fontSize = 14.sp,
                            color = Color(0xFF6A7282)
                        )
                    }
                } else {
                    droppedList.forEach { info ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = info.title,
                                fontFamily = SatoshiFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFF4D4D).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${info.count} drops",
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFFFF4D4D)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(Color(0xFF2C2C2C))
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Patterns don't lie. Use them.",
            fontFamily = SatoshiFontFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = Color(0xFF6A7282),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    barColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(113.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1A1A))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .background(barColor)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label.uppercase(java.util.Locale.US),
                fontFamily = SatoshiFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Color(0xFF6A7282),
                letterSpacing = (-0.36).sp
            )
            Text(
                text = value,
                fontFamily = DentonFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 32.sp,
                color = Color.White,
                letterSpacing = (-0.96).sp
            )
        }
    }
}

// Data class for completed signature strokes
data class ColoredStroke(
    val points: List<Offset>
)

private data class SignatureBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float = right - left
    val height: Float = bottom - top
}

private fun List<ColoredStroke>.signatureBounds(): SignatureBounds? {
    var left = Float.POSITIVE_INFINITY
    var top = Float.POSITIVE_INFINITY
    var right = Float.NEGATIVE_INFINITY
    var bottom = Float.NEGATIVE_INFINITY
    var hasPoints = false

    for (stroke in this) {
        for (point in stroke.points) {
            hasPoints = true
            left = min(left, point.x)
            top = min(top, point.y)
            right = max(right, point.x)
            bottom = max(bottom, point.y)
        }
    }

    return if (hasPoints) SignatureBounds(left, top, right, bottom) else null
}

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
    density: Float,
    shadowAlpha: Float = 0.33f,
    shadowBlur: Float = 2.3f,
    shadowOffsetY: Float = 4f
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
            canvasWidth * 0.72f,
            canvasHeight * 1.04f,
            intArrayOf(
                SignatureInkHighlight.toArgb(),
                SignatureInkStart.toArgb(),
                SignatureInkMid.toArgb(),
                SignatureInkEnd.toArgb()
            ),
            floatArrayOf(0f, 0.22f, 0.62f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        setShadowLayer(
            shadowBlur * density,
            0f,
            shadowOffsetY * density,
            SignatureShadowColor.copy(alpha = shadowAlpha).toArgb()
        )
    }
}

private fun drawSignatureStrokes(
    nativeCanvas: android.graphics.Canvas,
    strokes: List<ColoredStroke>,
    strokeWidthPx: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    density: Float,
    alpha: Float = 1f,
    shadowAlpha: Float = 0.33f,
    shadowBlur: Float = 2.3f,
    shadowOffsetY: Float = 4f
) {
    for (coloredStroke in strokes) {
        if (coloredStroke.points.size > 1) {
            val paint = signaturePaint(
                strokeWidthPx,
                canvasWidth,
                canvasHeight,
                density,
                shadowAlpha,
                shadowBlur,
                shadowOffsetY
            ).apply {
                this.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
            }
            val path = android.graphics.Path().apply {
                addSmoothedStroke(coloredStroke.points)
            }
            nativeCanvas.drawPath(path, paint)
        }
    }
}

private fun drawCenteredSignature(
    nativeCanvas: android.graphics.Canvas,
    strokes: List<ColoredStroke>,
    canvasWidth: Float,
    canvasHeight: Float,
    density: Float,
    progress: Float
) {
    val bounds = strokes.signatureBounds() ?: return
    if (bounds.width <= 0f || bounds.height <= 0f) return

    val safeProgress = progress.coerceIn(0f, 1f)
    val targetWidth = canvasWidth * 0.68f
    val targetHeight = canvasHeight * 0.26f
    val scale = min(targetWidth / bounds.width, targetHeight / bounds.height)
        .coerceIn(0.18f, 2.6f)
    val revealScale = 0.86f + (0.14f * safeProgress)
    val finalScale = scale * revealScale
    val centerX = canvasWidth * 0.5f
    val centerY = canvasHeight * 0.5f
    val signatureCenterX = (bounds.left + bounds.right) / 2f
    val signatureCenterY = (bounds.top + bounds.bottom) / 2f

    nativeCanvas.save()
    nativeCanvas.translate(centerX, centerY)
    nativeCanvas.scale(finalScale, finalScale)
    nativeCanvas.translate(-signatureCenterX, -signatureCenterY)

    drawSignatureStrokes(
        nativeCanvas = nativeCanvas,
        strokes = strokes,
        strokeWidthPx = (SignatureSavedStrokeDp * density) / finalScale,
        canvasWidth = canvasWidth / finalScale,
        canvasHeight = canvasHeight / finalScale,
        density = density,
        alpha = safeProgress,
        shadowAlpha = SignatureSavedShadowAlpha,
        shadowBlur = SignatureSavedShadowBlur,
        shadowOffsetY = SignatureSavedShadowOffsetY
    )

    nativeCanvas.restore()
}

@Suppress("SupportAnnotationUsage")
@android.annotation.SuppressLint("SupportAnnotationUsage")
@Composable
fun SigningScreen(
    task: TaskAlarm,
    onProceed: () -> Unit,
    onSnooze: (TaskAlarm) -> Unit = {}
) {
    BackHandler(enabled = true) {
        // Do nothing - user must sign to unlock the app
    }

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()
    var totalDrawingLength by remember { mutableStateOf(0f) }
    val currentStroke = remember { mutableStateOf<List<Offset>>(emptyList()) }
    val strokes = remember { mutableStateListOf<ColoredStroke>() }
    val localDensity = LocalDensity.current
    var isCardFlipped by remember { mutableStateOf(false) }
    var glareTiltY by remember { mutableStateOf(0f) }

    var timeString by remember { mutableStateOf("") }
    var amPmString by remember { mutableStateOf("") }
    val cardDragOffset = remember { Animatable(0f) }
    var rawSnoozeDrag by remember { mutableStateOf(0f) }
    var isSnoozeDrawerOpen by remember { mutableStateOf(false) }
    var showSnoozeConfirmation by remember { mutableStateOf(false) }
    var snoozeConfirmationText by remember { mutableStateOf("") }
    var hasSnoozedAway by remember { mutableStateOf(false) }
    val screenHeightPx = with(localDensity) { configuration.screenHeightDp.dp.toPx() }
    val snoozeThresholdPx = screenHeightPx / 3f
    val canSnooze = task.snoozeCount < 2
    val snoozePrompt = if (task.snoozeCount == 0) {
        "Need 10 mins\nmore?"
    } else {
        "Last chance.\n10 mins more."
    }
    val snoozeRevealProgress by animateFloatAsState(
        targetValue = if (canSnooze && (rawSnoozeDrag > 8f || isSnoozeDrawerOpen)) {
            (cardDragOffset.value / (snoozeThresholdPx * 0.6f)).coerceIn(0f, 1f)
        } else {
            0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "snoozeRevealProgress"
    )
    val cardDragProgress = (cardDragOffset.value / (snoozeThresholdPx * 0.6f)).coerceIn(0f, 1f)

    fun closeSnoozeDrawer() {
        isSnoozeDrawerOpen = false
        rawSnoozeDrag = 0f
        coroutineScope.launch {
            cardDragOffset.animateTo(
                0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    fun confirmSnooze() {
        if (!canSnooze || hasSnoozedAway) return
        hasSnoozedAway = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val snoozeTime = java.time.LocalDateTime.now().plusMinutes(10)
        snoozeConfirmationText = "See you at ${
            snoozeTime.format(
                java.time.format.DateTimeFormatter.ofPattern(
                    "h mm a",
                    java.util.Locale.US
                )
            )
        }"
        showSnoozeConfirmation = true
        closeSnoozeDrawer()
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = java.time.LocalTime.now()
            timeString = now.format(java.time.format.DateTimeFormatter.ofPattern("h:mm"))
            amPmString = now.format(java.time.format.DateTimeFormatter.ofPattern("a"))
            delay(1000)
        }
    }

    LaunchedEffect(showSnoozeConfirmation) {
        if (showSnoozeConfirmation) {
            delay(2000)
            showSnoozeConfirmation = false
            onSnooze(
                task.copy(
                    dateTime = java.time.LocalDateTime.now().plusMinutes(10).toString(),
                    snoozeCount = task.snoozeCount + 1
                )
            )
        }
    }

    val isThresholdMet by remember { derivedStateOf { totalDrawingLength > 1000f } }

    // ─── Signature touch response ──────────────────────────────────────────
    val isCardTouched by remember { derivedStateOf { currentStroke.value.isNotEmpty() } }
    val isSignatureComplete by remember { derivedStateOf { isThresholdMet && !isCardTouched } }
    val cardTouchGlowProgress by animateFloatAsState(
        targetValue = if (isCardTouched) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (isCardTouched) Spring.StiffnessMedium else Spring.StiffnessLow
        ),
        label = "cardTouchGlow"
    )
    val cardFlipRotation by animateFloatAsState(
        targetValue = if (isCardFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.74f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardFlipRotation"
    )
    val cardFlipDepth = 1f - (abs(cardFlipRotation - 90f) / 90f).coerceIn(0f, 1f)
    val isCardBackVisible = cardFlipRotation >= 90f
    val backHeaderAlpha by animateFloatAsState(
        targetValue = if (isCardFlipped) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "backHeaderAlpha"
    )
    val glareAlpha = 1f - (cardFlipRotation / 90f).coerceIn(0f, 1f)
    val signatureRevealProgress by animateFloatAsState(
        targetValue = if (isSignatureComplete && !isCardBackVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "signatureRevealProgress"
    )

    LaunchedEffect(isSignatureComplete) {
        if (isSignatureComplete && isCardFlipped) {
            delay(360)
            isCardFlipped = false
        }
    }

    // ─── Gyroscope parallax ────────────────────────────────────────────────
    val rotationXState = remember { mutableFloatStateOf(0f) }
    val rotationYState = remember { mutableFloatStateOf(0f) }
    val glareOffsetX by animateFloatAsState(
        targetValue = rotationYState.floatValue * 4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "glareOffsetX"
    )
    val glareOffsetY by animateFloatAsState(
        targetValue = rotationXState.floatValue * 2.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "glareOffsetY"
    )
    val backgroundGlareOffsetX by animateFloatAsState(
        targetValue = rotationYState.floatValue * 1.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 8f
        ),
        label = "backgroundGlareOffsetX"
    )
    val backgroundGlareOffsetY by animateFloatAsState(
        targetValue = rotationXState.floatValue * 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 8f
        ),
        label = "backgroundGlareOffsetY"
    )

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
            private var initialPitch: Float? = null
            private var initialRoll: Float? = null
            private var filteredTiltX = 0f
            private var filteredTiltY = 0f
            private var lastTiltUpdateNanos = 0L

            private fun getAngleDifference(target: Float, current: Float): Float {
                var diff = target - current
                while (diff < -180f) diff += 360f
                while (diff > 180f) diff -= 360f
                return diff
            }

            override fun onSensorChanged(event: SensorEvent) {
                if (
                    lastTiltUpdateNanos != 0L &&
                    event.timestamp - lastTiltUpdateNanos < CARD_TILT_UPDATE_INTERVAL_NANOS
                ) {
                    return
                }
                lastTiltUpdateNanos = event.timestamp

                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)

                val pitchDegrees = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                val rollDegrees = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                if (initialPitch == null) {
                    initialPitch = pitchDegrees
                    initialRoll = rollDegrees
                }

                val relPitch = getAngleDifference(pitchDegrees, initialPitch ?: pitchDegrees)
                val relRoll = getAngleDifference(rollDegrees, initialRoll ?: rollDegrees)

                val rawTiltX = ((relPitch * CARD_TILT_INTENSITY) / CARD_TILT_MAX_DEGREES)
                    .coerceIn(-1f, 1f)
                val rawTiltY = ((-relRoll * CARD_TILT_INTENSITY) / CARD_TILT_MAX_DEGREES)
                    .coerceIn(-1f, 1f)

                filteredTiltX = (filteredTiltX * CARD_TILT_LOW_PASS_KEEP) +
                    (rawTiltX * CARD_TILT_LOW_PASS_NEW)
                filteredTiltY = (filteredTiltY * CARD_TILT_LOW_PASS_KEEP) +
                    (rawTiltY * CARD_TILT_LOW_PASS_NEW)
                
                rotationXState.floatValue = (filteredTiltX.coerceIn(-1f, 1f) * CARD_TILT_MAX_DEGREES)
                rotationYState.floatValue = (filteredTiltY.coerceIn(-1f, 1f) * CARD_TILT_MAX_DEGREES)
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

    val signedStackProgress by animateFloatAsState(
        targetValue = if (isSignatureComplete) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "signedStackProgress"
    )
    val isActionEnabled = !isCardFlipped || isSignatureComplete
    val alarmCardShape = RoundedCornerShape(30.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Image(
            painter = painterResource(id = R.drawable.glare_for_card),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 34.dp, y = 84.dp)
                .fillMaxWidth()
                .height(680.dp)
                .graphicsLayer {
                    alpha = glareAlpha * 0.95f
                    scaleX = 1.9f
                    translationX = backgroundGlareOffsetX.dp.toPx()
                    translationY = backgroundGlareOffsetY.dp.toPx()
                }
                .safeBlur(8.dp, BlurredEdgeTreatment.Unbounded),
            contentScale = ContentScale.FillBounds
        )

        if (canSnooze) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 118.dp)
                    .graphicsLayer {
                        alpha = snoozeRevealProgress * (1f - backHeaderAlpha)
                        translationY = (-24).dp.toPx() * (1f - snoozeRevealProgress)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 620.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // 1. Embossed Bottom Highlight (White / Light) - Softened
                        Text(
                            text = snoozePrompt,
                            fontFamily = DentonFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 48.sp,
                            lineHeight = 52.sp,
                            color = Color.White.copy(alpha = 0.45f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(x = 0.5.dp, y = 0.8.dp)
                        )

                        // 2. Inner Shadow / Engraved Recess (Dark Gray) - Softened
                        Text(
                            text = snoozePrompt,
                            fontFamily = DentonFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 48.sp,
                            lineHeight = 52.sp,
                            color = Color(0xFF4A4A4A).copy(alpha = 0.22f),
                            textAlign = TextAlign.Center,
                            style = LocalTextStyle.current.copy(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.15f),
                                    offset = Offset(-0.3f, -0.3f),
                                    blurRadius = 0.8f
                                )
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(x = (-0.5).dp, y = (-0.6).dp)
                        )

                        // 3. Foreground Text with Linear Gradient - Softened
                        Text(
                            text = snoozePrompt,
                            fontFamily = DentonFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 48.sp,
                            lineHeight = 52.sp,
                            style = TextStyle(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White,
                                        Color(0xFFDADADA)
                                    )
                                ),
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.08f),
                                    offset = Offset(0f, 0.6f),
                                    blurRadius = 1.2f
                                )
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(22.dp))
                Box(
                    modifier = Modifier
                        .size(94.dp)
                        .shadow(
                            elevation = 15.dp,
                            shape = CircleShape,
                            ambientColor = Color(0xFF717171).copy(alpha = 0.5f),
                            spotColor = Color(0xFF717171).copy(alpha = 0.5f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.snooze_button),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                enabled = canSnooze && isSnoozeDrawerOpen && !hasSnoozedAway
                            ) {
                                confirmSnooze()
                            },
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        if (backHeaderAlpha > 0.01f) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 118.dp)
                    .graphicsLayer {
                        alpha = backHeaderAlpha
                        translationY = 24.dp.toPx() * (1f - backHeaderAlpha)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.image_leaf_final),
                        contentDescription = null,
                        modifier = Modifier
                            .size(width = 39.dp, height = 59.dp)
                            .graphicsLayer {
                                scaleX = -1f
                            }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.title,
                        fontFamily = DentonFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 36.sp,
                        lineHeight = 34.sp,
                        color = Color(0xFF211C1C),
                        textAlign = TextAlign.Center,
                        style = LocalTextStyle.current.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.36f),
                                offset = Offset(0f, 2f),
                                blurRadius = 2.3f
                            )
                        ),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Image(
                        painter = painterResource(id = R.drawable.image_leaf_final),
                        contentDescription = null,
                        modifier = Modifier
                            .size(width = 39.dp, height = 59.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    color = Color(0xFFA7A7A7),
                    textAlign = TextAlign.Center,
                    style = LocalTextStyle.current.copy(
                        shadow = Shadow(
                            color = Color.White.copy(alpha = 0.65f),
                            offset = Offset(0f, 1f),
                            blurRadius = 1.4f
                        )
                    ),
                    modifier = Modifier
                        .widthIn(max = 292.dp)
                        .padding(horizontal = 12.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        AnimatedVisibility(
            visible = showSnoozeConfirmation,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(420)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 92.dp)
        ) {
            Text(
                text = snoozeConfirmationText,
                fontFamily = SatoshiFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = Color(0xFF3A3A3A),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.72f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }

        // White signing card — centered, with flip, depth, and gyroscope parallax
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(488.dp)
                .graphicsLayer {
                    translationY = cardDragOffset.value
                }
                .drawBehind {
                    drawIntoCanvas { canvas ->
                        val blurPx = 15.dp.toPx()
                        val radiusPx = 30.dp.toPx()
                        val insetX = 18.dp.toPx()
                        val insetY = 16.dp.toPx()
                        
                        // Dynamic shadow offset shift based on card tilt
                        val shadowShiftX = (rotationYState.floatValue * 0.8f).dp.toPx()
                        val shadowShiftY = 2.dp.toPx() + (rotationXState.floatValue * 0.6f).dp.toPx()
                        
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = android.graphics.Color.argb(1, 113, 113, 113)
                            setShadowLayer(
                                blurPx,
                                shadowShiftX,
                                shadowShiftY,
                                android.graphics.Color.argb((0.15f * 255).toInt(), 113, 113, 113)
                            )
                        }
                        canvas.nativeCanvas.drawRoundRect(
                            android.graphics.RectF(
                                insetX,
                                insetY,
                                size.width - insetX,
                                size.height - insetY
                            ),
                            radiusPx,
                            radiusPx,
                            paint
                        )
                    }
                }
                .padding(horizontal = 18.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val tiltX = rotationXState.floatValue * (1f - signedStackProgress)
                        val tiltY = rotationYState.floatValue * (1f - signedStackProgress)
                        this.rotationX = tiltX - (cardFlipDepth * 3f)
                        this.rotationY = tiltY + if (isCardBackVisible) {
                            cardFlipRotation - 180f
                        } else {
                            cardFlipRotation
                        }
                        val depthScale = 1f - (cardFlipDepth * 0.035f)
                        scaleX = depthScale + (cardTouchGlowProgress * 0.008f)
                        scaleY = depthScale + (cardTouchGlowProgress * 0.008f)
                        translationY = cardFlipDepth * 10.dp.toPx()
                        cameraDistance = 12f * density
                        alpha = 1f - signedStackProgress * 0.25f
                        shape = alarmCardShape
                        clip = false
                    }
            )

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
                        .clip(alarmCardShape)
                        .background(Color(0xFFE9F8F8), alarmCardShape)
                        .border(
                            BorderStroke(1.dp, CardTouchGlow.copy(alpha = 0.22f)),
                            alarmCardShape
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
                        .clip(alarmCardShape)
                        .background(Color(0xFFF0F0F0), alarmCardShape)
                        .border(BorderStroke(1.dp, CardBorder), alarmCardShape)
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                // Parallax tilt from gyroscope
                .graphicsLayer {
                    val tiltX = rotationXState.floatValue * (1f - signedStackProgress)
                    val tiltY = rotationYState.floatValue * (1f - signedStackProgress)
                    this.rotationX = tiltX - (cardFlipDepth * 3f)
                    this.rotationY = tiltY + if (isCardBackVisible) {
                        cardFlipRotation - 180f
                    } else {
                        cardFlipRotation
                    }
                    val depthScale = 1f - (cardFlipDepth * 0.035f)
                    scaleX = depthScale + (cardTouchGlowProgress * 0.008f)
                    scaleY = depthScale + (cardTouchGlowProgress * 0.008f)
                    translationY = cardFlipDepth * 10.dp.toPx()
                    cameraDistance = 12f * density
                    shape = alarmCardShape
                    clip = false
                }
                .clip(alarmCardShape)
                .background(Color.White, alarmCardShape)
                .border(BorderStroke(0.7.dp, Color(0xFFF6F7F8)), alarmCardShape)
            ) {
                if (!isCardBackVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                if (isThresholdMet) {
                                    onProceed()
                                } else {
                                    isCardFlipped = true
                                }
                            }
	                            .pointerInput(Unit) {
	                                var totalDragX = 0f
                                    var totalDragY = 0f
	                                detectDragGestures(
	                                    onDragStart = {
	                                        totalDragX = 0f
	                                            totalDragY = 0f
	                                            rawSnoozeDrag = if (isSnoozeDrawerOpen) snoozeThresholdPx else 0f
	                                    },
	                                        onDragEnd = {
                                            if (canSnooze && rawSnoozeDrag > snoozeThresholdPx) {
                                                isSnoozeDrawerOpen = true
                                                rawSnoozeDrag = snoozeThresholdPx
                                                coroutineScope.launch {
                                                    cardDragOffset.animateTo(
                                                        snoozeThresholdPx * 0.6f,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessMediumLow
                                                        )
                                                    )
                                                }
                                            } else {
                                                closeSnoozeDrawer()
                                            }
	                                        },
	                                        onDragCancel = {
	                                            closeSnoozeDrawer()
	                                        },
	                                    onDrag = { change, dragAmount ->
	                                        totalDragX += dragAmount.x
                                            totalDragY += dragAmount.y
                                            if (
	                                                !isCardFlipped &&
	                                                dragAmount.y > 0f &&
	                                                abs(totalDragY) > abs(totalDragX)
	                                            ) {
                                                isSnoozeDrawerOpen = false
	                                                rawSnoozeDrag = (rawSnoozeDrag + dragAmount.y).coerceAtLeast(0f)
	                                                coroutineScope.launch {
	                                                    cardDragOffset.snapTo(rawSnoozeDrag * 0.6f)
	                                                }
                                            } else if (
                                                !isCardFlipped &&
                                                isSnoozeDrawerOpen &&
                                                dragAmount.y < 0f &&
                                                abs(totalDragY) > abs(totalDragX)
                                            ) {
                                                rawSnoozeDrag = (rawSnoozeDrag + dragAmount.y).coerceAtLeast(0f)
                                                coroutineScope.launch {
                                                    cardDragOffset.snapTo(rawSnoozeDrag * 0.6f)
                                                }
	                                            } else if (!isThresholdMet && abs(totalDragX) > 24f) {
	                                            isCardFlipped = true
	                                        }
	                                        change.consume()
	                                    }
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colorStops = arrayOf(
                                        0.00f to Color(0xFFE2FFFB).copy(alpha = 0.58f),
                                        0.34f to Color(0xFFF2FFE1).copy(alpha = 0.34f),
                                        0.58f to Color(0xFFFFEFE1).copy(alpha = 0.30f),
                                        0.76f to Color(0xFFFFE4FA).copy(alpha = 0.42f),
                                        1.00f to Color(0xFFE7E5FF).copy(alpha = 0.54f)
                                    ),
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, 0f)
                                )
                            )
                        }

                        // Card Front Content
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 102.dp, bottom = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            // 1. Time Row
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
	                                Box {
	                                    Text(
	                                        text = timeString,
	                                        fontFamily = DentonFontFamily,
	                                        fontWeight = FontWeight.Medium,
	                                        fontSize = 104.sp,
	                                        lineHeight = 96.sp,
	                                        style = LocalTextStyle.current.copy(
	                                            brush = Brush.horizontalGradient(
	                                                colorStops = arrayOf(
	                                                    0.0f to Color(0xFF211C1C),
	                                                    0.70f to Color(0xFF211C1C),
	                                                    1.0f to Color(0xFF6D5757)
	                                                )
		                                            ),
		                                            shadow = Shadow(
		                                                color = Color.Black.copy(alpha = 0.52f),
		                                                offset = Offset(0f, 2.4f),
		                                                blurRadius = 3.2f
		                                            )
		                                        )
		                                    )
	                                    Text(
	                                        text = timeString,
	                                        fontFamily = DentonFontFamily,
	                                        fontWeight = FontWeight.Medium,
	                                        fontSize = 104.sp,
	                                        lineHeight = 96.sp,
	                                        color = Color(0xFFC5C5C5),
	                                        style = LocalTextStyle.current.copy(
		                                            shadow = Shadow(
		                                                color = Color(0xFFC5C5C5),
		                                                offset = Offset(1.1f, 1.1f),
		                                                blurRadius = 2.2f
		                                            )
		                                        ),
		                                        modifier = Modifier
		                                            .offset(x = 1.1.dp, y = 1.1.dp)
		                                            .graphicsLayer {
		                                                alpha = 0.32f
		                                            }
		                                    )
	                                }
			                                Spacer(modifier = Modifier.width(2.dp))
			                                Text(
		                                    text = amPmString,
		                                    fontFamily = DentonFontFamily,
		                                    fontWeight = FontWeight.Medium,
		                                    fontSize = 26.sp,
                                    color = Color(0xFF545454),
                                    style = LocalTextStyle.current.copy(
                                        shadow = Shadow(
                                            color = Color(0xFF262626).copy(alpha = 0.43f),
                                            offset = Offset(0f, 1f),
	                                            blurRadius = 1.1f
	                                        )
	                                    ),
			                                    modifier = Modifier
                                                    .padding(bottom = 18.dp)
                                                    .offset(x = (-2).dp, y = 1.dp)
			                                )
                            }

                            Spacer(modifier = Modifier.height(26.dp))

                            // 2. Leaf-flanked title
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.image_leaf_final),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(width = 39.dp, height = 59.dp)
                                        .graphicsLayer {
                                            scaleX = -1f
                                        }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = task.title,
                                    fontFamily = DentonFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 36.sp,
                                    lineHeight = 34.sp,
                                    color = Color(0xFF211C1C),
                                    textAlign = TextAlign.Center,
                                    style = LocalTextStyle.current.copy(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.36f),
                                            offset = Offset(0f, 2f),
                                            blurRadius = 2.3f
                                        )
                                    ),
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Image(
                                    painter = painterResource(id = R.drawable.image_leaf_final),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(width = 39.dp, height = 59.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // 3. Description
                            Text(
                                text = task.description,
                                fontFamily = SatoshiFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 17.sp,
                                lineHeight = 24.sp,
                                color = Color(0xFFA7A7A7),
                                textAlign = TextAlign.Center,
                                style = LocalTextStyle.current.copy(
                                    shadow = Shadow(
                                        color = Color.White.copy(alpha = 0.65f),
                                        offset = Offset(0f, 1f),
                                        blurRadius = 1.4f
                                    )
                                ),
                                modifier = Modifier
                                    .widthIn(max = 292.dp)
                                    .padding(horizontal = 12.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Watermark bottom end
                        Text(
                            text = if (isThresholdMet) "~Tap to proceed" else "~Tap to flip",
                            fontFamily = DentonFontFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = 16.sp,
                            color = Color(0xFF2C2C2C),
                            style = LocalTextStyle.current.copy(
                                shadow = Shadow(
                                    color = Color(0xFF757575).copy(alpha = 0.43f),
                                    offset = Offset(0f, 0.8f),
                                    blurRadius = 0.8f
                                )
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 40.dp, bottom = 34.dp)
                        )

                        // Completed signature
                        if (isThresholdMet) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawIntoCanvas { canvas ->
                                    drawCenteredSignature(
                                        nativeCanvas = canvas.nativeCanvas,
                                        strokes = strokes,
                                        canvasWidth = size.width,
                                        canvasHeight = size.height,
                                        density = density,
                                        progress = signatureRevealProgress
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    alpha = cardDragProgress
                                }
                                .background(
                                    brush = Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.White.copy(alpha = 0.66f),
                                            0.32f to Color.White.copy(alpha = 0.34f),
                                            0.62f to Color.White.copy(alpha = 0.12f),
                                            1.0f to Color.Transparent
                                        )
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    alpha = glareAlpha * 0.85f
                                }
                                .clip(alarmCardShape)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.glare_for_card),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .graphicsLayer {
                                        scaleX = 2.35f
                                        scaleY = 1.36f
                                        translationX = (glareOffsetX * 0.62f).dp.toPx() + 10.dp.toPx()
                                        translationY = (glareOffsetY * 0.62f).dp.toPx() - 18.dp.toPx()
                                    },
                                contentScale = ContentScale.FillBounds
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Aurora background gradient from resource
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    alpha = (1f - glareAlpha) * 0.85f
                                }
                                .clip(alarmCardShape)
                        ) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                drawRect(
                                    brush = Brush.linearGradient(
                                        colorStops = arrayOf(
                                            0.00f to Color(0xFFE2FFFB).copy(alpha = 0.58f),
                                            0.34f to Color(0xFFF2FFE1).copy(alpha = 0.34f),
                                            0.58f to Color(0xFFFFEFE1).copy(alpha = 0.30f),
                                            0.76f to Color(0xFFFFE4FA).copy(alpha = 0.42f),
                                            1.00f to Color(0xFFE7E5FF).copy(alpha = 0.54f)
                                        ),
                                        start = Offset(0f, size.height),
                                        end = Offset(size.width, 0f)
                                    )
                                )
                            }
                            Image(
                                painter = painterResource(id = R.drawable.glare_for_card),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .graphicsLayer {
                                        scaleX = 2.35f
                                        scaleY = 1.36f
                                        translationX = (glareOffsetX * 0.62f).dp.toPx() + 10.dp.toPx()
                                        translationY = (glareOffsetY * 0.62f).dp.toPx() - 18.dp.toPx()
                                    },
                                contentScale = ContentScale.FillBounds
                            )
                        }

                        // Center placeholder text (fades out as user signs)
                        val placeholderAlpha by animateFloatAsState(
                            targetValue = if (strokes.isNotEmpty() || isCardTouched) 0f else 1f,
                            animationSpec = tween(durationMillis = 200),
                            label = "placeholderAlpha"
                        )
                        if (placeholderAlpha > 0.01f) {
                            val baseColor = Color(0xFFC9C9C9)
                            val shadowColor = Color.Black.copy(alpha = 0.22f)
                            val strokeWidthPx = with(LocalDensity.current) { 1.6.dp.toPx() }
                            
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .width(228.dp)
                                    .graphicsLayer {
                                        alpha = placeholderAlpha
                                        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                                    }
                            ) {
                                // 1. Base Text (drawn normally)
                                Text(
                                    text = "Sign on this to accept the\nagreement that you gonna start",
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    lineHeight = 16.sp,
                                    color = baseColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                // 2. Inner Shadow Stroke (offset and blurred, blended with SrcIn)
                                Text(
                                    text = "Sign on this to accept the\nagreement that you gonna start",
                                    fontFamily = SatoshiFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    lineHeight = 16.sp,
                                    color = shadowColor,
                                    textAlign = TextAlign.Center,
                                    style = TextStyle(
                                        drawStyle = Stroke(
                                            width = strokeWidthPx,
                                            join = StrokeJoin.Round
                                        )
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(x = 0.4.dp, y = 0.4.dp)
                                        .blur(0.8.dp)
                                        .graphicsLayer {
                                            blendMode = androidx.compose.ui.graphics.BlendMode.SrcIn
                                        }
                                )
                            }
                        }

                        // Bottom-right watermark text "~Khela khatam"
                        Text(
                            text = "~${task.title}",
                            fontFamily = DentonFontFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = 14.sp,
                            color = Color(0xFF2C2C2C),
                            style = LocalTextStyle.current.copy(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.43f),
                                    offset = Offset(0f, 1.2f),
                                    blurRadius = 1.3f
                                )
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 40.dp, bottom = 34.dp)
                        )

                        // Drawing canvas on top
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
                            val strokeWidthPx = SignatureLiveStrokeDp.dp.toPx()

                            drawIntoCanvas { canvas ->
                                val nativeCanvas = canvas.nativeCanvas

                                drawSignatureStrokes(
                                    nativeCanvas = nativeCanvas,
                                    strokes = strokes,
                                    strokeWidthPx = strokeWidthPx,
                                    canvasWidth = size.width,
                                    canvasHeight = size.height,
                                    density = density,
                                    shadowAlpha = SignatureLiveShadowAlpha,
                                    shadowBlur = SignatureLiveShadowBlur,
                                    shadowOffsetY = SignatureLiveShadowOffsetY
                                )

                                // Draw current stroke
                                val current = currentStroke.value
                                if (current.size > 1) {
                                    drawSignatureStrokes(
                                        nativeCanvas = nativeCanvas,
                                        strokes = listOf(ColoredStroke(current)),
                                        strokeWidthPx = strokeWidthPx,
                                        canvasWidth = size.width,
                                        canvasHeight = size.height,
                                        density = density,
                                        shadowAlpha = SignatureLiveShadowAlpha,
                                        shadowBlur = SignatureLiveShadowBlur,
                                        shadowOffsetY = SignatureLiveShadowOffsetY
                                    )
                                }
                            }
                        }
                    }
                }

                if (cardTouchGlowProgress > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(alarmCardShape)
                            .border(
                                width = (1.2f + 1.4f * cardTouchGlowProgress).dp,
                                color = CardTouchGlow.copy(alpha = 0.18f + 0.42f * cardTouchGlowProgress),
                                shape = alarmCardShape
                            )
                    )
                }

            }
        }


    }
}

private data class MonthConfig(
    val name: String,
    val year: Int,
    val monthVal: Int,
    val daysCount: Int,
    val offset: Int
)

class StreakViewModel : ViewModel() {
    private val _streakResult = MutableStateFlow<StreakResult>(
        StreakResult(0, java.time.LocalDate.now(), java.time.LocalDate.now())
    )
    val streakResult: StateFlow<StreakResult> = _streakResult.asStateFlow()
    
    private val _completionDates = MutableStateFlow<List<String>>(emptyList())
    val completionDates: StateFlow<List<String>> = _completionDates.asStateFlow()

    fun refresh(context: Context) {
        val stats = TaskStorage.getStats(context)
        _completionDates.value = stats.completionDates ?: emptyList()
        val result = computeCurrentStreak(stats.completionDates)
        _streakResult.value = result
    }
}

@Composable
fun StreakScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: StreakViewModel = remember { StreakViewModel() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val streakResult by viewModel.streakResult.collectAsStateWithLifecycle()
    val completionDates by viewModel.completionDates.collectAsStateWithLifecycle()

    val months = listOf(
        MonthConfig("JAN", 2026, 1, 31, 4),
        MonthConfig("FEB", 2026, 2, 28, 0),
        MonthConfig("MAR", 2026, 3, 31, 0),
        MonthConfig("APR", 2026, 4, 30, 3),
        MonthConfig("MAY", 2026, 5, 31, 5),
        MonthConfig("JUN", 2026, 6, 30, 1),
        MonthConfig("JUL", 2026, 7, 31, 3),
        MonthConfig("AUG", 2026, 8, 31, 6),
        MonthConfig("SEP", 2026, 9, 30, 2),
        MonthConfig("OCT", 2026, 10, 31, 4),
        MonthConfig("NOV", 2026, 11, 30, 0),
        MonthConfig("DEC", 2026, 12, 31, 2)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE1E2E6)) // Figma background color
    ) {
        // 1. Scrollable Month Container (Behind the header background)
        val scrollState = rememberScrollState()
        val density = LocalDensity.current
        val scrollOffsetDp = with(density) { scrollState.value.toDp() }

        val monthHeights = remember {
            months.map { month ->
                val totalSlots = month.offset + month.daysCount
                val rowsCount = (totalSlots + 6) / 7
                40.dp + (rowsCount * 52 - 4).dp + 24.dp
            }
        }

        val monthTops = remember(monthHeights) {
            var accum = 46.dp
            val tops = mutableListOf<androidx.compose.ui.unit.Dp>()
            for (height in monthHeights) {
                tops.add(accum)
                accum += height
            }
            tops
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(top = 312.dp) // Starts at the week labels to allow scroll overlap
                .verticalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(46.dp))

                months.forEachIndexed { i, month ->
                    val monthTop = monthTops[i]
                    val monthHeight = monthHeights[i]

                    Box(
                        modifier = Modifier
                            .width(360.dp)
                            .height(monthHeight)
                    ) {
                        // Month Label on the right
                        val labelScreenY = 312.dp + monthTop - scrollOffsetDp
                        Text(
                            text = month.name,
                            fontFamily = AppSatoshiFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = Color(0xFF3A3A3A),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(y = 0.dp)
                                .scrollFadeBlur(labelScreenY)
                        )

                        // Month Grid Box
                        val monthStart = java.time.LocalDate.of(month.year, month.monthVal, 1)
                        val monthEnd = java.time.LocalDate.of(month.year, month.monthVal, month.daysCount)
                        
                        val overlapDaysRange = if (streakResult.currentStreakCount > 0) {
                            val start = streakResult.streakStartDate
                            val end = streakResult.streakEndDate
                            
                            val overlapStart = if (start.isBefore(monthStart)) monthStart else if (start.isAfter(monthEnd)) null else start
                            val overlapEnd = if (end.isBefore(monthStart)) null else if (end.isAfter(monthEnd)) monthEnd else end
                            
                            if (overlapStart != null && overlapEnd != null && !overlapStart.isAfter(overlapEnd)) {
                                overlapStart.dayOfMonth..overlapEnd.dayOfMonth
                            } else {
                                null
                            }
                        } else {
                            null
                        }

                        val totalSlots = month.offset + month.daysCount
                        val daysList = (1..totalSlots).map { slot ->
                            if (slot <= month.offset) {
                                null
                            } else {
                                val day = slot - month.offset
                                val dateStr = String.format(java.util.Locale.US, "%04d-%02d-%02d", month.year, month.monthVal, day)
                                day to completionDates.contains(dateStr)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .offset(x = 0.dp, y = 40.dp)
                                .width(360.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                val chunkSize = 7
                                val rows = daysList.chunked(chunkSize)
                                rows.forEachIndexed { rowIndex, rowDays ->
                                    val rowScreenY = 312.dp + monthTop + 40.dp + (rowIndex * 52).dp - scrollOffsetDp
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.scrollFadeBlur(rowScreenY)
                                    ) {
                                        rowDays.forEach { slot ->
                                            if (slot == null) {
                                                Box(modifier = Modifier.size(48.dp))
                                            } else {
                                                val (day, isActive) = slot
                                                if (overlapDaysRange != null && day in overlapDaysRange) {
                                                    Box(modifier = Modifier.size(48.dp))
                                                } else {
                                                    CalendarDateCell(day = day, isActive = isActive)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Draw StreakHighlightPills dynamically for this month
                            if (overlapDaysRange != null) {
                                val startDay = overlapDaysRange.first
                                val endDay = overlapDaysRange.last
                                val startSlot = startDay + month.offset - 1
                                val endSlot = endDay + month.offset - 1
                                val startRow = startSlot / 7
                                val endRow = endSlot / 7
                                for (r in startRow..endRow) {
                                    val rowStartSlot = maxOf(startSlot, r * 7)
                                    val rowEndSlot = minOf(endSlot, r * 7 + 6)
                                    if (rowStartSlot <= rowEndSlot) {
                                        val startCol = rowStartSlot % 7
                                        val endCol = rowEndSlot % 7
                                        val numDays = endCol - startCol + 1
                                        val x = (startCol * 52).dp
                                        val y = (r * 52).dp
                                        val width = (numDays * 48 + (numDays - 1) * 4).dp
                                        val pillDays = (rowStartSlot - month.offset + 1 .. rowEndSlot - month.offset + 1).toList()
                                        val pillScreenY = 312.dp + monthTop + 40.dp + (r * 52).dp - scrollOffsetDp
                                        StreakHighlightPill(
                                            days = pillDays,
                                            modifier = Modifier
                                                .offset(x = x, y = y)
                                                .width(width)
                                                .height(48.dp)
                                                .scrollFadeBlur(pillScreenY)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(56.dp))
            }
        }

        // 2. Layer-Blurred Header Background (Overlaying the scrollable content)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(312.dp)
                .graphicsLayer { clip = false } // Allow glare to render beyond container boundary
        ) {
            // Underlay background color with blur to match the original frost overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE1E2E6))
                    .safeBlur(16.7.dp, BlurredEdgeTreatment.Unbounded)
            )

            // ── Decorative Figma Glare Stripes ──
            // Figma uses solid white rectangles (w=49.762dp, tall) rotated -48.48°
            // with 16.05dp blur. They naturally soft-merge into the gray background
            // without needing BlendMode.Screen or PNG assets.

            // Rectangle 22 (714:230) — Secondary edge glare
            // Container: 252.95 x 231.98, offset (-39, 128.39)
            // Inner white bar: 49.762 x 293.771, rounded 7dp, rotated -48.48°, blur 16.05dp
            Box(
                modifier = Modifier
                    .offset(x = (-39).dp, y = 128.39.dp)
                    .size(width = 252.95.dp, height = 231.98.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 49.762.dp, height = 293.771.dp)
                        .graphicsLayer { rotationZ = -48.48f }
                        .safeBlur(16.05.dp, BlurredEdgeTreatment.Unbounded)
                        .background(Color.White.copy(alpha = 0.30f), RoundedCornerShape(7.dp))
                )
            }

            // Rectangle 23 (714:231) — Primary main glare (largest)
            // Using exported Figma glare PNG asset, edge-to-edge
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.rectangle_23),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            }

            // Rectangle 24 (714:232) — Third glare
            // Repositioned to upper third of hero so it doesn't overlap day labels
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = 75.dp, y = (-20).dp)
                    .size(width = 346.85.dp, height = 180.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 49.762.dp, height = 280.dp)
                        .graphicsLayer { rotationZ = -48.48f }
                        .safeBlur(16.05.dp, BlurredEdgeTreatment.Unbounded)
                        .background(Color.White.copy(alpha = 0.45f))
                )
            }
        }

        // 3. Crisp Header Content (Rendered on top of the blurred background)
        // Back Button
        Box(
            modifier = Modifier
                .padding(start = 24.dp, top = 56.dp)
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.8f), Color.White.copy(alpha = 0.1f))
                    ),
                    shape = CircleShape
                )
                .clip(CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.back_button),
                contentDescription = "Back",
                modifier = Modifier.size(20.dp).offset(x = (-2).dp),
                contentScale = ContentScale.Fit
            )
        }

        // Header: "Streak" Title + Line + Watermark Number
        // Original values documented (Step 1):
        // Header Text:
        // - text = "Streak"
        // - fontFamily = AppDentonFontFamily
        // - fontWeight = FontWeight.Medium
        // - fontSize = 32.sp
        // - color = Color(0xFF3A3A3A)
        // - textAlign = TextAlign.Center
        // Divider:
        // - Box width = 93.dp, height = 1.dp, background = Color(0xFFD5D5D5)
        // Column padding: top = 54.dp
        // Spacer height = 4.dp
        // Watermark Box padding: top = 127.dp
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 54.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title block sized to the intrinsic width of the text so the underline matches it
            Column(
                modifier = Modifier.width(IntrinsicSize.Min),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Streak title text styled exactly to Figma node 672:168 specifications:
                // - Vertical gradient from #D3D3D3 to #A4A3A3
                // - Text shadow: 0px 1px 0.3px rgba(243, 243, 243, 0.59)
                Text(
                    text = "Streak",
                    fontFamily = AppDentonFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 32.sp,
                    style = TextStyle(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFD3D3D3),
                                Color(0xFFA4A3A3)
                            )
                        ),
                        shadow = Shadow(
                            color = Color(0xFFF3F3F3).copy(alpha = 0.59f),
                            offset = Offset(0f, 1f),
                            blurRadius = 0.3f
                        )
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Underline divider matching the text width (Step 6)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color(0xFFC8C8C8).copy(alpha = 0.5f))
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = streakResult.currentStreakCount.toString(),
                    fontFamily = AppDentonFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 128.sp,
                    color = Color(0xFF191919),
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0xFF505050).copy(alpha = 0.45f),
                            offset = Offset(1f, 1f),
                            blurRadius = 4.1f
                        )
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Days",
                    fontFamily = AppDentonFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 32.sp,
                    color = Color(0xFFA4A3A3),
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }
        }

        // S M T W T F S Labels row at top = 312.dp
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 312.dp)
                .width(360.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    fontFamily = AppSatoshiFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = Color(0xFF757575),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(48.dp)
                )
            }
        }
    }
}

@Composable
fun CalendarDateCell(day: Int, isActive: Boolean) {
    val cellGrad = if (isActive) {
        Brush.radialGradient(colors = listOf(Color(0xFFFFFFFF), Color(0xFFEFEFEF)))
    } else {
        Brush.radialGradient(colors = listOf(Color(0xFFF2F2F2), Color(0xFFEFEFEF)))
    }
    val textColor = if (isActive) Color(0xFF393939) else Color(0xFFA7A7A7)
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(8.dp),
                ambientColor = Color(0x82A7A7A7),
                spotColor = Color(0x82A7A7A7)
            )
            .background(cellGrad, RoundedCornerShape(8.dp))
            .border(1.2.dp, Color(0xFFFEFFFF), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            fontFamily = AppSatoshiFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = textColor
        )
    }
}

@Composable
fun StreakHighlightPill(days: List<Int>, modifier: Modifier = Modifier) {
    val localDensity = LocalDensity.current
    
    val brush15 = Brush.verticalGradient(
        colorStops = arrayOf(
            0.159f to Color.White,
            0.841f to Color(0xFFC6C6C6)
        )
    )
    val brushMiddle = Brush.verticalGradient(
        colorStops = arrayOf(
            0.486f to Color.White,
            1.0f to Color(0xFF323232)
        )
    )
    val brush19 = Brush.verticalGradient(
        colorStops = arrayOf(
            0.486f to Color.White,
            1.0f to Color(0xFF999999)
        )
    )
    
    Box(
        modifier = modifier
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val nativeCanvas = canvas.nativeCanvas
                    
                    val paint1 = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.BLACK
                        setShadowLayer(
                            7.1.dp.toPx(),
                            5.dp.toPx(),
                            0.dp.toPx(),
                            android.graphics.Color.argb((0.31f * 255).toInt(), 0, 0, 0)
                        )
                    }
                    nativeCanvas.drawRoundRect(
                        0f, 0f, size.width, size.height,
                        8.dp.toPx(), 8.dp.toPx(),
                        paint1
                    )
                    
                    val paint2 = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.BLACK
                        setShadowLayer(
                            10.9.dp.toPx(),
                            0.dp.toPx(),
                            12.dp.toPx(),
                            android.graphics.Color.argb((0.37f * 255).toInt(), 0, 0, 0)
                        )
                    }
                    nativeCanvas.drawRoundRect(
                        0f, 0f, size.width, size.height,
                        8.dp.toPx(), 8.dp.toPx(),
                        paint2
                    )
                }
            }
            .background(Color(0xFF000000), RoundedCornerShape(8.dp))
            .border(1.2.dp, Color(0xFFDBDBDB), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            days.forEach { day ->
                val brush = when (day) {
                    days.first() -> brush15
                    days.last() -> brush19
                    else -> brushMiddle
                }
                
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = day.toString(),
                            fontFamily = AppSatoshiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFFA7A7A7).copy(alpha = 0.51f),
                            modifier = Modifier.safeBlur(2.4.dp)
                        )
                        Text(
                            text = day.toString(),
                            fontFamily = AppSatoshiFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            style = TextStyle(
                                brush = brush,
                                shadow = Shadow(
                                    color = Color(0xFFA7A7A7).copy(alpha = 0.51f),
                                    offset = Offset.Zero,
                                    blurRadius = with(localDensity) { 2.4.dp.toPx() }
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}
