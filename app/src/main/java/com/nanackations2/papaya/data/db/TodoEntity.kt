package com.nanackations2.papaya.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: String,
    val title: String,
    val hour: Int,
    val minute: Int,
    val isDone: Boolean = false,
    val enabled: Boolean = true,
) {
    val timeString: String
        get() = String.format("%02d:%02d", hour, minute)
}
