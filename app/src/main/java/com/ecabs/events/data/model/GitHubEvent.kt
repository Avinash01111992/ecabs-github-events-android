
package com.ecabs.events.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubEvent(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String,
    @Json(name = "actor") val actor: Actor,
    @Json(name = "repo") val repo: Repo,
    @Json(name = "payload") val payload: Payload?,
    @Json(name = "public") val public: Boolean,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class Actor(
    @Json(name = "id") val id: Long,
    @Json(name = "login") val login: String,
    @Json(name = "display_login") val displayLogin: String?,
    @Json(name = "gravatar_id") val gravatarId: String?,
    @Json(name = "url") val url: String,
    @Json(name = "avatar_url") val avatarUrl: String
)

@JsonClass(generateAdapter = true)
data class Repo(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class Payload(
    @Json(name = "action") val action: String?,
    @Json(name = "push_id") val pushId: Long?,
    @Json(name = "size") val size: Int?,
    @Json(name = "distinct_size") val distinctSize: Int?,
    @Json(name = "ref") val ref: String?,
    @Json(name = "head") val head: String?,
    @Json(name = "before") val before: String?,
    @Json(name = "commits") val commits: List<Commit>?
)

@JsonClass(generateAdapter = true)
data class Commit(
    @Json(name = "sha") val sha: String,
    @Json(name = "author") val author: Author,
    @Json(name = "message") val message: String,
    @Json(name = "distinct") val distinct: Boolean,
    @Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class Author(
    @Json(name = "email") val email: String,
    @Json(name = "name") val name: String
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
