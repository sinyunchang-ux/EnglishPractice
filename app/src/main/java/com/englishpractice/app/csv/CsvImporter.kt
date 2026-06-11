package com.englishpractice.app.csv

import android.content.Context
import android.net.Uri
import com.englishpractice.app.data.Note
import com.englishpractice.app.data.NoteDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

data class CsvImportResult(
    val total: Int,
    val imported: Int,
    val skipped: Int,
    val errors: List<String>
)

class CsvImporter(
    private val noteDao: NoteDao,
    private val context: Context
) {
    suspend fun import(uri: Uri): CsvImportResult = withContext(Dispatchers.IO) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        var total = 0
        var imported = 0
        var skipped = 0
        val errors = mutableListOf<String>()

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext CsvImportResult(0, 0, 0, listOf("無法開啟檔案"))

            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            var lineNumber = 0

            reader.useLines { lines ->
                lines.forEach { rawLine ->
                    lineNumber++
                    val line = rawLine.trim()
                    if (line.isEmpty()) return@forEach

                    // 跳過第一行（header），簡單偵測：如果同時有 "english" 和 "chinese" 就跳
                    if (lineNumber == 1) {
                        val lower = line.lowercase()
                        if (lower.contains("english") && lower.contains("chinese")) return@forEach
                    }

                    total++
                    // CSV 解析：找第一個逗號當分隔
                    val firstComma = line.indexOf(',')
                    val secondComma = line.indexOf(',', firstComma + 1)

                    if (firstComma == -1) {
                        errors.add("列 $lineNumber：找不到逗號，格式應為 英文,中文")
                        skipped++
                        return@forEach
                    }

                    val english = line.substring(0, firstComma).trim().trim('"')
                    val chinese = if (secondComma != -1) {
                        line.substring(firstComma + 1, secondComma).trim().trim('"')
                    } else {
                        line.substring(firstComma + 1).trim().trim('"')
                    }

                    if (english.isEmpty()) {
                        errors.add("列 $lineNumber：英文欄位空白")
                        skipped++
                        return@forEach
                    }

                    // 去重：英文內容一模一樣就跳過
                    val existing = noteDao.findByEnglish(english)
                    if (existing != null) {
                        skipped++
                        return@forEach
                    }

                    noteDao.insert(
                        Note(
                            english = english,
                            chinese = chinese,
                            createdDate = today
                        )
                    )
                    imported++
                }
            }
        } catch (e: Exception) {
            errors.add("讀取錯誤：${e.message}")
        }

        CsvImportResult(total, imported, skipped, errors)
    }
}
