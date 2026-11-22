package com.example.falldetection.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.falldetection.FallDetectionApp

class FallSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = (applicationContext as FallDetectionApp).container.fallRepository
        if (!repository.hasPending()) return Result.success()
        val sent = repository.syncAll()
        return if (sent > 0 || !repository.hasPending()) Result.success() else Result.retry()
    }
}
