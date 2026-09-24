package com.android.launcher3

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.launcher3.iconpack.IconPackInfo
import com.android.launcher3.iconpack.IconPackManager
import com.android.launcher3.service.QuarkAccessibilityService
import com.android.launcher3.ui.theme.QuarkLauncherTheme
import com.android.launcher3.util.IconThemer

class QuarkSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("quark_launcher_prefs", Context.MODE_PRIVATE)

        setContent {
            QuarkLauncherTheme {
                SettingsScreen(
                    prefs = prefs,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: SharedPreferences,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val iconPackManager = remember { IconPackManager(context) }

    var doubleTapToSleep by remember {
        mutableStateOf(prefs.getBoolean("double_tap_to_sleep", true))
    }
    var themedIcons by remember {
        mutableStateOf(prefs.getBoolean("themed_icons", true))
    }
    var showDockSearch by remember {
        mutableStateOf(prefs.getBoolean("show_dock_search", true))
    }
    var globalIconPack by remember {
        mutableStateOf(prefs.getString("global_icon_pack", null))
    }

    var installedPacks by remember { mutableStateOf<List<IconPackInfo>>(emptyList()) }
    var showIconPackDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        installedPacks = iconPackManager.getInstalledIconPacks()
    }

    val selectedPackName = remember(globalIconPack, installedPacks) {
        if (globalIconPack.isNullOrBlank()) {
            "Default (System)"
        } else {
            installedPacks.find { it.packageName == globalIconPack }?.label ?: globalIconPack ?: "Default (System)"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.home_settings_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Gestures & Interaction",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Double Tap to Sleep
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.double_tap_to_sleep_title))
                },
                supportingContent = {
                    Column {
                        Text(stringResource(R.string.double_tap_to_sleep_desc))
                        if (doubleTapToSleep && !QuarkAccessibilityService.isServiceEnabled()) {
                            Text(
                                text = "Requires Accessibility service to lock screen. Tap to enable.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable {
                                        try {
                                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (ignored: Exception) {
                                        }
                                    }
                            )
                        }
                    }
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Switch(
                        checked = doubleTapToSleep,
                        onCheckedChange = { isChecked ->
                            doubleTapToSleep = isChecked
                            prefs.edit().putBoolean("double_tap_to_sleep", isChecked).apply()
                            if (isChecked && !QuarkAccessibilityService.isServiceEnabled()) {
                                QuarkAccessibilityService.lockScreen(context)
                            }
                        },
                        modifier = Modifier.testTag("switch_double_tap_to_sleep")
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Appearance & Icon Packs",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Global Icon Pack Selector Dropdown / Dialog
            ListItem(
                headlineContent = { Text("Icon Pack") },
                supportingContent = { Text(selectedPackName) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showIconPackDialog = true }
                    .testTag("item_global_icon_pack")
            )

            // Themed Icons Switch
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.themed_icons_title))
                },
                supportingContent = {
                    Text(stringResource(R.string.themed_icons_desc))
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Switch(
                        checked = themedIcons,
                        onCheckedChange = { isChecked ->
                            themedIcons = isChecked
                            prefs.edit().putBoolean("themed_icons", isChecked).apply()
                        },
                        modifier = Modifier.testTag("switch_themed_icons")
                    )
                }
            )

            // Search Bar in Dock Switch
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.search_bar_dock_title))
                },
                supportingContent = {
                    Text(stringResource(R.string.search_bar_dock_desc))
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Switch(
                        checked = showDockSearch,
                        onCheckedChange = { isChecked ->
                            showDockSearch = isChecked
                            prefs.edit().putBoolean("show_dock_search", isChecked).apply()
                        },
                        modifier = Modifier.testTag("switch_show_dock_search")
                    )
                }
            )

            // Wallpaper & Style
            ListItem(
                headlineContent = { Text("Wallpaper & style") },
                supportingContent = { Text("Change wallpaper and accent colors") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Rounded.Wallpaper,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                            context.startActivity(Intent.createChooser(intent, "Set wallpaper"))
                        } catch (e: Exception) {
                        }
                    }
                    .testTag("item_wallpaper_style")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quark Launcher",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pixel 6a Edition (Android 16 / API 36)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Clean, fast, and customizable launcher with Double Tap to Sleep, Predictive Back, 3rd-party Icon Packs, and Material You dynamic theming.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Global Icon Pack Selection Dialog
    if (showIconPackDialog) {
        AlertDialog(
            onDismissRequest = { showIconPackDialog = false },
            title = { Text("Select Icon Pack") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    // Default / System option
                    ListItem(
                        headlineContent = { Text("Default (System)") },
                        trailingContent = {
                            if (globalIconPack.isNullOrBlank()) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                globalIconPack = null
                                prefs.edit().remove("global_icon_pack").apply()
                                IconThemer.clearCache()
                                showIconPackDialog = false
                            }
                    )

                    // Installed 3rd-party icon packs
                    for (pack in installedPacks) {
                        val isSelected = pack.packageName == globalIconPack
                        ListItem(
                            headlineContent = { Text(pack.label) },
                            supportingContent = { Text(pack.packageName) },
                            trailingContent = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    globalIconPack = pack.packageName
                                    prefs.edit().putString("global_icon_pack", pack.packageName).apply()
                                    IconThemer.clearCache()
                                    showIconPackDialog = false
                                }
                        )
                    }

                    if (installedPacks.isEmpty()) {
                        Text(
                            text = "No third-party icon packs installed. You can download icon packs from Google Play.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIconPackDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
