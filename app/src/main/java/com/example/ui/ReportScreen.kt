package com.example.ui

import android.content.Intent
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.AnalysisReport
import com.example.model.GarmentEstimate
import java.util.Locale

private enum class ReportTab(val label: String) {
    REPORT_CARD("Report Card"),
    OVERVIEW("Overview"),
    FACE_BEHAVIOR("Face & Behavior"),
    MEDICAL_STYLE("Medical & Style"),
    VOICE_COST("Voice & Cost"),
    RECORDS_PLATE("Records & Plate")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    report: AnalysisReport,
    exportText: String,
    onBackToStudio: () -> Unit
) {
    BackHandler { onBackToStudio() }

    val context = LocalContext.current
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBackToStudio,
                        modifier = Modifier.testTag("report_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to upload studio"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Report #${report.reportId}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${report.timestampFormatted} • ${report.usageMode.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Clarity Lens Report #${report.reportId}")
                                putExtra(Intent.EXTRA_TEXT, exportText)
                            }
                            context.startActivity(
                                Intent.createChooser(sendIntent, "Export Analytical Report")
                            )
                        },
                        modifier = Modifier.testTag("export_report_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share or export analytical report"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ReportTab.entries.forEachIndexed { idx, tab ->
                    Tab(
                        selected = selectedTabIndex == idx,
                        onClick = { selectedTabIndex = idx },
                        text = { Text(tab.label) },
                        modifier = Modifier.testTag("report_tab_${tab.name.lowercase()}")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                when (ReportTab.entries[selectedTabIndex]) {
                    ReportTab.REPORT_CARD -> ReportCard(report = report)
                    ReportTab.OVERVIEW -> OverviewTabContent(report)
                    ReportTab.FACE_BEHAVIOR -> FaceAndBehaviorTabContent(report)
                    ReportTab.MEDICAL_STYLE -> MedicalAndStyleTabContent(report)
                    ReportTab.VOICE_COST -> VoiceAndCostTabContent(report)
                    ReportTab.RECORDS_PLATE -> RecordsAndPlateTabContent(report)
                }
            }
        }
    }
}

