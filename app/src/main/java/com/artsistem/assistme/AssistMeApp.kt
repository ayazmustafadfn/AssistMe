package com.artsistem.assistme

import android.app.Application
import com.artsistem.assistme.data.AppDatabase
import com.artsistem.assistme.data.ReminderRepository
import com.artsistem.assistme.reminder.Notifications

/**
 * Uygulama giriş noktası. Bildirim kanalını oluşturur ve basit bir bağımlılık
 * kabı (repository) sağlar.
 */
class AssistMeApp : Application() {

    val repository: ReminderRepository by lazy {
        ReminderRepository(AppDatabase.get(this).reminderDao())
    }

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)
    }
}
