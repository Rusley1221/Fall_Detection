package com.example.falldetection.detection

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.falldetection.data.remote.FallDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AlertState(
    val active: Boolean = false,
    val secondsLeft: Int = 0,
    val totalSeconds: Int = 20,
    val peakAcceleration: Double = 0.0,
)

/** State bersama antara foreground service dan UI (Compose). */
object DetectionStateHolder {
    private val _monitoring = MutableStateFlow(false)
    val monitoring: StateFlow<Boolean> = _monitoring.asStateFlow()

    private val _alert = MutableStateFlow(AlertState())
    val alert: StateFlow<AlertState> = _alert.asStateFlow()

    private val _smsStatus = MutableStateFlow("")
    val smsStatus: StateFlow<String> = _smsStatus.asStateFlow()

    private val _lastEvent = MutableStateFlow<FallDto?>(null)
    val lastEvent: StateFlow<FallDto?> = _lastEvent.asStateFlow()

    fun setMonitoring(value: Boolean) {
        _monitoring.value = value
    }

    fun showAlert(peak: Double, seconds: Int) {
        _alert.value = AlertState(true, seconds, seconds, peak)
    }

    fun setSecondsLeft(value: Int) {
        _alert.value = _alert.value.copy(secondsLeft = value)
    }

    fun hideAlert() {
        _alert.value = _alert.value.copy(active = false, secondsLeft = 0)
    }

    fun setSmsStatus(value: String) {
        _smsStatus.value = value
    }

    fun setLastEvent(value: FallDto?) {
        _lastEvent.value = value
    }

    private fun action(context: Context, name: String, foreground: Boolean = false) {
        val intent = Intent(context, DetectionService::class.java).setAction(name)
        if (foreground) {
            ContextCompat.startForegroundService(context, intent)
            return
        }
        try {
            context.startService(intent)
        } catch (e: IllegalStateException) {
            // Android membatasi startService dari background. Aksi yang menyangkut alert
            // harus tetap diproses, jadi dinaikkan ke foreground service.
            if (name == DetectionService.ACTION_CANCEL || name == DetectionService.ACTION_HELP) {
                ContextCompat.startForegroundService(context, intent)
            }
            // STOP / SETTINGS_CHANGED aman diabaikan bila service memang tidak berjalan.
        }
    }

    fun start(context: Context) = action(context, DetectionService.ACTION_START, foreground = true)
    fun stop(context: Context) = action(context, DetectionService.ACTION_STOP)
    fun cancel(context: Context) = action(context, DetectionService.ACTION_CANCEL)
    fun helpNow(context: Context) = action(context, DetectionService.ACTION_HELP)
    fun simulate(context: Context) = action(context, DetectionService.ACTION_SIMULATE, foreground = true)
    fun refreshSettings(context: Context) = action(context, DetectionService.ACTION_SETTINGS_CHANGED)
}
