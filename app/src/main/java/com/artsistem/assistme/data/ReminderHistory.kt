package com.artsistem.assistme.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tamamlanan / tetiklenen hatırlatmaların günlüğü ("Tamamlananlar geçmişi").
 *
 * Hatırlatma silinse bile geçmiş kaydı kalsın diye bilgiler kopyalanarak tutulur.
 *
 * @param kind "done" = kullanıcı Tamamlandı'ya bastı, "fired" = tek seferlik tetiklendi.
 */
@Entity(tableName = "reminder_history")
data class ReminderHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reminderId: Long,
    val title: String,
    val note: String,
    val groupId: Long?,
    val completedAtMillis: Long,
    val kind: String
)
