package com.example.shiftalarm.utils

import com.example.shiftalarm.data.ShiftType
import java.text.SimpleDateFormat
import java.util.*

object ShiftCalculator {

    private val dbFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun getStartCalendar(startDateStr: String): Calendar {
        val calendar = Calendar.getInstance()
        try {
            dbFormatter.parse(startDateStr)?.let { date ->
                calendar.time = date
            }
        } catch (e: Exception) {
            calendar.set(2026, Calendar.MAY, 20)
        }
        return calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun getShiftTypeForDate(year: Int, month: Int, day: Int, shiftCycle: Int, startDateStr: String): ShiftType {
        val calendar = Calendar.getInstance().apply {
            set(year, month - 1, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val daysSinceStart = daysBetween(getStartCalendar(startDateStr), calendar)
        val cycleLength = shiftCycle * 2
        val position = ((daysSinceStart % cycleLength) + cycleLength) % cycleLength

        return if (position < shiftCycle) ShiftType.WORK_DAY else ShiftType.OFF_DAY
    }

    fun shouldAlarmRingToday(alarmShiftType: ShiftType, alarmShiftCycle: Int, startDateStr: String): Boolean {
        if (alarmShiftType == ShiftType.ALL) return true
        val today = Calendar.getInstance()
        val year = today.get(Calendar.YEAR)
        val month = today.get(Calendar.MONTH) + 1
        val day = today.get(Calendar.DAY_OF_MONTH)
        val todayType = getShiftTypeForDate(year, month, day, alarmShiftCycle, startDateStr)
        return todayType == alarmShiftType
    }

    private fun daysBetween(start: Calendar, end: Calendar): Int {
        val diffMillis = end.timeInMillis - start.timeInMillis
        return kotlin.math.round(diffMillis.toDouble() / (24 * 60 * 60 * 1000)).toInt()
    }

    // ИСПРАВЛЕНО: Безопасный алгоритм линейного поиска без застревания в текущем цикле смен
    fun getNextAlarmDateTime(hour: Int, minute: Int, targetShiftType: ShiftType, shiftCycle: Int, startDateStr: String): Calendar? {
        val now = Calendar.getInstance()

        // Берем за основу текущий день устройства
        val checkDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Если время будильника на сегодня уже наступило или прошло,
        // мы ОДИН раз принудительно шагаем на завтрашний день перед проверками.
        if (checkDate.timeInMillis <= now.timeInMillis) {
            checkDate.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Запускаем последовательное сканирование дней вперед
        for (i in 0..365) {
            val year = checkDate.get(Calendar.YEAR)
            val month = checkDate.get(Calendar.MONTH) + 1
            val day = checkDate.get(Calendar.DAY_OF_MONTH)

            val shiftTypeForDate = getShiftTypeForDate(year, month, day, shiftCycle, startDateStr)

            // Если тип дня полностью совпадает с типом будильника — это то, что мы искали!
            if (targetShiftType == ShiftType.ALL || shiftTypeForDate == targetShiftType) {
                return checkDate.clone() as Calendar
            }

            // Если день не подошел, шагаем на +1 день вперед и проверяем его на следующей итерации
            checkDate.add(Calendar.DAY_OF_YEAR, 1)
        }
        return null
    }

    fun formatAlarmDateTime(calendar: Calendar): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return formatter.format(calendar.time)
    }
}