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
import com.android.launcher3.ui.icons.LauncherIcons as Icons
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
    val tintColor = MaterialTheme.colorScheme.primary

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
            Spacer(modifier = Modifier.height(8.dp))

            LauncherSearchBar()
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
    val haptics = LocalHapticFeedback.current
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
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
fun LauncherSearchBar(
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(androidx.compose.ui.graphics.Color(0xFF303134)) // Authentic Google Search Dark Gray
            .clickable {
                // Main Search Click
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_WEB_SEARCH).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val browserIntent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://www.google.com")
                    ).apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(browserIntent)
                }
            }
            .padding(horizontal = 16.dp),
        contentAlignment = androidx.compose.ui.Alignment.CenterStart
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
        ) {
            // The 'G' Logo
            androidx.compose.material3.Text(
                text = "G",
                fontSize = 22.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                color = androidx.compose.ui.graphics.Color.White
            )

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(12.dp))

            // Hint Text
            androidx.compose.material3.Text(
                text = "Ask Gemini or search...",
                fontSize = 16.sp,
                color = androidx.compose.ui.graphics.Color(0xFF9AA0A6),
                modifier = androidx.compose.ui.Modifier.weight(1f)
            )

            // 1. Gemini Icon (Sparkle)
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "Gemini",
                tint = androidx.compose.ui.graphics.Color(0xFFA8C7FA), // Soft Gemini Blue/Purple
                modifier = androidx.compose.ui.Modifier
                    .size(24.dp)
                    .clickable {
                        try {
                            // Try launching Gemini App directly
                            val geminiIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.bard") 
                                ?: android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://gemini.google.com"))
                            geminiIntent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(geminiIntent)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
            )

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(16.dp))

            // 2. Google Lens Icon (Camera)
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.CameraAlt,
                contentDescription = "Google Lens",
                tint = androidx.compose.ui.graphics.Color(0xFF9AA0A6),
                modifier = androidx.compose.ui.Modifier
                    .size(24.dp)
                    .clickable {
                        try {
                            val lensIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("googleapp://lens")).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(lensIntent)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
            )

            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(16.dp))

            // 3. Microphone Icon
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.Mic,
                contentDescription = "Voice Search",
                tint = androidx.compose.ui.graphics.Color(0xFF9AA0A6),
                modifier = androidx.compose.ui.Modifier
                    .size(24.dp)
                    .clickable {
                        try {
                            val voiceIntent = android.content.Intent(android.speech.RecognizerIntent.ACTION_WEB_SEARCH).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(voiceIntent)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
            )
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
    LauncherSearchBar(modifier = modifier)
}
