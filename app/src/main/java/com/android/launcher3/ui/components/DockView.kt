@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.android.launcher3.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.launcher3.iconpack.IconPackManager
import com.android.launcher3.model.AppItem
import com.android.launcher3.model.ImmutableList
import com.android.launcher3.repository.AppOverride
import com.android.launcher3.util.IconThemer

@Composable
fun DockView(
    dockApps: ImmutableList<AppItem> = ImmutableList.empty(),
    isThemedIcons: Boolean,
    showDockSearch: Boolean = true,
    overrides: Map<String, AppOverride> = emptyMap(),
    globalIconPackPackage: String? = null,
    globalAppFilter: Map<String, String> = emptyMap(),
    iconPackManager: IconPackManager? = null,
    onLaunchApp: (AppItem) -> Unit,
    onCustomizeApp: ((AppItem) -> Unit)? = null,
    onAppLongClick: ((AppItem, Int) -> Unit)? = null,
    onSearchBarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val containerColor = MaterialTheme.colorScheme.secondaryContainer
    val tintColor = MaterialTheme.colorScheme.onSecondaryContainer

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 5-Column Hotseat Icon Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .testTag("dock_icon_row"),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 5) {
                val app = dockApps.getOrNull(i)
                DockIconItem(
                    app = app,
                    slotIndex = i,
                    isThemed = isThemedIcons,
                    override = app?.let { overrides[it.componentKey] },
                    globalIconPackPackage = globalIconPackPackage,
                    globalAppFilter = globalAppFilter,
                    iconPackManager = iconPackManager,
                    containerColor = containerColor,
                    tintColor = tintColor,
                    onClick = {
                        if (app != null) {
                            onLaunchApp(app)
                        } else {
                            onSearchBarClick()
                        }
                    },
                    onCustomize = {
                        if (app != null) {
                            onCustomizeApp?.invoke(app)
                        }
                    },
                    onLongClick = {
                        if (app != null && onAppLongClick != null) {
                            onAppLongClick(app, i)
                        }
                    }
                )
            }
        }

        if (showDockSearch) {
            Spacer(modifier = Modifier.height(12.dp))

            // Persistent, slightly taller pill-shaped Search Bar directly below the dock icons
            PixelDockSearchBar(
                isThemed = isThemedIcons,
                onClick = onSearchBarClick,
                onLensClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("googleapp://lens")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        onSearchBarClick()
                    }
                },
                onVoiceClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        onSearchBarClick()
                    }
                }
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun DockIconItem(
    app: AppItem?,
    slotIndex: Int,
    isThemed: Boolean,
    override: AppOverride? = null,
    globalIconPackPackage: String? = null,
    globalAppFilter: Map<String, String> = emptyMap(),
    iconPackManager: IconPackManager? = null,
    containerColor: Color,
    tintColor: Color,
    onClick: () -> Unit,
    onCustomize: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showContextMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        if (app != null) {
                            showContextMenu = true
                        }
                        onLongClick?.invoke()
                    }
                )
                .testTag("dock_item_$slotIndex"),
            contentAlignment = Alignment.Center
        ) {
            if (app != null) {
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
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add app to dock",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        if (app != null) {
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
                            onCustomize?.invoke()
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    )

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
}

@Composable
fun PixelDockSearchBar(
    isThemed: Boolean,
    onClick: () -> Unit,
    onLensClick: () -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchBgColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp) // Slightly taller modern Pixel pill height
            .clip(RoundedCornerShape(28.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(28.dp)
            )
            .clickable(onClick = onClick)
            .testTag("dock_search_bar"),
        shape = RoundedCornerShape(28.dp),
        color = searchBgColor,
        tonalElevation = 6.dp,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Google G Logo + Search text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                GoogleLogoIcon(
                    isThemed = isThemed,
                    tintColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "Search your phone & more\u2026",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = contentColor.copy(alpha = 0.75f)
                    )
                )
            }

            // Google Lens & Mic Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onLensClick)
                        .testTag("dock_lens_button"),
                    contentAlignment = Alignment.Center
                ) {
                    GoogleLensIcon(
                        tintColor = if (isThemed) MaterialTheme.colorScheme.primary else Color(0xFF4285F4),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onVoiceClick)
                        .testTag("dock_mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Voice search",
                        tint = if (isThemed) MaterialTheme.colorScheme.primary else Color(0xFFEA4335),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
