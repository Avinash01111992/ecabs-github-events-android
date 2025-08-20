package com.ecabs.events.di

import com.ecabs.events.BuildConfig
import com.ecabs.events.data.remote.GitHubApi
import com.ecabs.events.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideGitHubTokenInterceptor(): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            
            val token = BuildConfig.GITHUB_TOKEN
            if (token.isNotEmpty()) {
                requestBuilder.addHeader(Constants.Network.AUTHORIZATION_HEADER, Constants.Network.TOKEN_PREFIX + token)
            }
            
            requestBuilder.addHeader(Constants.Network.ACCEPT_HEADER, Constants.Network.GITHUB_API_VERSION)
            requestBuilder.addHeader(Constants.Network.USER_AGENT_HEADER, Constants.Network.USER_AGENT_VALUE)
            
            chain.proceed(requestBuilder.build())
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        tokenInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(tokenInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(Constants.Network.CONNECT_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(Constants.Network.READ_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(Constants.Network.WRITE_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.Network.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGitHubApi(retrofit: Retrofit): GitHubApi {
        return retrofit.create(GitHubApi::class.java)
    }
}