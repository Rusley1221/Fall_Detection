package com.example.falldetection.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.falldetection.data.AppResult
import com.example.falldetection.data.local.UserPreferences
import com.example.falldetection.data.remote.FallDto
import com.example.falldetection.util.Format

data class SmsResult(val success: Boolean, val message: String, val count: Int)

/**
 * Mengirim SMS darurat ke seluruh kontak darurat yang tersedia ketika jatuh dikonfirmasi.
 * Pesan memuat identitas pengguna, waktu kejadian, dan tautan Google Maps lokasi jatuh.
 */
class SmsRepository(
    private val context: Context,
    private val contacts: ContactRepository,
    private val preferences: UserPreferences,
) {
    suspend fun sendForFall(fall: FallDto): SmsResult {
        if (!preferences.isSmsEnabled()) {
            return SmsResult(false, "SMS darurat dinonaktifkan", 0)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return SmsResult(false, "Izin SMS belum diberikan", 0)
        }

        val recipients = when (val result = contacts.list()) {
            is AppResult.Success -> result.data
            else -> contacts.cached()
        }
        if (recipients.isEmpty()) {
            return SmsResult(false, "Belum ada kontak darurat", 0)
        }

        val message = buildMessage(fall)
        val manager: SmsManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        if (manager == null) {
            return SmsResult(false, "Perangkat tidak mendukung SMS", 0)
        }

        var sent = 0
        val parts = try {
            manager.divideMessage(message)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menyiapkan SMS: ${e.message}")
            return SmsResult(false, "SMS tidak dapat disiapkan", 0)
        }
        for (contact in recipients) {
            val destination = Format.phone(contact.phone)
            if (destination.length < 8) {
                Log.e(TAG, "Nomor tidak valid untuk ${contact.name}: ${contact.phone}")
                continue
            }
            try {
                if (parts.size > 1) {
                    manager.sendMultipartTextMessage(destination, null, ArrayList(parts), null, null)
                } else {
                    manager.sendTextMessage(destination, null, message, null, null)
                }
                sent++
                Log.i(TAG, "SMS darurat terkirim ke ${contact.name} ($destination)")
            } catch (e: Exception) {
                Log.e(TAG, "Gagal kirim SMS ke $destination: ${e.message}")
            }
        }

        return if (sent > 0) {
            SmsResult(true, "SMS darurat dikirimkan ke $sent kontak", sent)
        } else {
            SmsResult(false, "SMS gagal dikirim", 0)
        }
    }

    fun buildMessage(fall: FallDto): String {
        val link = Format.mapsUrl(fall.latitude, fall.longitude)
        val location = link ?: "Lokasi tidak tersedia (GPS gagal)"
        return "DARURAT! Syauqi Akifur Rahman (NIM 21120124130097) terdeteksi jatuh " +
                "pada ${Format.dateTime(fall.detected_at)}. Lokasi: $location. Mohon segera bantu."
    }

    private companion object {
        const val TAG = "SmsRepository"
    }
}
