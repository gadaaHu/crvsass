package com.qcafr.gatherapp

import android.app.Application
import com.qcafr.gatherapp.data.local.AppDatabase
import com.qcafr.gatherapp.data.repository.SurveyRepository

class QcafrApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: SurveyRepository by lazy { SurveyRepository(database) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: QcafrApplication
            private set
    }
}
