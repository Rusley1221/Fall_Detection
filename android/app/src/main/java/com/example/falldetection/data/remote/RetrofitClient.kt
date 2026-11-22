package com.example.falldetection.data.remote

import com.example.falldetection.BuildConfig
import com.example.falldetection.data.local.UserPreferences
import com.example.falldetection.util.AppJson
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

private class HeaderInterceptor(private val preferences: UserPreferences) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val userId = runBlocking { preferences.getUserIdOnce() }
        val request = chain.request().newBuilder()
            .addHeader("X-API-Key", BuildConfig.API_KEY)
            .apply { if (!userId.isNullOrBlank()) addHeader("X-User-Id", userId) }
            .build()
        return chain.proceed(request)
    }
}

object RetrofitClient {
    fun create(preferences: UserPreferences): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(HeaderInterceptor(preferences))
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(AppJson.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }
}
