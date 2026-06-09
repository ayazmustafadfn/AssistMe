package com.artsistem.assistme

import android.app.Application
import com.artsistem.assistme.data.AppDatabase
import com.artsistem.assistme.data.MailRepository
import com.artsistem.assistme.data.NotesRepository
import com.artsistem.assistme.data.ReminderRepository
import com.artsistem.assistme.data.TasksRepository
import com.artsistem.assistme.mail.MailSource
import com.artsistem.assistme.mail.auth.MsalAuth
import com.artsistem.assistme.mail.graph.GraphMailSource
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

    val msalAuth: MsalAuth by lazy { MsalAuth(this) }

    /** Gerçek Microsoft Graph kaynağı (giriş gerektirir). */
    val mailSource: MailSource by lazy { GraphMailSource(msalAuth) }

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)
    }
}
