package com.example.falldetection.data

import com.example.falldetection.data.remote.ApiResponse
import java.io.IOException
import java.net.SocketTimeoutException

sealed class AppResult<out T> {
    data object Loading : AppResult<Nothing>()
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val message: String) : AppResult<Nothing>()
}

internal fun friendlyMessage(error: Exception): String = when (error) {
    is SocketTimeoutException -> "Server terlalu lama merespons. Silakan coba lagi."
    is IOException -> "Tidak dapat terhubung. Periksa internet Anda."
    else -> error.message?.takeIf { it.isNotBlank() } ?: "Terjadi kesalahan. Silakan coba lagi."
}

internal suspend fun <T> callApi(block: suspend () -> ApiResponse<T>): AppResult<T> = try {
    val response = block()
    if (response.success && response.data != null) {
        AppResult.Success(response.data)
    } else {
        val detail = response.errors?.values?.firstOrNull()
        AppResult.Error(detail ?: response.message ?: "Data tidak dapat dimuat")
    }
} catch (e: Exception) {
    AppResult.Error(friendlyMessage(e))
}

internal suspend fun callApiUnit(block: suspend () -> ApiResponse<Unit>): AppResult<Unit> = try {
    val response = block()
    if (response.success) AppResult.Success(Unit) else AppResult.Error(response.message ?: "Gagal menyimpan data")
} catch (e: Exception) {
    AppResult.Error(friendlyMessage(e))
}
