package com.android.launcher3.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.launcher3.model.AppItem

/**
 * Floating anchored pill-shaped Context Menu mimicking stock Pixel Launcher style.
 * Displays "Customize" at the top, followed by "Pause app", "Widgets", "App info",
 * and context-sensitive Pin/Remove actions.
 */
@Composable
fun AppFloatingContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    app: AppItem,
    onCustomize: () -> Unit,
    isPinned: Boolean = false,
    onTogglePin: (() -> Unit)? = null,
    onRemoveFromHome: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(20.dp))
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = modifier.widthIn(min = 190.dp)
        ) {
            // 1. Customize (Injected at the top of the pill menu)
            DropdownMenuItem(
                text = {
                    Text(
                        "Customize",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Customize",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    onDismissRequest()
                    onCustomize()
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            )

            // 2. Pause app
            DropdownMenuItem(
                text = {
                    Text(
                        "Pause app",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.HourglassEmpty,
                        contentDescription = "Pause app",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    onDismissRequest()
                    Toast.makeText(context, "${app.label} paused for today", Toast.LENGTH_SHORT).show()
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            )

            // 3. Widgets
            DropdownMenuItem(
                text = {
                    Text(
                        "Widgets",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Widgets,
                        contentDescription = "Widgets",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    onDismissRequest()
                    Toast.makeText(context, "Widgets for ${app.label}", Toast.LENGTH_SHORT).show()
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            )

            // 4. App info
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
                        contentDescription = "App info",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    onDismissRequest()
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

            // 5. Contextual actions: Remove (Workspace) or Pin / Unpin (Drawer)
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
                            contentDescription = "Remove",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onDismissRequest()
                        onRemoveFromHome()
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                )
            } else if (onTogglePin != null) {
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
                        onDismissRequest()
                        onTogglePin()
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}
