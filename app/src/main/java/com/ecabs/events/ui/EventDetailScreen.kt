package com.ecabs.events.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.ecabs.events.util.TimeUtils
import com.ecabs.events.util.EventUtils
import com.ecabs.events.util.IntentUtils
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn

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
@Composable
fun EventDetailsScreen(event: GitHubEvent?, onBack: () -> Unit) {
    val context = LocalContext.current
    val repoWebUrl = remember(event) { event?.repo?.name?.let { EventUtils.buildRepoUrl(it) } }

    Column(Modifier.fillMaxSize()) {
        EventDetailTopAppBar(
            event = event,
            onBack = onBack,
            onShare = { repoWebUrl?.let { IntentUtils.shareText(context, it) } }
        )

        if (event == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(Constants.UI.NO_EVENT_DATA, style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    InfoCard(
                        icon = Constants.UI.EVENT_INFO_ICON,
                        title = "Event Information",
                        trailingContent = {
                            EventTypeChip(eventType = event.type)
                        }
                    ) {
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                DetailItem("Event ID", event.id, isMonospace = true)
                                DetailItem("Public", if (event.public) "✅ Yes" else "❌ No")
                                DetailItem("Created", TimeUtils.formatRelativeTime(event.createdAt))
                            }
                            Column(Modifier.weight(1f)) {
                                DetailItem("Type", event.type)
                                DetailItem("Actor", event.actor.login)
                                DetailItem("Repository", event.repo.name)
                            }
                        }
                    }
                }
                
                item {
                    InfoCard(
                        icon = Constants.UI.ACTOR_INFO_ICON,
                        title = "Actor Information"
                    ) {
                        DetailItem("Actor ID", event.actor.id.toString(), isMonospace = true)
                        DetailItem("Username", event.actor.login)
                        if (event.actor.displayLogin != null && event.actor.displayLogin != event.actor.login) {
                            DetailItem("Display Name", event.actor.displayLogin)
                        }
                        DetailItem("Repository ID", event.repo.id.toString(), isMonospace = true)
                    }
                }
                
                if (event.payload != null) {
                    item {
                        InfoCard(
                            icon = Constants.UI.EVENT_DETAILS_ICON,
                            title = "Event Details"
                        ) {
                            if (event.payload.action != null) {
                                DetailItem("Action", event.payload.action)
                            }
                            if (event.payload.pushId != null) {
                                DetailItem("Push ID", event.payload.pushId.toString(), isMonospace = true)
                            }
                            if (event.payload.size != null) {
                                DetailItem("Commits", EventUtils.formatCommitCount(event.payload.size))
                            }
                            if (event.payload.ref != null) {
                                DetailItem("Branch", EventUtils.formatBranchName(event.payload.ref))
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
                
                item {
                    InfoCard(
                        icon = Constants.UI.ACTIONS_ICON,
                        title = "Actions"
                    ) {
                        ActionButtonsRow(
                            onViewProfile = { IntentUtils.openUrl(context, EventUtils.buildProfileUrl(event.actor.login)) },
                            onOpenRepo = { IntentUtils.openUrl(context, repoWebUrl ?: "") }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top app bar for event detail screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailTopAppBar(
    event: GitHubEvent?,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
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
            IconButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = Constants.UI.SHARE_REPO_DESCRIPTION)
            }
        }
    )
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
        onClick = {},
        label = { Text(EventUtils.formatEventType(eventType)) }
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