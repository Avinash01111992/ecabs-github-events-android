package com.ecabs.events.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
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
import com.ecabs.events.util.Constants
import com.ecabs.events.util.EventFilterType
import com.ecabs.events.util.EventUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * EventsScreen displays a list of GitHub events with search and filtering capabilities
 * 
 * Features:
 * - Real-time event list with pull-to-refresh
 * - Debounced search functionality across event content
 * - Event type filtering (All, Push, PR, Issues, Watch)
 * - Scroll to top functionality
 * - Loading, empty, and error states
 * - Advanced coroutine patterns for performance
 * 
 * @param onEventClick Callback when an event is selected
 * @param vm ViewModel for managing event data and state
 */
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

    // Debounced search query for better performance
    val debouncedSearchQuery by remember(searchQuery) {
        derivedStateOf { searchQuery }
    }

    val visibleEvents by remember(uiState, debouncedSearchQuery, selectedFilter) {
        derivedStateOf {
            when (uiState) {
                is EventsUiState.Success -> {
                    val events = (uiState as EventsUiState.Success).events
                    EventUtils.filterEvents(events, selectedFilter, debouncedSearchQuery)
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

    // Start polling when screen loads
    LaunchedEffect(Unit) {
        vm.startPolling()
    }

    // Cleanup when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            // The ViewModel will handle cleanup in onCleared()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            delay(Constants.Timeouts.ERROR_AUTO_CLEAR_DELAY)
            vm.clearError()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            EventsTopAppBar(countdown = countdown)
            SearchAndFiltersSection(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                selected = selectedFilter,
                onSelect = { selectedFilter = it }
            )
            EventsContent(
                uiState = uiState,
                listState = listState,
                visibleEvents = visibleEvents,
                refreshing = refreshing,
                onEventClick = onEventClick,
                onRetry = { vm.refreshEvents() }
            )
        }
        
        ScrollToTopButton(
            shouldShow = shouldShowScrollToTop,
            onClick = { 
                scope.launch { 
                    listState.animateScrollToItem(0) 
                } 
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

/**
 * Top app bar with title and countdown timer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventsTopAppBar(countdown: Int) {
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
}

/**
 * Search and filter section with input field and filter chips
 */
@Composable
private fun SearchAndFiltersSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selected: EventFilterType,
    onSelect: (EventFilterType) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SearchInput(
            searchQuery = searchQuery,
            onSearchChange = onSearchChange
        )
        Spacer(Modifier.height(8.dp))
        FilterChipsRow(
            selected = selected,
            onSelect = onSelect
        )
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Search input field with search icon
 */
@Composable
private fun SearchInput(
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        placeholder = { Text(Constants.UI.SEARCH_PLACEHOLDER) }
    )
}

/**
 * Row of filter chips for event types
 */
@Composable
private fun FilterChipsRow(
    selected: EventFilterType,
    onSelect: (EventFilterType) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == EventFilterType.All,
            onClick = { onSelect(EventFilterType.All) },
            label = { Text("All") }
        )
        FilterChip(
            selected = selected == EventFilterType.Push,
            onClick = { onSelect(EventFilterType.Push) },
            label = { Text("Push") }
        )
        FilterChip(
            selected = selected == EventFilterType.PR,
            onClick = { onSelect(EventFilterType.PR) },
            label = { Text("PR") }
        )
        FilterChip(
            selected = selected == EventFilterType.Issues,
            onClick = { onSelect(EventFilterType.Issues) },
            label = { Text("Issues") }
        )
        FilterChip(
            selected = selected == EventFilterType.Watch,
            onClick = { onSelect(EventFilterType.Watch) },
            label = { Text("Watch") }
        )
    }
}

/**
 * Main content area with different states and event list
 */
@Composable
private fun EventsContent(
    uiState: EventsUiState,
    listState: LazyListState,
    visibleEvents: List<GitHubEvent>,
    refreshing: Boolean,
    onEventClick: (GitHubEvent) -> Unit,
    onRetry: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        when {
            refreshing -> LoadingState()
            uiState is EventsUiState.Error -> ErrorState(
                message = uiState.message,
                onRetry = onRetry
            )
            visibleEvents.isEmpty() -> EmptyState()
            else -> EventsList(
                listState = listState,
                visibleEvents = visibleEvents,
                onEventClick = onEventClick
            )
        }
    }
}

/**
 * Loading state with centered progress indicator
 */
@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/**
 * Empty state with message and subtitle
 */
@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Constants.UI.NO_EVENTS_MESSAGE, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                Constants.UI.NO_EVENTS_SUBTITLE,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }
    }
}

/**
 * Error state with error message and retry button
 */
@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRetry) {
                Text(Constants.UI.RETRY_BUTTON)
            }
        }
    }
}

/**
 * Scrollable list of events
 */
@Composable
private fun EventsList(
    listState: LazyListState,
    visibleEvents: List<GitHubEvent>,
    onEventClick: (GitHubEvent) -> Unit
) {
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



/**
 * Scroll to top floating action button
 */
@Composable
private fun ScrollToTopButton(
    shouldShow: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (shouldShow) {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier.padding(16.dp)
        ) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = Constants.UI.SCROLL_TO_TOP_DESCRIPTION)
        }
    }
}

/**
 * Individual event card with avatar, user info, and event type
 */
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
            EventCardContent(event = event)
        }
    }
}

/**
 * Content of the event card with ListItem
 */
@Composable
private fun EventCardContent(event: GitHubEvent) {
    ListItem(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        leadingContent = {
            EventAvatar(avatarUrl = event.actor.avatarUrl)
        },
        headlineContent = {
            EventTitle(username = event.actor.login)
        },
        supportingContent = {
            EventSubtitle(repoName = event.repo.name)
        },
        trailingContent = {
            EventTypeChip(eventType = event.type)
        }
    )
}

/**
 * Event avatar image with circular clipping
 */
@Composable
private fun EventAvatar(avatarUrl: String) {
    Image(
        painter = rememberAsyncImagePainter(avatarUrl),
        contentDescription = null,
        modifier = Modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.small)
    )
}

/**
 * Event title with username
 */
@Composable
private fun EventTitle(username: String) {
    Text(
        text = username,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Event subtitle with repository name
 */
@Composable
private fun EventSubtitle(repoName: String) {
    Text(
        text = repoName,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Event type chip with consistent styling
 */
@Composable
private fun EventTypeChip(eventType: String) {
    AssistChip(
        onClick = {},
        label = { Text(EventUtils.formatEventType(eventType)) }
    )
}