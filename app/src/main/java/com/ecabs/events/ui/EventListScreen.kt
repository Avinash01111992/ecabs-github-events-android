package com.ecabs.events.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.ecabs.events.data.model.GitHubEvent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    onEventClick: (GitHubEvent) -> Unit,
    vm: EventsViewModel = hiltViewModel()
) {
    val events by vm.events.collectAsState()
    val refreshing by vm.isRefreshing.collectAsState()
    val nextPoll by vm.nextPoll.collectAsState()
    val countdown by vm.countdown.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(EventFilterType.All) }

    val visibleEvents by remember(events, searchQuery, selectedFilter) {
        derivedStateOf {
            events.filter { event ->
                val matchesType = when (selectedFilter) {
                    EventFilterType.All -> true
                    EventFilterType.Push -> event.type == "PushEvent"
                    EventFilterType.PR -> event.type == "PullRequestEvent"
                    EventFilterType.Issues -> event.type == "IssuesEvent"
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GitHub Events") },
                actions = {
                    Text(
                        text = "Next refresh: ${kotlin.math.max(0, countdown)}s",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            if (listState.firstVisibleItemIndex > 0) {
                FloatingActionButton(onClick = {
                    scope.launch { listState.animateScrollToItem(0) }
                }) { Text("↑") }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SearchAndFilters(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                selected = selectedFilter,
                onSelect = { selectedFilter = it }
            )
            Box(Modifier.fillMaxSize()) {
                if (visibleEvents.isEmpty() && refreshing) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                if (visibleEvents.isEmpty() && !refreshing) {
                    EmptyState(modifier = Modifier.align(Alignment.Center))
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(visibleEvents, key = { it.id }) { event ->
                        EventCard(event = event, onClick = { onEventClick(event) })
                    }
                }
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
            placeholder = { Text("Search events (UI only)") }
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = selected == EventFilterType.All, onClick = { onSelect(EventFilterType.All) }, label = { Text("All") })
            FilterChip(selected = selected == EventFilterType.Push, onClick = { onSelect(EventFilterType.Push) }, label = { Text("Push") })
            FilterChip(selected = selected == EventFilterType.PR, onClick = { onSelect(EventFilterType.PR) }, label = { Text("PR") })
            FilterChip(selected = selected == EventFilterType.Issues, onClick = { onSelect(EventFilterType.Issues) }, label = { Text("Issues") })
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EventCard(event: GitHubEvent, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberAsyncImagePainter(event.actor.avatarUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = event.actor.login, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = event.repo.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            AssistChip(onClick = {}, label = { Text(text = event.type.removeSuffix("Event")) })
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = event.createdAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Divider(Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No events yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text("Try again in a few seconds.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
    }
}

private enum class EventFilterType { All, Push, PR, Issues }