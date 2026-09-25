@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.android.launcher3.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.launcher3.iconpack.IconPackManager
import com.android.launcher3.model.AppItem
import com.android.launcher3.model.ImmutableList
import com.android.launcher3.repository.AppOverride
import com.android.launcher3.util.IconThemer

@Composable
fun AppDrawerView(
    apps: ImmutableList<AppItem> = ImmutableList.empty(),
    searchQuery: String,
    isThemedIcons: Boolean,
    backProgress: Float,
    overrides: Map<String, AppOverride> = emptyMap(),
    globalIconPackPackage: String? = null,
    globalAppFilter: Map<String, String> = emptyMap(),
    iconPackManager: IconPackManager? = null,
    pinnedKeys: Set<String> = emptySet(),
    onSearchQueryChange: (String) -> Unit,
    onLaunchApp: (AppItem) -> Unit,
    onCustomizeApp: (AppItem) -> Unit = {},
    onTogglePin: ((AppItem) -> Unit)? = null,
    onAppLongClick: ((AppItem) -> Unit)? = null,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    BackHandler(enabled = true) {
        onCloseDrawer()
    }

    val safeDrawing = WindowInsets.safeDrawing.asPaddingValues()
    val containerColor = MaterialTheme.colorScheme.secondaryContainer
    val tintColor = MaterialTheme.colorScheme.onSecondaryContainer

    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val scale = 1f - (backProgress * 0.12f)
                val cornerRadius = (backProgress * 32f).dp
                scaleX = scale
                scaleY = scale
                translationY = dragOffsetY
                alpha = 1f - (backProgress * 0.15f)
                clip = true
                shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
            }
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragOffsetY = 0f },
                    onDragEnd = {
                        if (dragOffsetY > 60f) {
                            onCloseDrawer()
                        }
                        dragOffsetY = 0f
                    },
                    onDragCancel = { dragOffsetY = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount > 0 || dragOffsetY > 0) {
                            change.consume()
                            dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                        }
                    }
                )
            }
            .testTag("app_drawer_surface"),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = safeDrawing.calculateTopPadding(),
                    bottom = safeDrawing.calculateBottomPadding()
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle pill
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    .clickable { onCloseDrawer() }
                    .testTag("drawer_drag_handle")
            )

            // Unified App Drawer Search Bar (matches Pixel Launcher all-apps search)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                "Search ${apps.size} apps\u2026",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                    fontSize = 16.sp
                                )
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboardController?.hide()
                            if (apps.isNotEmpty()) {
                                onLaunchApp(apps.first())
                            }
                        }),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .testTag("drawer_search_field")
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5-Column Grid of All Installed Apps (Stock Pixel Standard)
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("app_drawer_grid"),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(apps, key = { it.componentKey }) { app ->
                    val isPinned = remember(pinnedKeys, app.componentKey) {
                        pinnedKeys.contains(app.componentKey)
                    }
                    val override = remember(overrides, app.componentKey) {
                        overrides[app.componentKey]
                    }
                    DrawerAppGridItem(
                        app = app,
                        isThemed = isThemedIcons,
                        isPinned = isPinned,
                        override = override,
                        globalIconPackPackage = globalIconPackPackage,
                        globalAppFilter = globalAppFilter,
                        iconPackManager = iconPackManager,
                        containerColor = containerColor,
                        tintColor = tintColor,
                        onClick = { onLaunchApp(app) },
                        onCustomize = { onCustomizeApp(app) },
                        onTogglePin = onTogglePin?.let { { it(app) } }
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerAppGridItem(
    app: AppItem,
    isThemed: Boolean,
    isPinned: Boolean = false,
    override: AppOverride? = null,
    globalIconPackPackage: String? = null,
    globalAppFilter: Map<String, String> = emptyMap(),
    iconPackManager: IconPackManager? = null,
    containerColor: Color,
    tintColor: Color,
    onClick: () -> Unit,
    onCustomize: () -> Unit,
    onTogglePin: (() -> Unit)? = null,
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
                .padding(horizontal = 2.dp, vertical = 4.dp)
                .testTag("app_item_${app.packageName}"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val bitmap = remember(
                app,
                override,
                globalIconPackPackage,
                globalAppFilter,
                iconPackManager,
                isThemed,
                containerColor,
                tintColor
            ) {
                if (iconPackManager != null) {
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
            }

            val renderedLabel = remember(app, override) {
                IconThemer.getRenderedLabel(app, override)
            }

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

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = renderedLabel,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 13.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
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

                if (onTogglePin != null) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isPinned) "Remove from Home" else "Pin to Home",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isPinned) Icons.Outlined.Delete else Icons.Outlined.PushPin,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            showContextMenu = false
                            onTogglePin()
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
