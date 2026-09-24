package com.artsistem.assistme.ui

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.artsistem.assistme.mail.MailCategory
import com.artsistem.assistme.reminder.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class ModuleAccent { PRIMARY, SECONDARY, TERTIARY, NEUTRAL }

/** Ana ekran kutusu. [count] null ise büyük sayı yerine "Yakında"/boş gösterilir. */
private data class HomeModule(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val accent: ModuleAccent,
    val count: Int?,
    val detail: String,
    val onOpen: (() -> Unit)?
)

private val DEFAULT_ORDER = listOf("reminders", "tasks", "notes", "mail", "calendar")

/** Özet kartında sağa-sola kaydırılabilen gün aralığı (bugün ortada). */
private const val DAY_PAGES = 731
private const val TODAY_PAGE = DAY_PAGES / 2

private val trLocale = Locale("tr")

@Composable
fun HomeScreen(
    reminderViewModel: ReminderViewModel,
    notesViewModel: NotesViewModel,
    tasksViewModel: TasksViewModel,
    mailViewModel: MailViewModel,
    onOpenReminders: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenMail: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val reminders by reminderViewModel.reminders.collectAsState()
    val history by reminderViewModel.history.collectAsState()
    val notes by notesViewModel.notes.collectAsState()
    val tasks by tasksViewModel.tasks.collectAsState()
    val mails by mailViewModel.messages.collectAsState()
    val mailConnected by mailViewModel.connected.collectAsState()

    // Dakikada bir yenilenen "şimdi": sıradaki hatırlatma ve gün geçişi güncel kalsın.
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(60_000L - System.currentTimeMillis() % 60_000L)
            value = System.currentTimeMillis()
        }
    }

    val today = remember(reminders, history, now) { summarizeDay(0, reminders, history, now) }
    val openTasks = tasks.count { !it.done }
    val criticalMails = mails.count { it.category == MailCategory.CRITICAL.name }
    val replyMails = mails.count { it.category == MailCategory.NEEDS_REPLY.name }

    val modules = listOf(
        HomeModule(
            "reminders", "Hatırlatmalar", Icons.Filled.Notifications, ModuleAccent.PRIMARY,
            count = reminders.count { it.enabled },
            detail = listOfNotNull(
                "Bugün ${today.upcoming.size}",
                reminders.count { it.snoozedUntilMillis != null }.takeIf { it > 0 }?.let { "$it ertelendi" }
            ).joinToString(" · "),
            onOpen = onOpenReminders
        ),
        HomeModule(
            "tasks", "Görevler", Icons.Filled.Checklist, ModuleAccent.TERTIARY,
            count = openTasks,
            detail = "açık · ${tasks.size - openTasks} tamamlandı",
            onOpen = onOpenTasks
        ),
        HomeModule(
            "notes", "Notlar", Icons.Filled.Description, ModuleAccent.SECONDARY,
            count = notes.size,
            detail = notes.count { it.pinned }.let { if (it > 0) "$it sabitlenmiş" else "" },
            onOpen = onOpenNotes
        ),
        HomeModule(
            "mail", "Mail", Icons.Filled.Email, ModuleAccent.PRIMARY,
            count = if (mailConnected) mails.count { !it.isRead } else null,
            detail = if (mailConnected) "$criticalMails kritik · $replyMails yanıt bekliyor" else "Bağlı değil",
            onOpen = onOpenMail
        ),
        HomeModule(
            "calendar", "Takvim", Icons.Filled.CalendarMonth, ModuleAccent.NEUTRAL,
            count = null, detail = "", onOpen = null
        )
    ).associateBy { it.key }

    // Kayıtlı sıra + sonradan eklenen modüller sona.
    var order by remember {
        val saved = Settings.getHomeOrder(context).filter { it in DEFAULT_ORDER }
        mutableStateOf(saved + DEFAULT_ORDER.filter { it !in saved })
    }
    var editing by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState(), enabled = !editing)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "AssistMe",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                if (editing) {
                    FilledTonalButton(onClick = { editing = false }) { Text("Bitti") }
                } else {
                    OutlinedButton(onClick = { editing = true }) { Text("Düzenle") }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Ayarlar")
                }
            }

            DayBriefPager(
                reminders = reminders,
                history = history,
                now = now,
                openTasks = openTasks,
                replyMails = if (mailConnected) replyMails else 0
            )

            ReorderableModuleGrid(
                modules = order.mapNotNull { modules[it] },
                editing = editing,
                onMove = { from, to ->
                    order = order.toMutableList().apply { add(to, removeAt(from)) }
                    Settings.setHomeOrder(context, order)
                },
                onClick = { m ->
                    val open = m.onOpen
                    if (open != null) open()
                    else Toast.makeText(context, "${m.title} yakında eklenecek", Toast.LENGTH_SHORT).show()
                }
            )

            if (editing) {
                Text(
                    "Kutuyu tutup sürükleyerek yerini değiştirin",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Günlük özet kartı (sağa-sola kaydırılabilir)
// ---------------------------------------------------------------------------

@Composable
private fun DayBriefPager(
    reminders: List<com.artsistem.assistme.data.Reminder>,
    history: List<com.artsistem.assistme.data.ReminderHistory>,
    now: Long,
    openTasks: Int,
    replyMails: Int
) {
    val pagerState = rememberPagerState(initialPage = TODAY_PAGE) { DAY_PAGES }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalPager(state = pagerState, pageSpacing = 12.dp) { page ->
            val offset = page - TODAY_PAGE
            val summary = remember(offset, reminders, history, now) {
                summarizeDay(offset, reminders, history, now)
            }
            DayBriefCard(
                offset = offset,
                summary = summary,
                now = now,
                openTasks = openTasks,
                replyMails = replyMails,
                onPrev = { scope.launch { pagerState.animateScrollToPage(page - 1) } },
                onNext = { scope.launch { pagerState.animateScrollToPage(page + 1) } }
            )
        }
        if (pagerState.currentPage != TODAY_PAGE) {
            TextButton(
                onClick = { scope.launch { pagerState.animateScrollToPage(TODAY_PAGE) } },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text("Bugüne dön") }
        }
    }
}

@Composable
private fun DayBriefCard(
    offset: Int,
    summary: DaySummary,
    now: Long,
    openTasks: Int,
    replyMails: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val container = cs.inverseSurface
    val content = cs.inverseOnSurface
    val accent = cs.inversePrimary
    val day = Date(summary.dayStart)
    val bold = SpanStyle(fontWeight = FontWeight.Bold, color = accent)

    val sentence = buildAnnotatedString {
        when {
            offset < 0 -> {
                val fired = summary.history.count { it.kind == "fired" }
                val done = summary.history.count { it.kind == "done" }
                if (summary.history.isEmpty()) append("Bu günden kayıt yok.")
                else {
                    withStyle(bold) { append("$fired hatırlatma") }; append(" çaldı")
                    if (done > 0) { append(", "); withStyle(bold) { append("$done") }; append(" tamamlandı") }
                    append(".")
                }
            }
            offset == 0 -> {
                append("Bugün ")
                if (summary.upcoming.isEmpty()) append("hatırlatma yok")
                else withStyle(bold) { append("${summary.upcoming.size} hatırlatma") }
                append(" ve ")
                withStyle(bold) { append("$openTasks açık görev") }
                append(" var.")
                if (replyMails > 0) {
                    append(" "); withStyle(bold) { append("$replyMails mail") }; append(" yanıt bekliyor.")
                }
            }
            else -> {
                if (summary.upcoming.isEmpty()) append("Bu gün için hatırlatma yok.")
                else { withStyle(bold) { append("${summary.upcoming.size} hatırlatma") }; append(" var.") }
            }
        }
    }

    // Alt satır: bugün sıradaki; gelecekte ilk üç; geçmişte son kayıtlar.
    val detailLines: List<String> = when {
        offset < 0 -> summary.history.takeLast(3).map {
            val kind = if (it.kind == "done") "tamamlandı" else "çaldı"
            "${formatHm(it.completedAtMillis)} · ${it.title} ($kind)"
        }
        offset == 0 -> {
            val next = summary.upcoming.firstOrNull { it.atMillis >= now }
            when {
                next != null -> listOf("Sıradaki: ${formatHm(next.atMillis)} · ${next.title}")
                summary.upcoming.isNotEmpty() -> listOf("Bugünkü hatırlatmalar bitti")
                else -> emptyList()
            }
        }
        else -> summary.upcoming.take(3).map { "${formatHm(it.atMillis)} · ${it.title}" }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 168.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    SimpleDateFormat("d", trLocale).format(day),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 44.sp
                )
                Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(SimpleDateFormat("MMMM yyyy", trLocale).format(day), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        SimpleDateFormat("EEEE", trLocale).format(day),
                        style = MaterialTheme.typography.bodyMedium,
                        color = content.copy(alpha = 0.7f)
                    )
                }
                Surface(shape = RoundedCornerShape(50), color = content.copy(alpha = 0.12f), contentColor = content) {
                    Text(
                        relativeDayLabel(offset),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text(sentence, style = MaterialTheme.typography.bodyLarge)
            if (detailLines.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(content.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    detailLines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1) }
                }
            }
            // Kaydırma ipucu + dokunarak gün değiştirme.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DayNavButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, relativeDayLabel(offset - 1), content, onPrev)
                DayNavButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, relativeDayLabel(offset + 1), content, onNext, trailing = true)
            }
        }
    }
}

