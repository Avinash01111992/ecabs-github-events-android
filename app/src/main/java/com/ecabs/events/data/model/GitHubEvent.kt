
package com.ecabs.events.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubEvent(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String,
    @Json(name = "actor") val actor: Actor,
    @Json(name = "repo") val repo: Repo,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class Actor(
    @Json(name = "login") val login: String,
    @Json(name = "avatar_url") val avatarUrl: String
)

@JsonClass(generateAdapter = true)
data class Repo(
    @Json(name = "name") val name: String,
    @Json(name = "url") val url: String
)

enum class TrackedEventType(val raw: String) {
    Push("PushEvent"),
    PullRequest("PullRequestEvent"),
    Issues("IssuesEvent"),
    Fork("ForkEvent"),
    Watch("WatchEvent");
    
    companion object {
        val rawSet = entries.map { it.raw }.toSet()
    }
}
