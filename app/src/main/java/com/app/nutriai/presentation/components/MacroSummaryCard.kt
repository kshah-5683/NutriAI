package com.app.nutriai.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.nutriai.presentation.theme.CalorieColor
import com.app.nutriai.presentation.theme.CarbsColor
import com.app.nutriai.presentation.theme.FatColor
import com.app.nutriai.presentation.theme.NutriAiTheme
import com.app.nutriai.presentation.theme.ProteinColor
import kotlin.math.roundToInt

/**
 * Reusable card displaying daily macro summary with circular progress arcs
 * and numeric values for calories, protein, carbs, and fat.
 *
 * @param calories total calories consumed
 * @param protein total protein in grams
 * @param carbs total carbohydrates in grams
 * @param fat total fat in grams
 * @param calorieGoal daily calorie goal (for progress arc)
 * @param proteinGoal daily protein goal in grams
 * @param carbsGoal daily carbs goal in grams
 * @param fatGoal daily fat goal in grams
 */
@Composable
fun MacroSummaryCard(
    calories: Double,
    protein: Double,
    carbs: Double,
    fat: Double,
    calorieGoal: Double = 2000.0,
    proteinGoal: Double = 150.0,
    carbsGoal: Double = 250.0,
    fatGoal: Double = 65.0,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main calorie ring
            CircularMacroIndicator(
                value = calories,
                goal = calorieGoal,
                color = CalorieColor,
                size = 120.dp,
                strokeWidth = 10.dp,
                label = "kcal",
                showValue = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Macro breakdown row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroColumn(
                    label = "Protein",
                    value = protein,
                    goal = proteinGoal,
                    unit = "g",
                    color = ProteinColor
                )
                MacroColumn(
                    label = "Carbs",
                    value = carbs,
                    goal = carbsGoal,
                    unit = "g",
                    color = CarbsColor
                )
                MacroColumn(
                    label = "Fat",
                    value = fat,
                    goal = fatGoal,
                    unit = "g",
                    color = FatColor
                )
            }
        }
    }
}

/**
 * A single macro column with a small circular indicator, value, and label.
 */
@Composable
private fun MacroColumn(
    label: String,
    value: Double,
    goal: Double,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularMacroIndicator(
            value = value,
            goal = goal,
            color = color,
            size = 56.dp,
            strokeWidth = 5.dp,
            label = null,
            showValue = false
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${value.roundToInt()}$unit",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "/ ${goal.roundToInt()}$unit",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

/**
 * Circular progress arc for a single macro value against a goal.
 */
@Composable
private fun CircularMacroIndicator(
    value: Double,
    goal: Double,
    color: Color,
    size: Dp,
    strokeWidth: Dp,
    label: String?,
    showValue: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) (value / goal).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "macro_progress"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            // Progress arc
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        if (showValue) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${value.roundToInt()}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MacroSummaryCardPreview() {
    NutriAiTheme {
        MacroSummaryCard(
            calories = 1450.0,
            protein = 95.0,
            carbs = 180.0,
            fat = 42.0,
            modifier = Modifier.padding(16.dp)
        )
    }
}
