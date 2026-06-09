package com.artsistem.assistme.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Serbest metin notu.
 *
 * @param colorArgb Not kartının rengi (ARGB int); 0 = renksiz (yüzey rengi).
 * @param pinned Sabitlenmiş notlar listede üstte gösterilir.
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val colorArgb: Int = 0,
    val pinned: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)
