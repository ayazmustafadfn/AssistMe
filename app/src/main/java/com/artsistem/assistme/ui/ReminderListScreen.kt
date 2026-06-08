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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
    onEdit: (Long) -> Unit
) {
    val reminders by viewModel.reminders.collectAsState()
    val groups by viewModel.groups.collectAsState()

    // null = "Tümü" sekmesi
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }

    // Grup oluştur/düzenle penceresi: dialogGroup null + showDialog -> yeni; doluysa düzenle.
    var showGroupDialog by remember { mutableStateOf(false) }
    var groupBeingEdited by remember { mutableStateOf<ReminderGroup?>(null) }
    var groupToDelete by remember { mutableStateOf<ReminderGroup?>(null) }

    // Seçili grup silinmiş olabilir -> Tümü'ye dön.
    if (selectedGroupId != null && groups.none { it.id == selectedGroupId }) {
        selectedGroupId = null
    }

    val groupsById = remember(groups) { groups.associateBy { it.id } }
    val visibleReminders = remember(reminders, selectedGroupId) {
        if (selectedGroupId == null) reminders
        else reminders.filter { it.groupId == selectedGroupId }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Hatırlatmalar") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Hatırlatma ekle")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GroupTabsRow(
                groups = groups,
                selectedGroupId = selectedGroupId,
                onSelect = { selectedGroupId = it },
                onAddGroup = { groupBeingEdited = null; showGroupDialog = true },
                onEditGroup = { groupBeingEdited = it; showGroupDialog = true },
                onDeleteGroup = { groupToDelete = it }
            )

            if (visibleReminders.isEmpty()) {
                EmptyState(
                    hasGroupFilter = selectedGroupId != null,
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
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("Grubu sil") },
            text = { Text("\"${group.name}\" grubu silinecek. İçindeki hatırlatmalar silinmez, \"Grupsuz\" olur.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteGroup(group)
                    groupToDelete = null
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) { Text("İptal") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupTabsRow(
    groups: List<ReminderGroup>,
    selectedGroupId: Long?,
    onSelect: (Long?) -> Unit,
    onAddGroup: () -> Unit,
    onEditGroup: (ReminderGroup) -> Unit,
    onDeleteGroup: (ReminderGroup) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            TabChip(
                selected = selectedGroupId == null,
                colorArgb = null,
                label = "Tümü",
                onClick = { onSelect(null) },
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
                    onClick = { onSelect(group.id) },
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
            // "+" yeni grup
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
    val bg = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

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
                Box(
                    modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(colorArgb))
                )
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
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (reminder.enabled) TextDecoration.None else TextDecoration.LineThrough
                )
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
                        Box(
                            modifier = Modifier.size(10.dp).clip(CircleShape)
                                .background(Color(group.colorArgb))
                        )
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
private fun EmptyState(hasGroupFilter: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.padding(8.dp)
            )
            if (hasGroupFilter) {
                Text("Bu grupta hatırlatma yok", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Eklemek için + düğmesine dokun",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text("Henüz hatırlatma yok", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Eklemek için + düğmesine dokun",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
