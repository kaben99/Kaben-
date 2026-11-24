package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM analysis_reports ORDER BY createdAtMillis DESC")
    fun observeAllReports(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("DELETE FROM analysis_reports WHERE reportId = :reportId")
    suspend fun deleteReportById(reportId: String)

    @Query("DELETE FROM analysis_reports")
    suspend fun clearAllReports()
}
