package com.ecabs.events.util

import com.ecabs.events.data.model.GitHubEvent
import com.ecabs.events.data.model.TrackedEventType

/**
 * Utility class for event-related operations
 */
object EventUtils {
    
    /**
     * Formats event type by removing "Event" suffix
     * 
     * @param eventType The event type string (e.g., "PushEvent")
     * @return Formatted event type (e.g., "Push")
     */
    fun formatEventType(eventType: String): String {
        return eventType.removeSuffix("Event")
    }
    
    /**
     * Formats branch name by removing "refs/heads/" prefix
     * 
     * @param ref The reference string (e.g., "refs/heads/main")
     * @return Formatted branch name (e.g., "main")
     */
    fun formatBranchName(ref: String): String {
        return ref.removePrefix("refs/heads/")
    }
    
    /**
     * Formats commit count with proper pluralization
     * 
     * @param count The number of commits
     * @return Formatted commit count (e.g., "5 commits", "1 commit")
     */
    fun formatCommitCount(count: Int): String {
        return "$count commit${if (count != 1) "s" else ""}"
    }
    
    /**
     * Constructs GitHub repository URL
     * 
     * @param repoName The repository name (e.g., "owner/repo")
     * @return Full GitHub URL
     */
    fun buildRepoUrl(repoName: String): String {
        return "https://github.com/$repoName"
    }
    
    /**
     * Constructs GitHub user profile URL
     * 
     * @param username The GitHub username
     * @return Full GitHub profile URL
     */
    fun buildProfileUrl(username: String): String {
        return "https://github.com/$username"
    }
    
    /**
     * Filters events based on type and search query
     * 
     * @param events List of events to filter
     * @param selectedFilter Selected filter type
     * @param searchQuery Search query string
     * @return Filtered list of events
     */
    fun filterEvents(
        events: List<GitHubEvent>,
        selectedFilter: EventFilterType,
        searchQuery: String
    ): List<GitHubEvent> {
        return events.filter { event ->
            val matchesType = when (selectedFilter) {
                EventFilterType.All -> true
                EventFilterType.Push -> event.type == TrackedEventType.Push.raw
                EventFilterType.PR -> event.type == TrackedEventType.PullRequest.raw
                EventFilterType.Issues -> event.type == TrackedEventType.Issues.raw
                EventFilterType.Create -> event.type == TrackedEventType.Create.raw
                EventFilterType.Watch -> event.type == TrackedEventType.Watch.raw
            }
            
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isBlank() ||
                event.actor.login.lowercase().contains(q) ||
                event.repo.name.lowercase().contains(q) ||
                event.type.lowercase().contains(q)
                
            matchesType && matchesQuery
        }
    }
}

/**
 * Event filter types for UI
 */
enum class EventFilterType { All, Push, PR, Issues, Create, Watch }
