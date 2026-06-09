package com.artsistem.assistme.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artsistem.assistme.data.MailMessage
import com.artsistem.assistme.mail.MailCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailScreen(
    viewModel: MailViewModel,
    onBack: () -> Unit
) {
    val connected by viewModel.connected.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()

    // Bağlıyken cache boşsa (ör. süreç yeniden başladı) bir kez tazele.
    LaunchedEffect(connected) {
        if (connected && messages.isEmpty()) viewModel.sync()
    }

    var menuOpen by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf<MailCategory?>(null) } // null = Tümü

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Ana menü")
                    }
                },
                actions = {
                    if (connected) {
                        IconButton(onClick = { viewModel.sync() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Daha fazla")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Hesabı çıkar") },
                                    onClick = { menuOpen = false; viewModel.disconnect() }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (!connected) {
            ConnectContent(
                loading = loading,
                onConnect = { viewModel.connect() },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                CategoryTabs(
                    messages = messages,
                    selected = selectedTab,
                    onSelect = { selectedTab = it }
                )
                val visible = if (selectedTab == null) messages
                else messages.filter { it.category == selectedTab!!.name }

                if (visible.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (loading) "Yükleniyor…" else "Bu kategoride mail yok",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(visible, key = { it.id }) { msg -> MailCard(msg) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectContent(loading: Boolean, onConnect: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.MailOutline,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            "Mail asistanı",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            "Microsoft 365 hesabını bağla; yanıt bekleyen, kritik (lisans/ödeme) ve takip gereken mailleri senin için öne çıkarayım. Mailler cihazında işlenir, içeriği dışarı çıkmaz.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(onClick = onConnect, enabled = !loading, modifier = Modifier.padding(top = 24.dp)) {
            Text(if (loading) "Bağlanıyor…" else "Microsoft ile bağlan")
        }
        Text(
            "(Faz 1: örnek verilerle çalışır)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun CategoryTabs(
    messages: List<MailMessage>,
    selected: MailCategory?,
    onSelect: (MailCategory?) -> Unit
) {
    val counts = remember(messages) { messages.groupingBy { it.category }.eachCount() }
    val tabs = listOf(
        null to "Tümü",
        MailCategory.NEEDS_REPLY to MailCategory.NEEDS_REPLY.label,
        MailCategory.CRITICAL to MailCategory.CRITICAL.label,
        MailCategory.DEADLINE to MailCategory.DEADLINE.label,
        MailCategory.FOLLOW_UP to MailCategory.FOLLOW_UP.label
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs) { (cat, label) ->
            val count = if (cat == null) messages.size else counts[cat.name] ?: 0
            val isSel = selected == cat
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                onClick = { onSelect(cat) }
            ) {
                Text(
                    text = if (count > 0) "$label ($count)" else label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MailCard(msg: MailMessage) {
    val cs = MaterialTheme.colorScheme
    val (chipBg, chipFg) = when (msg.category) {
        MailCategory.CRITICAL.name -> cs.errorContainer to cs.onErrorContainer
        MailCategory.NEEDS_REPLY.name -> cs.primaryContainer to cs.onPrimaryContainer
        MailCategory.DEADLINE.name -> cs.tertiaryContainer to cs.onTertiaryContainer
        MailCategory.FOLLOW_UP.name -> cs.secondaryContainer to cs.onSecondaryContainer
        else -> cs.surfaceVariant to cs.onSurfaceVariant
    }
    val categoryLabel = runCatching { MailCategory.valueOf(msg.category).label }.getOrDefault("")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = msg.fromName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (!msg.isRead) FontWeight.Bold else FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDateTime(msg.receivedAtMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }
            Text(
                text = msg.subject,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (!msg.isRead) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = msg.preview,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (msg.category != MailCategory.OTHER.name && msg.reason.isNotBlank()) {
                Spacer(Modifier.size(8.dp))
                Surface(shape = RoundedCornerShape(50), color = chipBg) {
                    Text(
                        text = "$categoryLabel · ${msg.reason}",
                        style = MaterialTheme.typography.labelMedium,
                        color = chipFg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
