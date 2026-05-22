package com.app.nutriai.presentation.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.app.nutriai.presentation.screens.insights.InsightsPeriod
import com.app.nutriai.presentation.theme.NutriAiTheme

/**
 * Segmented button row for selecting the Insights time window.
 *
 * Matches the identical pattern already used in [LogScreen] for the
 * ingredient/recipe mode toggle — same API, same BOM version, safe to use.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodSelector(
    selectedPeriod: InsightsPeriod,
    onPeriodSelected: (InsightsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val periods = InsightsPeriod.entries

    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        periods.forEachIndexed { index, period ->
            SegmentedButton(
                selected = period == selectedPeriod,
                onClick = { onPeriodSelected(period) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size),
                label = { Text(period.label) },
                icon = { SegmentedButtonDefaults.Icon(active = period == selectedPeriod) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PeriodSelectorPreview() {
    NutriAiTheme {
        PeriodSelector(
            selectedPeriod = InsightsPeriod.WEEK,
            onPeriodSelected = {}
        )
    }
}
