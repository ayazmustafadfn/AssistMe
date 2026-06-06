package com.artsistem.assistme.data

import kotlinx.coroutines.flow.Flow

/**
 * Hatırlatma verisine erişim için tek giriş noktası.
 * UI ve broadcast alıcıları bu sınıf üzerinden çalışır.
 */
class ReminderRepository(private val dao: ReminderDao) {

    fun observeAll(): Flow<List<Reminder>> = dao.observeAll()

    suspend fun getById(id: Long): Reminder? = dao.getById(id)

    suspend fun getAllEnabled(): List<Reminder> = dao.getAllEnabled()

    suspend fun upsert(reminder: Reminder): Long {
        return if (reminder.id == 0L) {
            dao.insert(reminder)
        } else {
            dao.update(reminder)
            reminder.id
        }
    }

    suspend fun delete(reminder: Reminder) = dao.delete(reminder)

    suspend fun updateTriggerTime(id: Long, triggerAtMillis: Long) =
        dao.updateTriggerTime(id, triggerAtMillis)

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)
}
