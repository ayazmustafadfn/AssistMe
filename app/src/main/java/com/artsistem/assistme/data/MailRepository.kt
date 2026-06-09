package com.artsistem.assistme.data

import kotlinx.coroutines.flow.Flow

/** Mail metaverisine erişim. (Çekme/senkron kaynağı ayrı; bu sadece cache.) */
class MailRepository(private val dao: MailDao) {

    fun observeAll(): Flow<List<MailMessage>> = dao.observeAll()

    suspend fun replaceAll(messages: List<MailMessage>) {
        dao.clearAll()
        dao.upsertAll(messages)
    }

    suspend fun clear() = dao.clearAll()
}
