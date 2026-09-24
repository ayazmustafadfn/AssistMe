package com.artsistem.assistme.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import com.artsistem.assistme.data.Reminder
import com.artsistem.assistme.data.ReminderHistory
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
            "reminders", "Hatırlatmalar", Icons.Outlined.Notifications, ModuleAccent.PRIMARY,
            count = reminders.count { it.enabled },
            detail = listOfNotNull(
                "Bugün ${today.upcoming.size}",
                reminders.count { it.snoozedUntilMillis != null }.takeIf { it > 0 }?.let { "$it ertelendi" }
            ).joinToString(" · "),
            onOpen = onOpenReminders
        ),
        HomeModule(
            "tasks", "Görevler", Icons.Outlined.Checklist, ModuleAccent.TERTIARY,
            count = openTasks,
            detail = "${tasks.size - openTasks} tamamlandı",
            onOpen = onOpenTasks
        ),
        HomeModule(
            "notes", "Notlar", Icons.Outlined.Description, ModuleAccent.SECONDARY,
            count = notes.size,
            detail = notes.count { it.pinned }.let { if (it > 0) "$it sabitlenmiş" else "" },
            onOpen = onOpenNotes
        ),
        HomeModule(
            "mail", "Mail", Icons.Outlined.Email, ModuleAccent.PRIMARY,
            count = if (mailConnected) mails.count { !it.isRead } else null,
            detail = if (mailConnected) "$criticalMails kritik · $replyMails yanıt bekliyor" else "Bağlı değil",
            onOpen = onOpenMail
        ),
        HomeModule(
            "calendar", "Takvim", Icons.Outlined.CalendarMonth, ModuleAccent.NEUTRAL,
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sade üst çubuk: küçük harf aralıklı başlık, metin düğmesi, ayar ikonu.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "ASSISTME",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.6.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { editing = !editing }) {
                    Text(
                        if (editing) "Bitti" else "Düzenle",
                        color = if (editing) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Ayarlar")
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

/** Kutu/kart zemini: açık temada düz beyaz, koyuda zeminden bir ton açık. */
@Composable
private fun cardColor(): Color {
    val cs = MaterialTheme.colorScheme
    return if (cs.background.luminance() > 0.5f) cs.surfaceContainerLowest else cs.surfaceContainerHigh
}

// ---------------------------------------------------------------------------
// Günlük özet kartı (sağa-sola kaydırılabilir)
// ---------------------------------------------------------------------------

@Composable
private fun DayBriefPager(
    reminders: List<Reminder>,
    history: List<ReminderHistory>,
    now: Long,
    openTasks: Int,
    replyMails: Int
) {
    val pagerState = rememberPagerState(initialPage = TODAY_PAGE) { DAY_PAGES }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalPager(state = pagerState, pageSpacing = 12.dp) { page ->
            val offset = page - TODAY_PAGE
            val summary = remember(offset, reminders, history, now) {
                summarizeDay(offset, reminders, history, now)
            }
            DayBriefCard(offset, summary, now, openTasks, replyMails)
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
    replyMails: Int
) {
    val cs = MaterialTheme.colorScheme
    val day = Date(summary.dayStart)
    val strong = SpanStyle(fontWeight = FontWeight.Medium, color = cs.onSurface)

    val next = if (offset == 0) summary.upcoming.firstOrNull { it.atMillis >= now } else null
    val sentence = buildAnnotatedString {
        when {
            offset < 0 -> {
                val fired = summary.history.count { it.kind == "fired" }
                val done = summary.history.count { it.kind == "done" }
                if (summary.history.isEmpty()) append("Bu günden kayıt yok.")
                else {
                    withStyle(strong) { append("$fired hatırlatma") }; append(" çaldı")
                    if (done > 0) append(", $done tamamlandı")
                    append(".")
                    summary.history.lastOrNull()?.let { append(" Son: ${it.title}.") }
                }
            }
            offset == 0 -> {
                if (summary.upcoming.isEmpty()) append("Hatırlatma yok")
                else withStyle(strong) { append("${summary.upcoming.size} hatırlatma") }
                append(", $openTasks açık görev.")
                if (replyMails > 0) append(" $replyMails mail yanıt bekliyor.")
                when {
                    next != null -> append(" Sıradaki ${formatHm(next.atMillis)} ${next.title}.")
                    summary.upcoming.isNotEmpty() -> append(" Bugünkü hatırlatmalar bitti.")
                }
            }
            else -> {
                if (summary.upcoming.isEmpty()) append("Bu gün için hatırlatma yok.")
                else {
                    withStyle(strong) { append("${summary.upcoming.size} hatırlatma") }
                    val first = summary.upcoming.first()
                    append(". İlki ${formatHm(first.atMillis)} ${first.title}.")
                }
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor(), contentColor = cs.onSurface),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Takvim yaprağı: gün + kısa ay
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .size(48.dp)
                        .border(BorderStroke(1.dp, cs.outlineVariant), RoundedCornerShape(14.dp))
                        .padding(top = 6.dp)
                ) {
                    Text(
                        SimpleDateFormat("d", trLocale).format(day),
                        fontSize = 20.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium
                    )
                    Text(
                        SimpleDateFormat("MMM", trLocale).format(day).uppercase(trLocale),
                        fontSize = 9.sp, lineHeight = 10.sp, letterSpacing = 0.8.sp,
                        color = cs.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        SimpleDateFormat("EEEE", trLocale).format(day),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium
                    )
                    Text(relativeDayLabel(offset), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }
            Text(sentence, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            SegmentBar(offset, summary, now)
        }
    }
}

/**
 * Günün hatırlatmaları kadar parça: geçenler koyu, sıradaki vurgu renginde,
 * sonrakiler açık. Geçmiş günde tüm parçalar dolu (çalan kayıt sayısı kadar).
 */
@Composable
private fun SegmentBar(offset: Int, summary: DaySummary, now: Long) {
    val cs = MaterialTheme.colorScheme
    val count = if (offset < 0) summary.history.count { it.kind == "fired" } else summary.upcoming.size
    if (count == 0) return
    val nextIndex = if (offset == 0) summary.upcoming.indexOfFirst { it.atMillis >= now } else -1
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(count.coerceAtMost(12)) { i ->
            val color = when {
                offset < 0 -> cs.onSurface
                offset > 0 -> cs.surfaceVariant
                i == nextIndex -> cs.primary
                nextIndex == -1 || i < nextIndex -> cs.onSurface
                else -> cs.surfaceVariant
            }
            Box(modifier = Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(50)).background(color))
        }
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
        val gap = 10.dp
        val cellW = (maxWidth - gap) / 2
        val cellH = 116.dp
        val rows = (modules.size + 1) / 2
        val cellWPx = with(density) { cellW.toPx() }
        val cellHPx = with(density) { cellH.toPx() }
        val stepX = with(density) { (cellW + gap).toPx() }
        val stepY = with(density) { (cellH + gap).toPx() }

        fun slot(index: Int) = Offset((index % 2) * stepX, (index / 2) * stepY)

        Box(modifier = Modifier.fillMaxWidth().height(cellH * rows + gap * (rows - 1).coerceAtLeast(0))) {
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
                            .width(cellW)
                            .height(cellH)
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
                                        val center = slot(from) + dragOffset + Offset(cellWPx / 2, cellHPx / 2)
                                        val col = (center.x / stepX).toInt().coerceIn(0, 1)
                                        val row = (center.y / stepY).toInt().coerceIn(0, (list.size - 1) / 2)
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

/** Minimal kutu: modül renginde ince ikon + ince büyük rakam; başlık ve açıklama altta. */
@Composable
private fun ModuleCard(module: HomeModule, editing: Boolean, lifted: Boolean, onClick: () -> Unit) {
    val available = module.onOpen != null
    val cs = MaterialTheme.colorScheme
    val accent = when {
        !available -> cs.outline
        module.accent == ModuleAccent.PRIMARY -> cs.primary
        module.accent == ModuleAccent.SECONDARY -> cs.secondary
        module.accent == ModuleAccent.TERTIARY -> cs.tertiary
        else -> cs.outline
    }

    Card(
        modifier = Modifier.fillMaxSize().clickable(enabled = !editing, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor(), contentColor = cs.onSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (lifted) 6.dp else 0.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(module.icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
                    Text(
                        module.count?.toString() ?: if (available) "" else "Yakında",
                        fontSize = if (module.count != null) 30.sp else 12.sp,
                        lineHeight = if (module.count != null) 30.sp else 14.sp,
                        fontWeight = if (module.count != null) FontWeight.Light else FontWeight.Normal,
                        color = if (module.count != null) cs.onSurface else cs.outline,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
                Column {
                    Text(module.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (module.detail.isNotBlank()) {
                        Text(
                            module.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (editing) {
                Icon(
                    Icons.Filled.DragIndicator,
                    contentDescription = "Sürükle",
                    tint = cs.outline,
                    modifier = Modifier.align(Alignment.TopEnd).size(20.dp)
                )
            }
        }
    }
}
