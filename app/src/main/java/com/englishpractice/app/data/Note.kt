package com.englishpractice.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val english: String,
    val chinese: String,
    val audioPath: String? = null,   // 錄音檔路徑
    val createdDate: String,         // 新增日期 "2026-06-11"
    val recordDate: String? = null  // 錄音日期 "2026-06-11"，有錄音才填
)
