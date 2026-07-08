package com.example.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.network.NetworkModule
import com.example.data.network.NetworkReportCreate
import kotlinx.coroutines.flow.firstOrNull

class SyncReportWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SyncReportWorker", "Background offline report sync started...")
        val database = AppDatabase.getDatabase(applicationContext)
        val reportDao = database.citizenReportDao()
        
        val reports = reportDao.getAllReports().firstOrNull()
        if (reports.isNullOrEmpty()) {
            Log.d("SyncReportWorker", "No reports found in local cache to sync.")
            return Result.success()
        }

        var anyFailure = false
        val apiService = NetworkModule.backendApiService

        for (report in reports) {
            // Let's only attempt to sync local drafts or newly reported citizen reports
            if (report.status == "Reported" || report.status == "Draft") {
                try {
                    val networkReport = NetworkReportCreate(
                        issueId = report.issueId,
                        category = report.category,
                        description = report.description,
                        voiceFilePath = report.voiceFilePath,
                        imageUri = report.imageUri,
                        locationLatitude = report.locationLatitude,
                        locationLongitude = report.locationLongitude,
                        locationName = report.locationName,
                        urgency = report.urgency,
                        timestamp = report.timestamp,
                        priorityScore = report.priorityScore,
                        aiSummary = report.aiSummary,
                        evidenceStrength = report.evidenceStrength,
                        citizenSentiment = report.citizenSentiment,
                        explanationText = report.explanationText
                    )
                    
                    val response = apiService.submitReport(networkReport)
                    Log.d("SyncReportWorker", "Successfully synced report ${report.issueId}: $response")
                    
                    // Mark as synchronized locally by updating its status to "Synced" or keep current status
                    if (report.status == "Draft") {
                        reportDao.updateReport(report.copy(status = "Reported"))
                    }
                } catch (e: Exception) {
                    Log.e("SyncReportWorker", "Failed to sync report ${report.issueId}: ${e.message}")
                    anyFailure = true
                }
            }
        }

        return if (anyFailure) Result.retry() else Result.success()
    }
}
