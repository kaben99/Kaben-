package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AnalysisReport
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportCard(
    report: AnalysisReport,
    modifier: Modifier = Modifier,
    onOpenExternalUri: ((String) -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val openUrl: (String) -> Unit = { url ->
        if (onOpenExternalUri != null) {
            onOpenExternalUri(url)
        } else {
            runCatching { uriHandler.openUri(url) }
        }
    }

    var behavioralExpanded by rememberSaveable(report.reportId) { mutableStateOf(true) }
    var facialExpanded by rememberSaveable(report.reportId) { mutableStateOf(true) }
    var groomingExpanded by rememberSaveable(report.reportId) { mutableStateOf(true) }
    var publicRecordExpanded by rememberSaveable(report.reportId) { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("report_card_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header_${report.reportId}") {
            ReportCardHeaderBanner(report = report)
        }

        item(key = "overview_chart_${report.reportId}") {
            ComparativeConfidenceVicoChart(report = report)
        }

        item(key = "behavioral_${report.reportId}") {
            ReportCardSectionContainer(
                sectionNumber = "01",
                title = "Behavioral Indicators",
                subtitle = if (report.voice.audioAnalyzed) {
                    "Gaze, micro-expressions, cognitive load & voice cues"
                } else {
                    "Gaze, brow tension, lip cues & cognitive load signals"
                },
                icon = Icons.Filled.Psychology,
                accentColor = MaterialTheme.colorScheme.primary,
                expanded = behavioralExpanded,
                onToggleExpanded = { behavioralExpanded = !behavioralExpanded },
                testTag = "section_behavioral_indicators"
            ) {
                BehavioralSignalGaugeRow(
                    metrics = report.metrics,
                    voiceAnalyzed = report.voice.audioAnalyzed
                )
                SectionDetailBlock("Eye Gaze & Blinking", report.behavioral.gazeAndBlinking)
                SectionDetailBlock("Eyebrow & Forehead Tension", report.behavioral.eyebrowAndForehead)
                SectionDetailBlock("Mouth & Lip Movement", report.behavioral.mouthAndLipMovement)
                SectionDetailBlock(
                    "Cognitive Load vs. Natural Cues",
                    report.behavioral.cognitiveLoadVsNaturalSignals
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Probabilistic Analytical Note",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = report.behavioral.analyticalProbabilityNote,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                SectionDetailBlock(
                    "Research & Interviewing Context",
                    report.behavioral.researchContext
                )

                if (report.voice.audioAnalyzed) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Acoustic & Voice Communication Cues",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    SectionDetailBlock("Tone & Pitch", report.voice.toneAndPitch)
                    SectionDetailBlock("Pace & Pauses", report.voice.paceAndPauses)
                    SectionDetailBlock(
                        "Credibility & Vocal Tension",
                        report.voice.communicationCredibilityNote
                    )
                }
            }
        }

        item(key = "facial_${report.reportId}") {
            ReportCardSectionContainer(
                sectionNumber = "02",
                title = "Facial & Anthropometric Notes",
                subtitle = "Morphological shape, proportions, ancestry & non-diagnostic traits",
                icon = Icons.Filled.Face,
                accentColor = MaterialTheme.colorScheme.tertiary,
                expanded = facialExpanded,
                onToggleExpanded = { facialExpanded = !facialExpanded },
                testTag = "section_facial_anthropometric_notes"
            ) {
                FacialAnthropometricBalanceChart(
                    metrics = report.metrics,
                    faceShape = report.facialStructure.faceShapeClassification
                )

                SectionDetailBlock(
                    "Proportions & Symmetry",
                    report.facialStructure.proportionsAndSymmetry
                )
                SectionDetailBlock(
                    "Visual Landmarks",
                    report.facialStructure.landmarkObservations
                )
                SectionDetailBlock(
                    "Morphological Reference",
                    report.facialStructure.anthropometricReference
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Text(
                    text = "Regional & Ancestry Visual Estimation",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                report.ancestry.traitEstimates.forEach { trait ->
                    SectionBulletRow(text = trait)
                }
                SectionDetailBlock("Diversity Context", report.ancestry.diversityNote)

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Observational non-diagnostic note",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Observational Clinical Notes (Non-Diagnostic)",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Text(
                            text = report.medicalScreening.craniofacialObservations,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = report.medicalScreening.neurodevelopmentalObservations,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = report.medicalScreening.nonDiagnosticDisclaimer,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(key = "grooming_${report.reportId}") {
            ReportCardSectionContainer(
                sectionNumber = "03",
                title = "Grooming & Style Insights",
                subtitle = "Apparel coordination, self-care, psychology & outfit cost",
                icon = Icons.Filled.Checkroom,
                accentColor = MaterialTheme.colorScheme.primary,
                expanded = groomingExpanded,
                onToggleExpanded = { groomingExpanded = !groomingExpanded },
                testTag = "section_grooming_style_insights"
            ) {
                GroomingAndOutfitCostChart(
                    metrics = report.metrics,
                    outfitCost = report.outfitCost
                )
                SectionDetailBlock(
                    "Clothing Style & Color Coordination",
                    report.grooming.clothingStyleAndCoordination
                )
                SectionDetailBlock(
                    "Trend & Personal Alignment",
                    report.grooming.trendAndPersonalAlignment
                )
                SectionDetailBlock(
                    "Hair, Facial Grooming & Makeup",
                    report.grooming.hairFacialAndMakeupCare
                )
                SectionDetailBlock(
                    "Self-Care Presentation",
                    report.grooming.selfCarePresentation
                )
                SectionDetailBlock(
                    "Psychological & Lifestyle Insights",
                    report.grooming.psychologicalInsights
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingBag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Estimated Outfit & Footwear Cost",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "${report.outfitCost.items.size} benchmarked item(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = formatCardUsdRange(
                                report.outfitCost.totalMinUsd,
                                report.outfitCost.totalMaxUsd
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (report.outfitCost.items.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        report.outfitCost.items.forEach { garment ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = "${garment.itemName} (${formatCardUsdRange(garment.minPriceUsd, garment.maxPriceUsd)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "public_record_${report.reportId}") {
            ReportCardSectionContainer(
                sectionNumber = "04",
                title = "Public Record Summary",
                subtitle = "FBI watchlist grounding, license plate lookup & verified sources",
                icon = Icons.Filled.Policy,
                accentColor = MaterialTheme.colorScheme.secondary,
                expanded = publicRecordExpanded,
                onToggleExpanded = { publicRecordExpanded = !publicRecordExpanded },
                testTag = "section_public_record_summary"
            ) {
                val hasPlate = report.licensePlate.detectedPlateNumber.isNotBlank() &&
                    !report.licensePlate.detectedPlateNumber.equals("None provided", ignoreCase = true)
                PublicRecordCoverageChart(
                    metrics = report.metrics,
                    sourceCount = report.groundingSources.size,
                    hasDetectedPlate = hasPlate
                )
                SectionDetailBlock("Search Query Basis", report.watchlist.queryBasis)
                SectionDetailBlock(
                    "FBI & Public Record Summary",
                    report.watchlist.fbiAndPublicRecordSummary
                )
                SectionDetailBlock(
                    "Potential Matches or Leads",
                    report.watchlist.potentialMatchesOrLeads
                )
                SectionDetailBlock(
                    "Verification Guidance",
                    report.watchlist.verificationGuidance
                )

                OutlinedButton(
                    onClick = { openUrl("https://www.fbi.gov/wanted") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("report_card_fbi_button"),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Verify on Official FBI Wanted Directory",
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open FBI Wanted directory",
                        modifier = Modifier.size(16.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Plate ${report.licensePlate.detectedPlateNumber} (${report.licensePlate.detectedJurisdiction})",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                SectionDetailBlock(
                    "DC, Maryland & Virginia Summary",
                    report.licensePlate.dcMdVaSummary
                )
                SectionDetailBlock(
                    "Parking, HOV & Toll Notes",
                    report.licensePlate.parkingAndTollsNote
                )
                SectionDetailBlock(
                    "DMV / DPPA Portal Requirements",
                    report.licensePlate.officialPortalRequirement
                )

                if (report.groundingSources.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    Text(
                        text = "Grounded Web Sources (${report.groundingSources.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        report.groundingSources.forEach { source ->
                            OutlinedButton(
                                onClick = { openUrl(source.uri) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = source.title,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open grounded web source",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportCardHeaderBanner(report: AnalysisReport) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    )
    Card(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_card_header_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Multimodal Analytical Dossier",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "Report #${report.reportId} • ${report.timestampFormatted}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HeaderBadge(label = "Confidence: ${report.overallConfidence}")
                HeaderBadge(label = report.jurisdiction.displayName)
                HeaderBadge(label = report.usageMode.displayName)
            }

            Text(
                text = report.executiveSummary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun HeaderBadge(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ReportCardSectionContainer(
    sectionNumber: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .clickable(onClick = onToggleExpanded)
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SECTION $sectionNumber",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    content()
                }
            }
        }
    }
}

@Composable
private fun SectionDetailBlock(label: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SectionBulletRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatCardUsdRange(minVal: Double, maxVal: Double): String {
    val low = minVal.toInt()
    val high = maxVal.toInt()
    return if (low == high) {
        String.format(Locale.US, "$%d", low)
    } else {
        String.format(Locale.US, "$%d–$%d", low, high)
    }
}
