package com.artsistem.assistme.update

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File

/**
 * Açılışta sessizce güncelleme denetler; yeni sürüm varsa sorar, indirir ve
 * sistem kurulum ekranını açar. [manualCheck] her arttığında (Ayarlar'daki
 * "Güncellemeleri denetle") yeniden denetler ve sonucu Toast'la bildirir.
 */
@Composable
fun UpdatePrompt(manualCheck: Int = 0) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<UpdateInfo?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var downloaded by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(manualCheck) {
        val found = AppUpdater.checkForUpdate(context)
        info = found
        if (manualCheck > 0 && found == null) {
            Toast.makeText(context, "Uygulama güncel (${AppUpdater.currentVersionName(context)})", Toast.LENGTH_SHORT).show()
        }
    }

    fun installOrAskPermission(apk: File) {
        if (AppUpdater.canInstall(context)) AppUpdater.install(context, apk)
        else {
            Toast.makeText(context, "AssistMe'ye uygulama yükleme izni verip geri dönün", Toast.LENGTH_LONG).show()
            AppUpdater.openInstallPermissionSettings(context)
        }
    }

    val current = info ?: return
    AlertDialog(
        onDismissRequest = { if (!downloading) info = null },
        title = { Text("Yeni sürüm: ${current.versionName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (current.notes.isNotBlank()) Text(current.notes, style = MaterialTheme.typography.bodyMedium)
                when {
                    downloading -> LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    downloaded != null -> Text("İndirildi. Kurulum ekranında \"Güncelle\"ye basın.")
                    error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !downloading,
                onClick = {
                    downloaded?.let { installOrAskPermission(it); return@TextButton }
                    downloading = true; error = null
                    scope.launch {
                        try {
                            val apk = AppUpdater.download(context, current) { progress = it }
                            downloaded = apk
                            installOrAskPermission(apk)
                        } catch (e: Exception) {
                            error = "İndirilemedi. İnternet bağlantısını kontrol edip tekrar deneyin."
                        } finally {
                            downloading = false
                        }
                    }
                }
            ) { Text(if (downloaded != null) "Kur" else "Güncelle") }
        },
        dismissButton = {
            TextButton(enabled = !downloading, onClick = { info = null }) { Text("Sonra") }
        }
    )
}
