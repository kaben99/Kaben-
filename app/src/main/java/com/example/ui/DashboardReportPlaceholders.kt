package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AnalysisReport

data class ReportCardPlaceholderSpec(
    val tag: String,
    val title: String,
    val requiredInputLabel: String,
    val placeholderDescription: String,
    val icon: ImageVector,
    val isReady: Boolean,
    val populatedSummary: String? = null
)

@Composable
fun AnalyticalReportPlaceholderCard(
    spec: ReportCardPlaceholderSpec,
    accentColor: Color,
    onClick: (() -> Unit)? = null
) {
    val cardContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = spec.icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = spec.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = when {
                        spec.populatedSummary != null -> MaterialTheme.colorScheme.tertiaryContainer
                        spec.isReady -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when {
                            spec.populatedSummary != null -> "Analyzed"
                            spec.isReady -> "Input Ready"
                            else -> spec.requiredInputLabel
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = spec.populatedSummary ?: spec.placeholderDescription,
                style = MaterialTheme.typography.bodySmall,
                color = if (spec.populatedSummary != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(spec.tag)
        ) {
            cardContent()
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(spec.tag)
        ) {
            cardContent()
        }
    }
}

fun buildDashboardReportCards(
    hasVisualMedia: Boolean,
    hasAudio: Boolean,
    hasPlateOrContext: Boolean,
    hasSubjectContext: Boolean,
    lastReport: AnalysisReport?
): List<ReportCardPlaceholderSpec> = listOf(
    ReportCardPlaceholderSpec(
        tag = "placeholder_behavioral_card",
        title = "Face, Eye & Mouth Signals",
        requiredInputLabel = "Awaiting Photo/Video",
        placeholderDescription = "Behavioral psychology & non-verbal analysis of gaze, blinking, brow tension, lip cues, cognitive load vs. natural expression.",
        icon = Icons.Filled.Psychology,
        isReady = hasVisualMedia,
        populatedSummary = lastReport?.behavioral?.analyticalProbabilityNote
    ),
    ReportCardPlaceholderSpec(
        tag = "placeholder_facial_shape_card",
        title = "Facial Shape & Anthropometry",
        requiredInputLabel = "Awaiting Photo/Video",
        placeholderDescription = "Morphological classification (oval, round, square, oblong, heart, diamond) and facial landmark proportions.",
        icon = Icons.Filled.Face,
        isReady = hasVisualMedia,
        populatedSummary = lastReport?.let {
            "${it.facialStructure.faceShapeClassification} — ${it.facialStructure.proportionsAndSymmetry}"
        }
    ),
    ReportCardPlaceholderSpec(
        tag = "placeholder_ancestry_card",
        title = "Ancestry & Regional Visual Estimate",
        requiredInputLabel = "Awaiting Photo/Video",
        placeholderDescription = "High-level visual regional trait probability estimates with explicit human phenotypic diversity context.",
        icon = Icons.Filled.Public,
        isReady = hasVisualMedia,
        populatedSummary = lastReport?.ancestry?.traitEstimates?.joinToString(" • ")
    ),
    ReportCardPlaceholderSpec(
        tag = "placeholder_medical_card",
        title = "Medical & Neurodevelopmental Notes",
        requiredInputLabel = "Awaiting Photo/Video",
        placeholderDescription = "Non-diagnostic informational observations referencing clinical literature (e.g., craniofacial or behavioral traits) with clinician guidance.",
        icon = Icons.Filled.HealthAndSafety,
        isReady = hasVisualMedia,
        populatedSummary = lastReport?.medicalScreening?.craniofacialObservations
    ),
    ReportCardPlaceholderSpec(
        tag = "placeholder_grooming_card",
        title = "Grooming, Style & Psychology",
        requiredInputLabel = "Awaiting Photo/Video",
        placeholderDescription = "Evaluation of clothing coordination, hair/beard/makeup presentation, self-care habits, and psychological routine insights.",
        icon = Icons.Filled.Checkroom,
        isReady = hasVisualMedia,
        populatedSummary = lastReport?.grooming?.psychologicalInsights
    ),
    ReportCardPlaceholderSpec(
        tag = "placeholder_voice_card",
        title = "Voice Tone & Acoustic Analysis",
        requiredInputLabel = "Awaiting Audio",
        placeholderDescription = "Speech pitch, pace, pauses, vocal tension, and probabilistic communication credibility cues.",
        icon = Icons.Filled.GraphicEq,
        isReady = hasAudio,
        populatedSummary = lastReport?.voice?.communicationCredibilityNote
    ),
    ReportCardPlaceholderSpec(
        tag = "placeholder_outfit_cost_card",
        title = "Clothing & Footwear Cost Estimate",
        requiredInputLabel = "Awaiting Photo/Video",
        placeholderDescription = "Itemized garments, shoes, and accessories benchmarked against Amazon/retail pricing with calculated total USD range.",
        icon = Icons.Filled.ShoppingBag,
        isReady = hasVisualMedia,
        populatedSummary = lastReport?.let {
            "${it.outfitCost.items.size} items • Est. $${it.outfitCost.totalMinUsd.toInt()}–$${it.outfitCost.totalMaxUsd.toInt()} USD"
        }
    ),
    ReportCardPlaceholderSpec(
        tag = "placeholder_watchlist_card",
        title = "FBI & Public Watchlist Check",
        requiredInputLabel = "Optional Details",
        placeholderDescription = "Web-grounded public search across FBI Wanted directories and open public records with source citations.",
        icon = Icons.Filled.Policy,
        isReady = hasSubjectContext || hasVisualMedia,
        populatedSummary = lastReport?.watchlist?.fbiAndPublicRecordSummary
    ),
    ReportCardPlaceholderSpec(
        tag = "placeholder_plate_card",
        title = "License Plate Lookup (DC / MD / VA)",
        requiredInputLabel = "Awaiting Plate",
        placeholderDescription = "Plate OCR & public citation search for Washington, D.C., Maryland, and Virginia parking, HOV/tolls, traffic & DMV links.",
        icon = Icons.Filled.DirectionsCar,
        isReady = hasPlateOrContext,
        populatedSummary = lastReport?.let {
            "${it.licensePlate.detectedPlateNumber} (${it.licensePlate.detectedJurisdiction}) — ${it.licensePlate.dcMdVaSummary}"
        }
    )
)

@Composable
fun SavedHistorySheetContent(
    savedReports: List<AnalysisReport>,
    onOpenReport: (AnalysisReport) -> Unit,
    onDeleteReport: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Saved Analysis History",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${savedReports.size} report(s) stored locally",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (savedReports.isNotEmpty()) {
                androidx.compose.material3.TextButton(
                    onClick = onClearHistory,
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Text("Clear All")
                }
            }
        }

        if (savedReports.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No saved reports yet. Run a multimodal analysis and it will be automatically saved here for offline review.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    count = savedReports.size,
                    key = { idx -> savedReports[idx].reportId }
                ) { idx ->
                    val report = savedReports[idx]
                    Card(
                        onClick = { onOpenReport(report) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_report_${report.reportId}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Report #${report.reportId} • ${report.timestampFormatted}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "${report.jurisdiction.displayName} • ${report.usageMode.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = report.executiveSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            androidx.compose.material3.IconButton(
                                onClick = { onDeleteReport(report.reportId) },
                                modifier = Modifier.testTag("delete_history_${report.reportId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Delete saved report ${report.reportId}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
