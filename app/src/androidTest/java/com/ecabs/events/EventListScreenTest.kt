package com.ecabs.events

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ecabs.events.data.model.GitHubEvent
import com.ecabs.events.ui.EventListScreen
import com.ecabs.events.ui.EventsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testEventListScreen_EmptyState() {
        composeTestRule.setContent {
            EventListScreen(
                onEventClick = {},
                vm = FakeEventsViewModel(EventsUiState.Empty)
            )
        }

        composeTestRule.onNodeWithText("No events yet").assertExists()
        composeTestRule.onNodeWithText("Try again in a few seconds.").assertExists()
    }

    @Test
    fun testEventListScreen_SuccessState() {
        val mockEvents = listOf(
            createMockEvent("1", "PushEvent"),
            createMockEvent("2", "PullRequestEvent")
        )

        composeTestRule.setContent {
            EventListScreen(
                onEventClick = {},
                vm = FakeEventsViewModel(EventsUiState.Success(mockEvents))
            )
        }

        composeTestRule.onNodeWithText("user1").assertExists()
        composeTestRule.onNodeWithText("user2").assertExists()
        composeTestRule.onNodeWithText("repo1").assertExists()
        composeTestRule.onNodeWithText("repo2").assertExists()
    }

    @Test
    fun testEventListScreen_ErrorState() {
        composeTestRule.setContent {
            EventListScreen(
                onEventClick = {},
                vm = FakeEventsViewModel(EventsUiState.Error("Network error"))
            )
        }

        composeTestRule.onNodeWithText("Network error").assertExists()
        composeTestRule.onNodeWithText("Retry").assertExists()
    }

    @Test
    fun testEventListScreen_SearchFunctionality() {
        val mockEvents = listOf(
            createMockEvent("1", "PushEvent"),
            createMockEvent("2", "PullRequestEvent")
        )

        composeTestRule.setContent {
            EventListScreen(
                onEventClick = {},
                vm = FakeEventsViewModel(EventsUiState.Success(mockEvents))
            )
        }

        composeTestRule.onNodeWithText("Search events").performClick()
        composeTestRule.onNodeWithText("Search events").performTextInput("Push")
        
        composeTestRule.onNodeWithText("user1").assertExists()
        composeTestRule.onNodeWithText("user2").assertDoesNotExist()
    }

    @Test
    fun testEventListScreen_FilterChips() {
        val mockEvents = listOf(
            createMockEvent("1", "PushEvent"),
            createMockEvent("2", "PullRequestEvent")
        )

        composeTestRule.setContent {
            EventListScreen(
                onEventClick = {},
                vm = FakeEventsViewModel(EventsUiState.Success(mockEvents))
            )
        }

        composeTestRule.onNodeWithText("All").assertExists()
        composeTestRule.onNodeWithText("Push").assertExists()
        composeTestRule.onNodeWithText("PR").assertExists()
        composeTestRule.onNodeWithText("Issues").assertExists()
        composeTestRule.onNodeWithText("Create").assertExists()
        composeTestRule.onNodeWithText("Watch").assertExists()
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
