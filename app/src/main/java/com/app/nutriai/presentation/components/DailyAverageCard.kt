package com.app.nutriai.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.nutriai.domain.model.MacroGoals
import com.app.nutriai.presentation.theme.CalorieColor
import com.app.nutriai.presentation.theme.CarbsColor
import com.app.nutriai.presentation.theme.FatColor
import com.app.nutriai.presentation.theme.NutriAiTheme
import com.app.nutriai.presentation.theme.ProteinColor
import kotlin.math.roundToInt

/**
 * Card displaying average daily macro intake for the selected Insights period.
 *
 * Shows calories, protein, carbs, and fat averages alongside their % of goal,
 * giving the user an at-a-glance summary of how well they are hitting their targets.
 *
 * @param averageCalories Mean daily calories over the period.
 * @param averageProtein  Mean daily protein (g) over the period.
 * @param averageCarbs    Mean daily carbs (g) over the period.
 * @param averageFat      Mean daily fat (g) over the period.
 * @param macroGoals      User-configured daily targets.
 * @param periodLabel     "This Week" / "This Month" shown in the card header.
 * @param modifier        Layout modifier.
 */
@Composable
fun DailyAverageCard(
    averageCalories: Double,
    averageProtein: Double,
    averageCarbs: Double,
    averageFat: Double,
    macroGoals: MacroGoals,
    periodLabel: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Daily Average — $periodLabel",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Calorie row (full width, larger emphasis)
            AverageRow(
                label = "Calories",
                value = averageCalories,
                goal = macroGoals.calorieGoal,
                unit = "kcal",
                color = CalorieColor
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Macro trio in a horizontal row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroAverageColumn(
                    label = "Protein",
                    value = averageProtein,
                    goal = macroGoals.proteinGoal,
                    color = ProteinColor,
                    modifier = Modifier.weight(1f)
                )
                MacroAverageColumn(
                    label = "Carbs",
                    value = averageCarbs,
                    goal = macroGoals.carbsGoal,
                    color = CarbsColor,
                    modifier = Modifier.weight(1f)
                )
                MacroAverageColumn(
                    label = "Fat",
                    value = averageFat,
                    goal = macroGoals.fatGoal,
                    color = FatColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AverageRow(
    label: String,
    value: Double,
    goal: Double,
    unit: String,
    color: Color
) {
    val pct = if (goal > 0) ((value / goal) * 100).roundToInt() else 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "${value.roundToInt()} $unit",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$pct%",
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MacroAverageColumn(
    label: String,
    value: Double,
    goal: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val pct = if (goal > 0) ((value / goal) * 100).roundToInt() else 0
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${value.roundToInt()}g",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "$pct% of goal",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyAverageCardPreview() {
    NutriAiTheme {
        DailyAverageCard(
            averageCalories = 1840.0,
            averageProtein = 112.0,
            averageCarbs = 210.0,
            averageFat = 58.0,
            macroGoals = MacroGoals(),
            periodLabel = "This Week",
            modifier = Modifier.padding(16.dp)
        )
    }
}
