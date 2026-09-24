@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.android.launcher3.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.launcher3.QuarkSettingsActivity
import com.android.launcher3.iconpack.IconPackManager
import com.android.launcher3.model.AppItem
import com.android.launcher3.repository.AppOverride
import com.android.launcher3.service.QuarkAccessibilityService
import com.android.launcher3.ui.components.AppDrawerView
import com.android.launcher3.ui.components.AtAGlanceWidget
import com.android.launcher3.ui.components.DockView
import com.android.launcher3.ui.components.EditAppDialog
import com.android.launcher3.util.IconThemer
import com.android.launcher3.util.StatusBarHelper
import com.android.launcher3.viewmodel.LauncherViewModel
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val safeDrawing = WindowInsets.safeDrawing.asPaddingValues()

    val allApps by viewModel.allApps.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val pinnedApps by viewModel.pinnedApps.collectAsState()
    val dockApps by viewModel.dockApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()
    val isThemedIcons by viewModel.isThemedIcons.collectAsState()
    val isDoubleTapToSleep by viewModel.isDoubleTapToSleep.collectAsState()
    val isShowDockSearch by viewModel.isShowDockSearch.collectAsState()
    val backProgress by viewModel.backProgress.collectAsState()

    val overrides by viewModel.overrides.collectAsState()
    val globalIconPack by viewModel.globalIconPack.collectAsState()
    val globalAppFilter by viewModel.globalAppFilter.collectAsState()
    val installedIconPacks by viewModel.installedIconPacks.collectAsState()

    var appToEdit by remember { mutableStateOf<AppItem?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Android 16 Predictive Back Gesture Handler:
    // Fluidly scales and shrinks the app drawer down as user swipes back to reveal home screen
    PredictiveBackHandler(enabled = isDrawerOpen) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                viewModel.updateBackProgress(backEvent.progress)
            }
            viewModel.setDrawerOpen(false)
        } catch (e: CancellationException) {
            viewModel.updateBackProgress(0f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isDrawerOpen) {
                if (!isDrawerOpen) {
                    var accumulatedDrag = 0f
                    detectVerticalDragGestures(
                        onDragStart = { accumulatedDrag = 0f },
                        onDragEnd = {
                            if (accumulatedDrag < -40f) {
                                // Swiping up from anywhere on wallpaper or dock smoothly triggers app drawer
                                viewModel.setDrawerOpen(true)
                            } else if (accumulatedDrag > 40f) {
                                // Swiping down anywhere pulls down system notification shade
                                if (!QuarkAccessibilityService.expandNotifications()) {
                                    StatusBarHelper.expandNotificationShade(context)
                                }
                            }
                            accumulatedDrag = 0f
                        },
                        onDragCancel = { accumulatedDrag = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedDrag += dragAmount
                        }
                    )
                }
            }
            .pointerInput(isDoubleTapToSleep, isDrawerOpen) {
                if (!isDrawerOpen) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (isDoubleTapToSleep) {
                                QuarkAccessibilityService.lockScreen(context)
                            }
                        },
                        onLongPress = {
                            val intent = Intent(context, QuarkSettingsActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                }
            }
            .testTag("home_workspace")
    ) {
        // Main Workspace Area (Fixed 60Hz rendering, 20:9 Pixel 6a proportions)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = safeDrawing.calculateTopPadding(),
                    bottom = safeDrawing.calculateBottomPadding()
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Absolute Top: 'At a Glance' Date and Weather Widget
            AtAGlanceWidget(
                modifier = Modifier.padding(top = 4.dp)
            )

            // Center Section: Rigid 5x5 Home Screen Icon Grid
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("workspace_5x5_grid"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    userScrollEnabled = false // Rigid 5x5 single-page grid
                ) {
                    items(pinnedApps, key = { it.componentKey }) { app ->
                        PixelGridIconItem(
                            app = app,
                            isThemed = isThemedIcons,
                            override = overrides[app.componentKey],
                            globalIconPackPackage = globalIconPack,
                            globalAppFilter = globalAppFilter,
                            iconPackManager = viewModel.iconPackManager,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            tintColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = { viewModel.launchApp(context, app) },
                            onCustomize = { appToEdit = app },
                            onRemoveFromHome = { viewModel.unpinApp(app) }
                        )
                    }
                }
            }

            // Bottom Section: 5-Column Hotseat Dock + Taller Persistent Search Bar
            DockView(
                dockApps = dockApps,
                isThemedIcons = isThemedIcons,
                showDockSearch = isShowDockSearch,
                overrides = overrides,
                globalIconPackPackage = globalIconPack,
                globalAppFilter = globalAppFilter,
                iconPackManager = viewModel.iconPackManager,
                onLaunchApp = { app -> viewModel.launchApp(context, app) },
                onCustomizeApp = { app -> appToEdit = app },
                onSearchBarClick = {
                    viewModel.setDrawerOpen(true)
                }
            )
        }

        // Full-bleed edge-to-edge sliding App Drawer with Material 3 translucent background
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            ) + fadeOut()
        ) {
            val pinnedKeys = remember(pinnedApps) { pinnedApps.map { it.componentKey }.toSet() }

            AppDrawerView(
                apps = filteredApps,
                searchQuery = searchQuery,
                isThemedIcons = isThemedIcons,
                backProgress = backProgress,
                overrides = overrides,
                globalIconPackPackage = globalIconPack,
                globalAppFilter = globalAppFilter,
                iconPackManager = viewModel.iconPackManager,
                pinnedKeys = pinnedKeys,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onLaunchApp = { app ->
                    viewModel.launchApp(context, app)
                },
                onCustomizeApp = { app ->
                    appToEdit = app
                },
                onTogglePin = { app ->
                    if (pinnedKeys.contains(app.componentKey)) {
                        viewModel.unpinApp(app)
                    } else {
                        viewModel.pinApp(app)
                    }
                },
                onCloseDrawer = {
                    viewModel.setDrawerOpen(false)
                }
            )
        }

        // Home Settings & Wallpaper Dialog
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = {
                    Text(
                        "Home settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Column {
                        // Home Settings Activity
                        ListItem(
                            headlineContent = { Text("Home settings") },
                            supportingContent = { Text("Double tap to sleep, dock search & themes") },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = {
                                        showSettingsDialog = false
                                        val intent = Intent(context, QuarkSettingsActivity::class.java)
                                        context.startActivity(intent)
                                    }
                                )
                        )

                        // Wallpaper Picker
                        ListItem(
                            headlineContent = { Text("Wallpaper & style") },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Rounded.Wallpaper,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = {
                                        showSettingsDialog = false
                                        viewModel.openWallpaperPicker(context)
                                    }
                                )
                        )

                        // Material You Themed Icons Toggle
                        ListItem(
                            headlineContent = { Text("Themed icons") },
                            supportingContent = { Text("Tint icons with wallpaper colors") },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = isThemedIcons,
                                    onCheckedChange = {
                                        viewModel.toggleThemedIcons()
                                    }
                                )
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Done")
                    }
                }
            )
        }

        // Edit App Dialog
        if (appToEdit != null) {
            val targetApp = appToEdit!!
            EditAppDialog(
                app = targetApp,
                initialOverride = overrides[targetApp.componentKey],
                iconPackManager = viewModel.iconPackManager,
                installedIconPacks = installedIconPacks,
                onDismiss = { appToEdit = null },
                onSave = { customLabel, customPack, customDrawable ->
                    viewModel.saveAppOverride(
                        targetApp.componentKey,
                        customLabel,
                        customPack,
                        customDrawable
                    )
                    appToEdit = null
                },
                onReset = {
                    viewModel.resetAppOverride(targetApp.componentKey)
                    appToEdit = null
                }
            )
        }
    }
}

