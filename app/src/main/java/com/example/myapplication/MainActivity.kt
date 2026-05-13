package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme

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

class MainActivity : ComponentActivity() {
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

        setContent {
            MyApplicationTheme {
                TaskAlarmHomeScreen()
            }
        }
    }
}

@Composable
fun TaskAlarmHomeScreen() {
    var showSheet by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var tasks by remember { mutableStateOf(TaskStorage.getTasks(context)) }

    // Refresh tasks when sheet is dismissed
    LaunchedEffect(showSheet) {
        if (!showSheet) {
            tasks = TaskStorage.getTasks(context)
        }
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
                            }
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
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
