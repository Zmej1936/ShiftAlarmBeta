package com.example.shiftalarm.utils

import com.example.shiftalarm.data.ShiftType
import java.text.SimpleDateFormat
import java.util.*

object ShiftCalculator {

    var startDateString: String = "2026-05-20"
        private set

    private fun getStartCalendar(): Calendar {
        val parts = startDateString.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt() - 1
        val day = parts[2].toInt()
        return Calendar.getInstance().apply {
            set(year, month, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun setStartDate(date: String) {
        startDateString = date
    }

    fun getTodayShiftType(shiftCycle: Int): ShiftType {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val daysSinceStart = daysBetween(getStartCalendar(), today)
        return getShiftTypeByDays(daysSinceStart, shiftCycle)
    }

    fun getShiftTypeForDate(year: Int, month: Int, day: Int, shiftCycle: Int): ShiftType {
        val calendar = Calendar.getInstance().apply {
            set(year, month - 1, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val daysSinceStart = daysBetween(getStartCalendar(), calendar)
        return getShiftTypeByDays(daysSinceStart, shiftCycle)
    }

    fun shouldAlarmRingToday(alarmShiftType: ShiftType, alarmShiftCycle: Int): Boolean {
        if (alarmShiftType == ShiftType.ALL) return true
        val todayType = getTodayShiftType(alarmShiftCycle)
        return todayType == alarmShiftType
    }

    private fun daysBetween(start: Calendar, end: Calendar): Int {
        val diffMillis = end.timeInMillis - start.timeInMillis
        return (diffMillis / (24 * 60 * 60 * 1000)).toInt()
    }

    private fun getShiftTypeByDays(daysSinceStart: Int, shiftCycle: Int): ShiftType {
        val cycleLength = shiftCycle * 2
        var position = daysSinceStart % cycleLength
        if (position < 0) position += cycleLength
        return when (shiftCycle) {
            2 -> when (position) {
                0 -> ShiftType.WORK_DAY_1
                1 -> ShiftType.WORK_DAY_2
                else -> ShiftType.DAY_OFF
            }
            3 -> when (position) {
                0 -> ShiftType.WORK_DAY_1
                1 -> ShiftType.WORK_DAY_2
                2 -> ShiftType.WORK_DAY_3
                else -> ShiftType.DAY_OFF
            }
            else -> ShiftType.ALL
        }
    }

    // Новая функция для получения следующей даты срабатывания
    fun getNextAlarmDateTime(hour: Int, minute: Int, targetShiftType: ShiftType, shiftCycle: Int): Calendar? {
        val now = Calendar.getInstance()
        val startCal = getStartCalendar()

        val checkDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (checkDate.timeInMillis <= now.timeInMillis) {
            checkDate.add(Calendar.DAY_OF_YEAR, 1)
        }

        for (i in 0..60) {
            val year = checkDate.get(Calendar.YEAR)
            val month = checkDate.get(Calendar.MONTH) + 1
            val day = checkDate.get(Calendar.DAY_OF_MONTH)

            val shiftTypeForDate = getShiftTypeForDate(year, month, day, shiftCycle)

            if (shiftTypeForDate == targetShiftType) {
                return checkDate.clone() as Calendar
            }
            checkDate.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    fun formatAlarmDateTime(calendar: Calendar): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return formatter.format(calendar.time)
    }
}