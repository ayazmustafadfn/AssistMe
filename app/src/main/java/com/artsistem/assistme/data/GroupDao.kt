package com.artsistem.assistme.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query("SELECT * FROM groups ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<ReminderGroup>>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getById(id: Long): ReminderGroup?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM groups")
    suspend fun maxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: ReminderGroup): Long

    @Update
    suspend fun update(group: ReminderGroup)

    @Delete
    suspend fun delete(group: ReminderGroup)

    /** Grup silinmeden önce, ona bağlı hatırlatmaları "Grupsuz" yapar. */
    @Query("UPDATE reminders SET groupId = NULL WHERE groupId = :groupId")
    suspend fun clearGroupFromReminders(groupId: Long)
}
