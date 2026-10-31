package com.qcafr.gatherapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.qcafr.gatherapp.data.local.dao.CompletedSubmissionDao
import com.qcafr.gatherapp.data.local.dao.PendingSubmissionDao
import com.qcafr.gatherapp.data.local.dao.SurveyDao
import com.qcafr.gatherapp.data.local.entity.CompletedSubmissionEntity
import com.qcafr.gatherapp.data.local.entity.LocalSurveyEntity
import com.qcafr.gatherapp.data.local.entity.PendingSubmissionEntity

@Database(
    entities = [LocalSurveyEntity::class, PendingSubmissionEntity::class, CompletedSubmissionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun surveyDao(): SurveyDao
    abstract fun pendingSubmissionDao(): PendingSubmissionDao
    abstract fun completedSubmissionDao(): CompletedSubmissionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "crvs_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
