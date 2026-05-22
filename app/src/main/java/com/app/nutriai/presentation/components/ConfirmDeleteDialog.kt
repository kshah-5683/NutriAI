package com.app.nutriai.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.app.nutriai.presentation.theme.NutriAiTheme

/**
 * Reusable confirmation dialog shown before a destructive delete action.
 *
 * Displays an [AlertDialog] with a delete icon, configurable [title] and
 * [message], and two actions:
 *  - **Delete** (red, calls [onConfirm]) — proceeds with the deletion.
 *  - **Cancel** (calls [onDismiss]) — dismisses without taking action.
 *
 * Both buttons dismiss the dialog. The caller is responsible for actually
 * performing the delete operation inside [onConfirm].
 *
 * @param title  Dialog headline — e.g. "Delete Log Entry".
 * @param message Secondary text explaining what will be deleted.
 * @param onConfirm Called when the user taps **Delete**.
 * @param onDismiss Called when the user taps **Cancel** or the scrim.
 */
@Composable
fun ConfirmDeleteDialog(
    title: String = "Delete Item",
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview
@Composable
private fun ConfirmDeleteDialogPreview() {
    NutriAiTheme {
        ConfirmDeleteDialog(
            title = "Delete Log Entry",
            message = "Remove \"Oatmeal with Honey\" from today's log? This action cannot be undone.",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
