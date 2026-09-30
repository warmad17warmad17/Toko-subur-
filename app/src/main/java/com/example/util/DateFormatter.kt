package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateFormatter {
    private val localeId = Locale("id", "ID")
    private val fullDateTimeFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", localeId)
    private val receiptDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", localeId)
    private val shortDateFormat = SimpleDateFormat("dd MMM yyyy", localeId)
    private val dayAndDateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", localeId)
    private val timeOnlyFormat = SimpleDateFormat("HH:mm", localeId)

    fun formatFullDateTime(timestamp: Long): String {
        return fullDateTimeFormat.format(Date(timestamp))
    }

    fun formatReceiptDateTime(timestamp: Long): String {
        return receiptDateFormat.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        return shortDateFormat.format(Date(timestamp))
    }

    fun formatDayAndDate(timestamp: Long): String {
        return dayAndDateFormat.format(Date(timestamp))
    }

    fun formatTimeOnly(timestamp: Long): String {
        return timeOnlyFormat.format(Date(timestamp))
    }

    fun getStartOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Mengambil waktu awal 7 hari ke belakang (6 hari sebelum hari ini pada jam 00:00:00)
     */
    fun getStartOfLast7Days(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getStartOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getStartOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
