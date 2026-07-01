package com.example.shiftalarm.data

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String,
    val shiftType: ShiftType,
    val shiftCycle: Int,
    val isEnabled: Boolean,
    val startDate: String? = "2026-05-20",
    var nextTriggerDateTime: String? = "Не определено" // ИСПРАВЛЕНО: var вместо val
)