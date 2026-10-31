package com.qcafr.gatherapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "qcafr_settings")

class ThemePreferenceManager(private val context: Context) {

    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("is_dark_mode")

        @Volatile
        private var INSTANCE: ThemePreferenceManager? = null

        fun getInstance(context: Context): ThemePreferenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemePreferenceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /** Emits true = dark, false = light */
    val isDarkModeFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[DARK_MODE_KEY] ?: true } // default dark

    suspend fun setDarkMode(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = isDark
        }
    }
}
