package com.qcafr.gatherapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.qcafr.gatherapp.data.local.entity.CompletedSubmissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletedSubmissionDao {

    @Query("SELECT * FROM completed_submissions ORDER BY id DESC")
    fun getAllFlow(): Flow<List<CompletedSubmissionEntity>>

    @Query("SELECT * FROM completed_submissions ORDER BY id DESC")
    suspend fun getAll(): List<CompletedSubmissionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<CompletedSubmissionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CompletedSubmissionEntity)

    @Query("DELETE FROM completed_submissions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM completed_submissions")
    suspend fun clearAll()
}
