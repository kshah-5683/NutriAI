package com.app.nutriai.presentation.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.nutriai.domain.model.FoodItem
import com.app.nutriai.presentation.theme.CalorieColor
import com.app.nutriai.presentation.theme.CarbsColor
import com.app.nutriai.presentation.theme.FatColor
import com.app.nutriai.presentation.theme.NutriAiTheme
import com.app.nutriai.presentation.theme.ProteinColor
import kotlin.math.roundToInt

/**
 * Detail card showing the full nutritional breakdown for a food item.
 * Styled to resemble a real nutrition facts label.
 *
 * @param foodItem the food item to display nutritional information for
 */
@Composable
fun NutritionFactsCard(
    foodItem: FoodItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Text(
                text = "Nutrition Facts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = foodItem.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (foodItem.brand != null) {
                Text(
                    text = foodItem.brand,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.onSurface)

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Per serving (${foodItem.baseServingG.roundToInt()}g)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(thickness = 4.dp, color = MaterialTheme.colorScheme.onSurface)

            // Calories
            NutrientRow(
                label = "Calories",
                value = "${foodItem.baseCalories.roundToInt()}",
                unit = "kcal",
                isBold = true,
                color = CalorieColor
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Protein
            NutrientRow(
                label = "Protein",
                value = "${foodItem.baseProtein.roundToInt()}",
                unit = "g",
                isBold = true,
                color = ProteinColor
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Carbohydrates
            NutrientRow(
                label = "Total Carbohydrate",
                value = "${foodItem.baseCarbs.roundToInt()}",
                unit = "g",
                isBold = true,
                color = CarbsColor
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Fat
            NutrientRow(
                label = "Total Fat",
                value = "${foodItem.baseFat.roundToInt()}",
                unit = "g",
                isBold = true,
                color = FatColor
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/**
 * A single row in the nutrition facts card.
 */
@Composable
private fun NutrientRow(
    label: String,
    value: String,
    unit: String,
    isBold: Boolean = false,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$value$unit",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NutritionFactsCardPreview() {
    NutriAiTheme {
        NutritionFactsCard(
            foodItem = FoodItem(
                id = "1",
                catalogId = "cat1",
                name = "Oatmeal with Honey",
                brand = "Quaker",
                baseServingG = 150.0,
                baseCalories = 280.0,
                baseProtein = 8.0,
                baseCarbs = 48.0,
                baseFat = 6.0,
                lastModifiedAt = System.currentTimeMillis()
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
