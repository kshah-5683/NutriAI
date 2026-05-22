package com.app.nutriai.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.nutriai.domain.model.DailyLog
import com.app.nutriai.util.UnitConverter
import com.app.nutriai.util.formatQuantity
import com.app.nutriai.presentation.theme.CalorieColor
import com.app.nutriai.presentation.theme.CarbsColor
import com.app.nutriai.presentation.theme.FatColor
import com.app.nutriai.presentation.theme.NutriAiTheme
import com.app.nutriai.presentation.theme.ProteinColor
import kotlin.math.roundToInt

/**
 * Reusable row for a food log list entry. Shows food name, quantity,
 * macro summary, and visible edit/delete action buttons.
 * Also supports swipe-to-delete as a secondary gesture.
 *
 * @param log the daily log entry to display
 * @param foodName resolved food name (since DailyLog only has foodItemId)
 * @param onEdit callback when the user taps the edit button
 * @param onDelete callback when the user taps delete or swipes to dismiss
 */
@Composable
fun FoodLogItem(
    log: DailyLog,
    foodName: String,
    onEdit: ((String) -> Unit)? = null,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete(log.id)
        }
    }

    // Clip the entire swipe container to the card's shape so the red
    // delete background never bleeds into the rounded corners.
    val cardShape = MaterialTheme.shapes.medium

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(cardShape)
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        },
        enableDismissFromStartToEnd = false,
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = cardShape,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Top row: food name + macros
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Food name and quantity
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = foodName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${UnitConverter.toDisplayQty(log.consumedQty, log.consumedUnit).formatQuantity()} ${log.consumedUnit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Macro summary chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MacroChip(
                            value = "${log.totalCalories.roundToInt()}",
                            label = "kcal",
                            color = CalorieColor
                        )
                        MacroChip(
                            value = "${log.totalProtein.roundToInt()}",
                            label = "P",
                            color = ProteinColor
                        )
                        MacroChip(
                            value = "${log.totalCarbs.roundToInt()}",
                            label = "C",
                            color = CarbsColor
                        )
                        MacroChip(
                            value = "${log.totalFat.roundToInt()}",
                            label = "F",
                            color = FatColor
                        )
                    }
                }

                // Bottom row: action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onEdit != null) {
                        IconButton(
                            onClick = { onEdit(log.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit entry",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(
                        onClick = { onDelete(log.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete entry",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Small colored chip showing a macro value and label.
 */
@Composable
private fun MacroChip(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.7f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FoodLogItemPreview() {
    NutriAiTheme {
        FoodLogItem(
            log = DailyLog(
                id = "1",
                userId = "local_user",
                foodItemId = "food1",
                dateTimestamp = System.currentTimeMillis(),
                consumedQty = 1.5,
                consumedUnit = "bowl",
                totalCalories = 350.0,
                totalProtein = 12.0,
                totalCarbs = 45.0,
                totalFat = 8.0,
                lastModifiedAt = System.currentTimeMillis()
            ),
            foodName = "Oatmeal with Honey",
            onEdit = {},
            onDelete = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}
