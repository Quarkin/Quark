@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.android.launcher3.ui

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.launcher3.model.AppItem
import com.android.launcher3.ui.components.AppDrawerView
import com.android.launcher3.ui.components.DockView
import com.android.launcher3.util.StatusBarHelper
import com.android.launcher3.viewmodel.LauncherViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allApps by viewModel.allApps.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val pinnedApps by viewModel.pinnedApps.collectAsState()
    val dockApps by viewModel.dockApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()

    var selectedAppForSheet by remember { mutableStateOf<AppItem?>(null) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var dockSlotToAssign by remember { mutableStateOf<Int?>(null) }

    // Intercept back button when drawer is open
    BackHandler(enabled = isDrawerOpen) {
        viewModel.setDrawerOpen(false)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var accumulatedDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onDragEnd = {
                        if (accumulatedDrag < -40f) {
                            // Swiping up from anywhere on wallpaper or dock smoothly triggers app drawer
                            viewModel.setDrawerOpen(true)
                        } else if (accumulatedDrag > 40f) {
                            // Swiping down anywhere pulls down the system notification shade
                            StatusBarHelper.expandNotificationShade(context)
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
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    showWallpaperDialog = true
                }
            )
            .testTag("home_workspace")
    ) {
        // Main Workspace Area (Wallpaper behind, Clock, Pinned Apps, Bottom Dock)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: At a Glance Clock & Date Widget
            WorkspaceHeaderWidget(
                onDateClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).setData(CalendarContract.CONTENT_URI)
                        context.startActivity(intent)
                    } catch (ignored: Exception) {
                    }
                },
                onTimeClick = {
                    try {
                        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                        context.startActivity(intent)
                    } catch (ignored: Exception) {
                    }
                }
            )

            // Center Section: Pinned Apps Grid (Home Screen shortcuts)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (pinnedApps.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pinnedApps, key = { it.componentKey }) { app ->
                            PinnedAppGridItem(
                                app = app,
                                onClick = { viewModel.launchApp(context, app) },
                                onLongClick = { selectedAppForSheet = app }
                            )
                        }
                    }
                }
            }

            // Bottom Section: Dock Layout (5-column icon row + persistent pill search bar)
            DockView(
                dockApps = dockApps,
                onLaunchApp = { app -> viewModel.launchApp(context, app) },
                onAppLongClick = { app, slotIndex ->
                    dockSlotToAssign = slotIndex
                    selectedAppForSheet = app
                },
                onSearchBarClick = {
                    viewModel.setDrawerOpen(true)
                }
            )
        }

        // Animated App Drawer (Slide up from bottom with spring animation)
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
            AppDrawerView(
                apps = filteredApps,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onLaunchApp = { app ->
                    viewModel.launchApp(context, app)
                },
                onAppLongClick = { app ->
                    selectedAppForSheet = app
                },
                onCloseDrawer = {
                    viewModel.setDrawerOpen(false)
                }
            )
        }

        // Long Press App Options Bottom Sheet
        if (selectedAppForSheet != null) {
            val app = selectedAppForSheet!!
            val isPinned = pinnedApps.any { it.componentKey == app.componentKey }

            ModalBottomSheet(
                onDismissRequest = { selectedAppForSheet = null },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    // App Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        val bitmap = app.getImageBitmap()
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = app.label,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.size(16.dp))
                        Column {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Pin / Unpin Home Option
                    ListItem(
                        headlineContent = {
                            Text(if (isPinned) "Remove from Home screen" else "Pin to Home screen")
                        },
                        leadingContent = {
                            Icon(
                                imageVector = if (isPinned) Icons.Rounded.Delete else Icons.Rounded.PushPin,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = {
                                    if (isPinned) {
                                        viewModel.unpinApp(app)
                                    } else {
                                        viewModel.pinApp(app)
                                    }
                                    selectedAppForSheet = null
                                }
                            )
                    )

                    // Pin to Dock Option
                    ListItem(
                        headlineContent = { Text("Set as Dock app") },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Rounded.PushPin,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = {
                                    val targetSlot = dockSlotToAssign ?: 0
                                    viewModel.setDockSlot(targetSlot, app)
                                    selectedAppForSheet = null
                                    dockSlotToAssign = null
                                }
                            )
                    )

                    // App Info Option
                    ListItem(
                        headlineContent = { Text("App info") },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = {
                                    viewModel.openAppInfo(context, app)
                                    selectedAppForSheet = null
                                }
                            )
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Long Press Wallpaper Dialog
        if (showWallpaperDialog) {
            AlertDialog(
                onDismissRequest = { showWallpaperDialog = false },
                title = { Text("Home settings") },
                text = {
                    Column {
                        ListItem(
                            headlineContent = { Text("Change wallpaper") },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Rounded.Wallpaper,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = {
                                        showWallpaperDialog = false
                                        viewModel.openWallpaperPicker(context)
                                    }
                                )
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showWallpaperDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun WorkspaceHeaderWidget(
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = remember { Date() }
    val dayFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = timeFormat.format(now),
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                color = Color.White
            ),
            modifier = Modifier.combinedClickable(onClick = onTimeClick)
        )
        Text(
            text = dayFormat.format(now),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.9f)
            ),
            modifier = Modifier.combinedClickable(onClick = onDateClick)
        )
    }
}

@Composable
fun PinnedAppGridItem(
    app: AppItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val bitmap = app.getImageBitmap()
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = app.label,
                modifier = Modifier.size(52.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.label.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = Color.White,
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.7f),
                    blurRadius = 4f
                )
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
