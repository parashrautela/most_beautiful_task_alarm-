package com.example.myapplication

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// ─── Shared Font Families ───────────────────────────────────────────────────
// Previously declared 5× across MainActivity, NewTaskSheet, DatePickerSheet,
// TimePickerSheet, and AlarmDetailScreen. Now a single source of truth.

val AppDentonFontFamily = FontFamily(
    Font(R.font.denton_test_medium, FontWeight.Medium),
    Font(R.font.denton_condensed_test_bold, FontWeight.Bold)
)

val AppSatoshiFontFamily = FontFamily(
    Font(R.font.satoshi_medium, FontWeight.Medium)
)

// NOTE: The unified `innerShadow` modifier will be added here once the
// duplicate declarations in NewTaskSheet.kt and NeomorphicUtils.kt are removed.
// This avoids conflicting overload errors during the step-by-step migration.
