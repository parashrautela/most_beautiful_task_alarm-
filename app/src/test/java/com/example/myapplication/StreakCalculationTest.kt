package com.example.myapplication

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

class StreakCalculationTest {

    @Test
    fun testEmptyCompletions() {
        val today = LocalDate.now()
        val result = computeCurrentStreak(emptyList())
        assertEquals(0, result.currentStreakCount)
        assertEquals(today, result.streakStartDate)
        assertEquals(today, result.streakEndDate)
    }

    @Test
    fun testNullCompletions() {
        val today = LocalDate.now()
        val result = computeCurrentStreak(null)
        assertEquals(0, result.currentStreakCount)
        assertEquals(today, result.streakStartDate)
        assertEquals(today, result.streakEndDate)
    }

    @Test
    fun testCompletedTodayOnly() {
        val today = LocalDate.now()
        val dates = listOf(today.toString())
        val result = computeCurrentStreak(dates)
        assertEquals(1, result.currentStreakCount)
        assertEquals(today, result.streakStartDate)
        assertEquals(today, result.streakEndDate)
    }

    @Test
    fun testFiveConsecutiveDaysEndingToday() {
        val today = LocalDate.now()
        val dates = listOf(
            today.minusDays(4).toString(),
            today.minusDays(3).toString(),
            today.minusDays(2).toString(),
            today.minusDays(1).toString(),
            today.toString()
        )
        val result = computeCurrentStreak(dates)
        assertEquals(5, result.currentStreakCount)
        assertEquals(today.minusDays(4), result.streakStartDate)
        assertEquals(today, result.streakEndDate)
    }

    @Test
    fun testThreeConsecutiveDaysEndingYesterday() {
        val today = LocalDate.now()
        val dates = listOf(
            today.minusDays(3).toString(),
            today.minusDays(2).toString(),
            today.minusDays(1).toString()
        )
        val result = computeCurrentStreak(dates)
        assertEquals(3, result.currentStreakCount)
        assertEquals(today.minusDays(3), result.streakStartDate)
        assertEquals(today.minusDays(1), result.streakEndDate)
    }

    @Test
    fun testBrokenStreak() {
        val today = LocalDate.now()
        val dates = listOf(
            today.minusDays(4).toString(),
            today.minusDays(2).toString(),
            today.toString()
        )
        val result = computeCurrentStreak(dates)
        assertEquals(1, result.currentStreakCount)
        assertEquals(today, result.streakStartDate)
        assertEquals(today, result.streakEndDate)
    }

    @Test
    fun testBrokenStreakEndingYesterday() {
        val today = LocalDate.now()
        val dates = listOf(
            today.minusDays(4).toString(),
            today.minusDays(1).toString()
        )
        val result = computeCurrentStreak(dates)
        assertEquals(1, result.currentStreakCount)
        assertEquals(today.minusDays(1), result.streakStartDate)
        assertEquals(today.minusDays(1), result.streakEndDate)
    }

    @Test
    fun testNoRecentStreak() {
        val today = LocalDate.now()
        val dates = listOf(
            today.minusDays(4).toString(),
            today.minusDays(3).toString(),
            today.minusDays(2).toString()
        )
        val result = computeCurrentStreak(dates)
        assertEquals(0, result.currentStreakCount)
        assertEquals(today, result.streakStartDate)
        assertEquals(today, result.streakEndDate)
    }
}
