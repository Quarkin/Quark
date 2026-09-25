package com.android.launcher3

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// This creates the physical file "launcher_settings.preferences_pb" on the device
val Context.dataStore by preferencesDataStore(name = "launcher_settings")

class LauncherPrefs(private val context: Context) {
    companion object {
        // Define a permanent key for your icon pack setting
        val ICON_PACK = stringPreferencesKey("icon_pack")
    }
    
    // Constantly streams the saved value to your UI
    val iconPackFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ICON_PACK] ?: "" // Default value if nothing is saved yet
    }
    
    // Writes the new value to the physical disk
    suspend fun saveIconPack(packageName: String) {
        context.dataStore.edit { preferences ->
            preferences[ICON_PACK] = packageName
        }
    }
}
