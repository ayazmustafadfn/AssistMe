package com.artsistem.assistme.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class HomeModule(
    val title: String,
    val icon: ImageVector,
    val available: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenReminders: () -> Unit) {
    val context = LocalContext.current
    val modules = listOf(
        HomeModule("Hatırlatmalar", Icons.Filled.Notifications, true),
        HomeModule("Notlar", Icons.Filled.Description, false),
        HomeModule("Görevler", Icons.Filled.Checklist, false),
        HomeModule("Takvim", Icons.Filled.CalendarMonth, false)
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("AssistMe") }) }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(modules) { m ->
                ModuleCard(
                    module = m,
                    onClick = {
                        if (m.available) onOpenReminders()
                        else Toast.makeText(context, "${m.title} yakında eklenecek", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun ModuleCard(module: HomeModule, onClick: () -> Unit) {
    val container = if (module.available) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val content = if (module.available) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = module.icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = module.title,
                style = MaterialTheme.typography.titleMedium,
                color = content,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp)
            )
            if (!module.available) {
                Text(
                    text = "Yakında",
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
