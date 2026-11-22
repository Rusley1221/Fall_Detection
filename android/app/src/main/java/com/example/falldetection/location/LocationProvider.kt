package com.example.falldetection.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
)

class LocationProvider(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun Location.toFix() = LocationFix(latitude, longitude, accuracy)

    suspend fun current(): LocationFix? {
        if (!hasPermission()) return null
        return try {
            val fresh = withTimeout(8_000) {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            }
            fresh?.toFix() ?: withTimeout(3_000) { client.lastLocation.await() }?.toFix()
        } catch (_: TimeoutCancellationException) {
            lastLocationSafe()
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            lastLocationSafe()
        }
    }

    private suspend fun lastLocationSafe(): LocationFix? = try {
        if (!hasPermission()) null else client.lastLocation.await()?.toFix()
    } catch (_: Exception) {
        null
    }
}
