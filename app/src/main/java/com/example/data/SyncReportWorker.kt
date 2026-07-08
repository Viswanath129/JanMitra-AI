package com.example.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.network.NetworkModule
import kotlinx.coroutines.flow.firstOrNull

class SyncReportWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SyncReportWorker", "Background offline report processing started...")
        val database = AppDatabase.getDatabase(applicationContext)
        val reportDao = database.citizenReportDao()
        
        val reports = reportDao.getAllReports().firstOrNull()
        if (reports.isNullOrEmpty()) {
            Log.d("SyncReportWorker", "No reports found in local cache to process.")
            return Result.success()
        }

        val aiRepository = NetworkModule.aiRepository
        var hasFailure = false
        var hasRecoverableFailure = false

        for (rawReport in reports) {
            val report = com.example.data.repository.JanMitraRepository.decryptReport(rawReport)
            if (report.status == "PendingSubmission") {
                try {
                    // Perform background AI Analysis for the submitted report
                    val result = aiRepository.analyzeReport(
                        category = report.category,
                        description = report.description,
                        villageName = report.locationName,
                        isVoiceRecorded = !report.voiceFilePath.isNullOrEmpty(),
                        isPhotoAttached = !report.imageUri.isNullOrEmpty()
                    )
                    
                    val updatedReport = report.copy(
                        status = "Reported",
                        urgency = result.urgency,
                        aiSummary = result.summary
                    )
                    val encryptedReport = com.example.data.repository.JanMitraRepository.encryptReport(updatedReport)
                    reportDao.updateReport(encryptedReport)
                    Log.d("SyncReportWorker", "Successfully processed pending report ${report.issueId} in background")
                } catch (e: Exception) {
                    Log.e("SyncReportWorker", "Failed to process report ${report.issueId}: ${e.message}")
                    hasFailure = true
                    if (e is java.io.IOException || e is retrofit2.HttpException || e.javaClass.name.contains("Timeout") || e.javaClass.name.contains("Connect")) {
                        hasRecoverableFailure = true
                    }
                }
            }
        }

        return when {
            hasRecoverableFailure -> {
                Log.w("SyncReportWorker", "Recoverable failure detected, requesting retry from WorkManager.")
                Result.retry()
            }
            hasFailure -> {
                Log.e("SyncReportWorker", "Unrecoverable failure detected, marking task as failure.")
                Result.failure()
            }
            else -> {
                Log.d("SyncReportWorker", "All pending reports processed successfully.")
                Result.success()
            }
        }
    }
}
