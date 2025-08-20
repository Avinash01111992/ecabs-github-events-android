package com.ecabs.events.data

import com.ecabs.events.data.model.GitHubEvent
import com.ecabs.events.data.model.TrackedEventType
import com.ecabs.events.data.remote.GitHubApi
import com.ecabs.events.util.Constants
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventsRepository @Inject constructor(
    private val api: GitHubApi
) {
    private val mutex = Mutex()
    private var lastEtag: String? = null
    private var pollIntervalSeconds: Int = Constants.Timeouts.DEFAULT_POLL_INTERVAL

    suspend fun fetchNewEvents(): FetchResult {
        val response: Response<List<GitHubEvent>> = api.getPublicEvents(lastEtag)
        val newEtag = response.headers()[Constants.Headers.ETAG]
        val serverPoll = response.headers()[Constants.Headers.POLL_INTERVAL]?.toIntOrNull()
        if (serverPoll != null) pollIntervalSeconds = serverPoll

        if (response.code() == Constants.HttpStatus.NOT_MODIFIED) {
            return FetchResult(emptyList(), pollIntervalSeconds, notModified = true)
        }
        val body = response.body().orEmpty()
        val filtered = body.filter { TrackedEventType.rawSet.contains(it.type) }
        mutex.withLock {
            if (!newEtag.isNullOrBlank()) {
                lastEtag = newEtag
            }
        }
        return FetchResult(filtered, pollIntervalSeconds, notModified = false)
    }

    fun pollInterval(): Int = pollIntervalSeconds
}

data class FetchResult(
    val events: List<GitHubEvent>,
    val nextPollSeconds: Int,
    val notModified: Boolean
)