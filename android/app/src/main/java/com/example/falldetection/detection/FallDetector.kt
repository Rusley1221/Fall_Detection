package com.example.falldetection.detection

import kotlin.math.sqrt

/**
 * Mesin status deteksi jatuh murni Kotlin (tanpa dependensi Android) agar mudah diuji unit.
 * Pola: benturan besar -> jeda -> kondisi diam -> peringatan hitung mundur.
 */
class FallDetector(
    private val listener: Listener,
    initialConfig: Config = Config(),
) {
    data class Config(
        val impactThreshold: Double = 25.0,
        val settleMs: Long = 1_000L,
        val stillWindowMs: Long = 3_000L,
        val stillStddev: Double = 1.0,
        val restMin: Double = 7.8,
        val restMax: Double = 11.8,
        val cooldownMs: Long = 60_000L,
    )

    interface Listener {
        fun onFallDetected(peakAcceleration: Double)
        fun onStillFailed(peakAcceleration: Double)
    }

    enum class State { IDLE, POST_IMPACT, COUNTDOWN, COOLDOWN }

    var state: State = State.IDLE
        private set

    private var config: Config = initialConfig
    private var peak = 0.0
    private var impactTime = 0L
    private var cooldownUntil = 0L
    private val window = ArrayDeque<Pair<Long, Double>>()

    fun updateConfig(value: Config) {
        config = value
    }

    fun onSample(acceleration: Double, timeMs: Long) {
        if (state == State.COUNTDOWN) return
        if (state == State.COOLDOWN && timeMs >= cooldownUntil) state = State.IDLE
        if (timeMs < cooldownUntil) return

        when (state) {
            State.IDLE -> {
                if (acceleration >= config.impactThreshold) {
                    peak = acceleration
                    impactTime = timeMs
                    state = State.POST_IMPACT
                    window.clear()
                }
            }

            State.POST_IMPACT -> {
                if (timeMs - impactTime < config.settleMs) {
                    peak = maxOf(peak, acceleration)
                    window.clear()
                } else {
                    window.addLast(timeMs to acceleration)
                    while (window.isNotEmpty() && timeMs - window.first().first > config.stillWindowMs) {
                        window.removeFirst()
                    }
                    val span = if (window.isEmpty()) 0L else timeMs - window.first().first
                    if (span >= config.stillWindowMs) evaluate()
                }
            }

            else -> Unit
        }
    }

    private fun evaluate() {
        val values = window.map { it.second }
        if (values.isEmpty()) return
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        val deviation = sqrt(variance)
        if (deviation < config.stillStddev && mean in config.restMin..config.restMax) {
            state = State.COUNTDOWN
            listener.onFallDetected(peak)
        } else {
            listener.onStillFailed(peak)
            state = State.IDLE
            window.clear()
        }
    }

    /** Dipanggil ketika hitung mundur habis (terkonfirmasi). */
    fun confirm(nowMs: Long) = finish(nowMs)

    /** Dipanggil ketika pengguna menekan "Saya baik-baik saja". */
    fun cancel(nowMs: Long): Boolean {
        if (state != State.COUNTDOWN) return false
        finish(nowMs)
        return true
    }

    private fun finish(nowMs: Long) {
        state = State.COOLDOWN
        cooldownUntil = nowMs + config.cooldownMs
        window.clear()
    }

    /** Pemicu manual untuk demo/tombol Simulasi Jatuh; sengaja mengabaikan cooldown. */
    fun forceFall(nowMs: Long) {
        peak = 35.0
        state = State.COUNTDOWN
        window.clear()
        listener.onFallDetected(peak)
    }
}
