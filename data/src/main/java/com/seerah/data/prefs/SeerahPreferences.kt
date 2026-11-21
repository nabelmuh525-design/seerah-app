package com.seerah.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.seerah.domain.model.UserPreferencesState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.seerahDataStore by preferencesDataStore(name = "seerah_user_prefs")

class SeerahPreferences(private val context: Context) {

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LAST_READ_TITLE = stringPreferencesKey("last_read_title")
        val LAST_READ_ROUTE = stringPreferencesKey("last_read_route")
    }

    val state: Flow<UserPreferencesState> = context.seerahDataStore.data.map { prefs ->
        UserPreferencesState(
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            fontScale = prefs[Keys.FONT_SCALE] ?: 1.0f,
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false,
            lastReadTitle = prefs[Keys.LAST_READ_TITLE] ?: "",
            lastReadRoute = prefs[Keys.LAST_READ_ROUTE] ?: ""
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.seerahDataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setFontScale(scale: Float) {
        context.seerahDataStore.edit { it[Keys.FONT_SCALE] = scale.coerceIn(0.85f, 1.4f) }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.seerahDataStore.edit { it[Keys.ONBOARDING_DONE] = completed }
    }

    suspend fun saveLastRead(title: String, route: String) {
        context.seerahDataStore.edit {
            it[Keys.LAST_READ_TITLE] = title
            it[Keys.LAST_READ_ROUTE] = route
        }
    }
}
