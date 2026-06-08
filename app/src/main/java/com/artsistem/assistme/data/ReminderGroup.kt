package com.artsistem.assistme.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Hatırlatma grubu (ör. "İş", "Kişisel").
 *
 * @param colorArgb Grubun rengi (ARGB int). Listede renkli nokta olarak gösterilir.
 * @param sortOrder Sekmelerin sıralanma değeri (küçükten büyüğe).
 */
@Entity(tableName = "groups")
data class ReminderGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val sortOrder: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis()
)
