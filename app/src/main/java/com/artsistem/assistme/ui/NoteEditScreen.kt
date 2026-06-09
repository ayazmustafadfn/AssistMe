package com.artsistem.assistme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.artsistem.assistme.data.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    viewModel: NotesViewModel,
    noteId: Long,
    onDone: () -> Unit
) {
    var loaded by remember { mutableStateOf(noteId == 0L) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var color by remember { mutableIntStateOf(0) }
    var pinned by remember { mutableStateOf(false) }
    var createdAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(noteId) {
        if (noteId != 0L) {
            viewModel.getById(noteId)?.let { n ->
                title = n.title
                body = n.body
                color = n.colorArgb
                pinned = n.pinned
                createdAt = n.createdAtMillis
            }
            loaded = true
        }
    }

    fun saveAndExit() {
        if (title.isBlank() && body.isBlank()) {
            // Boş not: yeni ise hiç kaydetme; mevcutsa sil.
            if (noteId != 0L) {
                viewModel.delete(Note(id = noteId, title = title, body = body))
            }
            onDone(); return
        }
        viewModel.save(
            Note(
                id = noteId,
                title = title.trim(),
                body = body.trim(),
                colorArgb = color,
                pinned = pinned,
                createdAtMillis = if (noteId == 0L) System.currentTimeMillis() else createdAt
            )
        )
        onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == 0L) "Yeni Not" else "Notu Düzenle") },
                navigationIcon = {
                    IconButton(onClick = { saveAndExit() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kaydet ve geri")
                    }
                },
                actions = {
                    IconButton(onClick = { pinned = !pinned }) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Sabitle",
                            tint = if (pinned) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (noteId != 0L) {
                        IconButton(onClick = {
                            viewModel.delete(Note(id = noteId))
                            onDone()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Başlık") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = loaded
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Not") },
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp),
                enabled = loaded
            )
            // Renk seçici
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorSwatch(selected = color == 0, content = {
                    // "Renksiz"
                }, ringColor = MaterialTheme.colorScheme.onSurface, fill = MaterialTheme.colorScheme.surface, onClick = { color = 0 })
                GroupColorPalette.forEach { c ->
                    ColorSwatch(
                        selected = color == c,
                        content = {},
                        ringColor = MaterialTheme.colorScheme.onSurface,
                        fill = Color(c),
                        onClick = { color = c }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    selected: Boolean,
    content: @Composable () -> Unit,
    ringColor: Color,
    fill: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (selected) 34.dp else 30.dp)
            .clip(CircleShape)
            .background(fill)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) ringColor else ringColor.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}
