package com.example.falldetection.detection

import android.app.Service
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.falldetection.FallDetectionApp
import com.example.falldetection.data.local.PendingEvent
import com.example.falldetection.data.repository.toFallDto
import com.example.falldetection.location.LocationFix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

class DetectionService : Service(), SensorEventListener {
    companion object {
        const val ACTION_START = "com.example.falldetection.START"
        const val ACTION_STOP = "com.example.falldetection.STOP"
        const val ACTION_CANCEL = "com.example.falldetection.CANCEL"
        const val ACTION_HELP = "com.example.falldetection.HELP"
        const val ACTION_SIMULATE = "com.example.falldetection.SIMULATE"
        const val ACTION_SETTINGS_CHANGED = "com.example.falldetection.SETTINGS_CHANGED"
        private const val TAG = "DetectionService"
    }

    private lateinit var container: com.example.falldetection.di.AppContainer
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var detector: FallDetector

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null
    private var locationJob: Deferred<LocationFix?>? = null
    private var ringtone: Ringtone? = null

    private var started = false
    private var alertActive = false
    private var countdownSeconds = 20
    private var inactivitySeconds = 3.0
    private var remaining = 0
    private var peak = 0.0
    /** Mencegah dua aksi (habis hitung mundur + tombol bantuan) mengirim SMS ganda. */
    private val settling = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        container = (application as FallDetectionApp).container
        AppNotifications.ensureChannels(this)
        sensorManager = getSystemService(SensorManager::class.java)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        detector = FallDetector(
            listener = object : FallDetector.Listener {
                override fun onFallDetected(peakAcceleration: Double) {
                    onCountdownStart(peakAcceleration)
                }

                override fun onStillFailed(peakAcceleration: Double) {
                    Log.d(TAG, "Benturan tanpa pola diam, diabaikan (peak=$peakAcceleration)")
                }
            },
        )

