package com.artsistem.assistme.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.artsistem.assistme.AssistMeApp
import com.artsistem.assistme.data.MailMessage
import com.artsistem.assistme.data.MailRepository
import com.artsistem.assistme.mail.MailClassifier
import com.artsistem.assistme.mail.MailSource
import com.artsistem.assistme.mail.auth.MsalAuth
import com.artsistem.assistme.mail.auth.MsalUserCancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MailViewModel(
    application: Application,
    private val repository: MailRepository,
    private val source: MailSource,
    private val auth: MsalAuth
) : AndroidViewModel(application) {

    val messages: StateFlow<List<MailMessage>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { auth.isSignedIn() }.getOrDefault(false).let { _connected.value = it }
        }
    }

    /** Etkileşimli Microsoft girişi (Activity gerekir), sonra senkron. */
    fun connect(activity: Activity) {
        viewModelScope.launch {
            _error.value = null
            try {
                auth.signIn(activity)
                _connected.value = true
                sync()
            } catch (_: MsalUserCancel) {
                // Kullanıcı iptal etti; sessiz geç.
            } catch (e: Exception) {
                _error.value = "Giriş başarısız: ${e.message}"
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            runCatching { auth.signOut() }
            _connected.value = false
            repository.clear()
        }
    }

    fun clearError() { _error.value = null }

    fun sync() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val now = System.currentTimeMillis()
                val raws = source.fetchRecent()
                val msgs = raws.map { r ->
                    val ageDays = ((now - r.receivedAtMillis) / 86_400_000L).toInt()
                    val res = MailClassifier.classify(
                        MailClassifier.Input(
                            subject = r.subject,
                            preview = r.preview,
                            fromMe = r.fromMe,
                            isRead = r.isRead,
                            ageDays = ageDays,
                            lastMessageFromMe = r.lastMessageFromMe
                        )
                    )
                    MailMessage(
                        id = r.id,
                        conversationId = r.conversationId,
                        subject = r.subject,
                        fromName = r.fromName,
                        fromAddress = r.fromAddress,
                        preview = r.preview,
                        receivedAtMillis = r.receivedAtMillis,
                        isRead = r.isRead,
                        isFlagged = r.isFlagged,
                        fromMe = r.fromMe,
                        category = res.category.name,
                        reason = res.reason,
                        fetchedAtMillis = now
                    )
                }
                repository.replaceAll(msgs)
            } catch (e: Exception) {
                _error.value = "Mailler alınamadı: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AssistMeApp
                return MailViewModel(app, app.mailRepository, app.mailSource, app.msalAuth) as T
            }
        }
    }
}
