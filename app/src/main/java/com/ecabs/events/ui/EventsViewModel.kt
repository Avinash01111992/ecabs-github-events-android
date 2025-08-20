package com.ecabs.events.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecabs.events.data.EventsRepository
import com.ecabs.events.data.FetchResult
import com.ecabs.events.data.model.GitHubEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    val nextPoll: StateFlow<Int> = _nextPoll.asStateFlow()

    private val _countdown = MutableStateFlow(repo.pollInterval())
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    private val _uiState = MutableStateFlow<EventsUiState>(EventsUiState.Loading)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var pollJob: Job? = null

    init {
        startPolling()
    }

    fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            try {
                while (true) {
                    _isRefreshing.value = true
                    _uiState.value = EventsUiState.Loading
                    
                    val result: FetchResult = repo.fetchNewEvents()
                    _nextPoll.value = result.nextPollSeconds
                    
                    if (!result.notModified && result.events.isNotEmpty()) {
                        val existing = _events.value.associateBy { it.id }.toMutableMap()
                        for (e in result.events) {
                            existing[e.id] = e
                        }
                        _events.value = existing.values.sortedByDescending { it.createdAt }
                        _uiState.value = EventsUiState.Success(_events.value)
                    } else if (_events.value.isEmpty()) {
                        _uiState.value = EventsUiState.Empty
                    } else {
                        _uiState.value = EventsUiState.Success(_events.value)
                    }
                    
                    _isRefreshing.value = false
                    _errorMessage.value = null
                    
                    val sleep = kotlin.math.max(10, _nextPoll.value)
                    for (remaining in sleep downTo 0) {
                        _countdown.value = remaining
                        delay(1000L)
                    }
                }
            } catch (e: Exception) {
                _isRefreshing.value = false
                _errorMessage.value = e.message ?: "Unknown error occurred"
                _uiState.value = EventsUiState.Error(e.message ?: "Unknown error")
                
                delay(5000L)
                startPolling()
            }
        }
    }

    fun refreshEvents() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                _uiState.value = EventsUiState.Loading
                
                val result: FetchResult = repo.fetchNewEvents()
                if (!result.notModified && result.events.isNotEmpty()) {
                    val existing = _events.value.associateBy { it.id }.toMutableMap()
                    for (e in result.events) {
                        existing[e.id] = e
                    }
                    _events.value = existing.values.sortedByDescending { it.createdAt }
                    _uiState.value = EventsUiState.Success(_events.value)
                }
                
                _isRefreshing.value = false
                _errorMessage.value = null
            } catch (e: Exception) {
                _isRefreshing.value = false
                _errorMessage.value = e.message ?: "Failed to refresh events"
                _uiState.value = EventsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
        if (_uiState.value is EventsUiState.Error) {
            _uiState.value = EventsUiState.Success(_events.value)
        }
    }

    public override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}

sealed class EventsUiState {
    object Loading : EventsUiState()
    object Empty : EventsUiState()
    data class Success(val events: List<GitHubEvent>) : EventsUiState()
    data class Error(val message: String) : EventsUiState()
}