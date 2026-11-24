package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analysis_reports")
data class ReportEntity(
    @PrimaryKey val reportId: String,
    val createdAtMillis: Long,
    val timestampFormatted: String,
    val jurisdictionId: String,
    val usageModeId: String,
    val executiveSummary: String,
    val overallConfidence: String,
    val plateNumberSummary: String,
    val estimatedCostRange: String,
    val payloadJson: String
)
