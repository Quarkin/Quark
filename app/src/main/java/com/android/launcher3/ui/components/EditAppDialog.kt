@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.android.launcher3.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.launcher3.iconpack.IconPackInfo
import com.android.launcher3.iconpack.IconPackManager
import com.android.launcher3.model.AppItem
import com.android.launcher3.repository.AppOverride
import com.android.launcher3.util.IconThemer

@Composable
fun EditAppDialog(
    app: AppItem,
    initialOverride: AppOverride?,
    iconPackManager: IconPackManager,
    installedIconPacks: List<IconPackInfo>,
    onDismiss: () -> Unit,
    onSave: (customLabel: String?, customPack: String?, customDrawable: String?) -> Unit,
    onReset: () -> Unit
) {
    var customLabel by remember {
        mutableStateOf(initialOverride?.customLabel ?: app.label)
    }
    var selectedPack by remember {
        mutableStateOf(initialOverride?.customIconPackPackage)
    }
    var selectedDrawable by remember {
        mutableStateOf(initialOverride?.customIconDrawableName)
    }
    var showIconPicker by remember { mutableStateOf(false) }

    // Resolve preview bitmap
    val previewBitmap: ImageBitmap? = remember(selectedPack, selectedDrawable) {
        if (!selectedPack.isNullOrBlank() && !selectedDrawable.isNullOrBlank()) {
            val d = iconPackManager.loadDrawable(selectedPack!!, selectedDrawable!!)
            IconThemer.getStandardBitmap("$selectedPack/$selectedDrawable", d, 144)
        } else {
            IconThemer.getStandardBitmap(app.componentKey, app.icon, 144)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit App",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive Icon button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            CircleShape
                        )
                        .clickable { showIconPicker = true }
                        .testTag("edit_app_icon_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap,
                            contentDescription = "App Icon",
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    // Little edit badge in bottom right corner
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit icon",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = if (!selectedPack.isNullOrBlank()) "Icon Pack: $selectedPack" else "Tap icon to change",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Editable Name
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text("App Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_app_label_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(customLabel, selectedPack, selectedDrawable)
                },
                modifier = Modifier.testTag("edit_app_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (initialOverride != null && !initialOverride.isEmpty) {
                    TextButton(
                        onClick = onReset,
                        modifier = Modifier.testTag("edit_app_reset_button")
                    ) {
                        Text("Reset")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )

    if (showIconPicker) {
        IconPickerBottomSheet(
            app = app,
            installedIconPacks = installedIconPacks,
            iconPackManager = iconPackManager,
            onSelectDrawable = { pack, drawableName ->
                selectedPack = pack
                selectedDrawable = drawableName
                showIconPicker = false
            },
            onResetToDefault = {
                selectedPack = null
                selectedDrawable = null
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false }
        )
    }
}

@Composable
fun IconPickerBottomSheet(
    app: AppItem,
    installedIconPacks: List<IconPackInfo>,
    iconPackManager: IconPackManager,
    onSelectDrawable: (packPackage: String, drawableName: String) -> Unit,
    onResetToDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPackPackage by remember {
        mutableStateOf(installedIconPacks.firstOrNull()?.packageName)
    }
    var drawables by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var filterQuery by remember { mutableStateOf("") }

    LaunchedEffect(selectedPackPackage) {
        val pack = selectedPackPackage
        if (pack != null) {
            isLoading = true
            drawables = iconPackManager.getAvailableDrawables(pack)
            isLoading = false
        } else {
            drawables = emptyList()
        }
    }

    val filteredDrawables = remember(drawables, filterQuery) {
        if (filterQuery.isBlank()) drawables
        else drawables.filter { it.contains(filterQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Icon Picker",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onResetToDefault) {
                    Text("Use Default")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pack selector chips
            if (installedIconPacks.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(installedIconPacks, key = { it.packageName }) { pack ->
                        FilterChip(
                            selected = pack.packageName == selectedPackPackage,
                            onClick = { selectedPackPackage = pack.packageName },
                            label = { Text(pack.label) }
                        )
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "No 3rd-party icon packs detected on device. Install icon packs from Google Play or use default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (selectedPackPackage != null) {
                // Search filter for drawables
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    placeholder = { Text("Filter drawables...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (filterQuery.isNotEmpty()) {
                            IconButton(onClick = { filterQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            // Drawables grid
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredDrawables.isNotEmpty()) {
                val pack = selectedPackPackage!!
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredDrawables, key = { it }) { drawableName ->
                        IconPackItemCell(
                            packPackage = pack,
                            drawableName = drawableName,
                            iconPackManager = iconPackManager,
                            onClick = {
                                onSelectDrawable(pack, drawableName)
                            }
                        )
                    }
                }
            } else if (selectedPackPackage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No drawables found in this pack",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun IconPackItemCell(
    packPackage: String,
    drawableName: String,
    iconPackManager: IconPackManager,
    onClick: () -> Unit
) {
    val bitmap = remember(packPackage, drawableName) {
        val d = iconPackManager.loadDrawable(packPackage, drawableName)
        IconThemer.getStandardBitmap("$packPackage/$drawableName", d, 128)
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = drawableName,
                modifier = Modifier.size(48.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = drawableName,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
