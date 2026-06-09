package com.artsistem.assistme.data

import kotlinx.coroutines.flow.Flow

/** Görev (to-do) verisine erişim için tek giriş noktası. */
class TasksRepository(private val dao: TaskDao) {

    fun observeAll(): Flow<List<Task>> = dao.observeAll()

    suspend fun add(title: String): Long =
        dao.insert(Task(title = title.trim(), sortOrder = System.currentTimeMillis()))

    suspend fun setDone(task: Task, done: Boolean) =
        dao.setDone(task.id, done, if (done) System.currentTimeMillis() else null)

    suspend fun rename(task: Task, title: String) =
        dao.update(task.copy(title = title.trim()))

    suspend fun delete(task: Task) = dao.delete(task)
}
