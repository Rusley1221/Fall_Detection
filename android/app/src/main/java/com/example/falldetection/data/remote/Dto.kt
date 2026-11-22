package com.example.falldetection.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val message: String? = null,
    val data: T? = null,
    val errors: Map<String, String>? = null,
    val meta: Map<String, Int>? = null,
)

@Serializable
data class UserDto(
    val id: String,
    val full_name: String,
    val nim: String? = null,
    val phone: String? = null,
    val created_at: String? = null,
)

@Serializable
data class SettingsDto(
    val user_id: String,
    val impact_threshold: Double = 25.0,
    val inactivity_seconds: Int = 3,
    val countdown_seconds: Int = 20,
    val updated_at: String? = null,
)

@Serializable
data class ContactDto(
    val id: String,
    val user_id: String? = null,
    val name: String,
    val phone: String,
    val relationship: String,
    val is_primary: Boolean = false,
    val created_at: String? = null,
)

@Serializable
data class FallDto(
    val id: String,
    val user_id: String? = null,
    val detected_at: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val location_accuracy: Double? = null,
    val peak_acceleration: Double,
    val inactivity_seconds: Double? = null,
    val status: String,
    val notes: String? = null,
    val created_at: String? = null,
)

@Serializable
data class CreateUserRequest(
    val full_name: String,
    val phone: String? = null,
    val nim: String? = null,
)

@Serializable
data class UpdateUserRequest(
    val full_name: String,
    val phone: String? = null,
    val nim: String? = null,
)

@Serializable
data class UpdateSettingsRequest(
    val impact_threshold: Double,
    val inactivity_seconds: Int,
    val countdown_seconds: Int,
)

@Serializable
data class CreateFallRequest(
    val detected_at: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val location_accuracy: Double? = null,
    val peak_acceleration: Double,
    val inactivity_seconds: Double? = null,
    val status: String = "confirmed",
    val notes: String? = null,
    /** ID lokal dari antrean Room; dipakai server sebagai kunci idempotensi saat retry. */
    val client_id: String? = null,
)

@Serializable
data class UpdateFallRequest(
    val status: String,
    val notes: String? = null,
)

@Serializable
data class ContactRequest(
    val name: String,
    val phone: String,
    val relationship: String,
    val is_primary: Boolean = false,
)
