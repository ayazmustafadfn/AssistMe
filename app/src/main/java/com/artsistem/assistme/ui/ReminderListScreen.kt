package com.artsistem.assistme.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.artsistem.assistme.data.Reminder
import com.artsistem.assistme.data.ReminderGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    viewModel: ReminderViewModel,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onHistory: () -> Unit
) {
    val reminders by viewModel.reminders.collectAsState()
    val groups by viewModel.groups.collectAsState()

    var selectedGroupId by remember { mutableStateOf<Long?>(null) } // null = "Tümü"
    var onlyFlagged by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    var showGroupDialog by remember { mutableStateOf(false) }
    var groupBeingEdited by remember { mutableStateOf<ReminderGroup?>(null) }
    var groupToDelete by remember { mutableStateOf<ReminderGroup?>(null) }

    if (selectedGroupId != null && groups.none { it.id == selectedGroupId }) {
        selectedGroupId = null
    }

    val groupsById = remember(groups) { groups.associateBy { it.id } }
    val visibleReminders = remember(reminders, selectedGroupId, onlyFlagged, searching, query) {
        val base = when {
            searching && query.isNotBlank() -> {
                val q = query.trim().lowercase()
                reminders.filter {
                    it.title.lowercase().contains(q) || it.note.lowercase().contains(q)
                }
            }
            onlyFlagged -> reminders.filter { it.flagged }
            selectedGroupId == null -> reminders
            else -> reminders.filter { it.groupId == selectedGroupId }
        }
        // Önemliler üstte, sonra zamana göre.
        base.sortedWith(compareByDescending<Reminder> { it.flagged }.thenBy { it.triggerAtMillis })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searching) {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Ara…") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    } else {
                        Text("Hatırlatmalar")
                    }
                },
                navigationIcon = {
                    if (searching) {
                        IconButton(onClick = { searching = false; query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Aramayı kapat")
                        }
                    }
                },
                actions = {
                    if (!searching) {
                        IconButton(onClick = { searching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Ara")
                        }
                        IconButton(onClick = onHistory) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Geçmiş")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Hatırlatma ekle")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!searching) {
                GroupTabsRow(
                    groups = groups,
                    selectedGroupId = selectedGroupId,
                    onlyFlagged = onlyFlagged,
                    onSelectAll = { selectedGroupId = null; onlyFlagged = false },
                    onSelectFlagged = { onlyFlagged = true; selectedGroupId = null },
                    onSelectGroup = { selectedGroupId = it; onlyFlagged = false },
                    onAddGroup = { groupBeingEdited = null; showGroupDialog = true },
                    onEditGroup = { groupBeingEdited = it; showGroupDialog = true },
                    onDeleteGroup = { groupToDelete = it }
                )
            }

            if (visibleReminders.isEmpty()) {
                EmptyState(
                    searching = searching && query.isNotBlank(),
                    filtered = selectedGroupId != null || onlyFlagged,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleReminders, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            group = reminder.groupId?.let { groupsById[it] },
                            onClick = { onEdit(reminder.id) },
                            onToggle = { viewModel.toggleEnabled(reminder, it) },
                            onDelete = { viewModel.delete(reminder) }
                        )
                    }
                }
            }
        }
    }

    if (showGroupDialog) {
        GroupEditDialog(
            initial = groupBeingEdited,
            onConfirm = { name, color ->
                val editing = groupBeingEdited
                if (editing == null) viewModel.createGroup(name, color)
                else viewModel.renameGroup(editing, name, color)
                showGroupDialog = false
            },
            onDismiss = { showGroupDialog = false }
        )
    }

    groupToDelete?.let { group ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("Grubu sil") },
            text = { Text("\"${group.name}\" grubu silinecek. İçindeki hatırlatmalar silinmez, \"Grupsuz\" olur.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.deleteGroup(group)
                    groupToDelete = null
                }) { Text("Sil") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { groupToDelete = null }) { Text("İptal") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupTabsRow(
    groups: List<ReminderGroup>,
    selectedGroupId: Long?,
    onlyFlagged: Boolean,
    onSelectAll: () -> Unit,
    onSelectFlagged: () -> Unit,
    onSelectGroup: (Long) -> Unit,
    onAddGroup: () -> Unit,
    onEditGroup: (ReminderGroup) -> Unit,
    onDeleteGroup: (ReminderGroup) -> Unit
) {
    val allSelected = selectedGroupId == null && !onlyFlagged
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            TabChip(selected = allSelected, colorArgb = null, label = "Tümü", onClick = onSelectAll, onLongClick = null)
        }
        item {
            TabChip(
                selected = onlyFlagged,
                colorArgb = null,
                label = "⚑ Önemli",
                onClick = onSelectFlagged,
                onLongClick = null
            )
        }
        items(groups, key = { it.id }) { group ->
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                TabChip(
                    selected = selectedGroupId == group.id,
                    colorArgb = group.colorArgb,
                    label = group.name,
                    onClick = { onSelectGroup(group.id) },
                    onLongClick = { menuOpen = true }
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Yeniden adlandır") },
                        onClick = { menuOpen = false; onEditGroup(group) }
                    )
                    DropdownMenuItem(
                        text = { Text("Sil") },
                        onClick = { menuOpen = false; onDeleteGroup(group) }
                    )
                }
            }
        }
        item {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(36.dp).clickable { onAddGroup() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "Grup ekle")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabChip(
    selected: Boolean,
    colorArgb: Int?,
    label: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        color = bg,
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (colorArgb != null) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(colorArgb)))
            }
            Text(text = label, color = fg, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    group: ReminderGroup?,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (reminder.flagged) {
                        Icon(
                            Icons.Default.Flag,
                            contentDescription = "Önemli",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp).padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (reminder.enabled) TextDecoration.None else TextDecoration.LineThrough
                    )
                }
                if (reminder.note.isNotBlank()) {
                    Text(
                        text = reminder.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.padding(top = 4.dp))
                Text(
                    text = formatDateTime(reminder.triggerAtMillis),
                    style = MaterialTheme.typography.bodySmall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (reminder.isRepeating) {
                        Icon(
                            Icons.Outlined.Repeat,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = repeatLabel(reminder.repeatType, reminder.customIntervalMinutes),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    if (group != null) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(group.colorArgb)))
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
            Switch(checked = reminder.enabled, onCheckedChange = onToggle)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Sil")
            }
        }
    }
}

@Composable
private fun EmptyState(searching: Boolean, filtered: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.padding(8.dp))
            val title = when {
                searching -> "Eşleşen hatırlatma yok"
                filtered -> "Bu görünümde hatırlatma yok"
                else -> "Henüz hatırlatma yok"
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (!searching) {
                Text(
                    "Eklemek için + düğmesine dokun",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
