package com.app.nutriai.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.nutriai.domain.model.MonthlyMacroSummary
import com.app.nutriai.presentation.theme.CarbsColor
import com.app.nutriai.presentation.theme.FatColor
import com.app.nutriai.presentation.theme.NutriAiTheme
import com.app.nutriai.presentation.theme.ProteinColor
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Yearly macro overview — grouped vertical bar chart with 12 months on the x-axis.
 *
 * Shows Protein, Carbs, and Fat as three side-by-side bars per month.
 * Month labels are abbreviated (Jan, Feb, …, Dec).
 * Bar heights are animated with [animateFloatAsState] on composition entry.
 *
 * This component follows the same Canvas drawing conventions as [MacroBarChart]
 * so both charts look visually consistent.
 *
 * @param summaries  Exactly 12 [MonthlyMacroSummary] entries (oldest → newest).
 * @param modifier   Layout modifier applied to the chart column.
 */
@Composable
fun MacroYearChart(
    summaries: List<MonthlyMacroSummary>,
    modifier: Modifier = Modifier
) {
    if (summaries.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)

    // y-axis scale — maximum of any single macro across all months, at least 50g
    val maxGrams = summaries.maxOf {
        maxOf(it.totalProtein, it.totalCarbs, it.totalFat)
    }.coerceAtLeast(50.0).toFloat()

    // Animate each bar fraction 0 → target
    val proteinFractions = summaries.map { (it.totalProtein / maxGrams).toFloat().coerceIn(0f, 1f) }
    val carbsFractions   = summaries.map { (it.totalCarbs   / maxGrams).toFloat().coerceIn(0f, 1f) }
    val fatFractions     = summaries.map { (it.totalFat     / maxGrams).toFloat().coerceIn(0f, 1f) }

    val animatedProtein = proteinFractions.map { target ->
        val anim by animateFloatAsState(target, animationSpec = tween(700), label = "yr_p")
        anim
    }
    val animatedCarbs = carbsFractions.map { target ->
        val anim by animateFloatAsState(target, animationSpec = tween(700), label = "yr_c")
        anim
    }
    val animatedFat = fatFractions.map { target ->
        val anim by animateFloatAsState(target, animationSpec = tween(700), label = "yr_f")
        anim
    }

    val monthLabels = remember(summaries) {
        summaries.map {
            it.yearMonth.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            val chartWidth  = size.width
            val chartHeight = size.height
            val yAxisWidth  = 36.dp.toPx()
            val xAxisHeight = 20.dp.toPx()
            val plotWidth   = chartWidth - yAxisWidth
            val plotHeight  = chartHeight - xAxisHeight

            val gridLineCount = 4
            // Horizontal gridlines + y-axis labels
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

            val monthCount     = summaries.size            // 12
            val slotWidth      = plotWidth / monthCount
            val groupPadding   = slotWidth * 0.12f
            val groupWidth     = slotWidth - groupPadding * 2
            val barCount       = 3
            val barGap         = 1.5.dp.toPx()
            val barWidth       = (groupWidth - barGap * (barCount - 1)) / barCount
            val cornerRadius   = CornerRadius(2.dp.toPx())

            summaries.forEachIndexed { index, _ ->
                val groupLeft = yAxisWidth + index * slotWidth + groupPadding

                drawYearBar(animatedProtein[index], groupLeft, plotHeight, barWidth, ProteinColor, cornerRadius)
                drawYearBar(animatedCarbs[index],   groupLeft + barWidth + barGap, plotHeight, barWidth, CarbsColor, cornerRadius)
                drawYearBar(animatedFat[index],     groupLeft + (barWidth + barGap) * 2, plotHeight, barWidth, FatColor, cornerRadius)

                // Month label centred under the bar group
                val label = monthLabels.getOrElse(index) { "" }
                val measured = textMeasurer.measure(label, style = labelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = (groupLeft + groupWidth / 2 - measured.size.width / 2)
                            .coerceIn(0f, chartWidth - measured.size.width),
                        y = plotHeight + 4.dp.toPx()
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        // Reuse the same legend as the weekly bar chart
        ChartLegend()
    }
}

private fun DrawScope.drawYearBar(
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

@Preview(showBackground = true)
@Composable
private fun MacroYearChartPreview() {
    NutriAiTheme {
        val currentMonth = YearMonth.now()
        MacroYearChart(
            summaries = (0 until 12).map { i ->
                val ym = currentMonth.minusMonths((11 - i).toLong())
                MonthlyMacroSummary(
                    yearMonth = ym,
                    totalCalories = (38000 + i * 800).toDouble(),
                    totalProtein = (2400 + i * 80).toDouble(),
                    totalCarbs = (5000 + i * 150).toDouble(),
                    totalFat = (1200 + i * 40).toDouble(),
                    daysWithData = 20 + (i % 8)
                )
            },
            modifier = Modifier.padding(16.dp)
        )
    }
}
