package com.qcafr.gatherapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surveys")
data class LocalSurveyEntity(
    @PrimaryKey val id: Long,
    val code: String,
    val titleAm: String,
    val titleEn: String,
    val descriptionAm: String?,
    val descriptionEn: String?,
    val totalPoints: Int,
    val passingScore: Int,
    val rawJson: String
)

@Entity(tableName = "pending_submissions")
data class PendingSubmissionEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val surveyId: Long,
    val participantCode: String,
    val testType: String,
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val syncAttempts: Int = 0,
    val lastError: String? = null
)

@Entity(tableName = "completed_submissions")
data class CompletedSubmissionEntity(
    @PrimaryKey val id: Long,
    val participantCode: String,
    val weredaKebele: String?,
    val role: String?,
    val testType: String,
    val totalScore: Float,
    val maxScore: Float,
    val scorePercentage: Float,
    val performanceBand: String,
    val submittedAt: String
)

