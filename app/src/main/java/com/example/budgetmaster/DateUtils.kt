package com.example.budgetmaster.utils

import java.util.Calendar
import java.util.Locale

object DateUtils {

    fun getStartOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getEndOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun getStartOfWeek(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        }.timeInMillis
    }

    fun getEndOfWeek(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = getStartOfWeek(timestamp)
            add(Calendar.WEEK_OF_YEAR, 1)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis
    }

    fun getStartOfMonth(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
    }

    fun getEndOfMonth(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = getStartOfMonth(timestamp)
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis
    }

    fun getMonthName(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
    }

    fun getYear(timestamp: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return calendar.get(Calendar.YEAR)
    }

    fun getDayOfMonth(timestamp: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return calendar.get(Calendar.DAY_OF_MONTH)
    }

    fun getDayOfWeek(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()) ?: ""
    }
}