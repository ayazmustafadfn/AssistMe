package com.artsistem.assistme.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache'lenen mail metaverisi. GİZLİLİK: gövdenin tam metni saklanmaz, sadece
 * [preview] (Graph bodyPreview). Sınıflandırma ve liste için yeterli alanlar.
 */
@Entity(tableName = "mail_messages")
data class MailMessage(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val subject: String,
    val fromName: String,
    val fromAddress: String,
    val preview: String,
    val receivedAtMillis: Long,
    val isRead: Boolean,
    val isFlagged: Boolean,
    val fromMe: Boolean,
    /** [com.artsistem.assistme.mail.MailCategory] adı. */
    val category: String,
    val reason: String,
    val fetchedAtMillis: Long
)
