package com.ecabs.events.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecabs.events.data.model.GitHubEvent
import com.ecabs.events.util.Constants
import java.time.Duration
import java.time.Instant
import androidx.core.net.toUri
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.automirrored.filled.ArrowBack

/**
 * EventDetailsScreen displays comprehensive information about a GitHub event
 * 
 * Features:
 * - Event metadata (ID, type, public status, creation time)
 * - Actor information (user details, repository info)
 * - Event-specific payload details (commits, branches, actions)
 * - Action buttons (view profile, open repository)
 * 
 * @param event The GitHub event to display, null shows empty state
 * @param onBack Navigation callback to return to previous screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(event: GitHubEvent?, onBack: () -> Unit) {
    val context = LocalContext.current
    val repoWebUrl = remember(event) { event?.repo?.name?.let { "https://github.com/$it" } }

    Column(Modifier.fillMaxSize()) {
        // Top app bar with dynamic title and action buttons
        TopAppBar(
            title = { 
                Text(
                    text = Constants.UI.EVENT_DETAILS_TITLE_PREFIX + (event?.actor?.login ?: "") + Constants.UI.EVENT_DETAILS_TITLE_SUFFIX,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) { 
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Constants.UI.BACK_BUTTON_DESCRIPTION) 
                }
            },
            actions = {
                if (repoWebUrl != null) {
                    IconButton(onClick = { shareText(context, repoWebUrl) }) {
                        Icon(Icons.Filled.Share, contentDescription = Constants.UI.SHARE_REPO_DESCRIPTION)
                    }
                }
            }
        )

        if (event == null) {
            // Empty state when no event data is available
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(Constants.UI.NO_EVENT_DATA, style = MaterialTheme.typography.titleMedium)
            }
        } else {
            // Main content area with scrollable event details
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Event Information Section
                item {
                    InfoCard(
                        icon = Constants.UI.EVENT_INFO_ICON,
                        title = "Event Information",
                        trailingContent = {
                            EventTypeChip(eventType = event.type)
                        }
                    ) {
                        // Event details in a two-column grid layout
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                DetailItem("Event ID", event.id, isMonospace = true)
                                DetailItem("Public", if (event.public) "✅ Yes" else "❌ No")
                                DetailItem("Created", formatRelativeTime(event.createdAt))
                            }
                            Column(Modifier.weight(1f)) {
                                DetailItem("Type", event.type)
                                DetailItem("Actor", event.actor.login)
                                DetailItem("Repository", event.repo.name)
                            }
                        }
                    }
                }
                
                // Actor Information Section
                item {
                    InfoCard(
                        icon = Constants.UI.ACTOR_INFO_ICON,
                        title = "Actor Information"
                    ) {
                        DetailItem("Actor ID", event.actor.id.toString(), isMonospace = true)
                        DetailItem("Username", event.actor.login)
                        // Display name only if different from username
                        if (event.actor.displayLogin != null && event.actor.displayLogin != event.actor.login) {
                            DetailItem("Display Name", event.actor.displayLogin)
                        }
                        DetailItem("Repository ID", event.repo.id.toString(), isMonospace = true)
                    }
                }
                
                // Event Payload Details Section (conditional)
                if (event.payload != null) {
                    item {
                        InfoCard(
                            icon = Constants.UI.EVENT_DETAILS_ICON,
                            title = "Event Details"
                        ) {
                            // Display payload information based on event type
                            if (event.payload.action != null) {
                                DetailItem("Action", event.payload.action)
                            }
                            if (event.payload.pushId != null) {
                                DetailItem("Push ID", event.payload.pushId.toString(), isMonospace = true)
                            }
                            if (event.payload.size != null) {
                                DetailItem("Commits", "${event.payload.size} commit${if (event.payload.size != 1) "s" else ""}")
                            }
                            if (event.payload.ref != null) {
                                DetailItem("Branch", event.payload.ref.removePrefix("refs/heads/"))
                            }
                            if (event.payload.head != null) {
                                DetailItem("Head SHA", event.payload.head, isMonospace = true)
                            }
                            if (event.payload.commits != null && event.payload.commits.isNotEmpty()) {
                                DetailItem("Latest Commit", event.payload.commits.first().message)
                                DetailItem("Commit SHA", event.payload.commits.first().sha, isMonospace = true)
                            }
                        }
                    }
                }
                
                // Action Buttons Section
                item {
                    InfoCard(
                        icon = Constants.UI.ACTIONS_ICON,
                        title = "Actions"
                    ) {
                        ActionButtonsRow(
                            onViewProfile = { openUrl(context, "https://github.com/${event.actor.login}") },
                            onOpenRepo = { openUrl(context, repoWebUrl ?: "") }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable info card with consistent styling and layout
 * 
 * @param icon Emoji icon for the section
 * @param title Section title
 * @param trailingContent Optional trailing content (e.g., chips, badges)
 * @param content Main content of the card
 */
@Composable
private fun InfoCard(
    icon: String,
    title: String,
    trailingContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Section header with icon, title, and optional trailing content
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$icon $title",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                trailingContent?.invoke()
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

/**
 * Event type chip with consistent styling
 * 
 * @param eventType The event type string (e.g., "PushEvent")
 */
@Composable
private fun EventTypeChip(eventType: String) {
    AssistChip(
        onClick = {}, // Read-only chip
        label = { Text(eventType.removeSuffix("Event")) }
    )
}

/**
 * Action buttons row with consistent layout and spacing
 * 
 * @param onViewProfile Callback for viewing actor's GitHub profile
 * @param onOpenRepo Callback for opening the repository
 */
@Composable
private fun ActionButtonsRow(
    onViewProfile: () -> Unit,
    onOpenRepo: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onViewProfile,
            modifier = Modifier.weight(1f)
        ) {
            Text("${Constants.UI.VIEW_PROFILE_ICON} View Profile")
        }
        OutlinedButton(
            onClick = onOpenRepo,
            modifier = Modifier.weight(1f)
        ) {
            Text("${Constants.UI.OPEN_REPO_ICON} Open Repo")
        }
    }
}

/**
 * Reusable detail item with title and value
 * 
 * @param title Label for the detail item
 * @param value The value to display
 * @param isMonospace Whether to use monospace font (useful for IDs, SHAs)
 */
@Composable
private fun DetailItem(
    title: String, 
    value: String, 
    isMonospace: Boolean = false
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = if (isMonospace) {
                MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            } else {
                MaterialTheme.typography.bodyLarge
            },
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Opens a URL in the default browser
 * 
 * @param context Android context for starting the intent
 * @param url The URL to open
 */
private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}

/**
 * Formats ISO UTC timestamp to relative time (e.g., "3h ago", "2d ago")
 * 
 * @param isoUtc ISO 8601 formatted UTC timestamp
 * @return Human-readable relative time string
 */
private fun formatRelativeTime(isoUtc: String): String {
    return try {
        val then = Instant.parse(isoUtc)
        val now = Instant.now()
        val seconds = Duration.between(then, now).seconds.coerceAtLeast(0)
        
        when {
            seconds < 60 -> "${seconds}s ago"
            seconds < 60 * 60 -> "${seconds / 60}m ago"
            seconds < 60 * 60 * 24 -> "${seconds / 3600}h ago"
            else -> "${seconds / 86400}d ago"
        }
    } catch (_: Exception) {
        // Fallback to original timestamp if parsing fails
        isoUtc
    }
}

/**
 * Shares text content using system share intent
 * 
 * @param context Android context for starting the intent
 * @param text The text content to share
 */
private fun shareText(context: android.content.Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}