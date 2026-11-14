package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    private fun isoFormatter(timeZone: TimeZone = TimeZone.getDefault()): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            this.timeZone = timeZone
            isLenient = false
        }
    }

    private fun displayFormatter(): SimpleDateFormat {
        return SimpleDateFormat("MMM d, yyyy", Locale.US)
    }

    private fun shortDayFormatter(): SimpleDateFormat {
        return SimpleDateFormat("EEE d", Locale.US)
    }

    private fun monthYearFormatter(): SimpleDateFormat {
        return SimpleDateFormat("MMMM yyyy", Locale.US)
    }

    private fun shortMonthFormatter(): SimpleDateFormat {
        return SimpleDateFormat("MMM yyyy", Locale.US)
    }

    fun todayDateString(nowMillis: Long = System.currentTimeMillis()): String {
        return isoFormatter().format(Date(nowMillis))
    }

    fun yesterdayDateString(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isoFormatter().format(cal.time)
    }

    fun parseIsoToCalendar(dateString: String): Calendar? {
        return try {
            val parsed = isoFormatter().parse(dateString) ?: return null
            Calendar.getInstance().apply {
                time = parsed
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun utcPickerMillisToIsoDate(utcMillis: Long): String {
        return isoFormatter(TimeZone.getTimeZone("UTC")).format(Date(utcMillis))
    }

    fun isoDateToUtcPickerMillis(dateString: String): Long {
        return try {
            isoFormatter(TimeZone.getTimeZone("UTC")).parse(dateString)?.time
                ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    fun startOfWeek(dateString: String, weekStartsOnMonday: Boolean): String {
        val cal = parseIsoToCalendar(dateString) ?: return dateString
        val firstDay = if (weekStartsOnMonday) Calendar.MONDAY else Calendar.SUNDAY
        cal.firstDayOfWeek = firstDay
        val currentDow = cal.get(Calendar.DAY_OF_WEEK)
        var diff = currentDow - firstDay
        if (diff < 0) diff += 7
        cal.add(Calendar.DAY_OF_YEAR, -diff)
        return isoFormatter().format(cal.time)
    }

    fun endOfWeek(dateString: String, weekStartsOnMonday: Boolean): String {
        val start = parseIsoToCalendar(startOfWeek(dateString, weekStartsOnMonday)) ?: return dateString
        start.add(Calendar.DAY_OF_YEAR, 6)
        return isoFormatter().format(start.time)
    }

    fun previousWeekRange(today: String, weekStartsOnMonday: Boolean): Pair<String, String> {
        val startCal = parseIsoToCalendar(startOfWeek(today, weekStartsOnMonday))
            ?: return today to today
        startCal.add(Calendar.DAY_OF_YEAR, -7)
        val prevStart = isoFormatter().format(startCal.time)
        startCal.add(Calendar.DAY_OF_YEAR, 6)
        val prevEnd = isoFormatter().format(startCal.time)
        return prevStart to prevEnd
    }

    fun startOfMonth(dateString: String): String {
        val cal = parseIsoToCalendar(dateString) ?: return dateString
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return isoFormatter().format(cal.time)
    }

    fun endOfMonth(dateString: String): String {
        val cal = parseIsoToCalendar(dateString) ?: return dateString
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        return isoFormatter().format(cal.time)
    }

    fun previousMonthRange(today: String): Pair<String, String> {
        val cal = parseIsoToCalendar(startOfMonth(today)) ?: return today to today
        cal.add(Calendar.MONTH, -1)
        val start = isoFormatter().format(cal.time)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val end = isoFormatter().format(cal.time)
        return start to end
    }

    fun startOfYear(dateString: String): String {
        val year = dateString.take(4)
        return "$year-01-01"
    }

    fun endOfYear(dateString: String): String {
        val year = dateString.take(4)
        return "$year-12-31"
    }

    fun formatDisplayDate(dateString: String): String {
        val cal = parseIsoToCalendar(dateString) ?: return dateString
        return displayFormatter().format(cal.time)
    }

    fun formatRelativeHeader(dateString: String, today: String = todayDateString()): String {
        if (dateString == today) return "Today • ${formatDisplayDate(dateString)}"
        val calToday = parseIsoToCalendar(today)
        if (calToday != null) {
            calToday.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = isoFormatter().format(calToday.time)
            if (dateString == yesterday) return "Yesterday • ${formatDisplayDate(dateString)}"
        }
        return formatDisplayDate(dateString)
    }

    fun formatShortDay(dateString: String): String {
        val cal = parseIsoToCalendar(dateString) ?: return dateString
        return shortDayFormatter().format(cal.time)
    }

    fun formatWeekRangeLabel(startIso: String, endIso: String): String {
        val s = parseIsoToCalendar(startIso) ?: return "$startIso - $endIso"
        val e = parseIsoToCalendar(endIso) ?: return "$startIso - $endIso"
        val sFmt = SimpleDateFormat("MMM d", Locale.US)
        val eFmt = if (s.get(Calendar.MONTH) == e.get(Calendar.MONTH)) {
            SimpleDateFormat("d, yyyy", Locale.US)
        } else {
            SimpleDateFormat("MMM d, yyyy", Locale.US)
        }
        return "${sFmt.format(s.time)} – ${eFmt.format(e.time)}"
    }

    fun formatMonthYear(yearMonth: String): String {
        val cal = parseIsoToCalendar("$yearMonth-01") ?: return yearMonth
        return monthYearFormatter().format(cal.time)
    }

    fun formatShortMonth(yearMonth: String): String {
        val cal = parseIsoToCalendar("$yearMonth-01") ?: return yearMonth
        return shortMonthFormatter().format(cal.time)
    }

    fun addDays(dateString: String, days: Int): String {
        val cal = parseIsoToCalendar(dateString) ?: return dateString
        cal.add(Calendar.DAY_OF_YEAR, days)
        return isoFormatter().format(cal.time)
    }

    fun isValidIsoDate(dateString: String): Boolean {
        return Regex("""^\d{4}-\d{2}-\d{2}$""").matches(dateString) && parseIsoToCalendar(dateString) != null
    }
}
