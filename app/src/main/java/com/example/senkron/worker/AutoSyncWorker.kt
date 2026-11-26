package com.example.senkron.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.senkron.data.drive.GoogleDriveManager
import com.example.senkron.data.repository.FileFilterHelper
import java.util.concurrent.TimeUnit

class AutoSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val driveManager = GoogleDriveManager(applicationContext)
        val signedInAccount = driveManager.getSignedInAccount()

        if (signedInAccount == null) {
            // Kullanıcı giriş yapmamışsa çalışmayı sonlandır
            return Result.failure()
        }

        try {
            // Fotoğraflar hariç tüm dosyaları tıkla/tara
            val syncableFiles = FileFilterHelper.getSyncableFilesFromMediaStore(applicationContext)
                .filter { !FileFilterHelper.isPhoto(it.name, it.mimeType) }

            var successCount = 0
            for (file in syncableFiles) {
                val driveFileId = driveManager.uploadFile(signedInAccount, file)
                if (driveFileId != null) {
                    successCount++
                }
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "auto_cloud_sync_worker"

        fun schedulePeriodicSync(
            context: Context,
            requiresWifi: Boolean = true
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (requiresWifi) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncWorkRequest = PeriodicWorkRequestBuilder<AutoSyncWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
            )
        }

        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
