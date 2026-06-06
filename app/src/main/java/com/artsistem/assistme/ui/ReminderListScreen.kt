package com.artsistem.assistme.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.artsistem.assistme.data.Reminder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    viewModel: ReminderViewModel,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val reminders by viewModel.reminders.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Hatırlatmalar") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Hatırlatma ekle")
            }
        }
    ) { padding ->
        if (reminders.isEmpty()) {
            EmptyState(Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onClick = { onEdit(reminder.id) },
                        onToggle = { viewModel.toggleEnabled(reminder, it) },
                        onDelete = { viewModel.delete(reminder) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
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
                if (reminder.isRepeating) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Repeat,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = repeatLabel(reminder.repeatType, reminder.customIntervalMinutes),
                            style = MaterialTheme.typography.bodySmall
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
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.padding(8.dp)
            )
            Text("Henüz hatırlatma yok", style = MaterialTheme.typography.titleMedium)
            Text(
                "Eklemek için + düğmesine dokun",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
