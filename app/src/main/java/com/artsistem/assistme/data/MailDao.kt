package com.artsistem.assistme.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MailDao {

    @Query("SELECT * FROM mail_messages ORDER BY receivedAtMillis DESC")
    fun observeAll(): Flow<List<MailMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MailMessage>)

    @Query("DELETE FROM mail_messages")
    suspend fun clearAll()
}
