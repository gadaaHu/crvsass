package com.qcafr.gatherapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.qcafr.gatherapp.data.local.entity.PendingSubmissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSubmissionDao {
    @Query("SELECT * FROM pending_submissions WHERE isSynced = 0 ORDER BY timestamp ASC")
    fun getUnsyncedSubmissions(): Flow<List<PendingSubmissionEntity>>

    @Query("SELECT * FROM pending_submissions WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedSubmissionsList(): List<PendingSubmissionEntity>

    @Query("SELECT COUNT(*) FROM pending_submissions WHERE isSynced = 0")
    fun getPendingCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: PendingSubmissionEntity): Long

    @Update
    suspend fun updateSubmission(submission: PendingSubmissionEntity)

    @Query("UPDATE pending_submissions SET isSynced = 1 WHERE localId = :localId")
    suspend fun markAsSynced(localId: Long)

    @Query("DELETE FROM pending_submissions WHERE isSynced = 1")
    suspend fun clearSynced()
}
