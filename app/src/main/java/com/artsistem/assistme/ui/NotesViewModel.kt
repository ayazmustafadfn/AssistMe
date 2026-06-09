package com.artsistem.assistme.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.artsistem.assistme.AssistMeApp
import com.artsistem.assistme.data.Note
import com.artsistem.assistme.data.NotesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(
    application: Application,
    private val repository: NotesRepository
) : AndroidViewModel(application) {

    val notes: StateFlow<List<Note>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(note: Note) {
        viewModelScope.launch {
            repository.upsert(note.copy(updatedAtMillis = System.currentTimeMillis()))
        }
    }

    fun delete(note: Note) {
        viewModelScope.launch { repository.delete(note) }
    }

    suspend fun getById(id: Long): Note? = repository.getById(id)

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AssistMeApp
                return NotesViewModel(app, app.notesRepository) as T
            }
        }
    }
}
