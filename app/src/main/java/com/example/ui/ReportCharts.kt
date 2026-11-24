package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AnalysisReport
import com.example.model.OutfitCostSection
import com.example.model.ReportMetrics
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val ChartLabelsKey = ExtraStore.Key<List<String>>()

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComparativeConfidenceVicoChart(
    report: AnalysisReport,
    modifier: Modifier = Modifier
) {
    val metrics = report.metrics
    val categories = remember(report.reportId, metrics, report.voice.audioAnalyzed) {
        buildList {
            add("Behav" to metrics.behavioralComposureScore)
            add("Face" to metrics.facialSymmetryScore)
            add("Ancst" to metrics.ancestryConfidenceScore)
            add("Groom" to metrics.groomingCareScore)
            add("Voice" to if (report.voice.audioAnalyzed) metrics.voiceClarityScore else 0)
            add("Recs" to metrics.publicRecordConfidenceScore)
        }
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(categories) {
        modelProducer.runTransaction {
            columnSeries {
                series(categories.map { it.second.toFloat() })
            }
            extras { store ->
                store[ChartLabelsKey] = categories.map { it.first }
            }
        }
    }

    val bottomAxisFormatter = remember {
        CartesianValueFormatter { context, x, _ ->
            val labels = context.model.extraStore.getOrNull(ChartLabelsKey).orEmpty()
            labels.getOrElse(x.toInt()) { "" }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("vico_comparative_confidence_chart")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cross-Module Confidence Matrix (%)",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "0–100 Scale",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            ProvideVicoTheme(rememberM3VicoTheme()) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(),
                        startAxis = VerticalAxis.rememberStart(),
                        bottomAxis = HorizontalAxis.rememberBottom(
                            valueFormatter = bottomAxisFormatter
                        )
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { (label, score) ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$label: ${if (label == "Voice" && !report.voice.audioAnalyzed) "N/A" else "$score%"}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BehavioralSignalGaugeRow(
    metrics: ReportMetrics,
    voiceAnalyzed: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("chart_behavioral_signals")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Comparative Behavioral Signal Balance",
                style = MaterialTheme.typography.titleSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularMetricGauge(
                    score = metrics.behavioralComposureScore,
                    label = "Natural Composure",
                    accentColor = MaterialTheme.colorScheme.primary,
                    size = 86.dp
                )
                CircularMetricGauge(
                    score = metrics.cognitiveLoadScore,
                    label = "Cognitive Load",
                    accentColor = MaterialTheme.colorScheme.secondary,
                    size = 86.dp
                )
                if (voiceAnalyzed) {
                    CircularMetricGauge(
                        score = metrics.voiceClarityScore,
                        label = "Voice Clarity",
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        size = 86.dp
                    )
                }
            }

            HorizontalRatioComparisonBar(
                leftLabel = "Composure (${metrics.behavioralComposureScore}%)",
                rightLabel = "Stress Load (${metrics.cognitiveLoadScore}%)",
                leftValue = metrics.behavioralComposureScore.toFloat(),
                rightValue = metrics.cognitiveLoadScore.toFloat(),
                leftColor = MaterialTheme.colorScheme.primary,
                rightColor = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun FacialAnthropometricBalanceChart(
    metrics: ReportMetrics,
    faceShape: String,
    modifier: Modifier = Modifier
) {
    val axisPoints = remember(metrics) {
        listOf(
            "Symmetry" to metrics.facialSymmetryScore,
            "Trait Clarity" to metrics.ancestryConfidenceScore,
            "Expression" to metrics.behavioralComposureScore,
            "Grooming Fit" to metrics.groomingCareScore
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("chart_facial_anthropometric")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MorphologyRadarCanvas(
                axes = axisPoints,
                accentColor = MaterialTheme.colorScheme.tertiary,
                gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                modifier = Modifier.size(110.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Shape: $faceShape",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                axisPoints.forEach { (title, score) ->
                    AnimatedHorizontalMetricBar(
                        label = title,
                        score = score,
                        barColor = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
fun GroomingAndOutfitCostChart(
    metrics: ReportMetrics,
    outfitCost: OutfitCostSection,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("chart_grooming_and_cost")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Presentation Metrics & Retail Benchmark Range",
                style = MaterialTheme.typography.titleSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CircularMetricGauge(
                    score = metrics.groomingCareScore,
                    label = "Grooming Care",
                    accentColor = MaterialTheme.colorScheme.primary,
                    size = 84.dp
                )
                CircularMetricGauge(
                    score = metrics.styleCoordinationScore,
                    label = "Style Cohesion",
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    size = 84.dp
                )
            }

            if (outfitCost.items.isNotEmpty()) {
                val itemLabels = remember(outfitCost.items) {
                    outfitCost.items.take(6).map { item ->
                        item.category.take(6).ifBlank { item.itemName.take(6) }
                    }
                }
                val priceProducer = remember { CartesianChartModelProducer() }
                LaunchedEffect(outfitCost.items) {
                    priceProducer.runTransaction {
                        columnSeries {
                            series(outfitCost.items.take(6).map { it.minPriceUsd.toFloat() })
                            series(outfitCost.items.take(6).map { it.maxPriceUsd.toFloat() })
                        }
                        extras { store ->
                            store[ChartLabelsKey] = itemLabels
                        }
                    }
                }

                val itemAxisFormatter = remember {
                    CartesianValueFormatter { context, x, _ ->
                        val labels = context.model.extraStore.getOrNull(ChartLabelsKey).orEmpty()
                        labels.getOrElse(x.toInt()) { "" }
                    }
                }

                Text(
                    text = "Visible Garment Price Comparison (Min vs. Max USD)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                ProvideVicoTheme(rememberM3VicoTheme()) {
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberColumnCartesianLayer(),
                            startAxis = VerticalAxis.rememberStart(),
                            bottomAxis = HorizontalAxis.rememberBottom(
                                valueFormatter = itemAxisFormatter
                            )
                        ),
                        modelProducer = priceProducer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PublicRecordCoverageChart(
    metrics: ReportMetrics,
    sourceCount: Int,
    hasDetectedPlate: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("chart_public_record_coverage")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularMetricGauge(
                score = metrics.publicRecordConfidenceScore,
                label = "Record Coverage",
                accentColor = MaterialTheme.colorScheme.secondary,
                size = 90.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedHorizontalMetricBar(
                    label = "Search Grounding (${sourceCount} src)",
                    score = (sourceCount * 25).coerceIn(15, 100),
                    barColor = MaterialTheme.colorScheme.secondary
                )
                AnimatedHorizontalMetricBar(
                    label = if (hasDetectedPlate) "Plate OCR / Format" else "Plate Context",
                    score = if (hasDetectedPlate) 92 else 25,
                    barColor = MaterialTheme.colorScheme.primary
                )
                AnimatedHorizontalMetricBar(
                    label = "DPPA / Portal Alignment",
                    score = metrics.publicRecordConfidenceScore,
                    barColor = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun CircularMetricGauge(
    score: Int,
    label: String,
    accentColor: Color,
    size: Dp
) {
    val clamped = score.coerceIn(0, 100)
    val animatedSweep by animateFloatAsState(
        targetValue = (clamped / 100f) * 270f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "gauge_$label"
    )
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.semantics { contentDescription = "$label score $clamped percent" }
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val strokeWidth = 8.dp.toPx()
                val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                drawArc(
                    color = trackColor,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = accentColor,
                    startAngle = 135f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Text(
                text = "$clamped%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HorizontalRatioComparisonBar(
    leftLabel: String,
    rightLabel: String,
    leftValue: Float,
    rightValue: Float,
    leftColor: Color,
    rightColor: Color
) {
    val total = max(1f, leftValue + rightValue)
    val leftRatio = (leftValue / total).coerceIn(0.1f, 0.9f)
    val animatedRatio by animateFloatAsState(
        targetValue = leftRatio,
        animationSpec = tween(durationMillis = 600),
        label = "split_bar"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            val splitX = size.width * animatedRatio
            drawRoundRect(
                color = leftColor,
                topLeft = Offset.Zero,
                size = Size(max(4f, splitX - 2.dp.toPx()), size.height),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            drawRoundRect(
                color = rightColor,
                topLeft = Offset(splitX + 2.dp.toPx(), 0f),
                size = Size(max(4f, size.width - splitX - 2.dp.toPx()), size.height),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendDotText(color = leftColor, text = leftLabel)
            LegendDotText(color = rightColor, text = rightLabel)
        }
    }
}

@Composable
private fun LegendDotText(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AnimatedHorizontalMetricBar(
    label: String,
    score: Int,
    barColor: Color
) {
    val clamped = score.coerceIn(0, 100)
    val progress by animateFloatAsState(
        targetValue = clamped / 100f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "bar_$label"
    )
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$clamped%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = barColor
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            drawRoundRect(
                color = trackColor,
                size = size,
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawRoundRect(
                color = barColor,
                size = Size(size.width * progress, size.height),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}

@Composable
private fun MorphologyRadarCanvas(
    axes: List<Pair<String, Int>>,
    accentColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val count = max(3, axes.size)
        val radius = min(size.width, size.height) * 0.42f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Concentric grid rings
        listOf(0.4f, 0.7f, 1.0f).forEach { scale ->
            val ringPath = Path()
            for (i in 0 until count) {
                val angle = (-Math.PI / 2.0) + (2.0 * Math.PI * i / count)
                val x = center.x + (radius * scale * cos(angle)).toFloat()
                val y = center.y + (radius * scale * sin(angle)).toFloat()
                if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
            }
            ringPath.close()
            drawPath(
                path = ringPath,
                color = gridColor,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Radial spokes
        for (i in 0 until count) {
            val angle = (-Math.PI / 2.0) + (2.0 * Math.PI * i / count)
            drawLine(
                color = gridColor,
                start = center,
                end = Offset(
                    center.x + (radius * cos(angle)).toFloat(),
                    center.y + (radius * sin(angle)).toFloat()
                ),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Data polygon
        val dataPath = Path()
        axes.forEachIndexed { idx, (_, rawScore) ->
            val normalized = (rawScore.coerceIn(10, 100) / 100f)
            val angle = (-Math.PI / 2.0) + (2.0 * Math.PI * idx / count)
            val x = center.x + (radius * normalized * cos(angle)).toFloat()
            val y = center.y + (radius * normalized * sin(angle)).toFloat()
            if (idx == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()

        drawPath(
            path = dataPath,
            color = accentColor.copy(alpha = 0.28f)
        )
        drawPath(
            path = dataPath,
            color = accentColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
