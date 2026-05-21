package com.example.shiftalarm.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class ShiftType {
    WORK_DAY_1, WORK_DAY_2, WORK_DAY_3, DAY_OFF, ALL
}

@Parcelize
data class Alarm(
    val id: Int = System.currentTimeMillis().toInt(),
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val label: String = "Будильник",
    val shiftType: ShiftType = ShiftType.ALL,
    val shiftCycle: Int = 2,
    ) : Parcelable