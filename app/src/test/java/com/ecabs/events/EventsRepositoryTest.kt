package com.ecabs.events

import com.ecabs.events.data.EventsRepository
import com.ecabs.events.data.remote.GitHubApi
import com.ecabs.events.data.model.TrackedEventType
import com.ecabs.events.util.Constants
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class EventsRepositoryTest {

    @Test
    fun testFetchFiltersAndHandlesHeaders() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(Constants.Headers.ETAG, "abc")
                .setHeader(Constants.Headers.POLL_INTERVAL, "60")
                .setBody("""[
                    {"id":"1","type":"${TrackedEventType.Push.raw}","actor":{"id":1,"login":"u1","display_login":"u1","gravatar_id":"","url":"https://api.github.com/users/u1","avatar_url":"https://avatars.githubusercontent.com/u/1?v=4"},"repo":{"id":1,"name":"r1","url":"https://api.github.com/repos/r1"},"payload":null,"public":true,"created_at":"2024-01-01T00:00:00Z"},
                    {"id":"2","type":"UnknownEvent","actor":{"id":2,"login":"u2","display_login":"u2","gravatar_id":"","url":"https://api.github.com/users/u2","avatar_url":"https://avatars.githubusercontent.com/u/2?v=4"},"repo":{"id":2,"name":"r2","url":"https://api.github.com/repos/r2"},"payload":null,"public":true,"created_at":"2024-01-01T00:00:00Z"}
                ]""")
        )
        server.start()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val client = OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor()).build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApi::class.java)
        val repo = EventsRepository(api)
        val result = repo.fetchNewEvents()
        assertEquals(1, result.events.size)
        assertEquals(60, result.nextPollSeconds)
        server.shutdown()
    }
}