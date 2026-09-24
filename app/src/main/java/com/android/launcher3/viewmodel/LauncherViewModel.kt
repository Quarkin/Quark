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
import com.android.launcher3.model.AppItem
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

    val filteredApps: StateFlow<List<AppItem>> = combine(_allApps, _searchQuery) { apps, query ->
        if (query.isBlank()) {
            apps
        } else {
            apps.filter {
                it.label.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadApps()
        }
    }

    init {
        registerPackageReceiver()
        loadApps()
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
        if (!open) {
            _searchQuery.value = ""
        }
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

        // Pinned apps
        val pinnedKeys = prefs.getStringSet("pinned_keys", null) ?: emptySet()
        _pinnedApps.value = pinnedKeys.mapNotNull { appMap[it] }

        // Dock apps (5 slots)
        val savedDockKeys = prefs.getString("dock_keys", null)?.split(";")?.filter { it.isNotBlank() }
        val dockList = mutableListOf<AppItem>()

        if (!savedDockKeys.isNullOrEmpty()) {
            savedDockKeys.forEach { key ->
                appMap[key]?.let { dockList.add(it) }
            }
        }

        // If dock has fewer than 5 apps, pick intelligent defaults
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

            // Still under 5? Fill with remaining apps
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
        if (!current.any { it.componentKey == app.componentKey }) {
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
            getApplication<Application>().unregisterReceiver(packageReceiver)
        } catch (ignored: Exception) {
        }
    }
}
