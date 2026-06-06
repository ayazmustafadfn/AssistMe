package com.artsistem.assistme.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.artsistem.assistme.data.Reminder
import com.artsistem.assistme.data.RepeatType
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderEditScreen(
    viewModel: ReminderViewModel,
    reminderId: Long,
    onDone: () -> Unit
) {
    var loaded by remember { mutableStateOf(reminderId == 0L) }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var triggerAt by remember {
        mutableLongStateOf(
            // Varsayılan: 1 saat sonrası, dakikalar sıfırlanmış.
            Calendar.getInstance().apply {
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    var repeatType by remember { mutableStateOf(RepeatType.NONE) }
    var customMinutes by remember { mutableStateOf("30") }

    // Düzenleme modunda mevcut kaydı yükle.
    LaunchedEffect(reminderId) {
        if (reminderId != 0L) {
            viewModel.getById(reminderId)?.let { r ->
                title = r.title
                note = r.note
                triggerAt = r.triggerAtMillis
                repeatType = r.repeatType
                if (r.repeatType == RepeatType.CUSTOM) {
                    customMinutes = r.customIntervalMinutes.toString()
                }
            }
            loaded = true
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (reminderId == 0L) "Yeni Hatırlatma" else "Hatırlatmayı Düzenle") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Başlık") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Not (isteğe bağlı)") },
                modifier = Modifier.fillMaxWidth()
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Zaman", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                    Text(formatDateTime(triggerAt), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showDatePicker = true }) { Text("Tarih") }
                        OutlinedButton(onClick = { showTimePicker = true }) { Text("Saat") }
                    }
                }
            }

            Text("Tekrar", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepeatType.entries.forEach { type ->
                    FilterChip(
                        selected = repeatType == type,
                        onClick = { repeatType = type },
                        label = { Text(repeatChipLabel(type)) }
                    )
                }
            }

            if (repeatType == RepeatType.CUSTOM) {
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { v -> customMinutes = v.filter { it.isDigit() } },
                    label = { Text("Kaç dakikada bir") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    val custom = customMinutes.toLongOrNull() ?: 0L
                    val reminder = Reminder(
                        id = reminderId,
                        title = title.trim().ifBlank { "Hatırlatma" },
                        note = note.trim(),
                        triggerAtMillis = triggerAt,
                        repeatType = repeatType,
                        customIntervalMinutes = if (repeatType == RepeatType.CUSTOM) custom else 0L,
                        enabled = true
                    )
                    viewModel.save(reminder)
                    onDone()
                },
                enabled = loaded,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kaydet")
            }
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = triggerAt)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { picked ->
                        triggerAt = combineDate(picked, triggerAt)
                    }
                    showDatePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("İptal") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = triggerAt }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimePicker(state = timeState)
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("İptal") }
                        TextButton(onClick = {
                            triggerAt = combineTime(triggerAt, timeState.hour, timeState.minute)
                            showTimePicker = false
                        }) { Text("Tamam") }
                    }
                }
            }
        }
    }
}

private fun repeatChipLabel(type: RepeatType): String = when (type) {
    RepeatType.NONE -> "Yok"
    RepeatType.HOURLY -> "Saatlik"
    RepeatType.DAILY -> "Günlük"
    RepeatType.WEEKLY -> "Haftalık"
    RepeatType.MONTHLY -> "Aylık"
    RepeatType.CUSTOM -> "Özel"
}

/** Seçilen tarihi (gün) mevcut saatle birleştirir. */
private fun combineDate(dateMillis: Long, currentTrigger: Long): Long {
    val date = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val time = Calendar.getInstance().apply { timeInMillis = currentTrigger }
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, date.get(Calendar.YEAR))
        set(Calendar.MONTH, date.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY))
        set(Calendar.MINUTE, time.get(Calendar.MINUTE))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun combineTime(currentTrigger: Long, hour: Int, minute: Int): Long {
    return Calendar.getInstance().apply {
        timeInMillis = currentTrigger
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
