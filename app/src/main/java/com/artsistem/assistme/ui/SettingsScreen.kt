package com.artsistem.assistme.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.artsistem.assistme.reminder.Settings

/** Uygulama ayarları: alarm melodisi + ele alınca sessize alma. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var soundUri by remember { mutableStateOf(Settings.getAlarmSoundUri(context)) }
    var pickupMute by remember { mutableStateOf(Settings.isPickupMuteEnabled(context)) }

    // Sistem zil sesi seçicisi (önizlemeyi kendisi çalar).
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val picked: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        // "Sessiz" seçimi alarmda anlamsız → varsayılana dön.
        Settings.setAlarmSoundUri(context, picked)
        soundUri = Settings.getAlarmSoundUri(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Alarm",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )
            ListItem(
                modifier = Modifier.clickable {
                    picker.launch(
                        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alarm melodisi")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, soundUri)
                        }
                    )
                },
                leadingContent = { Icon(Icons.Filled.MusicNote, contentDescription = null) },
                headlineContent = { Text("Alarm melodisi") },
                supportingContent = { Text(ringtoneTitle(context, soundUri)) }
            )
            HorizontalDivider()
            ListItem(
                modifier = Modifier.clickable {
                    pickupMute = !pickupMute
                    Settings.setPickupMuteEnabled(context, pickupMute)
                },
                leadingContent = { Icon(Icons.Filled.PanTool, contentDescription = null) },
                headlineContent = { Text("Eline alınca sessize al") },
                supportingContent = {
                    Text("Alarm çalarken telefonu kaldırınca ses kesilir, alarm ekranı açık kalır. Ses tuşları da sessize alır.")
                },
                trailingContent = {
                    Switch(checked = pickupMute, onCheckedChange = {
                        pickupMute = it
                        Settings.setPickupMuteEnabled(context, it)
                    })
                }
            )
        }
    }
}

private fun ringtoneTitle(context: Context, uri: Uri?): String {
    if (uri == null) return "Varsayılan"
    return runCatching { RingtoneManager.getRingtone(context, uri)?.getTitle(context) }
        .getOrNull() ?: "Varsayılan"
}
