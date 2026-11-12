package com.artsistem.assistme.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.artsistem.assistme.AssistMeApp
import com.artsistem.assistme.data.Reminder
import com.artsistem.assistme.data.ReminderGroup
import com.artsistem.assistme.data.ReminderHistory
import com.artsistem.assistme.data.ReminderRepository
import com.artsistem.assistme.reminder.Recurrence
import com.artsistem.assistme.reminder.ReminderScheduler
import com.artsistem.assistme.reminder.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderViewModel(
    application: Application,
    private val repository: ReminderRepository
) : AndroidViewModel(application) {

    val reminders: StateFlow<List<Reminder>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val groups: StateFlow<List<ReminderGroup>> = repository.observeGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<ReminderHistory>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    fun createGroup(name: String, colorArgb: Int) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.createGroup(trimmed, colorArgb) }
    }

    fun renameGroup(group: ReminderGroup, name: String, colorArgb: Int) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.updateGroup(group.copy(name = trimmed, colorArgb = colorArgb))
        }
    }

    fun deleteGroup(group: ReminderGroup) {
        viewModelScope.launch { repository.deleteGroup(group) }
    }

    fun save(reminder: Reminder) {
        viewModelScope.launch {
            // Tekrarlıda geçmiş başlangıç → ilk gelecek tekrara kaydır (yoksa hiç kurulmazdı).
            val adjusted = advanceIfPast(reminder)
            // Düzenlenen hatırlatmanın bekleyen ertelemesi geçersiz olur.
            val id = repository.upsert(adjusted.copy(snoozedUntilMillis = null))
            val saved = adjusted.copy(id = id, snoozedUntilMillis = null)
            val context = getApplication<Application>()
            // Önce eski alarmı iptal et, sonra (aktifse) yenisini kur.
            ReminderScheduler.cancel(context, saved.id)
            if (saved.enabled && saved.triggerAtMillis > System.currentTimeMillis()) {
                ReminderScheduler.schedule(context, saved)
            }
        }
    }

    /**
     * Aç/kapa. Zamanı geçmiş TEK SEFERLİK hatırlatma açılamaz (geriye dönük
     * alarm olmaz) → false döner, ekran uyarı gösterir. Tekrarlıda geçmiş
     * zaman ilk gelecek tekrara kaydırılır.
     */
    fun toggleEnabled(reminder: Reminder, enabled: Boolean): Boolean {
        val now = System.currentTimeMillis()
        if (enabled && !reminder.isRepeating && reminder.triggerAtMillis <= now) return false
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (!enabled) {
                repository.setEnabled(reminder.id, false)
                repository.setSnoozedUntil(reminder.id, null)
                ReminderScheduler.cancel(context, reminder.id)
                return@launch
            }
            val adjusted = advanceIfPast(reminder)
            if (adjusted.triggerAtMillis != reminder.triggerAtMillis) {
                repository.updateTriggerTime(reminder.id, adjusted.triggerAtMillis)
            }
            repository.setEnabled(reminder.id, true)
            ReminderScheduler.schedule(context, adjusted.copy(enabled = true))
        }
        return true
    }

    private fun advanceIfPast(reminder: Reminder): Reminder {
        if (!reminder.isRepeating || reminder.triggerAtMillis > System.currentTimeMillis()) return reminder
        val next = Recurrence.nextTrigger(reminder) ?: return reminder
        return reminder.copy(triggerAtMillis = next)
    }

    fun delete(reminder: Reminder) {
        viewModelScope.launch {
            ReminderScheduler.cancel(getApplication(), reminder.id)
            repository.delete(reminder)
        }
    }

    suspend fun getById(id: Long): Reminder? = repository.getById(id)

    fun snoozeMinutes(): Int = Settings.getSnoozeMinutes(getApplication())

    fun setSnoozeMinutes(minutes: Int) = Settings.setSnoozeMinutes(getApplication(), minutes)

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AssistMeApp
                return ReminderViewModel(app, app.repository) as T
            }
        }
    }
}
