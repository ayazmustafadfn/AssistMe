package com.artsistem.assistme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.artsistem.assistme.data.ReminderGroup

/** Gruplar için hazır renk paleti (ARGB int). */
val GroupColorPalette: List<Int> = listOf(
    0xFFEF5350.toInt(), // kırmızı
    0xFFFF7043.toInt(), // turuncu
    0xFFFFCA28.toInt(), // sarı
    0xFF66BB6A.toInt(), // yeşil
    0xFF26A69A.toInt(), // turkuaz
    0xFF42A5F5.toInt(), // mavi
    0xFF7E57C2.toInt(), // mor
    0xFFEC407A.toInt()  // pembe
)

/** Listede grubu temsil eden küçük renkli nokta. */
@Composable
fun GroupColorDot(colorArgb: Int, modifier: Modifier = Modifier, size: Int = 12) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(colorArgb))
    )
}

/**
 * Grup oluşturma / düzenleme penceresi.
 *
 * @param initial null ise yeni grup; doluysa düzenleme.
 * @param onConfirm (isim, renk) ile çağrılır.
 */
@Composable
fun GroupEditDialog(
    initial: ReminderGroup?,
    onConfirm: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var color by remember { mutableIntStateOf(initial?.colorArgb ?: GroupColorPalette.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Yeni grup" else "Grubu düzenle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Grup adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Renk")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GroupColorPalette.forEach { c ->
                        val selected = c == color
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(if (selected) 32.dp else 28.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .then(
                                    if (selected) Modifier.border(
                                        2.dp,
                                        androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                        CircleShape
                                    ) else Modifier
                                )
                                .clickable { color = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, color) },
                enabled = name.isNotBlank()
            ) { Text("Kaydet") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
