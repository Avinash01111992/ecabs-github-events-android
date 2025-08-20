package com.ecabs.events.di

import android.content.Context
import com.ecabs.events.data.remote.GitHubApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton
import com.ecabs.events.BuildConfig

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides @Singleton
    fun provideOkHttp(@ApplicationContext context: Context): OkHttpClient {
        val cache = Cache(directory = context.cacheDir.resolve("http_cache"), maxSize = 5L * 1024 * 1024)
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val authInterceptor = Interceptor { chain ->
            val token = BuildConfig.GITHUB_TOKEN
            val request = if (!token.isNullOrBlank()) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "token $token")
                    .build()
            } else chain.request()
            chain.proceed(request)
        }
        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides @Singleton
    fun provideRetrofit(moshi: Moshi, okHttp: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttp)
            .build()

    @Provides @Singleton
    fun provideApi(retrofit: Retrofit): GitHubApi = retrofit.create(GitHubApi::class.java)
}