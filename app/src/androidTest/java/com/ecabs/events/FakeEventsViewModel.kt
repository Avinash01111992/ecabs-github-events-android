package com.ecabs.events

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.ecabs.events.data.model.GitHubEvent
import com.ecabs.events.ui.EventsUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeEventsViewModel(
    initialState: EventsUiState = EventsUiState.Empty
) {
    
    private val _uiState = mutableStateOf(initialState)
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()
    
    private val _isRefreshing = mutableStateOf(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _countdown = mutableStateOf(30)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()
    
    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun setUiState(state: EventsUiState) {
        _uiState.value = state
    }
    
    fun setRefreshing(refreshing: Boolean) {
        _isRefreshing.value = refreshing
    }
    
    fun setCountdown(count: Int) {
        _countdown.value = count
    }
    
    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }
    
    fun startPolling() {
        // No-op for testing
    }
    
    fun refreshEvents() {
        // No-op for testing
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