@Composable
private fun OverviewTabContent(report: AnalysisReport) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ReportModuleCard(
                title = "Executive Analytical Summary",
                icon = Icons.Filled.Verified,
                accentColor = MaterialTheme.colorScheme.primary
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Confidence: ${report.overallConfidence}  •  Jurisdiction: ${report.jurisdiction.displayName}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = report.executiveSummary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        item {
            ReportModuleCard(
                title = "Methodological Limitations & Scope",
                icon = Icons.Filled.Info,
                accentColor = MaterialTheme.colorScheme.secondary
            ) {
                report.keyLimitations.forEach { limitation ->
                    BulletLine(text = limitation)
                }
            }
        }

        item {
            ReportModuleCard(
                title = "Live Search & Deep Research Synthesis",
                icon = Icons.Filled.Public,
                accentColor = MaterialTheme.colorScheme.tertiary
            ) {
                Text(
                    text = report.groundedResearchBrief,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (report.groundingSources.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Verified Web Sources",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        report.groundingSources.forEach { source ->
                            OutlinedButton(
                                onClick = {
                                    runCatching { uriHandler.openUri(source.uri) }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = source.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open source link",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                report.searchEntryPointHtml?.let { htmlContent ->
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = false
                                    loadDataWithBaseURL(
                                        "https://www.google.com",
                                        htmlContent,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp, max = 140.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FaceAndBehaviorTabContent(report: AnalysisReport) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ReportModuleCard(
                title = "Multi-Angle Face, Eye & Mouth Signals",
                icon = Icons.Filled.Psychology,
                accentColor = MaterialTheme.colorScheme.primary
            ) {
                LabeledParagraph("Eye Gaze & Blinking", report.behavioral.gazeAndBlinking)
                LabeledParagraph("Eyebrow & Forehead Tension", report.behavioral.eyebrowAndForehead)
                LabeledParagraph("Mouth & Lip Movement", report.behavioral.mouthAndLipMovement)
                LabeledParagraph("Cognitive Load vs. Natural Cues", report.behavioral.cognitiveLoadVsNaturalSignals)
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Analytical Probability Note",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = report.behavioral.analyticalProbabilityNote,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                LabeledParagraph("Behavioral & Forensic Research Context", report.behavioral.researchContext)
            }
        }

        item {
            ReportModuleCard(
                title = "Facial Shape & Anthropometric Structure",
                icon = Icons.Filled.Face,
                accentColor = MaterialTheme.colorScheme.tertiary
            ) {
                LabeledParagraph("Classification", report.facialStructure.faceShapeClassification)
                LabeledParagraph("Proportions & Symmetry", report.facialStructure.proportionsAndSymmetry)
                LabeledParagraph("Visual Landmarks", report.facialStructure.landmarkObservations)
                LabeledParagraph("Morphological Reference", report.facialStructure.anthropometricReference)
            }
        }

        item {
            ReportModuleCard(
                title = "Ancestry / Regional Visual Estimation",
                icon = Icons.Filled.Public,
                accentColor = MaterialTheme.colorScheme.secondary
            ) {
                report.ancestry.traitEstimates.forEach { estimate ->
                    BulletLine(text = estimate)
                }
                Spacer(Modifier.height(8.dp))
                LabeledParagraph("Confidence & Visual Nuance", report.ancestry.confidenceAndNuance)
                LabeledParagraph("Human Diversity Context", report.ancestry.diversityNote)
            }
        }
    }
}

@Composable
private fun MedicalAndStyleTabContent(report: AnalysisReport) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ReportModuleCard(
                title = "Medical & Neurodevelopmental Screening Notes",
                icon = Icons.Filled.HealthAndSafety,
                accentColor = MaterialTheme.colorScheme.secondary
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Informational disclaimer",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = report.medicalScreening.nonDiagnosticDisclaimer,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                LabeledParagraph("Craniofacial Observations", report.medicalScreening.craniofacialObservations)
                LabeledParagraph("Neurodevelopmental Trait Notes", report.medicalScreening.neurodevelopmentalObservations)
                LabeledParagraph("Clinical Literature Context", report.medicalScreening.clinicalReferenceContext)
            }
        }

        item {
            ReportModuleCard(
                title = "Grooming, Fashion, Self-Care & Psychology",
                icon = Icons.Filled.Checkroom,
                accentColor = MaterialTheme.colorScheme.primary
            ) {
                LabeledParagraph("Clothing Style & Color Coordination", report.grooming.clothingStyleAndCoordination)
                LabeledParagraph("Fashion Trend & Personal Alignment", report.grooming.trendAndPersonalAlignment)
                LabeledParagraph("Hair, Facial Grooming & Makeup Care", report.grooming.hairFacialAndMakeupCare)
                LabeledParagraph("Self-Care Presentation", report.grooming.selfCarePresentation)
                LabeledParagraph("Psychological & Routine Insights", report.grooming.psychologicalInsights)
            }
        }
    }
}

@Composable
private fun VoiceAndCostTabContent(report: AnalysisReport) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ReportModuleCard(
                title = "Voice Tone & Acoustic Communication Analysis",
                icon = Icons.Filled.GraphicEq,
                accentColor = MaterialTheme.colorScheme.primary
            ) {
                LabeledParagraph("Tone & Pitch", report.voice.toneAndPitch)
                LabeledParagraph("Pace, Rhythm & Pauses", report.voice.paceAndPauses)
                LabeledParagraph("Vocal Tension & Speaking Style", report.voice.vocalTensionAndStyle)
                LabeledParagraph("Communication & Credibility Assessment", report.voice.communicationCredibilityNote)
            }
        }

        item {
            ReportModuleCard(
                title = "Clothing & Footwear Cost Estimation",
                icon = Icons.Filled.ShoppingBag,
                accentColor = MaterialTheme.colorScheme.tertiary
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Estimated Outfit Total",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "${report.outfitCost.items.size} visible items benchmarked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatUsdRange(
                                report.outfitCost.totalMinUsd,
                                report.outfitCost.totalMaxUsd
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (report.outfitCost.items.isEmpty()) {
                    Text(
                        text = "No garments or footwear were visible in the submitted media.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        report.outfitCost.items.forEach { item ->
                            GarmentRowCard(item)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                LabeledParagraph("Shopping & Amazon Benchmark Notes", report.outfitCost.shoppingBenchmarkSummary)
            }
        }
    }
}

@Composable
private fun RecordsAndPlateTabContent(report: AnalysisReport) {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ReportModuleCard(
                title = "FBI & Public Search Watchlist Check",
                icon = Icons.Filled.Policy,
                accentColor = MaterialTheme.colorScheme.secondary
            ) {
                LabeledParagraph("Query Basis", report.watchlist.queryBasis)
                LabeledParagraph("Public Record & FBI Check Summary", report.watchlist.fbiAndPublicRecordSummary)
                LabeledParagraph("Matches or Public Leads", report.watchlist.potentialMatchesOrLeads)
                LabeledParagraph("Verification Guidance", report.watchlist.verificationGuidance)

                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { runCatching { uriHandler.openUri("https://www.fbi.gov/wanted") } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text("Open Official FBI Wanted Public Directory")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        item {
            ReportModuleCard(
                title = "License Plate Lookup (DC / MD / VA & General)",
                icon = Icons.Filled.DirectionsCar,
                accentColor = MaterialTheme.colorScheme.primary
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
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
                        Column {
                            Text("Detected / Entered Plate", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = report.licensePlate.detectedPlateNumber,
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = MaterialTheme.typography.labelMedium.fontFamily
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = report.licensePlate.detectedJurisdiction,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                LabeledParagraph("DC, Maryland & Virginia Summary", report.licensePlate.dcMdVaSummary)
                LabeledParagraph("Parking Citations, HOV & Toll Balances", report.licensePlate.parkingAndTollsNote)
                LabeledParagraph("Traffic & Public Incident Notes", report.licensePlate.trafficAndIncidentNote)
                LabeledParagraph("Official DMV / DPPA Portal Boundaries", report.licensePlate.officialPortalRequirement)

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Direct Public Portal Verification Links",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PortalLinkButton("DC DMV Ticket & Citation Portal", "https://dmv.dc.gov/service/pay-tickets", uriHandler)
                    PortalLinkButton("Maryland MVA & E-ZPass MD Portal", "https://www.ezpassmd.com", uriHandler)
                    PortalLinkButton("Virginia DMV & E-ZPass VA Portal", "https://www.dmv.virginia.gov", uriHandler)
                }
            }
        }
    }
}

@Composable
private fun PortalLinkButton(
    label: String,
    url: String,
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    OutlinedButton(
        onClick = { runCatching { uriHandler.openUri(url) } },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Open external portal",
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun GarmentRowCard(item: GarmentEstimate) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.itemName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${item.category} • ${item.brandOrStyleTier}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.benchmarkSource,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = formatUsdRange(item.minPriceUsd, item.maxPriceUsd),
                style = MaterialTheme.typography.labelLarge,
                fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ReportModuleCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            content()
        }
    }
}

@Composable
private fun LabeledParagraph(label: String, body: String) {
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
private fun BulletLine(text: String) {
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

private fun formatUsdRange(minVal: Double, maxVal: Double): String {
    val low = minVal.toInt()
    val high = maxVal.toInt()
    return if (low == high) {
        String.format(Locale.US, "$%d", low)
    } else {
        String.format(Locale.US, "$%d–$%d", low, high)
    }
}