@Composable
private fun DayNavButton(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit, trailing: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        if (!trailing) Icon(icon, contentDescription = null, tint = tint.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint.copy(alpha = 0.7f))
        if (trailing) Icon(icon, contentDescription = null, tint = tint.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
    }
}

private fun relativeDayLabel(offset: Int): String = when {
    offset == 0 -> "Bugün"
    offset == 1 -> "Yarın"
    offset == -1 -> "Dün"
    offset > 1 -> "$offset gün sonra"
    else -> "${-offset} gün önce"
}

private fun formatHm(millis: Long): String = SimpleDateFormat("HH:mm", trLocale).format(Date(millis))

// ---------------------------------------------------------------------------
// Sürükle-bırak ile sıralanabilen 2 sütunlu kutu ızgarası
// ---------------------------------------------------------------------------

/**
 * Tüm kutular aynı ebeveynde mutlak konumlanır (key ile); böylece sıra
 * değişince sürüklenen kutunun jest durumu kaybolmaz. Hücre konumu indeksten
 * hesaplanır, çarpışma testi de aynı geometriyle yapılır.
 */
@Composable
private fun ReorderableModuleGrid(
    modules: List<HomeModule>,
    editing: Boolean,
    onMove: (from: Int, to: Int) -> Unit,
    onClick: (HomeModule) -> Unit
) {
    val currentModules by rememberUpdatedState(modules)
    val currentOnMove by rememberUpdatedState(onMove)
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    if (!editing && draggingKey != null) { draggingKey = null; dragOffset = Offset.Zero }

    val wiggle = rememberInfiniteTransition(label = "wiggle")
    val angle by wiggle.animateFloat(
        initialValue = -0.8f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(140), RepeatMode.Reverse), label = "angle"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val gap = 12.dp
        val cell = (maxWidth - gap) / 2
        val rows = (modules.size + 1) / 2
        val cellPx = with(density) { cell.toPx() }
        val stepPx = with(density) { (cell + gap).toPx() }

        fun slot(index: Int) = Offset((index % 2) * stepPx, (index / 2) * stepPx)

        Box(modifier = Modifier.fillMaxWidth().height(cell * rows + gap * (rows - 1).coerceAtLeast(0))) {
            modules.forEachIndexed { index, module ->
                key(module.key) {
                    val isDragging = draggingKey == module.key
                    val target = slot(index)
                    val animated by animateIntOffsetAsState(
                        IntOffset(target.x.roundToInt(), target.y.roundToInt()), label = "slot"
                    )
                    Box(
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .offset {
                                if (isDragging) IntOffset(
                                    (target.x + dragOffset.x).roundToInt(),
                                    (target.y + dragOffset.y).roundToInt()
                                ) else animated
                            }
                            .size(cell)
                            .rotate(if (editing && !isDragging) (if (index % 2 == 0) angle else -angle) else 0f)
                            .pointerInput(module.key, editing) {
                                if (!editing) return@pointerInput
                                detectDragGestures(
                                    onDragStart = { draggingKey = module.key; dragOffset = Offset.Zero },
                                    onDragEnd = { draggingKey = null; dragOffset = Offset.Zero },
                                    onDragCancel = { draggingKey = null; dragOffset = Offset.Zero },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount
                                        val list = currentModules
                                        val from = list.indexOfFirst { it.key == module.key }
                                        if (from < 0) return@detectDragGestures
                                        val center = slot(from) + dragOffset + Offset(cellPx / 2, cellPx / 2)
                                        val col = (center.x / stepPx).toInt().coerceIn(0, 1)
                                        val row = (center.y / stepPx).toInt().coerceIn(0, (list.size - 1) / 2)
                                        val to = (row * 2 + col).coerceAtMost(list.size - 1)
                                        if (to != from) {
                                            currentOnMove(from, to)
                                            // Kutu yeni hücreye geçti: parmağın altında kalsın.
                                            dragOffset -= slot(to) - slot(from)
                                        }
                                    }
                                )
                            }
                    ) {
                        ModuleCard(
                            module = module,
                            editing = editing,
                            lifted = isDragging,
                            onClick = { if (!editing) onClick(module) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleCard(module: HomeModule, editing: Boolean, lifted: Boolean, onClick: () -> Unit) {
    val available = module.onOpen != null
    val cs = MaterialTheme.colorScheme
    val (container, content) = when {
        !available -> cs.surfaceVariant to cs.onSurfaceVariant
        module.accent == ModuleAccent.PRIMARY -> cs.primaryContainer to cs.onPrimaryContainer
        module.accent == ModuleAccent.SECONDARY -> cs.secondaryContainer to cs.onSecondaryContainer
        module.accent == ModuleAccent.TERTIARY -> cs.tertiaryContainer to cs.onTertiaryContainer
        else -> cs.surfaceVariant to cs.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxSize().clickable(enabled = !editing, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        elevation = CardDefaults.cardElevation(defaultElevation = if (lifted) 8.dp else 0.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(content.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(module.icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
                }
                Column {
                    when {
                        module.count != null -> Text(
                            module.count.toString(),
                            fontSize = 36.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        !available -> Surface(
                            shape = RoundedCornerShape(50),
                            color = content.copy(alpha = 0.12f),
                            contentColor = content,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                "Yakında",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(module.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (module.detail.isNotBlank()) {
                        Text(
                            module.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = content.copy(alpha = 0.8f),
                            maxLines = 2
                        )
                    }
                }
            }
            if (editing) {
                Icon(
                    Icons.Filled.DragIndicator,
                    contentDescription = "Sürükle",
                    tint = content.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.TopEnd).size(22.dp)
                )
            }
        }
    }
}
