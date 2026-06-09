package com.artsistem.assistme

import android.app.Application
import com.artsistem.assistme.data.AppDatabase
import com.artsistem.assistme.data.MailRepository
import com.artsistem.assistme.data.NotesRepository
import com.artsistem.assistme.data.ReminderRepository
import com.artsistem.assistme.data.TasksRepository
import com.artsistem.assistme.mail.FakeMailSource
import com.artsistem.assistme.mail.MailSource
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

    val mailRepository: MailRepository by lazy {
        MailRepository(AppDatabase.get(this).mailDao())
    }

    /** Faz 1: sahte kaynak. Azure/Graph hazır olunca GraphMailSource ile değişir. */
    val mailSource: MailSource by lazy { FakeMailSource() }

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)
    }
}
