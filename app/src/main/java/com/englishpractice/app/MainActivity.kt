package com.englishpractice.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.englishpractice.app.audio.AudioPlayer
import com.englishpractice.app.csv.CsvImporter
import com.englishpractice.app.data.Note
import com.englishpractice.app.data.NoteDao
import com.englishpractice.app.share.ShareUtil
import com.englishpractice.app.ui.add.AddEditNoteScreen
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = (application as EnglishPracticeApp).database
        setContent {
            MaterialTheme {
                MainApp(database.noteDao())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(noteDao: NoteDao) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddScreen by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }

    val tableVM: TableViewModel = remember { TableViewModel(noteDao) }
    val calendarVM: CalendarViewModel = remember { CalendarViewModel(noteDao) }

    val context = LocalContext.current

    if (showAddScreen || editingNote != null) {
        AddEditNoteScreen(
            existingNote = editingNote,
            onSave = { english, chinese, audioPath ->
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                if (editingNote != null) {
                    val updated = editingNote!!.copy(
                        english = english,
                        chinese = chinese,
                        audioPath = audioPath ?: editingNote!!.audioPath,
                        recordDate = if (audioPath != null && editingNote!!.recordDate == null) today else editingNote!!.recordDate
                    )
                    tableVM.update(updated)
                } else {
                    tableVM.insert(
                        Note(
                            english = english,
                            chinese = chinese,
                            audioPath = audioPath,
                            createdDate = today,
                            recordDate = if (audioPath != null) today else null
                        )
                    )
                }
                showAddScreen = false
                editingNote = null
            },
            onDismiss = {
                showAddScreen = false
                editingNote = null
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.TableChart, "列表") },
                        label = { Text("試算表") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.CalendarMonth, "月曆") },
                        label = { Text("月曆") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                }
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(
                        onClick = { showAddScreen = true }
                    ) {
                        Icon(Icons.Default.Add, "新增")
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> TableScreenContent(
                        viewModel = tableVM,
                        onEdit = { note -> editingNote = note },
                        onInsert = { showAddScreen = true }
                    )
                    1 -> CalendarScreenContent(viewModel = calendarVM)
                }
            }
        }
    }
}

// ─── Table Screen Content ───
@Composable
private fun TableScreenContent(
    viewModel: TableViewModel,
    onEdit: (Note) -> Unit,
    onInsert: () -> Unit
) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsState()
    val playingPath = remember { mutableStateOf<String?>(null) }
    val audioPlayer = remember { AudioPlayer() }

    DisposableEffect(Unit) {
        onDispose { audioPlayer.release() }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val importer = CsvImporter(viewModel.dao, context)
            viewModelScope.launch {
                val result = importer.import(it)
                Toast.makeText(
                    context,
                    "匯入完成：新增 ${result.imported} 筆，跳過 ${result.skipped} 筆",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("📝 英文句子列表", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            actions = {
                IconButton(onClick = {
                    csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                }) {
                    Icon(Icons.Default.FileOpen, "匯入 CSV")
                }
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, "重新整理")
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
                    val isPlaying = playingPath.value == note.audioPath
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEdit(note) }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${note.id}", modifier = Modifier.width(36.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(note.english, modifier = Modifier.weight(1f), fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(note.chinese, modifier = Modifier.weight(1f), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Row(modifier = Modifier.width(70.dp)) {
                            if (note.audioPath != null) {
                                IconButton(onClick = {
                                    val path = note.audioPath!!
                                    if (playingPath.value == path) {
                                        audioPlayer.stop()
                                        playingPath.value = null
                                    } else {
                                        audioPlayer.stop()
                                        audioPlayer.play(path)
                                        playingPath.value = path
                                    }
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, "播放", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { ShareUtil.shareToLine(context, note.audioPath!!) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Share, "分享", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                }
            }
        }
    }
}

// ─── Calendar Screen Content ───
@Composable
private fun CalendarScreenContent(viewModel: CalendarViewModel) {
    val recordCounts by viewModel.recordCounts.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()

    val calendar = remember(currentYear, currentMonth) {
        Calendar.getInstance().apply { set(currentYear, currentMonth - 1, 1) }
    }
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val monthLabel = SimpleDateFormat("yyyy 年 MM 月", Locale.getDefault()).format(calendar.time)
    val totalCells = firstDayOfWeek + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("📅 月曆檢視", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        // 月切換
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.prevMonth() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上一月")
            }
            Text(monthLabel, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.nextMonth() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下一月")
            }
        }

        // 星期抬頭
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val day = cellIndex - firstDayOfWeek + 1
                        if (day in 1..daysInMonth) {
                            val dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", currentYear, currentMonth, day)
                            val count = recordCounts[dateStr] ?: 0
                            val hasRecord = count > 0

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .background(
                                        color = if (hasRecord) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        0.5.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("$day", fontSize = 14.sp, fontWeight = if (hasRecord) FontWeight.Bold else FontWeight.Normal, color = if (hasRecord) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                if (count > 0) {
                                    Text("$count", fontSize = 10.sp, color = if (hasRecord) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 當月統計
        val monthStr = String.format(Locale.getDefault(), "%04d-%02d", currentYear, currentMonth)
        val monthNotes = allNotes.filter { it.createdDate.startsWith(monthStr) }
        val monthRecorded = monthNotes.count { it.recordDate != null }

        Row(modifier = Modifier.padding(horizontal = 16.dp)) {
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("總筆記", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${monthNotes.size}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("有錄音", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("$monthRecorded", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// ─── ViewModels ───
class TableViewModel(private val noteDao: NoteDao) : ViewModel() {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes
    val dao: NoteDao = noteDao

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { noteDao.getAllNotes().collect { _notes.value = it } }
    }

    fun insert(note: Note) {
        viewModelScope.launch { noteDao.insert(note) }
    }

    fun update(note: Note) {
        viewModelScope.launch { noteDao.update(note) }
    }
}

class CalendarViewModel(private val noteDao: NoteDao) : ViewModel() {
    private val _year = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _month = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)

    val currentYear: StateFlow<Int> = _year.asStateFlow()
    val currentMonth: StateFlow<Int> = _month.asStateFlow()

    val recordCounts: StateFlow<Map<String, Int>> = noteDao.getRecordDates()
        .map { rows -> rows.associate { it.createdDate to it.cnt } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val allNotes: StateFlow<List<Note>> = combine(_year, _month) { y, m ->
        String.format(Locale.getDefault(), "%04d-%02d", y, m)
    }.flatMapLatest { yearMonth ->
        noteDao.getNotesByMonth(yearMonth)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun prevMonth() {
        if (_month.value == 1) {
            _month.value = 12
            _year.value -= 1
        } else {
            _month.value -= 1
        }
    }

    fun nextMonth() {
        if (_month.value == 12) {
            _month.value = 1
            _year.value += 1
        } else {
            _month.value += 1
        }
    }
}
