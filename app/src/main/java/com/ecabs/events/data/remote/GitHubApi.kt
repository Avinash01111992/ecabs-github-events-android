package com.ecabs.events.data.remote

import com.ecabs.events.data.model.GitHubEvent
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface GitHubApi {
    @GET("events")
    suspend fun getPublicEvents(
        @Header("If-None-Match") etag: String? = null
    ): Response<List<GitHubEvent>>
}