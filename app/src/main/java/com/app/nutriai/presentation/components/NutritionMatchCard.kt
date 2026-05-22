package com.app.nutriai.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.nutriai.domain.model.NutritionInfo
import com.app.nutriai.util.formatMacro
import com.app.nutriai.presentation.theme.CarbsColor
import com.app.nutriai.presentation.theme.CalorieColor
import com.app.nutriai.presentation.theme.FatColor
import com.app.nutriai.presentation.theme.NutriAiTheme
import com.app.nutriai.presentation.theme.ProteinColor
import kotlin.math.roundToInt

/**
 * Reusable card displaying a matched [NutritionInfo] result from USDA FDC.
 *
 * Phase 5: Shown in the AI input section of the Log screen when the nutrition
 * database returns a match for a parsed food item. Displays:
 * - Matched product name and brand
 * - Per-100g macro values (cal / protein / carbs / fat)
 * - Attribution badge (data source name)
 *
 * The card is informational only — users can still override macros in the
 * manual entry form after accepting a parsed food.
 */
@Composable
fun NutritionMatchCard(
    nutritionInfo: NutritionInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Header row: check icon + product name + source badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Nutrition found",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = nutritionInfo.productName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!nutritionInfo.brand.isNullOrBlank()) {
                            Text(
                                text = nutritionInfo.brand,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Source attribution badge
                SourceBadge(source = nutritionInfo.source)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Per-100g macro row
            Text(
                text = "Per 100g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.55f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutritionMatchMacroItem(
                    label = "Cal",
                    value = "${nutritionInfo.caloriesPer100g.roundToInt()}",
                    color = CalorieColor
                )
                NutritionMatchMacroItem(
                    label = "Protein",
                    value = "${nutritionInfo.proteinPer100g.formatMacro()}g",
                    color = ProteinColor
                )
                NutritionMatchMacroItem(
                    label = "Carbs",
                    value = "${nutritionInfo.carbsPer100g.formatMacro()}g",
                    color = CarbsColor
                )
                NutritionMatchMacroItem(
                    label = "Fat",
                    value = "${nutritionInfo.fatPer100g.formatMacro()}g",
                    color = FatColor
                )
            }
        }
    }
}

@Composable
private fun NutritionMatchMacroItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun SourceBadge(
    source: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            modifier = Modifier.size(10.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = source,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
        )
    }
}

// ─── Previews ─────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun NutritionMatchCardPreview() {
    NutriAiTheme {
        NutritionMatchCard(
            nutritionInfo = NutritionInfo(
                productName = "Rolled Oats",
                brand = "Quaker",
                caloriesPer100g = 379.0,
                proteinPer100g = 13.2,
                carbsPer100g = 67.7,
                fatPer100g = 6.9,
                fiberPer100g = 10.3,
                source = "USDA FDC",
                externalId = "0030000014079"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NutritionMatchCardNoBrandPreview() {
    NutriAiTheme {
        NutritionMatchCard(
            nutritionInfo = NutritionInfo(
                productName = "Besan (Chickpea Flour)",
                brand = null,
                caloriesPer100g = 340.0,
                proteinPer100g = 22.0,
                carbsPer100g = 57.8,
                fatPer100g = 6.7,
                source = "USDA FDC",
                externalId = null
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
