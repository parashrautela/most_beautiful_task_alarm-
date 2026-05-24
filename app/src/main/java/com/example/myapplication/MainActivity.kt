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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

enum class AppScreen { Home, Patterns }

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
                                TaskStorage.logDrop(this@MainActivity, task.title)
                                TaskStorage.deleteTask(this@MainActivity, task.id)
                                setActiveSigningTaskId(null)
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
                                TaskStorage.logDrop(this@MainActivity, fallbackTask.title)
                                TaskStorage.deleteTask(this@MainActivity, fallbackTask.id)
                                setActiveSigningTaskId(null)
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
            // Insights Bar Chart
            Canvas(modifier = Modifier.size(24.dp).clickable { currentScreen = AppScreen.Patterns }) {
                val barW = 2.dp.toPx()
                val gap = 4.dp.toPx()
                val startX = 4.dp.toPx()
                // Bar 1
                drawRect(Color.Black, topLeft = Offset(startX, 10.dp.toPx()), size = Size(barW, 10.dp.toPx()))
                // Bar 2
                drawRect(Color.Black, topLeft = Offset(startX + barW + gap, 4.dp.toPx()), size = Size(barW, 16.dp.toPx()))
                // Bar 3
                drawRect(Color.Black, topLeft = Offset(startX + (barW + gap) * 2, 8.dp.toPx()), size = Size(barW, 12.dp.toPx()))
                // Bottom line
                drawLine(Color.Black, Offset(2.dp.toPx(), 20.dp.toPx()), Offset(22.dp.toPx(), 20.dp.toPx()), strokeWidth = 1.dp.toPx())
            }
            // Notifications Bell
            Canvas(modifier = Modifier.size(24.dp)) {
                val strokeW = 1.5.dp.toPx()
                // Bell dome
                drawArc(
                    color = Color.Black,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = Size(16.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(width = strokeW)
                )
                // Bell sides
                drawLine(Color.Black, Offset(4.dp.toPx(), 12.dp.toPx()), Offset(2.dp.toPx(), 18.dp.toPx()), strokeWidth = strokeW)
                drawLine(Color.Black, Offset(20.dp.toPx(), 12.dp.toPx()), Offset(22.dp.toPx(), 18.dp.toPx()), strokeWidth = strokeW)
                // Bell bottom
                drawLine(Color.Black, Offset(1.dp.toPx(), 18.dp.toPx()), Offset(23.dp.toPx(), 18.dp.toPx()), strokeWidth = strokeW, cap = StrokeCap.Round)
                // Clapper
                drawArc(
                    color = Color.Black,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(10.dp.toPx(), 18.dp.toPx()),
                    size = Size(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = strokeW)
                )
            }
        }

        // FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 32.dp, bottom = 48.dp)
                .size(60.dp)
                .shadow(elevation = 12.dp, shape = CircleShape, spotColor = Color(0x33000000), ambientColor = Color(0x33000000))
                .background(Color.White, CircleShape)
                .clickable { showSheet = true },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                val strokeW = 1.5.dp.toPx()
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
    val signatureRevealProgress by animateFloatAsState(
        targetValue = if (isSignatureComplete) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "signatureRevealProgress"
    )

    // Instruction prompt fades out as user draws
    val drawingProgress = (totalDrawingLength / 1000f).coerceIn(0f, 1f)
    val instructionAlpha by animateFloatAsState(
        targetValue = (1f - drawingProgress * 1.4f).coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "instructionAlpha"
    )

    // ─── Gyroscope parallax ────────────────────────────────────────────────
    val rotationXState = remember { mutableFloatStateOf(0f) }
    val rotationYState = remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor == null) return@DisposableEffect onDispose {}

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
                if (lastTiltUpdateNanos != 0L &&
                    event.timestamp - lastTiltUpdateNanos < CARD_TILT_UPDATE_INTERVAL_NANOS
                ) return
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
                val rawTiltX = ((relPitch * CARD_TILT_INTENSITY) / CARD_TILT_MAX_DEGREES).coerceIn(-1f, 1f)
                val rawTiltY = ((-relRoll * CARD_TILT_INTENSITY) / CARD_TILT_MAX_DEGREES).coerceIn(-1f, 1f)
                filteredTiltX = filteredTiltX * CARD_TILT_LOW_PASS_KEEP + rawTiltX * CARD_TILT_LOW_PASS_NEW
                filteredTiltY = filteredTiltY * CARD_TILT_LOW_PASS_KEEP + rawTiltY * CARD_TILT_LOW_PASS_NEW
                rotationXState.floatValue = filteredTiltX.coerceIn(-1f, 1f) * CARD_TILT_MAX_DEGREES
                rotationYState.floatValue = filteredTiltY.coerceIn(-1f, 1f) * CARD_TILT_MAX_DEGREES
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val alarmCardShape = RoundedCornerShape(24.dp)

    // ─── Figma Colors for rainbow stripe ─────────────────────────────────────
    val rainbowColors = listOf(
        Color(0xFFCCE9FF),
        Color(0xFFBDFFEF),
        Color(0xFFD0FFCA),
        Color(0xFFF4FDCF),
        Color(0xFFFFD0CD),
        Color(0xFFFFBAFB),
        Color(0xFFB4B4FF)
    )
    val rainbowColorsAlpha = rainbowColors.map { it.copy(alpha = 0.64f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ─── 1. Wide background rainbow stripe (Figma: Rectangle 17) ──────
        // Position: left=-86dp, top=196dp, size=508×269dp, rotated -25.88°, blur=25.5dp
        Box(
            modifier = Modifier
                .offset(x = (-86).dp, y = 196.dp)
                .size(width = 509.dp, height = 270.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .rotate(-25.88f)
                    .width(549.dp)
                    .height(33.dp)
                    .background(
                        brush = Brush.horizontalGradient(colors = rainbowColors)
                    )
                    .safeBlur(25.5.dp, BlurredEdgeTreatment.Unbounded)
            )
        }

        // ─── 2. Snooze drawer overlay (appears on drag) ─────────────────────
        if (canSnooze) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 118.dp)
                    .graphicsLayer {
                        alpha = snoozeRevealProgress
                        translationY = (-24).dp.toPx() * (1f - snoozeRevealProgress)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 620.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Bottom emboss layer
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
                    // Inner shadow layer
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
                    // Gradient foreground layer
                    Text(
                        text = snoozePrompt,
                        fontFamily = DentonFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 48.sp,
                        lineHeight = 52.sp,
                        style = TextStyle(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White, Color(0xFFDADADA))
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
                            ) { confirmSnooze() },
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // ─── 3. Snooze confirmation toast ────────────────────────────────────
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

        // ─── 4. Header: Frame 187 – Leaf | Title | Leaf ──────────────────────
        // Figma: x=50dp, y=82dp, width=301dp, height=73dp
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 82.dp)
                .graphicsLayer {
                    // Hide header when snooze drawer is open
                    alpha = (1f - snoozeRevealProgress).coerceIn(0f, 1f)
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.wrapContentSize()
            ) {
                // Left leaf: rotated 180° around Y and scaleY = -1 (flipped)
                Image(
                    painter = painterResource(id = R.drawable.image_leaf_final),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 47.dp, height = 73.dp)
                        .graphicsLayer {
                            rotationZ = 180f
                            scaleY = -1f
                            shadowElevation = with(localDensity) { 2.6.dp.toPx() }
                        }
                        .offset(x = (-4).dp)
                )

                // Title text
                Text(
                    text = task.title,
                    fontFamily = DentonFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 36.sp,
                    lineHeight = 30.sp,
                    color = Color(0xFF211C1C),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = LocalTextStyle.current.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.36f),
                            offset = Offset(0f, 2f),
                            blurRadius = 2.3f
                        )
                    ),
                    modifier = Modifier
                        .widthIn(max = 215.dp)
                        .offset(x = (-4).dp)
                )

                // Right leaf: default orientation
                Image(
                    painter = painterResource(id = R.drawable.image_leaf_final),
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 47.dp, height = 73.dp)
                        .graphicsLayer {
                            shadowElevation = with(localDensity) { 2.6.dp.toPx() }
                        }
                )
            }
        }

        // ─── 5. Description text (Figma: top=155dp) ──────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 155.dp)
                .widthIn(max = 275.dp)
                .graphicsLayer {
                    alpha = (1f - snoozeRevealProgress).coerceIn(0f, 1f)
                }
        ) {
            Text(
                text = task.description,
                fontFamily = SatoshiFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = (16 * 0.9258f).sp,
                color = Color(0xFFC8C8C8),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.48).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = LocalTextStyle.current.copy(
                    shadow = Shadow(
                        color = Color.White.copy(alpha = 0.25f),
                        offset = Offset(0f, 0.1f),
                        blurRadius = 1.1f
                    )
                )
            )
        }

        // ─── 6. Signing card (Figma: left=18dp, top=218dp, 366×456dp) ────────
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 218.dp)
                .size(width = 366.dp, height = 456.dp)
                .graphicsLayer {
                    translationY = cardDragOffset.value
                }
                // Card drop shadow: 0 0 15.7dp rgba(112,112,112,0.25)
                .drawBehind {
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = android.graphics.Color.argb(1, 255, 255, 255)
                            setShadowLayer(
                                15.7.dp.toPx(),
                                0f,
                                0f,
                                android.graphics.Color.argb(
                                    (0.25f * 255).toInt(), 112, 112, 112
                                )
                            )
                        }
                        canvas.nativeCanvas.drawRoundRect(
                            android.graphics.RectF(
                                0f, 0f, size.width, size.height
                            ),
                            24.dp.toPx(), 24.dp.toPx(),
                            paint
                        )
                    }
                }
                .clip(alarmCardShape)
                .background(Color.White, alarmCardShape)
                // Drag gesture for snooze
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
                        onDragCancel = { closeSnoozeDrawer() },
                        onDrag = { change, dragAmount ->
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y
                            if (dragAmount.y > 0f && abs(totalDragY) > abs(totalDragX) && canSnooze) {
                                isSnoozeDrawerOpen = false
                                rawSnoozeDrag = (rawSnoozeDrag + dragAmount.y).coerceAtLeast(0f)
                                coroutineScope.launch {
                                    cardDragOffset.snapTo(rawSnoozeDrag * 0.6f)
                                }
                            } else if (isSnoozeDrawerOpen && dragAmount.y < 0f &&
                                abs(totalDragY) > abs(totalDragX)
                            ) {
                                rawSnoozeDrag = (rawSnoozeDrag + dragAmount.y).coerceAtLeast(0f)
                                coroutineScope.launch {
                                    cardDragOffset.snapTo(rawSnoozeDrag * 0.6f)
                                }
                            }
                            change.consume()
                        }
                    )
                }
        ) {
            // ─── 6a. Internal rainbow glare stripe (Figma: Rectangle 18) ───
            // left=-69dp, top=16dp, size=474×253dp, rotated -25.88°, blur=15.1dp
            Box(
                modifier = Modifier
                    .offset(x = (-69).dp, y = 16.dp)
                    .size(width = 474.dp, height = 253.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .rotate(-25.88f)
                        .width(511.dp)
                        .height(33.dp)
                        .background(
                            brush = Brush.horizontalGradient(colors = rainbowColorsAlpha)
                        )
                        .safeBlur(15.1.dp, BlurredEdgeTreatment.Unbounded)
                )
            }

            // ─── 6b. Drawing canvas (signature area) ─────────────────────
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
                // Render completed centered signature once threshold is met
                if (isThresholdMet) {
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

            // ─── 6c. "Sign on this..." instruction text (center of card) ────
            // Figma: Satoshi Medium, 14sp, #C9C9C9, letter-spacing 0.42px, 228dp wide, centered
            // Fades out as user draws
            if (instructionAlpha > 0.01f) {
                Text(
                    text = "Sign on this to accept the agreement that you gonna start",
                    fontFamily = SatoshiFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = (14 * 1.0585f).sp,
                    color = Color(0xFFC9C9C9),
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.42.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(228.dp)
                        .graphicsLayer { alpha = instructionAlpha }
                )
            }

            // ─── 6d. Watermark bottom-right (Figma: ~Khela khatam) ───────────
            // Figma: Denton Light, 14sp, #2c2c2c, text-shadow 0 1.2 1.3 rgba(0,0,0,0.43)
            // Shows task title watermark when drawing, "~Tap to proceed" when complete
            Text(
                text = if (isThresholdMet) "~Tap to proceed" else "~${task.title.lowercase(java.util.Locale.US)}",
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
                    .padding(end = 20.dp, bottom = 20.dp)
                    .then(
                        if (isThresholdMet) Modifier.clickable { onProceed() } else Modifier
                    )
            )

            // ─── 6e. Touch glow border when signing ──────────────────────────
            if (cardTouchGlowProgress > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(alarmCardShape)
                        .border(
                            width = (0.8f + 1.0f * cardTouchGlowProgress).dp,
                            color = CardTouchGlow.copy(alpha = 0.12f + 0.28f * cardTouchGlowProgress),
                            shape = alarmCardShape
                        )
                )
            }

            // ─── 6f. Snooze drag fade overlay ──────────────────────────────
            if (cardDragProgress > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = cardDragProgress }
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
            }
        }

        // ─── 7. Gyroscope-driven glare on background ─────────────────────────
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
    }
}

