package com.example.falldetection.data.repository

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.falldetection.data.AppResult
import com.example.falldetection.data.callApi
import com.example.falldetection.data.callApiUnit
import com.example.falldetection.data.friendlyMessage
import com.example.falldetection.data.local.PendingEvent
import com.example.falldetection.data.local.PendingEventDao
import com.example.falldetection.data.remote.ApiService
import com.example.falldetection.data.remote.CreateFallRequest
import com.example.falldetection.data.remote.FallDto
import com.example.falldetection.data.remote.UpdateFallRequest
import com.example.falldetection.worker.FallSyncWorker
import kotlinx.coroutines.flow.Flow

data class ReportResult(
    val serverEvent: FallDto?,
    val delivered: Boolean,
    val message: String,
)

fun PendingEvent.toRequest() = CreateFallRequest(
    detected_at = detected_at,
    latitude = latitude,
    longitude = longitude,
    location_accuracy = location_accuracy,
    peak_acceleration = peak_acceleration,
    inactivity_seconds = inactivity_seconds,
    status = status,
    notes = notes,
    client_id = id,
)

fun PendingEvent.toFallDto() = FallDto(
    id = id,
    detected_at = detected_at,
    latitude = latitude,
    longitude = longitude,
    location_accuracy = location_accuracy,
    peak_acceleration = peak_acceleration,
    inactivity_seconds = inactivity_seconds,
    status = status,
    notes = notes,
)

class FallRepository(
    private val api: ApiService,
    private val dao: PendingEventDao,
    private val workManager: WorkManager,
) {
    suspend fun hasPending(): Boolean = dao.unsynced().isNotEmpty()

    suspend fun list(status: String? = null): AppResult<List<FallDto>> =
        callApi { api.getFalls(status = status, page = 1, limit = 100) }

    suspend fun detail(id: String): AppResult<FallDto> = callApi { api.getFall(id) }

    suspend fun create(request: CreateFallRequest): AppResult<FallDto> = callApi { api.createFall(request) }

    suspend fun patch(id: String, status: String, notes: String? = null): AppResult<FallDto> =
        callApi { api.patchFall(id, UpdateFallRequest(status, notes)) }

    suspend fun delete(id: String): AppResult<Unit> = callApiUnit { api.deleteFall(id) }

    fun pending(): Flow<List<PendingEvent>> = dao.observeAll()

    suspend fun queueAndSend(event: PendingEvent): ReportResult {
        dao.upsert(event)
        return try {
            val response = api.createFall(event.toRequest())
            if (response.success && response.data != null) {
                dao.markSynced(event.id)
                ReportResult(response.data, true, "Kejadian terkirim ke server")
            } else {
                scheduleSync()
                ReportResult(null, false, "Tersimpan di perangkat, menunggu terkirim")
            }
        } catch (e: Exception) {
            scheduleSync()
            ReportResult(null, false, "${friendlyMessage(e)} Data tersimpan di perangkat.")
        }
    }

    suspend fun syncAll(): Int {
        var sent = 0
        for (event in dao.unsynced()) {
            try {
                val response = api.createFall(event.toRequest())
                if (response.success) {
                    dao.markSynced(event.id)
                    sent++
                }
            } catch (_: Exception) {
                // Worker akan dijadwalkan ulang dengan backoff oleh WorkManager.
            }
        }
        return sent
    }

    private fun scheduleSync() {
        val request = OneTimeWorkRequestBuilder<FallSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        workManager.enqueueUniqueWork("fall-sync", ExistingWorkPolicy.KEEP, request)
    }
}
