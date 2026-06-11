package com.englishpractice.app.ui.table

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishpractice.app.audio.AudioPlayer
import com.englishpractice.app.data.Note
import com.englishpractice.app.share.ShareUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableScreen(
    notes: List<Note>,
    onPlay: (String) -> Unit,
    onShare: (String) -> Unit,
    onEdit: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onCsvImport: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("📝 英文句子列表", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            actions = {
                IconButton(onClick = onCsvImport) {
                    Icon(Icons.Default.FileOpen, contentDescription = "匯入 CSV")
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "重新整理")
                }
            }
        )

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(vertical = 10.dp, horizontal = 8.dp)
        ) {
            Text("ID", modifier = Modifier.width(36.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("英文", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("中文", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("錄音", modifier = Modifier.width(70.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("建立", modifier = Modifier.width(72.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("錄音日", modifier = Modifier.width(72.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        HorizontalDivider()

        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("尚無筆記，點右下角 + 新增或匯入 CSV", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(notes, key = { it.id }) { note ->
                    NoteRow(
                        note = note,
                        onPlayClick = { note.audioPath?.let { onPlay(it) } },
                        onShareClick = { note.audioPath?.let { onShare(it) } },
                        onEditClick = { onEdit(note) },
                        onDeleteClick = { onDelete(note) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun NoteRow(
    note: Note,
    onPlayClick: () -> Unit,
    onShareClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var playing by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${note.id}",
            modifier = Modifier.width(36.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            note.english,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            note.chinese,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Row(modifier = Modifier.width(70.dp)) {
            if (note.audioPath != null) {
                IconButton(onClick = onPlayClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (playing) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "播放",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onShareClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "分享",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Text("-", color = MaterialTheme.colorScheme.outlineVariant, fontSize = 12.sp)
            }
        }
        Text(note.createdDate.takeLast(5), modifier = Modifier.width(72.dp), fontSize = 12.sp)
        Text(
            note.recordDate?.takeLast(5) ?: "-",
            modifier = Modifier.width(72.dp),
            fontSize = 12.sp,
            color = if (note.recordDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    }
}
