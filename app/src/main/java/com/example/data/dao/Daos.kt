package com.example.data.dao

import androidx.room.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CitizenReportDao {
    @Query("SELECT * FROM citizen_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<CitizenReport>>

    @Query("SELECT * FROM citizen_reports WHERE id = :id LIMIT 1")
    suspend fun getReportById(id: Int): CitizenReport?

    @Query("SELECT * FROM citizen_reports WHERE category = :category ORDER BY timestamp DESC")
    fun getReportsByCategory(category: String): Flow<List<CitizenReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: CitizenReport): Long

    @Update
    suspend fun updateReport(report: CitizenReport)

    @Query("DELETE FROM citizen_reports WHERE id = :id")
    suspend fun deleteReportById(id: Int)

    @Query("DELETE FROM citizen_reports")
    suspend fun clearAllReports()
}

@Dao
interface InfrastructureAssetDao {
    @Query("SELECT * FROM infrastructure_assets ORDER BY name ASC")
    fun getAllAssets(): Flow<List<InfrastructureAsset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: InfrastructureAsset): Long

    @Query("DELETE FROM infrastructure_assets")
    suspend fun clearAllAssets()
}

@Dao
interface VillageStatisticsDao {
    @Query("SELECT * FROM village_statistics ORDER BY villageName ASC")
    fun getAllVillageStats(): Flow<List<VillageStatistics>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVillageStat(stat: VillageStatistics): Long

    @Query("DELETE FROM village_statistics")
    suspend fun clearAllVillageStats()
}

@Dao
interface AiChatMessageDao {
    @Query("SELECT * FROM ai_chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<AiChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiChatMessage): Long

    @Query("DELETE FROM ai_chat_messages")
    suspend fun clearAllMessages()
}
