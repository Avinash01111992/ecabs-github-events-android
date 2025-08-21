package com.ecabs.events

import com.ecabs.events.data.EventsRepository
import com.ecabs.events.data.FetchResult
import com.ecabs.events.data.model.GitHubEvent
import com.ecabs.events.data.model.TrackedEventType
import com.ecabs.events.ui.EventsUiState
import com.ecabs.events.ui.EventsViewModel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import androidx.arch.core.executor.testing.InstantTaskExecutorRule

@RunWith(org.junit.runners.JUnit4::class)
class EventsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var mockRepository: EventsRepository
    private lateinit var viewModel: EventsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        setMain(testDispatcher)
        mockRepository = mock()
        whenever(mockRepository.pollInterval()).thenReturn(30)
        viewModel = EventsViewModel(mockRepository)
    }

    @Test
    fun `should emit Success state when events are fetched successfully`() = runTest {
        val mockEvents = listOf(
            createMockEvent("1", TrackedEventType.Push.raw),
            createMockEvent("2", TrackedEventType.PullRequest.raw)
        )
        val fetchResult = FetchResult(mockEvents, 30, false)
        
        whenever(mockRepository.fetchNewEvents()).thenReturn(fetchResult)

        viewModel.startPolling()
        
        // Wait for the coroutine to complete
        advanceTimeBy(100)
        
        val uiState = viewModel.uiState.value
        assertTrue(uiState is EventsUiState.Success)
        assertEquals(mockEvents.size, (uiState as EventsUiState.Success).events.size)
    }

    @Test
    fun `should emit Empty state when no events are returned`() = runTest {
        val fetchResult = FetchResult(emptyList(), 30, false)
        
        whenever(mockRepository.fetchNewEvents()).thenReturn(fetchResult)

        viewModel.startPolling()
        
        // Wait for the coroutine to complete
        advanceTimeBy(100)
        
        val uiState = viewModel.uiState.value
        assertTrue(uiState is EventsUiState.Empty)
    }

    @Test
    fun `should emit Error state when repository throws exception`() = runTest {
        val errorMessage = "Network error"
        whenever(mockRepository.fetchNewEvents()).thenThrow(RuntimeException(errorMessage))

        viewModel.startPolling()
        
        // Wait for the coroutine to complete
        advanceTimeBy(100)
        
        val uiState = viewModel.uiState.value
        assertTrue(uiState is EventsUiState.Error)
        assertEquals(errorMessage, (uiState as EventsUiState.Error).message)
    }

    @Test
    fun `should refresh events when refreshEvents is called`() = runTest {
        val mockEvents = listOf(createMockEvent("1", TrackedEventType.Push.raw))
        val fetchResult = FetchResult(mockEvents, 30, false)
        
        whenever(mockRepository.fetchNewEvents()).thenReturn(fetchResult)

        viewModel.refreshEvents()
        
        // Wait for the coroutine to complete
        advanceTimeBy(100)
        
        val uiState = viewModel.uiState.value
        assertTrue(uiState is EventsUiState.Success)
        assertEquals(1, (uiState as EventsUiState.Success).events.size)
    }

    @Test
    fun `should clear error when clearError is called`() = runTest {
        whenever(mockRepository.fetchNewEvents()).thenThrow(RuntimeException("Test error"))
        
        // First start polling to create error state
        viewModel.startPolling()
        advanceTimeBy(100)
        
        assertTrue(viewModel.uiState.value is EventsUiState.Error)
        
        // Clear the error
        viewModel.clearError()
        
        // Check that error message is cleared
        assertNull(viewModel.errorMessage.value)
        
        // Check that UI state is updated to Empty (since no events were loaded)
        val uiState = viewModel.uiState.value
        assertTrue(uiState is EventsUiState.Empty)
    }

    @Test
    fun `should update countdown every second`() = runTest {
        whenever(mockRepository.fetchNewEvents()).thenReturn(FetchResult(emptyList(), 5, false))

        viewModel.startPolling()
        
        // Wait for initial countdown to start
        advanceTimeBy(100)
        
        // Check that countdown is working (should be less than initial value)
        val initialCountdown = viewModel.countdown.value
        assertTrue(initialCountdown <= 5)
        
        // Advance time and check countdown decreases
        advanceTimeBy(1000)
        val nextCountdown = viewModel.countdown.value
        assertTrue(nextCountdown < initialCountdown || nextCountdown == 0)
    }

    @Test
    fun `should start polling when startPolling is called`() = runTest {
        val mockEvents = listOf(createMockEvent("1", TrackedEventType.Push.raw))
        val fetchResult = FetchResult(mockEvents, 30, false)
        
        whenever(mockRepository.fetchNewEvents()).thenReturn(fetchResult)

        // Start polling manually
        viewModel.startPolling()
        
        // Wait for the coroutine to complete
        advanceTimeBy(100)
        
        val uiState = viewModel.uiState.value
        assertTrue(uiState is EventsUiState.Success)
        assertEquals(1, (uiState as EventsUiState.Success).events.size)
    }

    private fun createMockEvent(id: String, type: String): GitHubEvent {
        return GitHubEvent(
            id = id,
            type = type,
            actor = GitHubEvent.Actor(
                id = id.toInt(),
                login = "user$id",
                displayLogin = "user$id",
                gravatarId = "",
                url = "https://api.github.com/users/user$id",
                avatarUrl = "https://avatars.githubusercontent.com/u/$id?v=4"
            ),
            repo = GitHubEvent.Repo(
                id = id.toInt(),
                name = "repo$id",
                url = "https://api.github.com/repos/repo$id"
            ),
            payload = null,
            public = true,
            createdAt = "2024-01-01T00:00:00Z"
        )
    }
}
