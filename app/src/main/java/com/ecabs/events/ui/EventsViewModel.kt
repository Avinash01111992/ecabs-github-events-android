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

    private var pollJob: Job? = null

    init {
        startPolling()
    }

    fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                _isRefreshing.value = true
                val result: FetchResult = repo.fetchNewEvents()
                _nextPoll.value = result.nextPollSeconds
                if (!result.notModified && result.events.isNotEmpty()) {
                    val existing = _events.value.associateBy { it.id }.toMutableMap()
                    for (e in result.events) {
                        existing[e.id] = e
                    }
                    _events.value = existing.values.sortedByDescending { it.createdAt }
                }
                _isRefreshing.value = false
                val sleep = kotlin.math.max(10, _nextPoll.value)
                for (remaining in sleep downTo 0) {
                    _countdown.value = remaining
                    delay(1000L)
                }
            }
        }
    }
}