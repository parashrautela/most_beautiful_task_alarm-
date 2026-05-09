package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                modifier = Modifier.padding(start = 62.dp, end = 0.dp),
            ) {
                TaskCard(
                    title = "Project Review",
                    subtitle = "Wed, 22 Apr · 1:00 PM",
                    cardBrush = Brush.verticalGradient(
                        listOf(Card1GradientTop, Card1GradientBottom),
                    ),
                )
                Spacer(modifier = Modifier.height(20.dp))
                TaskCard(
                    title = "Project Review",
                    subtitle = "Wed, 22 Apr · 1:00 PM",
                )
            }
        }
        FloatingActionButton(
            onClick = { showSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(60.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = FabGreen,
            contentColor = Color.White,
        ) {
            Text("+", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Light)
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
