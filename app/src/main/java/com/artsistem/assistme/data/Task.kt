package com.artsistem.assistme.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Basit yapılacak (to-do) görevi.
 *
 * @param done Tamamlandı mı (işaretliyse listede en alta iner).
 * @param sortOrder Sıralama anahtarı (ekleme zamanı); küçükten büyüğe.
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val done: Boolean = false,
    val sortOrder: Long = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val completedAtMillis: Long? = null
)
