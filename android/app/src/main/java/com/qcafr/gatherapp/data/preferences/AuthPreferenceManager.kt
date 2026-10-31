package com.qcafr.gatherapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.qcafr.gatherapp.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "qcafr_auth")

class AuthPreferenceManager(private val context: Context) {

    companion object {
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USER_ID = longPreferencesKey("user_id")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_FULL_NAME = stringPreferencesKey("full_name")
        private val KEY_ROLE = stringPreferencesKey("role")
        private val KEY_PARTICIPANT_CODE = stringPreferencesKey("participant_code")
        private val KEY_WEREDA_KEBELE = stringPreferencesKey("wereda_kebele")
        private val KEY_GENDER = stringPreferencesKey("gender")
        private val KEY_EXPERIENCE_YEARS = floatPreferencesKey("experience_years")

        @Volatile
        private var INSTANCE: AuthPreferenceManager? = null

        fun getInstance(context: Context): AuthPreferenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthPreferenceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val currentUserFlow: Flow<User?> = context.authDataStore.data.map { prefs ->
        val isLoggedIn = prefs[KEY_IS_LOGGED_IN] ?: false
        if (!isLoggedIn) {
            null
        } else {
            val id = prefs[KEY_USER_ID] ?: 0L
            val username = prefs[KEY_USERNAME] ?: ""
            val fullName = prefs[KEY_FULL_NAME] ?: ""
            val role = prefs[KEY_ROLE] ?: "EXAMINEE"
            val participantCode = prefs[KEY_PARTICIPANT_CODE]
            val weredaKebele = prefs[KEY_WEREDA_KEBELE]
            val gender = prefs[KEY_GENDER]
            val experience = prefs[KEY_EXPERIENCE_YEARS]

            User(
                id = id,
                username = username,
                fullName = fullName,
                role = role,
                participantCode = participantCode,
                weredaKebele = weredaKebele,
                gender = gender,
                experienceYears = experience
            )
        }
    }

    val tokenFlow: Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[KEY_TOKEN]
    }

    suspend fun saveUserSession(user: User, token: String) {
        context.authDataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = true
            prefs[KEY_TOKEN] = token
            prefs[KEY_USER_ID] = user.id
            prefs[KEY_USERNAME] = user.username
            prefs[KEY_FULL_NAME] = user.fullName
            prefs[KEY_ROLE] = user.role
            if (user.participantCode != null) prefs[KEY_PARTICIPANT_CODE] = user.participantCode else prefs.remove(KEY_PARTICIPANT_CODE)
            if (user.weredaKebele != null) prefs[KEY_WEREDA_KEBELE] = user.weredaKebele else prefs.remove(KEY_WEREDA_KEBELE)
            if (user.gender != null) prefs[KEY_GENDER] = user.gender else prefs.remove(KEY_GENDER)
            if (user.experienceYears != null) prefs[KEY_EXPERIENCE_YEARS] = user.experienceYears else prefs.remove(KEY_EXPERIENCE_YEARS)
        }
    }

    suspend fun clearUserSession() {
        context.authDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
