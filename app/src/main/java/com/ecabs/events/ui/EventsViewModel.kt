package com.ecabs.events.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecabs.events.data.EventsRepository
import com.ecabs.events.data.model.GitHubEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repo: EventsRepository
) : ViewModel() {

    private val _events = MutableStateFlow<List<GitHubEvent>>(emptyList())
    val events: StateFlow<List<GitHubEvent>> = _events.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _nextPoll = MutableStateFlow(repo.pollInterval())

    private val _countdown = MutableStateFlow(repo.pollInterval())
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Empty)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var pollJob: Job? = null

    fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                try {
                    pollEvents()
                    countdownTimer()
                } catch (e: Exception) {
                    handleError(e)
                    delay(5000)
                }
            }
        }
    }

    private suspend fun pollEvents() {
        _isRefreshing.value = true
        
        try {
            val result = repo.fetchNewEvents()
            _nextPoll.value = result.nextPollSeconds
            
            if (!result.notModified && result.events.isNotEmpty()) {
                updateEvents(result.events)
                _uiState.value = EventsUiState.Success(_events.value)
            } else if (_events.value.isEmpty()) {
                _uiState.value = EventsUiState.Empty
            } else {
                _uiState.value = EventsUiState.Success(_events.value)
            }
            
            _errorMessage.value = null
        } catch (e: Exception) {
            handleError(e)
        } finally {
            _isRefreshing.value = false
        }
    }

    private suspend fun countdownTimer() {
        val sleep = maxOf(10, _nextPoll.value)
        repeat(sleep + 1) { remaining ->
            _countdown.value = sleep - remaining
            delay(1000L)
        }
    }

    private fun updateEvents(newEvents: List<GitHubEvent>) {
        val existing = _events.value.associateBy { it.id }.toMutableMap()
        for (event in newEvents) {
            existing[event.id] = event
        }
        _events.value = existing.values.toList()
    }

    fun refreshEvents() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                
                val result = repo.fetchNewEvents()
                
                if (!result.notModified && result.events.isNotEmpty()) {
                    updateEvents(result.events)
                    _uiState.value = EventsUiState.Success(_events.value)
                }
                
                _errorMessage.value = null
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun handleError(exception: Throwable) {
        _isRefreshing.value = false
        
        val errorMessage = exception.message ?: "Unknown error occurred"
        _errorMessage.value = errorMessage
        _uiState.value = EventsUiState.Error(errorMessage)
    }

    fun clearError() {
        _errorMessage.value = null
        if (_uiState.value is EventsUiState.Error) {
            _uiState.value = if (_events.value.isNotEmpty()) {
                EventsUiState.Success(_events.value)
            } else {
                EventsUiState.Empty
            }
        }
    }

    public override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}

sealed class EventsUiState {
    object Empty : EventsUiState()
    data class Success(val events: List<GitHubEvent>) : EventsUiState()
    data class Error(val message: String) : EventsUiState()
}