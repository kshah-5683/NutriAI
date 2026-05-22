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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.nutriai.domain.model.DailyMacroSummary
import com.app.nutriai.domain.model.MacroGoals
import com.app.nutriai.presentation.theme.CarbsColor
import com.app.nutriai.presentation.theme.FatColor
import com.app.nutriai.presentation.theme.NutriAiTheme
import com.app.nutriai.presentation.theme.ProteinColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Monthly macro trend line chart rendered with Compose [Canvas].
 *
 * Draws smooth cubic Bézier lines for Protein, Carbs, and Fat over 30 days.
 * Dashed horizontal goal reference lines are drawn for each macro from [macroGoals].
 * X-axis labels appear every 5 days; y-axis shows a gram scale with gridlines.
 *
 * Reveal animation: all lines animate from 0 to full opacity via [animateFloatAsState].
 *
 * @param summaries  30 [DailyMacroSummary] entries (oldest → newest), zero-filled for empty days.
 * @param macroGoals User-configured daily targets — drawn as dashed goal reference lines.
 * @param modifier   Layout modifier applied to the chart column.
 */
@Composable
fun MacroLineChart(
    summaries: List<DailyMacroSummary>,
    macroGoals: MacroGoals,
    modifier: Modifier = Modifier
) {
    if (summaries.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)
    val goalDash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

    // Compute y-axis max: max observed grams, at least the largest goal, minimum 50g
    val maxData = summaries.maxOf {
        maxOf(it.totalProtein, it.totalCarbs, it.totalFat)
    }
    val maxGoal = maxOf(macroGoals.proteinGoal, macroGoals.carbsGoal, macroGoals.fatGoal)
    val maxGrams = maxOf(maxData, maxGoal, 50.0).toFloat()

    // Animate reveal: fraction goes 0 → 1 when the composable first enters
    val revealAnim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
        label = "line_reveal"
    )

    val xAxisFormatter = remember { DateTimeFormatter.ofPattern("d MMM") }
    val xLabels = remember(summaries) {
        summaries.mapIndexed { index, summary ->
            if (index % 5 == 0 || index == summaries.lastIndex) {
                summary.date.format(xAxisFormatter)
            } else null
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val chartWidth = size.width
            val chartHeight = size.height
            val yAxisWidth = 40.dp.toPx()
            val xAxisHeight = 20.dp.toPx()
            val plotWidth = chartWidth - yAxisWidth
            val plotHeight = chartHeight - xAxisHeight

            val gridLineCount = 4
            // Gridlines + y-axis labels
            for (i in 0..gridLineCount) {
                val fraction = i.toFloat() / gridLineCount
                val y = plotHeight * (1f - fraction)
                val gramsLabel = (maxGrams * fraction).roundToInt().toString()

                drawLine(
                    color = gridColor,
                    start = Offset(yAxisWidth, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 0.8.dp.toPx()
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

            // Goal reference lines (dashed)
            drawGoalLine(
                grams = macroGoals.proteinGoal.toFloat(),
                maxGrams = maxGrams,
                plotHeight = plotHeight,
                chartWidth = chartWidth,
                yAxisWidth = yAxisWidth,
                color = ProteinColor.copy(alpha = 0.45f),
                pathEffect = goalDash
            )
            drawGoalLine(
                grams = macroGoals.carbsGoal.toFloat(),
                maxGrams = maxGrams,
                plotHeight = plotHeight,
                chartWidth = chartWidth,
                yAxisWidth = yAxisWidth,
                color = CarbsColor.copy(alpha = 0.45f),
                pathEffect = goalDash
            )
            drawGoalLine(
                grams = macroGoals.fatGoal.toFloat(),
                maxGrams = maxGrams,
                plotHeight = plotHeight,
                chartWidth = chartWidth,
                yAxisWidth = yAxisWidth,
                color = FatColor.copy(alpha = 0.45f),
                pathEffect = goalDash
            )

            val count = summaries.size

            // Helper: x position of a data point
            fun xFor(index: Int): Float =
                yAxisWidth + (index.toFloat() / (count - 1).coerceAtLeast(1)) * plotWidth

            // Helper: y position of a value
            fun yFor(value: Double): Float =
                plotHeight * (1f - (value / maxGrams).toFloat().coerceIn(0f, 1f))

            // Draw smooth cubic Bézier lines for each macro
            drawMacroLine(
                values = summaries.map { it.totalProtein },
                color = ProteinColor,
                alpha = revealAnim,
                plotHeight = plotHeight,
                maxGrams = maxGrams,
                count = count,
                yAxisWidth = yAxisWidth,
                plotWidth = plotWidth,
                xFor = ::xFor,
                yFor = ::yFor
            )
            drawMacroLine(
                values = summaries.map { it.totalCarbs },
                color = CarbsColor,
                alpha = revealAnim,
                plotHeight = plotHeight,
                maxGrams = maxGrams,
                count = count,
                yAxisWidth = yAxisWidth,
                plotWidth = plotWidth,
                xFor = ::xFor,
                yFor = ::yFor
            )
            drawMacroLine(
                values = summaries.map { it.totalFat },
                color = FatColor,
                alpha = revealAnim,
                plotHeight = plotHeight,
                maxGrams = maxGrams,
                count = count,
                yAxisWidth = yAxisWidth,
                plotWidth = plotWidth,
                xFor = ::xFor,
                yFor = ::yFor
            )

            // X-axis labels every 5 days
            summaries.forEachIndexed { index, _ ->
                val label = xLabels.getOrNull(index) ?: return@forEachIndexed
                val x = xFor(index)
                val measured = textMeasurer.measure(label, style = labelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = (x - measured.size.width / 2).coerceIn(0f, chartWidth - measured.size.width),
                        y = plotHeight + 4.dp.toPx()
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        // Legend row (reuses the same ChartLegend from MacroBarChart via shared private composable)
        LineChartLegend(macroGoals = macroGoals)
    }
}

private fun DrawScope.drawGoalLine(
    grams: Float,
    maxGrams: Float,
    plotHeight: Float,
    chartWidth: Float,
    yAxisWidth: Float,
    color: Color,
    pathEffect: PathEffect
) {
    val y = plotHeight * (1f - (grams / maxGrams).coerceIn(0f, 1f))
    drawLine(
        color = color,
        start = Offset(yAxisWidth, y),
        end = Offset(chartWidth, y),
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = pathEffect
    )
}

private fun DrawScope.drawMacroLine(
    values: List<Double>,
    color: Color,
    alpha: Float,
    plotHeight: Float,
    maxGrams: Float,
    count: Int,
    yAxisWidth: Float,
    plotWidth: Float,
    xFor: (Int) -> Float,
    yFor: (Double) -> Float
) {
    if (values.size < 2) return

    val path = Path()
    path.moveTo(xFor(0), yFor(values[0]))

    for (i in 1 until count) {
        val x0 = xFor(i - 1)
        val y0 = yFor(values[i - 1])
        val x1 = xFor(i)
        val y1 = yFor(values[i])
        // Cubic Bézier with horizontal control points for smooth curves
        val cpX = (x0 + x1) / 2f
        path.cubicTo(cpX, y0, cpX, y1, x1, y1)
    }

    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

@Composable
private fun LineChartLegend(macroGoals: MacroGoals) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendLineItem(color = ProteinColor, label = "Protein (goal: ${macroGoals.proteinGoal.toInt()}g)")
        Spacer(modifier = Modifier.width(12.dp))
        LegendLineItem(color = CarbsColor, label = "Carbs (${macroGoals.carbsGoal.toInt()}g)")
        Spacer(modifier = Modifier.width(12.dp))
        LegendLineItem(color = FatColor, label = "Fat (${macroGoals.fatGoal.toInt()}g)")
    }
}

@Composable
private fun LegendLineItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier
            .width(16.dp)
            .height(3.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = size.height,
                cap = StrokeCap.Round
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MacroLineChartPreview() {
    NutriAiTheme {
        val today = LocalDate.now()
        MacroLineChart(
            summaries = (0 until 30).map { i ->
                DailyMacroSummary(
                    date = today.minusDays((29 - i).toLong()),
                    totalCalories = (1600 + (i % 5) * 100).toDouble(),
                    totalProtein = (90 + (i % 7) * 8).toDouble(),
                    totalCarbs = (180 + (i % 6) * 12).toDouble(),
                    totalFat = (45 + (i % 4) * 6).toDouble()
                )
            },
            macroGoals = MacroGoals(),
            modifier = Modifier.padding(16.dp)
        )
    }
}
