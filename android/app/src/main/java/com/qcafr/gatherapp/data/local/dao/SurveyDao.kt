package com.qcafr.gatherapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.qcafr.gatherapp.data.local.entity.LocalSurveyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyDao {
    @Query("SELECT * FROM surveys ORDER BY id ASC")
    fun getAllSurveys(): Flow<List<LocalSurveyEntity>>

    @Query("SELECT * FROM surveys WHERE id = :id LIMIT 1")
    suspend fun getSurveyById(id: Long): LocalSurveyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurvey(survey: LocalSurveyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurveys(surveys: List<LocalSurveyEntity>)

    @Query("DELETE FROM surveys")
    suspend fun clearAll()
}
