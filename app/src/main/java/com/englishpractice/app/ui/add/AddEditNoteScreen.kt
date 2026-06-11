package com.englishpractice.app.ui.add

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.englishpractice.app.audio.AudioPlayer
import com.englishpractice.app.audio.AudioRecorder
import com.englishpractice.app.data.Note
import com.englishpractice.app.share.ShareUtil
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddEditNoteScreen(
    existingNote: Note? = null,
    onSave: (english: String, chinese: String, audioPath: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var english by remember { mutableStateOf(existingNote?.english ?: "") }
    var chinese by remember { mutableStateOf(existingNote?.chinese ?: "") }
    var audioPath by remember { mutableStateOf(existingNote?.audioPath) }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var showRecordHint by remember { mutableStateOf(false) }

    val recorder = remember { AudioRecorder(context) }
    val player = remember { AudioPlayer() }

    // 計時器
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                elapsedSeconds = recorder.getElapsedSeconds()
                delay(1000)
            }
        }
    }

    // 播放完成監聽
    LaunchedEffect(player) {
        player.onCompletion = { isPlaying = false }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) recorder.cancelRecording()
            player.release()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "需要麥克風權限才能錄音", Toast.LENGTH_SHORT).show()
        }
    }

    val recordColor by animateColorAsState(
        targetValue = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        label = "recordColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    if (existingNote != null) "✏️ 編輯筆記" else "➕ 新增筆記",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "關閉")
                }
            },
            actions = {
                TextButton(
                    onClick = {
                        if (english.isBlank()) {
                            Toast.makeText(context, "英文欄位不可空白", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        onSave(english.trim(), chinese.trim(), audioPath)
                    },
                    enabled = english.isNotBlank()
                ) {
                    Text("儲存", fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 英文欄位
            OutlinedTextField(
                value = english,
                onValueChange = { english = it },
                label = { Text("英文句子 *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            // 中文欄位
            OutlinedTextField(
                value = chinese,
                onValueChange = { chinese = it },
                label = { Text("中文翻譯") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            HorizontalDivider()

            // 錄音區塊
            Text("🎙 錄音（最長 180 秒）", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 時間顯示
                    Text(
                        formatTime(elapsedSeconds),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRecording) recordColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "最大 03:00",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 錄音/停止按鈕
                    FilledIconButton(
                        onClick = {
                            if (isRecording) {
                                // 停止錄音
                                val path = recorder.stopRecording()
                                isRecording = false
                                elapsedSeconds = 0
                                if (path != null) {
                                    audioPath = path
                                    Toast.makeText(context, "錄音完成", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // 開始錄音
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                if (!hasPermission) {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    try {
                                        val path = recorder.startRecording()
                                        audioPath = path
                                        isRecording = true
                                        showRecordHint = true
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "錄音失敗：${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = recordColor
                        )
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "停止" else "錄音",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        if (isRecording) "點擊停止" else "點擊開始錄音",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // 已錄音：試聽+分享
                    if (audioPath != null && !isRecording) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (isPlaying) {
                                        player.stop()
                                        isPlaying = false
                                    } else {
                                        player.play(audioPath!!)
                                        isPlaying = true
                                    }
                                }
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "試聽",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isPlaying) "停止" else "試聽")
                            }
                            FilledTonalButton(
                                onClick = { ShareUtil.shareToLine(context, audioPath!!) }
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "分享",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("分享到 LINE")
                            }
                        }
                        TextButton(
                            onClick = {
                                audioPath = null
                                Toast.makeText(context, "已移除錄音", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "刪除", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("移除錄音", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 建立日期（自動，不給改）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "建立日期：${existingNote?.createdDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                if (existingNote?.recordDate != null) {
                    Text(
                        "錄音日期：${existingNote.recordDate}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}
