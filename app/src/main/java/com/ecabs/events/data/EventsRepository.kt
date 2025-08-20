package com.ecabs.events.data

import com.ecabs.events.data.model.GitHubEvent
import com.ecabs.events.data.model.TrackedEventType
import com.ecabs.events.data.remote.GitHubApi
import com.ecabs.events.util.Constants
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
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
        return try {
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
            
            FetchResult(filtered, pollIntervalSeconds, notModified = false)
        } catch (e: Exception) {
            throw RepositoryException("Failed to fetch events: ${e.message}", e)
        }
    }

    fun fetchEventsAsFlow(): Flow<FetchResult> = flow {
        emit(fetchNewEvents())
    }.catch { exception ->
        throw RepositoryException("Flow error: ${exception.message}", exception)
    }.map { result ->
        result
    }

    fun pollInterval(): Int = pollIntervalSeconds

    fun getLastEtag(): String? = lastEtag

    fun resetEtag() {
        mutex.withLock {
            lastEtag = null
        }
    }
}

data class FetchResult(
    val events: List<GitHubEvent>,
    val nextPollSeconds: Int,
    val notModified: Boolean
)

class RepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)