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

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("GitHub Events", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    AssistChip(onClick = {}, label = {
                        Text("Next: ${'$'}{kotlin.math.max(10, nextPoll)}s")
                    })
                    Spacer(Modifier.width(12.dp))
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
            SearchAndFilters()
            Box(Modifier.fillMaxSize()) {
                if (events.isEmpty() && refreshing) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                if (events.isEmpty() && !refreshing) {
                    EmptyState(modifier = Modifier.align(Alignment.Center))
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(events, key = { it.id }) { event ->
                        EventCard(event = event, onClick = { onEventClick(event) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAndFilters() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("Search events (UI only)") }
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = true, onClick = {}, label = { Text("All") })
            FilterChip(selected = false, onClick = {}, label = { Text("Push") })
            FilterChip(selected = false, onClick = {}, label = { Text("PR") })
            FilterChip(selected = false, onClick = {}, label = { Text("Issues") })
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