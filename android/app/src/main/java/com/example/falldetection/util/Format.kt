package com.example.falldetection.util

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Format {
    private val displayFormatter = DateTimeFormatter
        .ofPattern("EEEE, dd MMM yyyy, HH.mm", Locale("id", "ID"))
        .withZone(ZoneId.of("Asia/Jakarta"))

    private val dateFormatter = DateTimeFormatter
        .ofPattern("EEEE, dd MMM yyyy", Locale("id", "ID"))
        .withZone(ZoneId.of("Asia/Jakarta"))

    private val timeFormatter = DateTimeFormatter
        .ofPattern("HH.mm", Locale("id", "ID"))
        .withZone(ZoneId.of("Asia/Jakarta"))

    fun dateTime(iso: String): String = try {
        "${displayFormatter.format(OffsetDateTime.parse(iso).toInstant())} WIB"
    } catch (_: Exception) {
        iso
    }

    fun date(iso: String): String = try {
        dateFormatter.format(OffsetDateTime.parse(iso).toInstant())
    } catch (_: Exception) {
        iso
    }

    fun time(iso: String): String = try {
        timeFormatter.format(OffsetDateTime.parse(iso).toInstant())
    } catch (_: Exception) {
        "--.--"
    }

    fun mapsUrl(latitude: Double?, longitude: Double?): String? =
        if (latitude != null && longitude != null) {
            "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
        } else {
            null
        }

    /** Menyamakan nomor SMS: buang spasi/tanda baca, ubah +62 menjadi 0. */
    fun phone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.startsWith("620") -> "0" + digits.removePrefix("620")
            digits.startsWith("62") -> "0" + digits.removePrefix("62")
            digits.startsWith("0") -> digits
            else -> digits
        }
    }

    fun coordinates(latitude: Double?, longitude: Double?): String =
        if (latitude != null && longitude != null) {
            String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
        } else {
            "Lokasi tidak tersedia"
        }

    fun statusLabel(status: String): String = when (status) {
        "confirmed" -> "Terkonfirmasi"
        "cancelled" -> "Dibatalkan"
        "resolved" -> "Sudah ditangani"
        else -> status.replaceFirstChar { it.uppercase() }
    }

    /** Nama sensitivitas mengikuti ambang batas yang dipakai Profil. */
    fun sensitivityLabel(threshold: Double?): String? = when {
        threshold == null -> null
        threshold >= 28 -> "Rendah"
        threshold >= 23 -> "Sedang"
        threshold >= 18 -> "Tinggi"
        else -> "Demo"
    }
}