@Composable
fun PixelGridIconItem(
    app: AppItem,
    isThemed: Boolean,
    override: AppOverride? = null,
    globalIconPackPackage: String? = null,
    globalAppFilter: Map<String, String> = emptyMap(),
    iconPackManager: IconPackManager? = null,
    containerColor: Color,
    tintColor: Color,
    onClick: () -> Unit,
    onCustomize: () -> Unit,
    onRemoveFromHome: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showContextMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showContextMenu = true }
                )
                .padding(vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val bitmap = if (iconPackManager != null) {
                IconThemer.getRenderedIcon(
                    app = app,
                    override = override,
                    globalIconPackPackage = globalIconPackPackage,
                    globalAppFilter = globalAppFilter,
                    iconPackManager = iconPackManager,
                    isThemedIcons = isThemed,
                    containerColor = containerColor,
                    tintColor = tintColor
                )
            } else if (isThemed) {
                IconThemer.getThemedBitmap(app.componentKey, app.icon, containerColor, tintColor)
            } else {
                IconThemer.getStandardBitmap(app.componentKey, app.icon)
            }

            val renderedLabel = IconThemer.getRenderedLabel(app, override)

            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = renderedLabel,
                    modifier = Modifier.size(50.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = renderedLabel.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = tintColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = renderedLabel,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.75f),
                        blurRadius = 4f
                    )
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        // Anchored Floating Context Menu (Pixel Launcher Style)
        MaterialTheme(
            shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(20.dp))
        ) {
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.widthIn(min = 180.dp)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "Customize",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        onCustomize()
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                )

                if (onRemoveFromHome != null) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Remove",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            showContextMenu = false
                            onRemoveFromHome()
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    )
                }

                DropdownMenuItem(
                    text = {
                        Text(
                            "App info",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", app.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}
