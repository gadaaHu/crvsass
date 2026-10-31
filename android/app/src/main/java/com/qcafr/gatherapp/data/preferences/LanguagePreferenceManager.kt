package com.qcafr.gatherapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists the user's preferred display language.
 * true  → Amharic (default)
 * false → English
 *
 * Re-uses the same "qcafr_settings" DataStore as [ThemePreferenceManager]
 * so no additional file infrastructure is needed.
 */
class LanguagePreferenceManager(private val context: Context) {

    companion object {
        private val IS_AMHARIC_KEY = booleanPreferencesKey("is_amharic")

        @Volatile
        private var INSTANCE: LanguagePreferenceManager? = null

        fun getInstance(context: Context): LanguagePreferenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LanguagePreferenceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /** Emits true = Amharic, false = English. Default is Amharic. */
    val isAmharicFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[IS_AMHARIC_KEY] ?: true }

    suspend fun setAmharic(isAmharic: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_AMHARIC_KEY] = isAmharic
        }
    }
}
