package com.ecabs.events.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.ecabs.events.data.model.GitHubEvent
import com.ecabs.events.data.model.TrackedEventType
import com.ecabs.events.ui.theme.formatRelativeTime
import com.ecabs.events.util.Constants
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    onEventClick: (GitHubEvent) -> Unit,
    vm: EventsViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val refreshing by vm.isRefreshing.collectAsState()
    val countdown by vm.countdown.collectAsState()
    val errorMessage by vm.errorMessage.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(EventFilterType.All) }

    val visibleEvents by remember(uiState, searchQuery, selectedFilter) {
        derivedStateOf {
            when (uiState) {
                is EventsUiState.Success -> {
                    val events = (uiState as EventsUiState.Success).events
                    events.filter { event ->
                        val matchesType = when (selectedFilter) {
                            EventFilterType.All -> true
                            EventFilterType.Push -> event.type == TrackedEventType.Push.raw
                            EventFilterType.PR -> event.type == TrackedEventType.PullRequest.raw
                            EventFilterType.Issues -> event.type == TrackedEventType.Issues.raw
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
                else -> emptyList()
            }
        }
    }

    val shouldShowScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(Constants.UI.APP_TITLE) },
            actions = {
                Text(
                    text = Constants.UI.NEXT_REFRESH_PREFIX + "${kotlin.math.max(0, countdown)}s",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        )

        SearchAndFilters(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            selected = selectedFilter,
            onSelect = { selectedFilter = it }
        )

        Box(Modifier.fillMaxSize()) {
            when (uiState) {
                is EventsUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is EventsUiState.Empty -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState()
                    }
                }
                is EventsUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorState(
                            message = (uiState as EventsUiState.Error).message,
                            onRetry = { vm.refreshEvents() }
                        )
                    }
                }
                is EventsUiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visibleEvents, key = { it.id }) { event ->
                            EventCard(event = event, onClick = { onEventClick(event) })
                        }
                    }
                }
            }

            if (refreshing) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (shouldShowScrollToTop) {
            FloatingActionButton(
                onClick = {
                    scope.launch { listState.animateScrollToItem(0) }
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = Constants.UI.SCROLL_TO_TOP_DESCRIPTION)
            }
        }
    }
}

@Composable
private fun SearchAndFilters(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selected: EventFilterType,
    onSelect: (EventFilterType) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text(Constants.UI.SEARCH_PLACEHOLDER) }
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selected == EventFilterType.All, onClick = { onSelect(EventFilterType.All) }, label = { Text("All") })
            FilterChip(selected = selected == EventFilterType.Push, onClick = { onSelect(EventFilterType.Push) }, label = { Text("Push") })
            FilterChip(selected = selected == EventFilterType.PR, onClick = { onSelect(EventFilterType.PR) }, label = { Text("PR") })
            FilterChip(selected = selected == EventFilterType.Issues, onClick = { onSelect(EventFilterType.Issues) }, label = { Text("Issues") })
            FilterChip(selected = selected == EventFilterType.Watch, onClick = { onSelect(EventFilterType.Watch) }, label = { Text("Watch") })
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EventCard(event: GitHubEvent, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        ElevatedCard(
            onClick = onClick,
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            ListItem(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                leadingContent = {
                    Image(
                        painter = rememberAsyncImagePainter(event.actor.avatarUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                },
                headlineContent = {
                    Text(
                        text = event.actor.login,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                supportingContent = {
                    Text(
                        text = event.repo.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                trailingContent = {
                    AssistChip(onClick = {}, label = { Text(text = event.type.removeSuffix("Event")) })
                }
            )
        }
    }
}

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
        isoUtc
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(Constants.UI.NO_EVENTS_MESSAGE, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(Constants.UI.NO_EVENTS_SUBTITLE, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRetry) {
            Text(Constants.UI.RETRY_BUTTON)
        }
    }
}

private enum class EventFilterType { All, Push, PR, Issues, Watch }