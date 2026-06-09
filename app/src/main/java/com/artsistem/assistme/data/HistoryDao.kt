package com.artsistem.assistme.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM reminder_history ORDER BY completedAtMillis DESC")
    fun observeAll(): Flow<List<ReminderHistory>>

    @Insert
    suspend fun insert(entry: ReminderHistory): Long

    @Query("DELETE FROM reminder_history")
    suspend fun clearAll()
}
