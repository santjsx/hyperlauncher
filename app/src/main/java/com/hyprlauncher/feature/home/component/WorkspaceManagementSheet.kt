package com.hyprlauncher.feature.home.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hyprlauncher.core.designsystem.component.HyprStatusBadge
import com.hyprlauncher.core.designsystem.theme.HyprTheme
import com.hyprlauncher.domain.model.Workspace

/**
 * Hyprland-inspired Workspace Management Dialog (PRD Section 17 & 18).
 * Provides interactive workspace creation, deletion, renaming, and switching.
 */
@Composable
fun WorkspaceManagementDialog(
    workspaces: List<Workspace>,
    activeWorkspaceId: Int,
    onSelectWorkspace: (Int) -> Unit,
    onCreateWorkspace: (String) -> Unit,
    onRenameWorkspace: (Int, String) -> Unit,
    onDeleteWorkspace: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var newWorkspaceName by remember { mutableStateOf("") }
    var editingWorkspaceId by remember { mutableStateOf<Int?>(null) }
    var editingWorkspaceName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = HyprTheme.shapes.medium,
            color = HyprTheme.colors.surface,
            border = BorderStroke(HyprTheme.shapes.borderWidth, HyprTheme.colors.border)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "hyprctl: workspaces",
                        style = HyprTheme.typography.monospaceMedium,
                        color = HyprTheme.colors.accent,
                        fontWeight = FontWeight.Bold
                    )
                    HyprStatusBadge(
                        text = "${workspaces.size} active",
                        accentColor = HyprTheme.colors.accentSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Workspaces List
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(workspaces, key = { it.id }) { ws ->
                        val isActive = ws.id == activeWorkspaceId
                        val isEditing = editingWorkspaceId == ws.id

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            color = if (isActive) HyprTheme.colors.surfaceElevated else HyprTheme.colors.background,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isActive) HyprTheme.colors.accent else HyprTheme.colors.border
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onSelectWorkspace(ws.id)
                                            onDismiss()
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "[${ws.id}]",
                                        style = HyprTheme.typography.monospaceSmall,
                                        color = if (isActive) HyprTheme.colors.accent else HyprTheme.colors.textSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (isEditing) {
                                        BasicTextField(
                                            value = editingWorkspaceName,
                                            onValueChange = { editingWorkspaceName = it },
                                            textStyle = HyprTheme.typography.monospaceSmall.copy(
                                                color = HyprTheme.colors.textPrimary
                                            ),
                                            cursorBrush = SolidColor(HyprTheme.colors.accent),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Text(
                                            text = ws.name,
                                            style = HyprTheme.typography.monospaceSmall,
                                            color = HyprTheme.colors.textPrimary,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isEditing) {
                                        Text(
                                            text = "[save]",
                                            style = HyprTheme.typography.monospaceSmall,
                                            color = HyprTheme.colors.accent,
                                            modifier = Modifier.clickable {
                                                if (editingWorkspaceName.isNotBlank()) {
                                                    onRenameWorkspace(ws.id, editingWorkspaceName)
                                                }
                                                editingWorkspaceId = null
                                            }
                                        )
                                    } else {
                                        Text(
                                            text = "[edit]",
                                            style = HyprTheme.typography.monospaceSmall,
                                            color = HyprTheme.colors.textSecondary,
                                            modifier = Modifier.clickable {
                                                editingWorkspaceId = ws.id
                                                editingWorkspaceName = ws.name
                                            }
                                        )
                                    }

                                    // Delete (only enabled if more than 1 workspace remains)
                                    if (workspaces.size > 1) {
                                        Text(
                                            text = "[x]",
                                            style = HyprTheme.typography.monospaceSmall,
                                            color = HyprTheme.colors.error,
                                            modifier = Modifier.clickable {
                                                onDeleteWorkspace(ws.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Create New Workspace Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp),
                        color = HyprTheme.colors.background,
                        border = BorderStroke(1.dp, HyprTheme.colors.border)
                    ) {
                        BasicTextField(
                            value = newWorkspaceName,
                            onValueChange = { newWorkspaceName = it },
                            textStyle = HyprTheme.typography.monospaceSmall.copy(
                                color = HyprTheme.colors.textPrimary
                            ),
                            cursorBrush = SolidColor(HyprTheme.colors.accent),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (newWorkspaceName.isEmpty()) {
                                    Text(
                                        text = "new workspace name...",
                                        style = HyprTheme.typography.monospaceSmall,
                                        color = HyprTheme.colors.textSecondary.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = HyprTheme.colors.surfaceElevated,
                        border = BorderStroke(1.dp, HyprTheme.colors.accent),
                        modifier = Modifier.clickable {
                            if (newWorkspaceName.isNotBlank()) {
                                onCreateWorkspace(newWorkspaceName.trim())
                                newWorkspaceName = ""
                            }
                        }
                    ) {
                        Text(
                            text = "+ add",
                            style = HyprTheme.typography.monospaceSmall,
                            color = HyprTheme.colors.accent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Close Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDismiss),
                    shape = RoundedCornerShape(4.dp),
                    color = HyprTheme.colors.surfaceElevated
                ) {
                    Text(
                        text = "close",
                        style = HyprTheme.typography.monospaceSmall,
                        color = HyprTheme.colors.textSecondary,
                        modifier = Modifier.padding(vertical = 6.dp),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
