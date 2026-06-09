package com.artsistem.assistme.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private enum class ModuleAccent { PRIMARY, SECONDARY, TERTIARY, NEUTRAL }

private data class HomeModule(
    val title: String,
    val icon: ImageVector,
    val accent: ModuleAccent,
    val onOpen: (() -> Unit)?
)

@Composable
fun HomeScreen(
    onOpenReminders: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenTasks: () -> Unit
) {
    val context = LocalContext.current
    val modules = listOf(
        HomeModule("Hatırlatmalar", Icons.Filled.Notifications, ModuleAccent.PRIMARY, onOpenReminders),
        HomeModule("Notlar", Icons.Filled.Description, ModuleAccent.SECONDARY, onOpenNotes),
        HomeModule("Görevler", Icons.Filled.Checklist, ModuleAccent.TERTIARY, onOpenTasks),
        HomeModule("Takvim", Icons.Filled.CalendarMonth, ModuleAccent.NEUTRAL, null)
    )

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp)) {
                Text("AssistMe", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Bugün ne yapmak istersin?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(modules) { m ->
                    ModuleCard(
                        module = m,
                        onClick = {
                            val open = m.onOpen
                            if (open != null) open()
                            else Toast.makeText(context, "${m.title} yakında eklenecek", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleCard(module: HomeModule, onClick: () -> Unit) {
    val available = module.onOpen != null
    val cs = MaterialTheme.colorScheme
    val container: Color
    val content: Color
    when {
        !available -> { container = cs.surfaceVariant; content = cs.onSurfaceVariant }
        module.accent == ModuleAccent.PRIMARY -> { container = cs.primaryContainer; content = cs.onPrimaryContainer }
        module.accent == ModuleAccent.SECONDARY -> { container = cs.secondaryContainer; content = cs.onSecondaryContainer }
        module.accent == ModuleAccent.TERTIARY -> { container = cs.tertiaryContainer; content = cs.onTertiaryContainer }
        else -> { container = cs.surfaceVariant; content = cs.onSurfaceVariant }
    }

    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // İkon rozeti
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(content.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(module.icon, contentDescription = null, tint = content, modifier = Modifier.size(28.dp))
            }
            Column {
                Text(module.title, style = MaterialTheme.typography.titleMedium, color = content)
                if (!available) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = content.copy(alpha = 0.12f),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Text(
                            "Yakında",
                            style = MaterialTheme.typography.labelMedium,
                            color = content,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
