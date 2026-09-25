package com.android.launcher3.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

@Immutable
data class AppOverride(
    val customLabel: String? = null,
    val customIconPackPackage: String? = null,
    val customIconDrawableName: String? = null
) {
    val isEmpty: Boolean
        get() = customLabel.isNullOrBlank() && customIconPackPackage.isNullOrBlank() && customIconDrawableName.isNullOrBlank()

    fun toJson(): String {
        val obj = JSONObject()
        customLabel?.let { obj.put("label", it) }
        customIconPackPackage?.let { obj.put("pack", it) }
        customIconDrawableName?.let { obj.put("drawable", it) }
        return obj.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): AppOverride? {
            return try {
                val obj = JSONObject(jsonStr)
                AppOverride(
                    customLabel = if (obj.has("label")) obj.getString("label") else null,
                    customIconPackPackage = if (obj.has("pack")) obj.getString("pack") else null,
                    customIconDrawableName = if (obj.has("drawable")) obj.getString("drawable") else null
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

class AppOverridesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("quark_app_overrides", Context.MODE_PRIVATE)

    private val _overrides = MutableStateFlow<Map<String, AppOverride>>(emptyMap())
    val overrides: StateFlow<Map<String, AppOverride>> = _overrides.asStateFlow()

    init {
        loadAll()
    }

    private fun loadAll() {
        val map = mutableMapOf<String, AppOverride>()
        prefs.all.forEach { (key, value) ->
            if (value is String) {
                AppOverride.fromJson(value)?.let { map[key] = it }
            }
        }
        _overrides.value = map
    }

    fun getOverride(componentKey: String): AppOverride? {
        return _overrides.value[componentKey]
    }

    fun setOverride(componentKey: String, override: AppOverride) {
        if (override.isEmpty) {
            removeOverride(componentKey)
            return
        }
        prefs.edit().putString(componentKey, override.toJson()).apply()
        val updated = _overrides.value.toMutableMap()
        updated[componentKey] = override
        _overrides.value = updated
    }

    fun removeOverride(componentKey: String) {
        prefs.edit().remove(componentKey).apply()
        val updated = _overrides.value.toMutableMap()
        updated.remove(componentKey)
        _overrides.value = updated
    }
}
