package com.artsistem.assistme.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.artsistem.assistme.update.AppUpdater

/** Bir sürümde gelenler. Yeni sürümde listenin EN ÜSTÜNE ekle. */
data class VersionNote(val version: String, val date: String, val items: List<String>)

val VersionHistory: List<VersionNote> = listOf(
    VersionNote(
        "0.3.0", "11.11.2026",
        listOf(
            "Eline alınca sessize alma düzeltildi: artık ses ve titreşim birlikte kesiliyor",
            "Hatırlatmada \"Sadece titreşim\" seçeneği",
            "Alarm ekranı rengi seçilebiliyor (Ayarlar)",
            "Erteleme → Diğer… içinde belirli tarih ve saate erteleme",
            "Geçmiş zamana hatırlatma kurulamıyor; geçmiş gün seçilemiyor",
            "Geçmiş saatle kaydedilen tekrarlı hatırlatma ilk gelecek tekrara kayıyor",
            "Bildirim kaydırılıp kapatılınca alarm sesi de susuyor",
            "Mail modülü kaldırıldı (pasif)",
            "Sürüm geçmişi ekranı"
        )
    ),
    VersionNote(
        "0.2.0", "24.09.2026",
        listOf(
            "Uygulama içi güncelleme (GitHub Releases)",
            "Yeni ana ekran: gün özeti kartı (sağa-sola kaydırma), sürükle-bırak kutu sırası",
            "Saat seçimi için kaydırmalı çark",
            "Erteleme düzeltmesi: tek seferlik hatırlatmanın ertelemesi artık çalıyor",
            "Alarm melodisi seçimi (Ayarlar)",
            "Eline alınca / ses tuşuyla sessize alma"
        )
    ),
    VersionNote(
        "0.1.0", "09.06.2026",
        listOf(
            "Hatırlatmalar: tam ekran sesli alarm, tekrar seçenekleri, gruplar",
            "Esnek erteleme (15 dk / 1 saat / özel süre)",
            "Arama, önemli bayrağı, tekrar bitişi, tamamlananlar geçmişi",
            "Notlar modülü (renkli, sabitlenebilir)",
            "Görevler modülü (basit yapılacaklar listesi)",
            "\"Sıcak & Sakin\" tasarım, açık ve koyu tema"
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionHistoryScreen(onBack: () -> Unit) {
    val current = AppUpdater.currentVersionName(LocalContext.current)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sürüm geçmişi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(VersionHistory, key = { it.version }) { v ->
                val isCurrent = v.version == current
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Sürüm ${v.version}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                if (isCurrent) "Yüklü · ${v.date}" else v.date,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        v.items.forEach { item ->
                            Row {
                                Text("•  ", style = MaterialTheme.typography.bodyMedium)
                                Text(item, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
