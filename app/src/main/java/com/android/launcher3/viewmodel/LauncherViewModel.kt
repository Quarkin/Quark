package com.android.launcher3.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.launcher3.iconpack.IconPackInfo
import com.android.launcher3.iconpack.IconPackManager
import com.android.launcher3.model.AppItem
import com.android.launcher3.repository.AppOverride
import com.android.launcher3.repository.AppOverridesRepository
import com.android.launcher3.util.IconThemer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences("quark_launcher_prefs", Context.MODE_PRIVATE)

    private val packageManager: PackageManager = application.packageManager

    val overridesRepository = AppOverridesRepository(application)
    val iconPackManager = IconPackManager(application)

    val overrides: StateFlow<Map<String, AppOverride>> = overridesRepository.overrides

    private val _installedIconPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    val installedIconPacks: StateFlow<List<IconPackInfo>> = _installedIconPacks

    private val _globalIconPack = MutableStateFlow<String?>(prefs.getString("global_icon_pack", null))
    val globalIconPack: StateFlow<String?> = _globalIconPack

    private val _globalAppFilter = MutableStateFlow<Map<String, String>>(emptyMap())
    val globalAppFilter: StateFlow<Map<String, String>> = _globalAppFilter

    private val _allApps = MutableStateFlow<List<AppItem>>(emptyList())
    val allApps: StateFlow<List<AppItem>> = _allApps

    private val _pinnedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val pinnedApps: StateFlow<List<AppItem>> = _pinnedApps

    private val _dockApps = MutableStateFlow<List<AppItem>>(emptyList())
    val dockApps: StateFlow<List<AppItem>> = _dockApps

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen

    private val _isThemedIcons = MutableStateFlow(prefs.getBoolean("themed_icons", true))
    val isThemedIcons: StateFlow<Boolean> = _isThemedIcons

    private val _isDoubleTapToSleep = MutableStateFlow(prefs.getBoolean("double_tap_to_sleep", true))
    val isDoubleTapToSleep: StateFlow<Boolean> = _isDoubleTapToSleep

    private val _isShowDockSearch = MutableStateFlow(prefs.getBoolean("show_dock_search", true))
    val isShowDockSearch: StateFlow<Boolean> = _isShowDockSearch

    // Predictive back gesture states (0f..1f)
    private val _backProgress = MutableStateFlow(0f)
    val backProgress: StateFlow<Float> = _backProgress

    val filteredApps: StateFlow<List<AppItem>> = combine(_allApps, _searchQuery, overrides) { apps, query, appOverrides ->
        if (query.isBlank()) {
            apps
        } else {
            apps.filter { app ->
                val customLabel = appOverrides[app.componentKey]?.customLabel
                val effectiveLabel = if (!customLabel.isNullOrBlank()) customLabel else app.label
                effectiveLabel.contains(query, ignoreCase = true) ||
                        app.label.contains(query, ignoreCase = true) ||
                        app.packageName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadApps()
            refreshInstalledIconPacks()
        }
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
        when (key) {
            "themed_icons" -> {
                _isThemedIcons.value = sp.getBoolean("themed_icons", true)
                IconThemer.clearCache()
            }
            "double_tap_to_sleep" -> {
                _isDoubleTapToSleep.value = sp.getBoolean("double_tap_to_sleep", true)
            }
            "show_dock_search" -> {
                _isShowDockSearch.value = sp.getBoolean("show_dock_search", true)
            }
            "global_icon_pack" -> {
                val pack = sp.getString("global_icon_pack", null)
                _globalIconPack.value = pack
                loadGlobalAppFilter(pack)
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        registerPackageReceiver()
        loadApps()
        refreshInstalledIconPacks()
        loadGlobalAppFilter(_globalIconPack.value)
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplication<Application>().registerReceiver(
                packageReceiver,
                filter,
                Context.RECEIVER_EXPORTED
            )
        } else {
            getApplication<Application>().registerReceiver(packageReceiver, filter)
        }
    }

    fun setDrawerOpen(open: Boolean) {
        _isDrawerOpen.value = open
        _backProgress.value = 0f
        if (!open) {
            _searchQuery.value = ""
        }
    }

    fun updateBackProgress(progress: Float) {
        _backProgress.value = progress.coerceIn(0f, 1f)
    }

    fun toggleThemedIcons() {
        val newValue = !_isThemedIcons.value
        _isThemedIcons.value = newValue
        prefs.edit().putBoolean("themed_icons", newValue).apply()
        IconThemer.clearCache()
    }

    fun toggleDoubleTapToSleep() {
        val newValue = !_isDoubleTapToSleep.value
        _isDoubleTapToSleep.value = newValue
        prefs.edit().putBoolean("double_tap_to_sleep", newValue).apply()
    }

    fun toggleShowDockSearch() {
        val newValue = !_isShowDockSearch.value
        _isShowDockSearch.value = newValue
        prefs.edit().putBoolean("show_dock_search", newValue).apply()
    }

    fun setGlobalIconPack(packageName: String?) {
        _globalIconPack.value = packageName
        if (packageName.isNullOrBlank()) {
            prefs.edit().remove("global_icon_pack").apply()
        } else {
            prefs.edit().putString("global_icon_pack", packageName).apply()
        }
        loadGlobalAppFilter(packageName)
    }

    private fun loadGlobalAppFilter(packageName: String?) {
        viewModelScope.launch {
            if (packageName.isNullOrBlank()) {
                _globalAppFilter.value = emptyMap()
            } else {
                _globalAppFilter.value = iconPackManager.getAppFilter(packageName)
            }
            IconThemer.clearCache()
        }
    }

    fun refreshInstalledIconPacks() {
        viewModelScope.launch(Dispatchers.IO) {
            val packs = iconPackManager.getInstalledIconPacks()
            _installedIconPacks.value = packs
        }
    }

    suspend fun getDrawablesForIconPack(packageName: String): List<String> {
        return iconPackManager.getAvailableDrawables(packageName)
    }

    fun saveAppOverride(
        componentKey: String,
        customLabel: String?,
        customIconPack: String?,
        customDrawableName: String?
    ) {
        val override = AppOverride(
            customLabel = customLabel?.trim()?.ifBlank { null },
            customIconPackPackage = customIconPack?.trim()?.ifBlank { null },
            customIconDrawableName = customDrawableName?.trim()?.ifBlank { null }
        )
        overridesRepository.setOverride(componentKey, override)
        IconThemer.clearCache()
    }

    fun resetAppOverride(componentKey: String) {
        overridesRepository.removeOverride(componentKey)
        IconThemer.clearCache()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveList = packageManager.queryIntentActivities(intent, 0)
                resolveList.mapNotNull { resolveInfo ->
                    try {
                        val label = resolveInfo.loadLabel(packageManager).toString()
                        val packageName = resolveInfo.activityInfo.packageName
                        val activityName = resolveInfo.activityInfo.name
                        val icon = resolveInfo.loadIcon(packageManager)
                        AppItem(label, packageName, activityName, icon)
                    } catch (e: Exception) {
                        null
                    }
                }.sortedBy { it.label.lowercase() }
            }

            _allApps.value = apps
            refreshPinnedAndDock(apps)
        }
    }

    private fun refreshPinnedAndDock(apps: List<AppItem>) {
        val appMap = apps.associateBy { it.componentKey }

        // Pinned apps on 5x5 grid (up to 25 items)
        val savedPinnedKeys = prefs.getStringSet("pinned_keys", null)
        val pinnedList = mutableListOf<AppItem>()

        if (savedPinnedKeys != null) {
            savedPinnedKeys.mapNotNull { appMap[it] }.forEach { pinnedList.add(it) }
        } else {
            val sampleApps = apps.take(10)
            pinnedList.addAll(sampleApps)
            prefs.edit().putStringSet("pinned_keys", sampleApps.map { it.componentKey }.toSet()).apply()
        }
        _pinnedApps.value = pinnedList.take(25)

        // Dock apps (5 slots)
        val savedDockKeys = prefs.getString("dock_keys", null)?.split(";")?.filter { it.isNotBlank() }
        val dockList = mutableListOf<AppItem>()

        if (!savedDockKeys.isNullOrEmpty()) {
            savedDockKeys.forEach { key ->
                appMap[key]?.let { dockList.add(it) }
            }
        }

        if (dockList.size < 5 && apps.isNotEmpty()) {
            val preferredCategories = listOf(
                listOf("dialer", "phone"),
                listOf("messaging", "mms", "sms", "chat"),
                listOf("browser", "chrome"),
                listOf("camera"),
                listOf("settings", "contact")
            )

            val usedKeys = dockList.map { it.componentKey }.toMutableSet()

            for (keywords in preferredCategories) {
                if (dockList.size >= 5) break
                val found = apps.firstOrNull { app ->
                    !usedKeys.contains(app.componentKey) &&
                            keywords.any { kw ->
                                app.packageName.contains(kw, ignoreCase = true) ||
                                        app.label.contains(kw, ignoreCase = true)
                            }
                }
                if (found != null) {
                    dockList.add(found)
                    usedKeys.add(found.componentKey)
                }
            }

            for (app in apps) {
                if (dockList.size >= 5) break
                if (!usedKeys.contains(app.componentKey)) {
                    dockList.add(app)
                    usedKeys.add(app.componentKey)
                }
            }
        }

        _dockApps.value = dockList.take(5)
    }

    fun pinApp(app: AppItem) {
        val current = _pinnedApps.value.toMutableList()
        if (current.size < 25 && !current.any { it.componentKey == app.componentKey }) {
            current.add(app)
            _pinnedApps.value = current
            val keys = current.map { it.componentKey }.toSet()
            prefs.edit().putStringSet("pinned_keys", keys).apply()
        }
    }

    fun unpinApp(app: AppItem) {
        val current = _pinnedApps.value.filter { it.componentKey != app.componentKey }
        _pinnedApps.value = current
        val keys = current.map { it.componentKey }.toSet()
        prefs.edit().putStringSet("pinned_keys", keys).apply()
    }

    fun setDockSlot(slotIndex: Int, app: AppItem) {
        val current = _dockApps.value.toMutableList()
        while (current.size <= slotIndex) {
            current.add(app)
        }
        current[slotIndex] = app
        val newDock = current.take(5)
        _dockApps.value = newDock
        prefs.edit().putString("dock_keys", newDock.joinToString(";") { it.componentKey }).apply()
    }

    fun launchApp(context: Context, app: AppItem) {
        try {
            context.startActivity(app.launchIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openAppInfo(context: Context, app: AppItem) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", app.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWallpaperPicker(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_SET_WALLPAPER)
            context.startActivity(Intent.createChooser(intent, "Set wallpaper"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open wallpaper chooser", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        } catch (ignored: Exception) {
        }
        try {
            getApplication<Application>().unregisterReceiver(packageReceiver)
        } catch (ignored: Exception) {
        }
        IconThemer.clearCache()
    }
}
