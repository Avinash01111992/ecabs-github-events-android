package com.ecabs.events.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
            TopAppBar(title = { Text("GitHub Events") }, actions = {
                Text(
                    text = "Next poll≥ ${'$'}{kotlin.math.max(10, nextPoll)}s",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(end = 12.dp)
                )
            })
        },
        floatingActionButton = {
            if (listState.firstVisibleItemIndex > 0) {
                FloatingActionButton(onClick = {
                    scope.launch { listState.animateScrollToItem(0) }
                }) { Text("↑") }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (events.isEmpty() && refreshing) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    EventRow(event = event, onClick = { onEventClick(event) })
                    Divider()
                }
            }
        }
    }
}

@Composable
fun EventRow(event: GitHubEvent, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(event.actor.avatarUrl),
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text = event.actor.login, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = "${'$'}{event.type} in ${'$'}{event.repo.name}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = event.createdAt, style = MaterialTheme.typography.labelSmall)
        }
    }
}