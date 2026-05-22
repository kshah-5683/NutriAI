package com.app.nutriai.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.nutriai.domain.model.DailyMacroSummary
import com.app.nutriai.presentation.theme.CarbsColor
import com.app.nutriai.presentation.theme.FatColor
import com.app.nutriai.presentation.theme.NutriAiTheme
import com.app.nutriai.presentation.theme.ProteinColor
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Grouped vertical bar chart for weekly macro visualization.
 *
 * Renders 3 bars per day (Protein, Carbs, Fat) using Compose [Canvas].
 * Bar heights are animated with [animateFloatAsState] for a smooth entry transition.
 * Y-axis shows a gram scale with subtle gridlines; x-axis shows abbreviated day names.
 *
 * @param summaries  Exactly 7 [DailyMacroSummary] entries, one per day (oldest → newest).
 * @param modifier   Layout modifier applied to the chart Canvas.
 */
@Composable
fun MacroBarChart(
    summaries: List<DailyMacroSummary>,
    modifier: Modifier = Modifier
) {
    if (summaries.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    // Compute max value for y-axis scale (minimum 50g so chart isn't jumpy on sparse data)
    val maxGrams = summaries.maxOf {
        maxOf(it.totalProtein, it.totalCarbs, it.totalFat)
    }.coerceAtLeast(50.0).toFloat()

    // Animate each bar independently from 0 → actual fraction
    val proteinFractions = summaries.map { (it.totalProtein / maxGrams).toFloat().coerceIn(0f, 1f) }
    val carbsFractions   = summaries.map { (it.totalCarbs   / maxGrams).toFloat().coerceIn(0f, 1f) }
    val fatFractions     = summaries.map { (it.totalFat     / maxGrams).toFloat().coerceIn(0f, 1f) }

    val animatedProtein = proteinFractions.map { target ->
        val anim by animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 600),
            label = "protein_bar"
        )
        anim
    }
    val animatedCarbs = carbsFractions.map { target ->
        val anim by animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 600),
            label = "carbs_bar"
        )
        anim
    }
    val animatedFat = fatFractions.map { target ->
        val anim by animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 600),
            label = "fat_bar"
        )
        anim
    }

    val dayLabels = remember(summaries) {
        summaries.map { it.date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val chartWidth = size.width
            val chartHeight = size.height
            val yAxisWidth = 36.dp.toPx()
            val xAxisHeight = 20.dp.toPx()
            val plotWidth = chartWidth - yAxisWidth
            val plotHeight = chartHeight - xAxisHeight

            val gridLineCount = 4
            // Draw horizontal gridlines + y-axis labels
            for (i in 0..gridLineCount) {
                val fraction = i.toFloat() / gridLineCount
                val y = plotHeight * (1f - fraction)
                val gramsLabel = (maxGrams * fraction).roundToInt().toString()

                drawLine(
                    color = gridColor,
                    start = Offset(yAxisWidth, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = gramsLabel,
                    style = labelStyle,
                    topLeft = Offset(0f, y - 6.dp.toPx()),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    size = Size(yAxisWidth - 4.dp.toPx(), 14.dp.toPx())
                )
            }

            val dayCount = summaries.size
            val daySlotWidth = plotWidth / dayCount
            val barGroupPadding = daySlotWidth * 0.15f
            val barGroupWidth = daySlotWidth - barGroupPadding * 2
            val barCount = 3
            val barGap = 2.dp.toPx()
            val barWidth = (barGroupWidth - barGap * (barCount - 1)) / barCount
            val cornerRadius = CornerRadius(2.dp.toPx())

            summaries.forEachIndexed { index, _ ->
                val groupLeft = yAxisWidth + index * daySlotWidth + barGroupPadding

                // Draw 3 bars: Protein, Carbs, Fat
                drawAnimatedBar(
                    fraction = animatedProtein[index],
                    left = groupLeft,
                    plotHeight = plotHeight,
                    barWidth = barWidth,
                    color = ProteinColor,
                    cornerRadius = cornerRadius
                )
                drawAnimatedBar(
                    fraction = animatedCarbs[index],
                    left = groupLeft + barWidth + barGap,
                    plotHeight = plotHeight,
                    barWidth = barWidth,
                    color = CarbsColor,
                    cornerRadius = cornerRadius
                )
                drawAnimatedBar(
                    fraction = animatedFat[index],
                    left = groupLeft + (barWidth + barGap) * 2,
                    plotHeight = plotHeight,
                    barWidth = barWidth,
                    color = FatColor,
                    cornerRadius = cornerRadius
                )

                // X-axis day label centred under the bar group
                val labelCenter = groupLeft + barGroupWidth / 2
                val label = dayLabels.getOrElse(index) { "" }
                val measured = textMeasurer.measure(label, style = labelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = (labelCenter - measured.size.width / 2).coerceAtLeast(0f),
                        y = plotHeight + 4.dp.toPx()
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        ChartLegend()
    }
}

private fun DrawScope.drawAnimatedBar(
    fraction: Float,
    left: Float,
    plotHeight: Float,
    barWidth: Float,
    color: Color,
    cornerRadius: CornerRadius
) {
    if (fraction <= 0f) return
    val barHeight = plotHeight * fraction
    drawRoundRect(
        color = color,
        topLeft = Offset(x = left, y = plotHeight - barHeight),
        size = Size(width = barWidth, height = barHeight),
        cornerRadius = cornerRadius
    )
}

/**
 * Small horizontal legend row: Protein · Carbs · Fat swatches with labels.
 * Internal so it can be reused by [MacroYearChart] within the same module.
 */
@Composable
internal fun ChartLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = ProteinColor, label = "Protein")
        Spacer(modifier = Modifier.width(16.dp))
        LegendItem(color = CarbsColor, label = "Carbs")
        Spacer(modifier = Modifier.width(16.dp))
        LegendItem(color = FatColor, label = "Fat")
    }
}

@Composable
internal fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawRoundRect(color = color, cornerRadius = CornerRadius(3.dp.toPx()))
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MacroBarChartPreview() {
    NutriAiTheme {
        val today = LocalDate.now()
        MacroBarChart(
            summaries = (0 until 7).map { i ->
                DailyMacroSummary(
                    date = today.minusDays((6 - i).toLong()),
                    totalCalories = (1400 + i * 80).toDouble(),
                    totalProtein = (80 + i * 5).toDouble(),
                    totalCarbs = (160 + i * 10).toDouble(),
                    totalFat = (40 + i * 3).toDouble()
                )
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}
