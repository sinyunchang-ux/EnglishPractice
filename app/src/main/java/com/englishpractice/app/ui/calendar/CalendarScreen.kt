package com.englishpractice.app.ui.calendar

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishpractice.app.data.Note
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    recordDateCounts: Map<String, Int>,
    allNotes: List<Note>,
    onMonthChange: (Int, Int) -> Unit,
    currentYear: Int,
    currentMonth: Int
) {
    val calendar = remember { Calendar.getInstance() }
    calendar.set(currentYear, currentMonth - 1, 1)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun

    val monthLabel = SimpleDateFormat("yyyy 年 MM 月", Locale.getDefault())
        .format(calendar.time)

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("📅 月曆檢視", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // 月切換
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val m = if (currentMonth == 1) 12 else currentMonth - 1
                val y = if (currentMonth == 1) currentYear - 1 else currentYear
                onMonthChange(y, m)
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上一月")
            }
            Text(monthLabel, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = {
                val m = if (currentMonth == 12) 1 else currentMonth + 1
                val y = if (currentMonth == 12) currentYear + 1 else currentYear
                onMonthChange(y, m)
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下一月")
            }
        }

        // 星期抬頭
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(
                    day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 日期格子
        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7

        Column(modifier = Modifier.fillMaxWidth()) {
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val day = cellIndex - firstDayOfWeek + 1

                        if (day in 1..daysInMonth) {
                            val dateStr = String.format(
                                Locale.getDefault(),
                                "%04d-%02d-%02d",
                                currentYear, currentMonth, day
                            )
                            val count = recordDateCounts[dateStr] ?: 0
                            val hasRecord = count > 0

                            CalendarCell(
                                day = day,
                                count = count,
                                hasRecord = hasRecord,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 當月摘要
        val monthStr = String.format(Locale.getDefault(), "%04d-%02d", currentYear, currentMonth)
        val monthNotes = allNotes.filter { it.createdDate.startsWith(monthStr) }
        val monthRecorded = monthNotes.count { it.recordDate != null }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                "本月統計",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                StatChip(label = "總筆記", value = "${monthNotes.size}")
                Spacer(modifier = Modifier.width(12.dp))
                StatChip(label = "有錄音", value = "$monthRecorded", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CalendarCell(day: Int, count: Int, hasRecord: Boolean, modifier: Modifier = Modifier) {
    val bgColor = when {
        hasRecord -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        hasRecord -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = if (hasRecord) 0.dp else 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "$day",
            fontSize = 14.sp,
            color = textColor,
            fontWeight = if (hasRecord) FontWeight.Bold else FontWeight.Normal
        )
        if (count > 0) {
            Text(
                "$count",
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: androidx.compose.ui.graphics.Color? = null) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = (color ?: MaterialTheme.colorScheme.secondaryContainer).copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}
