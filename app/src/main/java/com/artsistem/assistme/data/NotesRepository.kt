package com.artsistem.assistme.data

import kotlinx.coroutines.flow.Flow

/** Not verisine erişim için tek giriş noktası. */
class NotesRepository(private val dao: NoteDao) {

    fun observeAll(): Flow<List<Note>> = dao.observeAll()

    suspend fun getById(id: Long): Note? = dao.getById(id)

    suspend fun upsert(note: Note): Long {
        return if (note.id == 0L) dao.insert(note)
        else { dao.update(note); note.id }
    }

    suspend fun delete(note: Note) = dao.delete(note)
}
