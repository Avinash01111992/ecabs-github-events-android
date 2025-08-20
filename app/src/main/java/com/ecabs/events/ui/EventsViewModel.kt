package com.ecabs.events.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecabs.events.data.EventsRepository
import com.ecabs.events.data.FetchResult
import com.ecabs.events.data.model.GitHubEvent
import com.ecabs.events.util.Constants
import com.ecabs.events.util.CoroutineUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repo: EventsRepository
) : ViewModel() {

    private val _events = MutableStateFlow<List<GitHubEvent>>(emptyList())
    val events: StateFlow<List<GitHubEvent>> = _events
        .map { events -> events.sortedByDescending { it.createdAt } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        handleException(exception)
    }

    init {
        startPolling()
    }

    fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch(exceptionHandler) {
            supervisorScope {
                while (true) {
                    try {
                        pollEvents()
                        countdownTimer()
                    } catch (e: Exception) {
                        handleError(e)
                        delay(Constants.Timeouts.ERROR_RETRY_DELAY)
                    }
                }
            }
        }
    }

    private suspend fun pollEvents() {
        _isRefreshing.value = true
        
        val result = CoroutineUtils.retryWithBackoff(
            maxAttempts = Constants.Timeouts.RETRY_MAX_ATTEMPTS,
            initialDelay = Constants.Timeouts.RETRY_INITIAL_DELAY,
            maxDelay = Constants.Timeouts.RETRY_MAX_DELAY,
            factor = Constants.Timeouts.RETRY_BACKOFF_FACTOR
        ) {
            withContext(Dispatchers.IO) {
                repo.fetchNewEvents()
            }
        }
        
        _nextPoll.value = result.nextPollSeconds
        
        if (!result.notModified && result.events.isNotEmpty()) {
            updateEvents(result.events)
            _uiState.value = EventsUiState.Success(_events.value)
        } else if (_events.value.isEmpty()) {
            _uiState.value = EventsUiState.Empty
        } else {
            _uiState.value = EventsUiState.Success(_events.value)
        }
        
        _isRefreshing.value = false
        _errorMessage.value = null
    }

    private suspend fun countdownTimer() {
        val sleep = maxOf(10, _nextPoll.value)
        for (remaining in sleep downTo 0) {
            _countdown.value = remaining
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
        viewModelScope.launch(exceptionHandler) {
            try {
                _isRefreshing.value = true
                
                val result = CoroutineUtils.retryWithBackoff(
                    maxAttempts = Constants.Timeouts.RETRY_MAX_ATTEMPTS,
                    initialDelay = Constants.Timeouts.RETRY_INITIAL_DELAY,
                    maxDelay = Constants.Timeouts.RETRY_MAX_DELAY,
                    factor = Constants.Timeouts.RETRY_BACKOFF_FACTOR
                ) {
                    withContext(Dispatchers.IO) {
                        repo.fetchNewEvents()
                    }
                }
                
                if (!result.notModified && result.events.isNotEmpty()) {
                    updateEvents(result.events)
                    _uiState.value = EventsUiState.Success(_events.value)
                }
                
                _isRefreshing.value = false
                _errorMessage.value = null
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun handleException(exception: Throwable) {
        when (exception) {
            is IOException -> _errorMessage.value = "Network error: ${exception.message}"
            is HttpException -> _errorMessage.value = "API error: ${exception.message}"
            is CancellationException -> return
            else -> _errorMessage.value = "Unexpected error: ${exception.message}"
        }
        
        _isRefreshing.value = false
        _uiState.value = EventsUiState.Error(_errorMessage.value ?: "Unknown error")
    }

    private fun handleError(exception: Exception) {
        _isRefreshing.value = false
        _errorMessage.value = exception.message ?: "Unknown error occurred"
        _uiState.value = EventsUiState.Error(_errorMessage.value ?: "Unknown error")
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