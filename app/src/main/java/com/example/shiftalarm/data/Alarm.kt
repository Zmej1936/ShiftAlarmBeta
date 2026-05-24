package com.example.shiftalarm.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String,
    val shiftType: ShiftType,
    val shiftCycle: Int,   // теперь всегда равен глобальному циклу из настроек
    val isEnabled: Boolean
) : Parcelable