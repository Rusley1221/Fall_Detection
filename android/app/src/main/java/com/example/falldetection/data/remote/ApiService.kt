package com.example.falldetection.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("health")
    suspend fun health(): ApiResponse<Map<String, String>>

    @POST("users")
    suspend fun createUser(@Body body: CreateUserRequest): ApiResponse<UserDto>

    @GET("users/me")
    suspend fun getUser(): ApiResponse<UserDto>

    @PUT("users/me")
    suspend fun updateUser(@Body body: UpdateUserRequest): ApiResponse<UserDto>

    @GET("settings")
    suspend fun getSettings(): ApiResponse<SettingsDto>

    @PUT("settings")
    suspend fun updateSettings(@Body body: UpdateSettingsRequest): ApiResponse<SettingsDto>

    @GET("falls")
    suspend fun getFalls(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
    ): ApiResponse<List<FallDto>>

    @GET("falls/{id}")
    suspend fun getFall(@Path("id") id: String): ApiResponse<FallDto>

    @POST("falls")
    suspend fun createFall(@Body body: CreateFallRequest): ApiResponse<FallDto>

    @PATCH("falls/{id}")
    suspend fun patchFall(@Path("id") id: String, @Body body: UpdateFallRequest): ApiResponse<FallDto>

    @DELETE("falls/{id}")
    suspend fun deleteFall(@Path("id") id: String): ApiResponse<Unit>

    @GET("contacts")
    suspend fun getContacts(): ApiResponse<List<ContactDto>>

    @GET("contacts/{id}")
    suspend fun getContact(@Path("id") id: String): ApiResponse<ContactDto>

    @POST("contacts")
    suspend fun createContact(@Body body: ContactRequest): ApiResponse<ContactDto>

    @PUT("contacts/{id}")
    suspend fun updateContact(@Path("id") id: String, @Body body: ContactRequest): ApiResponse<ContactDto>

    @DELETE("contacts/{id}")
    suspend fun deleteContact(@Path("id") id: String): ApiResponse<Unit>
}
