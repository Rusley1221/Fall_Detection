package com.example.falldetection.detection

import org.junit.Assert.assertEquals
import org.junit.Test

class FallDetectorTest {
    private class Recorder : FallDetector.Listener {
        var detected = 0
        var rejected = 0
        var peak = 0.0

        override fun onFallDetected(peakAcceleration: Double) {
            detected++
            peak = peakAcceleration
        }

        override fun onStillFailed(peakAcceleration: Double) {
            rejected++
        }
    }

    private fun detectorWith(config: FallDetector.Config = FallDetector.Config()): Pair<FallDetector, Recorder> {
        val recorder = Recorder()
        return FallDetector(recorder, config) to recorder
    }

    @Test
    fun `benturan lalu diam memicu peringatan`() {
        val (detector, recorder) = detectorWith()
        detector.onSample(30.0, 0L)
        for (t in 1_000L..4_000L step 100L) detector.onSample(9.8, t)
        assertEquals(1, recorder.detected)
        assertEquals(30.0, recorder.peak, 0.01)
        assertEquals(FallDetector.State.COUNTDOWN, detector.state)
    }

    @Test
    fun `benturan lalu tetap bergerak tidak memicu peringatan`() {
        val (detector, recorder) = detectorWith()
        detector.onSample(30.0, 0L)
        for (t in 1_000L..4_000L step 100L) detector.onSample(15.0, t)
        assertEquals(0, recorder.detected)
        assertEquals(1, recorder.rejected)
        assertEquals(FallDetector.State.IDLE, detector.state)
    }

    @Test
    fun `guncangan di bawah ambang diabaikan`() {
        val (detector, recorder) = detectorWith()
        detector.onSample(18.0, 0L)
        for (t in 1_000L..4_000L step 100L) detector.onSample(9.8, t)
        assertEquals(0, recorder.detected)
        assertEquals(FallDetector.State.IDLE, detector.state)
    }

    @Test
    fun `cooldown mencegah dua peringatan berturut-turut`() {
        val (detector, recorder) = detectorWith()
        detector.onSample(30.0, 0L)
        for (t in 1_000L..4_000L step 100L) detector.onSample(9.8, t)
        detector.confirm(5_000L)
        detector.onSample(30.0, 10_000L)
        for (t in 11_000L..14_000L step 100L) detector.onSample(9.8, t)
        assertEquals(1, recorder.detected)
    }

    @Test
    fun `deteksi kembali bekerja setelah cooldown dengan clock yang sama`() {
        val (detector, recorder) = detectorWith()
        // Simulasi SystemClock.elapsedRealtime(): bukan epoch milliseconds.
        detector.onSample(30.0, 1_000L)
        for (t in 2_000L..5_000L step 100L) detector.onSample(9.8, t)
        detector.confirm(6_000L)
        assertEquals(1, recorder.detected)

        // anteseden bug: confirm(System.currentTimeMillis()) membuat cooldownUntil ~1.7e12
        // sehingga semua sampel elapsedRealtime berikutnya selalu < cooldownUntil.
        detector.onSample(30.0, 70_000L)
        for (t in 71_000L..74_000L step 100L) detector.onSample(9.8, t)
        assertEquals(2, recorder.detected)
    }

    @Test
    fun `pembatalan mengembalikan status ke cooldown`() {
        val (detector, _) = detectorWith()
        detector.onSample(30.0, 0L)
        for (t in 1_000L..4_000L step 100L) detector.onSample(9.8, t)
        assertEquals(true, detector.cancel(4_100L))
        assertEquals(FallDetector.State.COOLDOWN, detector.state)
        assertEquals(false, detector.cancel(4_200L))
    }
}
