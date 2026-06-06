package com.artsistem.assistme.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.artsistem.assistme.AssistMeApp
import com.artsistem.assistme.data.Reminder
import com.artsistem.assistme.data.ReminderRepository
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

    fun save(reminder: Reminder) {
        viewModelScope.launch {
            val id = repository.upsert(reminder)
            val saved = reminder.copy(id = id)
            val context = getApplication<Application>()
            // Önce eski alarmı iptal et, sonra (aktifse) yenisini kur.
            ReminderScheduler.cancel(context, saved.id)
            if (saved.enabled && saved.triggerAtMillis > System.currentTimeMillis()) {
                ReminderScheduler.schedule(context, saved)
            }
        }
    }

    fun toggleEnabled(reminder: Reminder, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(reminder.id, enabled)
            val context = getApplication<Application>()
            if (enabled && reminder.triggerAtMillis > System.currentTimeMillis()) {
                ReminderScheduler.schedule(context, reminder.copy(enabled = true))
            } else {
                ReminderScheduler.cancel(context, reminder.id)
            }
        }
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
