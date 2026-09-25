package com.android.launcher3

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "launcher_settings")

class LauncherPrefs(private val context: Context) {
    companion object {
        val ICON_PACK = stringPreferencesKey("icon_pack")
        val WEATHER_TEMP = stringPreferencesKey("weather_temp")
        val LAST_FETCH = longPreferencesKey("last_fetch")
    }
    
    val iconPackFlow: Flow<String> = context.dataStore.data.map { it[ICON_PACK] ?: "" }
    val weatherTempFlow: Flow<String> = context.dataStore.data.map { it[WEATHER_TEMP] ?: "..." }
    val lastFetchFlow: Flow<Long> = context.dataStore.data.map { it[LAST_FETCH] ?: 0L }
    
    suspend fun saveIconPack(packageName: String) {
        context.dataStore.edit { prefs ->
            prefs[ICON_PACK] = packageName
        }
    }

    suspend fun saveWeather(temp: String, timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[WEATHER_TEMP] = temp
            prefs[LAST_FETCH] = timestamp
        }
    }
}
