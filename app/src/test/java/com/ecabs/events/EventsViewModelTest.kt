package com.ecabs.events

import com.ecabs.events.data.EventsRepository
import com.ecabs.events.data.FetchResult
import com.ecabs.events.data.model.GitHubEvent
import com.ecabs.events.data.model.Actor
import com.ecabs.events.data.model.Repo
import com.ecabs.events.ui.EventsViewModel
import com.ecabs.events.ui.EventsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: EventsViewModel
    private lateinit var mockRepository: EventsRepository

    @Before
    fun setup() {
        mockRepository = mock()
        viewModel = EventsViewModel(mockRepository)
    }

    @Test
    fun `initial state should be Loading`() = runTest {
        val initialState = viewModel.uiState.first()
        assertTrue(initialState is EventsUiState.Loading)
    }

    @Test
    fun `should emit Success state when events are fetched successfully`() = runTest {
        val mockEvents = listOf(
            createMockEvent("1", "PushEvent"),
            createMockEvent("2", "PullRequestEvent")
        )
        val fetchResult = FetchResult(mockEvents, 30, false)
        
        whenever(mockRepository.fetchNewEvents()).thenReturn(fetchResult)
        whenever(mockRepository.pollInterval()).thenReturn(30)

        viewModel.startPolling()
        
        advanceTimeBy(100)
        
        val uiState = viewModel.uiState.first()
        assertTrue(uiState is EventsUiState.Success)
        assertEquals(mockEvents.size, (uiState as EventsUiState.Success).events.size)
    }

    @Test
    fun `should emit Empty state when no events are returned`() = runTest {
        val fetchResult = FetchResult(emptyList(), 30, false)
        
        whenever(mockRepository.fetchNewEvents()).thenReturn(fetchResult)
        whenever(mockRepository.pollInterval()).thenReturn(30)

        viewModel.startPolling()
        
        advanceTimeBy(100)
        
        val uiState = viewModel.uiState.first()
        assertTrue(uiState is EventsUiState.Empty)
    }

    @Test
    fun `should emit Error state when repository throws exception`() = runTest {
        val errorMessage = "Network error"
        whenever(mockRepository.fetchNewEvents()).thenThrow(RuntimeException(errorMessage))
        whenever(mockRepository.pollInterval()).thenReturn(30)

        viewModel.startPolling()
        
        advanceTimeBy(100)
        
        val uiState = viewModel.uiState.first()
        assertTrue(uiState is EventsUiState.Error)
        assertEquals(errorMessage, (uiState as EventsUiState.Error).message)
    }

    @Test
    fun `should update countdown every second`() = runTest {
        whenever(mockRepository.pollInterval()).thenReturn(5)
        whenever(mockRepository.fetchNewEvents()).thenReturn(FetchResult(emptyList(), 5, false))

        viewModel.startPolling()
        
        advanceTimeBy(1000)
        assertEquals(4, viewModel.countdown.first())
        
        advanceTimeBy(1000)
        assertEquals(3, viewModel.countdown.first())
    }

    @Test
    fun `should refresh events when refreshEvents is called`() = runTest {
        val mockEvents = listOf(createMockEvent("1", "PushEvent"))
        val fetchResult = FetchResult(mockEvents, 30, false)
        
        whenever(mockRepository.fetchNewEvents()).thenReturn(fetchResult)
        whenever(mockRepository.pollInterval()).thenReturn(30)

        viewModel.refreshEvents()
        
        advanceTimeBy(100)
        
        val uiState = viewModel.uiState.first()
        assertTrue(uiState is EventsUiState.Success)
        assertEquals(1, (uiState as EventsUiState.Success).events.size)
    }

    @Test
    fun `should clear error when clearError is called`() = runTest {
        whenever(mockRepository.fetchNewEvents()).thenThrow(RuntimeException("Test error"))
        whenever(mockRepository.pollInterval()).thenReturn(30)
        
        viewModel.startPolling()
        advanceTimeBy(100)
        
        assertTrue(viewModel.uiState.first() is EventsUiState.Error)
        
        viewModel.clearError()
        
        assertNull(viewModel.errorMessage.first())
    }

    @Test
    fun `should cancel polling job when ViewModel is cleared`() = runTest {
        whenever(mockRepository.pollInterval()).thenReturn(30)
        whenever(mockRepository.fetchNewEvents()).thenReturn(FetchResult(emptyList(), 30, false))
        
        viewModel.startPolling()
        
        viewModel.onCleared()
        
        val initialCountdown = viewModel.countdown.first()
        advanceTimeBy(2000)
        val finalCountdown = viewModel.countdown.first()
        
        assertEquals(initialCountdown, finalCountdown)
    }

    private fun createMockEvent(id: String, type: String): GitHubEvent {
        return GitHubEvent(
            id = id,
            type = type,
            actor = Actor(
                id = 1L,
                login = "testuser",
                displayLogin = null,
                gravatarId = null,
                url = "https://api.github.com/users/testuser",
                avatarUrl = "https://avatars.githubusercontent.com/u/1?v=4"
            ),
            repo = Repo(
                id = 1L,
                name = "testuser/testrepo",
                url = "https://api.github.com/repos/testuser/testrepo"
            ),
            payload = null,
            public = true,
            createdAt = "2025-08-20T12:00:00Z"
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    private val testDispatcher = StandardTestDispatcher()

    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}