        refreshSettings()
    }

    private fun refreshSettings() {
        scope.launch {
            val settings = container.userRepository.settingsOnce() ?: return@launch
            countdownSeconds = settings.countdown_seconds
            inactivitySeconds = settings.inactivity_seconds.toDouble()
            detector.updateConfig(
                FallDetector.Config(
                    impactThreshold = settings.impact_threshold,
                    stillWindowMs = settings.inactivity_seconds * 1_000L,
                ),
            )
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP -> {
                stopMonitoring()
                stopSelf()
            }

            ACTION_CANCEL -> {
                // Bila service di-start ulang lewat fallback foreground, pastikan FGS aktif.
                if (!started) startMonitoring()
                cancelFall()
            }

            ACTION_HELP -> {
                if (!started) startMonitoring()
                scope.launch { confirmFall(manual = true) }
            }
            ACTION_SIMULATE -> {
                if (!started) startMonitoring()
                detector.forceFall(SystemClock.elapsedRealtime())
            }

            ACTION_SETTINGS_CHANGED -> refreshSettings()
            else -> {
                // START_STICKY mengirim intent null saat service dibangun ulang sistem.
                if (!started) startMonitoring()
            }
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        if (started) return
        started = true
        DetectionStateHolder.setMonitoring(true)
        startForegroundNotification("Pemantauan jatuh aktif", "Aplikasi siap mendeteksi jatuh")

        val sensor = accelerometer
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            Log.w(TAG, "Perangkat tidak memiliki akselerometer; gunakan tombol Simulasi Jatuh")
        }
    }

    private fun startForegroundNotification(title: String, text: String) {
        val notification = AppNotifications.monitorNotification(this, title, text)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasLocationPermission()) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            // Izin lokasi ditolak: sensor tetap dipantau tanpa FGS bertipe location.
            0
        }
        try {
            ServiceCompat.startForeground(this, AppNotifications.ID_MONITOR, notification, type)
        } catch (e: SecurityException) {
            Log.w(TAG, "FGS location ditolak, coba tanpa tipe lokasi: ${e.message}")
            ServiceCompat.startForeground(this, AppNotifications.ID_MONITOR, notification, 0)
        }
    }

    private fun updateMonitorNotification(title: String, text: String) {
        if (!started) return
        AppNotifications.show(
            this,
            AppNotifications.ID_MONITOR,
            AppNotifications.monitorNotification(this, title, text),
        )
    }

    override fun onSensorChanged(event: SensorEvent) {
        val values = event.values
        if (values.size < 3) return
        val acceleration = sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
        // Gunakan satu clock yang sama dengan confirm()/cancel()/forceFall().
        detector.onSample(acceleration.toDouble(), SystemClock.elapsedRealtime())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun onCountdownStart(peakAcceleration: Double) {
        if (alertActive) return
        settling.set(false)
        alertActive = true
        peak = peakAcceleration
        remaining = countdownSeconds

        // Lokasi diminta sejak peringatan mulai agar siap saat hitung mundur habis.
        locationJob = scope.async { container.locationProvider.current() }

        DetectionStateHolder.showAlert(peak, countdownSeconds)
        startAlertFeedback()
        AppNotifications.show(
            this,
            AppNotifications.ID_ALERT,
            AppNotifications.alertNotification(this, "Tekan layar untuk membatalkan dalam $countdownSeconds detik"),
        )
        openAlertScreen()
        updateMonitorNotification("Jatuh terdeteksi", "Tekan Saya baik-baik saja bila Anda aman")

        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (remaining > 0 && alertActive) {
                delay(1_000)
                remaining--
                DetectionStateHolder.setSecondsLeft(remaining)
            }
            if (alertActive) confirmFall(manual = false)
        }
    }

    private fun openAlertScreen() {
        val intent = Intent(this, AlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private fun startAlertFeedback() {
        try {
            val vibrator = getSystemService(Vibrator::class.java)
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 400, 600), 0))
            ringtone = RingtoneManager.getRingtone(
                this,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            )
            ringtone?.play()
        } catch (e: Exception) {
            Log.w(TAG, "Tidak dapat memainkan bunyi/getar: ${e.message}")
        }
    }

    private fun stopAlertFeedback() {
        try {
            getSystemService(Vibrator::class.java)?.cancel()
            ringtone?.stop()
            ringtone = null
        } catch (_: Exception) {
            // Abaikan: umpan balik tidak boleh menggagalkan pencatatan kejadian.
        }
    }

    private suspend fun confirmFall(manual: Boolean) {
        if (!alertActive && !manual) return
        // Idempoten: hanya satu aksi yang boleh memproses alert ini.
        if (!settling.compareAndSet(false, true)) return
        try {
            tickerJob?.cancel()
            alertActive = false
            stopAlertFeedback()

            // Saat tombol bantuan ditekan dari Beranda tidak ada locationJob dari countdown,
            // jadi permintaan lokasi dibuat di sini agar SMS tetap memuat Maps link.
            val location = locationJob ?: scope.async { container.locationProvider.current() }
                .also { locationJob = it }
            val fix: LocationFix? = try {
                withTimeout(4_000) { location.await() }
            } catch (_: Exception) {
                null
            }

            val recordedPeak = if (peak > 0.0) peak else 30.0
            val event = PendingEvent(
                id = UUID.randomUUID().toString(),
                detected_at = OffsetDateTime.now().toString(),
                latitude = fix?.latitude,
                longitude = fix?.longitude,
                location_accuracy = fix?.accuracy?.toDouble(),
                peak_acceleration = recordedPeak,
                inactivity_seconds = inactivitySeconds,
                status = "confirmed",
                notes = if (manual) "Bantuan diminta pengguna" else null,
            )

            val report = container.fallRepository.queueAndSend(event)
            val finalEvent = report.serverEvent ?: event.toFallDto()
            val sms = container.smsRepository.sendForFall(finalEvent)

            DetectionStateHolder.setLastEvent(finalEvent)
            DetectionStateHolder.setSmsStatus("${report.message}. ${sms.message}")
            DetectionStateHolder.hideAlert()
            AppNotifications.cancel(this, AppNotifications.ID_ALERT)
            // Semua waktu detektor memakai elapsedRealtime () yang sama dengan onSample().
            detector.confirm(SystemClock.elapsedRealtime())

            AppNotifications.show(
                this,
                AppNotifications.ID_RESULT,
                AppNotifications.resultNotification(
                    this,
                    "Jatuh terkonfirmasi",
                    "${report.message}. ${sms.message}",
                ),
            )
            updateMonitorNotification("Pemantauan aktif", "Pemantauan jatuh aktif")
        } finally {
            settling.set(false)
        }
    }

    private fun cancelFall() {
        if (!alertActive) return
        if (!settling.compareAndSet(false, true)) return
        try {
            tickerJob?.cancel()
            alertActive = false
            stopAlertFeedback()
            detector.cancel(SystemClock.elapsedRealtime())

            val event = PendingEvent(
                id = UUID.randomUUID().toString(),
                detected_at = OffsetDateTime.now().toString(),
                latitude = null,
                longitude = null,
                location_accuracy = null,
                peak_acceleration = peak,
                inactivity_seconds = inactivitySeconds,
                status = "cancelled",
                notes = "Dibatalkan pengguna",
            )

            scope.launch {
                val report = container.fallRepository.queueAndSend(event)
                DetectionStateHolder.setLastEvent(report.serverEvent ?: event.toFallDto())
                DetectionStateHolder.setSmsStatus("Tidak ada SMS: peringatan dibatalkan")
                DetectionStateHolder.hideAlert()
                AppNotifications.cancel(this@DetectionService, AppNotifications.ID_ALERT)
                AppNotifications.show(
                    this@DetectionService,
                    AppNotifications.ID_RESULT,
                    AppNotifications.resultNotification(
                        this@DetectionService,
                        "Peringatan dibatalkan",
                        "Kejadian dicatat sebagai peringatan palsu.",
                    ),
                )
            }
            updateMonitorNotification("Pemantauan aktif", "Pemantauan jatuh aktif")
        } finally {
            settling.set(false)
        }
    }

    private fun stopMonitoring() {
        started = false
        alertActive = false
        tickerJob?.cancel()
        stopAlertFeedback()
        sensorManager.unregisterListener(this)
        DetectionStateHolder.setMonitoring(false)
        DetectionStateHolder.hideAlert()
        AppNotifications.cancel(this, AppNotifications.ID_ALERT)
    }

    override fun onDestroy() {
        stopMonitoring()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
