package com.artsistem.assistme

import android.app.Application
import com.artsistem.assistme.data.AppDatabase
import com.artsistem.assistme.data.NotesRepository
import com.artsistem.assistme.data.ReminderRepository
import com.artsistem.assistme.data.TasksRepository
import com.artsistem.assistme.reminder.Notifications

/**
 * Uygulama giriş noktası. Bildirim kanalını oluşturur ve basit bir bağımlılık
 * kabı (repository) sağlar.
 */
class AssistMeApp : Application() {

    val repository: ReminderRepository by lazy {
        val db = AppDatabase.get(this)
        ReminderRepository(db.reminderDao(), db.groupDao(), db.historyDao())
    }

    val notesRepository: NotesRepository by lazy {
        NotesRepository(AppDatabase.get(this).noteDao())
    }

    val tasksRepository: TasksRepository by lazy {
        TasksRepository(AppDatabase.get(this).taskDao())
    }

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)
    }
}
