package com.ecabs.events

import com.ecabs.events.data.EventsRepository
import com.ecabs.events.data.remote.GitHubApi
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
                .setHeader("ETag", "abc")
                .setHeader("X-Poll-Interval", "60")
                .setBody("""[
                    {"id":"1","type":"PushEvent","actor":{"login":"u1","avatar_url":""},"repo":{"name":"r1","url":""},"created_at":"2024-01-01T00:00:00Z"},
                    {"id":"2","type":"UnknownEvent","actor":{"login":"u2","avatar_url":""},"repo":{"name":"r2","url":""},"created_at":"2024-01-01T00:00:00Z"}
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