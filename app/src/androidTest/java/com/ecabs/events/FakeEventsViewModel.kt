package com.ecabs.events

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.ecabs.events.data.model.GitHubEvent
import com.ecabs.events.ui.EventsUiState
import com.ecabs.events.ui.EventsViewModel

class FakeEventsViewModel(
    initialState: EventsUiState = EventsUiState.Empty
) : EventsViewModel(FakeEventsRepository()) {
    
    private val _uiState = mutableStateOf(initialState)
    override val uiState: State<EventsUiState> = _uiState
    
    private val _isRefreshing = mutableStateOf(false)
    override val isRefreshing: State<Boolean> = _isRefreshing
    
    private val _countdown = mutableStateOf(30)
    override val countdown: State<Int> = _countdown
    
    private val _errorMessage = mutableStateOf<String?>(null)
    override val errorMessage: State<String?> = _errorMessage
    
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
}

class FakeEventsRepository : com.ecabs.events.data.EventsRepository {
    override suspend fun fetchNewEvents(): com.ecabs.events.data.FetchResult {
        return com.ecabs.events.data.FetchResult(emptyList(), 30, false)
    }
    
    override fun pollInterval(): Int = 30
}
