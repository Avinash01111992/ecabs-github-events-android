package com.ecabs.events.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecabs.events.data.model.GitHubEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(event: GitHubEvent?, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event details") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (event == null) {
                Text("No event data", style = MaterialTheme.typography.titleMedium)
                return@Column
            }
            DetailRow("Type", event.type)
            DetailRow("Actor", event.actor.login)
            DetailRow("Repo", event.repo.name)
            DetailRow("Created", event.createdAt)
        }
    }
}

@Composable
private fun DetailRow(title: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}