package com.artsistem.assistme.data

import kotlinx.coroutines.flow.Flow

/**
 * Hatırlatma verisine erişim için tek giriş noktası.
 * UI ve broadcast alıcıları bu sınıf üzerinden çalışır.
 */
class ReminderRepository(
    private val dao: ReminderDao,
    private val groupDao: GroupDao? = null,
    private val historyDao: HistoryDao? = null
) {

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

    suspend fun updateRepeatCount(id: Long, count: Int?) = dao.updateRepeatCount(id, count)

    // --- Tamamlananlar geçmişi ---

    fun observeHistory(): Flow<List<ReminderHistory>> =
        requireHistoryDao().observeAll()

    /** Bir hatırlatmanın tamamlanmasını/tetiklenmesini geçmişe yazar. */
    suspend fun addHistory(reminder: Reminder, kind: String, atMillis: Long) {
        requireHistoryDao().insert(
            ReminderHistory(
                reminderId = reminder.id,
                title = reminder.title,
                note = reminder.note,
                groupId = reminder.groupId,
                completedAtMillis = atMillis,
                kind = kind
            )
        )
    }

    suspend fun clearHistory() = requireHistoryDao().clearAll()

    private fun requireHistoryDao(): HistoryDao =
        historyDao ?: error("HistoryDao bu repository örneğinde mevcut değil")

    // --- Gruplar ---

    fun observeGroups(): Flow<List<ReminderGroup>> =
        requireGroupDao().observeAll()

    suspend fun createGroup(name: String, colorArgb: Int): Long {
        val dao = requireGroupDao()
        val order = dao.maxSortOrder() + 1
        return dao.insert(ReminderGroup(name = name, colorArgb = colorArgb, sortOrder = order))
    }

    suspend fun updateGroup(group: ReminderGroup) = requireGroupDao().update(group)

    /** Grubu siler; ona bağlı hatırlatmalar "Grupsuz" olur (silinmez). */
    suspend fun deleteGroup(group: ReminderGroup) {
        val dao = requireGroupDao()
        dao.clearGroupFromReminders(group.id)
        dao.delete(group)
    }

    private fun requireGroupDao(): GroupDao =
        groupDao ?: error("GroupDao bu repository örneğinde mevcut değil")
}
