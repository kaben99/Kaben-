package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.MultimodalAnalysisRepository
import com.example.data.PreferencesRepository
import com.example.data.UserGatewayPreferences
import com.example.data.local.ClarityDatabase
import com.example.model.AnalysisFocusModule
import com.example.model.AnalysisReport
import com.example.model.AncestrySection
import com.example.model.BehavioralSection
import com.example.model.FacialStructureSection
import com.example.model.GarmentEstimate
import com.example.model.GroomingSection
import com.example.model.GroundingSource
import com.example.model.JurisdictionOption
import com.example.model.LicensePlateSection
import com.example.model.MediaInputs
import com.example.model.MedicalScreeningSection
import com.example.model.OutfitCostSection
import com.example.model.ReportMetrics
import com.example.model.UsageMode
import com.example.model.VoiceSection
import com.example.model.WatchlistSection
import com.example.ui.AnalysisUiState
import com.example.ui.AudioRecordingStudioContent
import com.example.ui.CameraCaptureScreen
import com.example.ui.CameraCaptureTarget
import com.example.ui.JurisdictionSelectionScreen
import com.example.ui.MainDashboardScreen
import com.example.ui.ReportCard
import com.example.ui.theme.ClarityLensTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `app name matches Clarity Lens`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Clarity Lens", appName)
    }

    @Test
    fun `jurisdiction selection screen saves country and usage mode to DataStore`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = PreferencesRepository(context)

        composeRule.setContent {
            ClarityLensTheme {
                JurisdictionSelectionScreen(
                    initialJurisdiction = JurisdictionOption.US_DMV_CORRIDOR,
                    initialUsageMode = UsageMode.PERSONAL,
                    onAgreeAndEnter = { jurisdiction, mode ->
                        runBlocking {
                            prefsRepo.saveGatewayAgreement(jurisdiction, mode, agreed = true)
                        }
                    }
                )
            }
        }

        composeRule.onNodeWithTag("jurisdiction_dropdown").performScrollTo().performClick()
        composeRule.onNodeWithTag("jurisdiction_option_ethiopia").performClick()
        composeRule.onNodeWithTag("usage_mode_business").performScrollTo().performClick()
        composeRule.onNodeWithTag("agree_enter_button").assertIsDisplayed().performClick()

        val saved = runBlocking { prefsRepo.preferencesFlow.first() }
        assertEquals(JurisdictionOption.ETHIOPIA, saved.jurisdiction)
        assertEquals(UsageMode.BUSINESS, saved.usageMode)
        assertTrue(saved.hasAgreed)
    }

    @Test
    fun `main dashboard displays primary action buttons and report card placeholders`() {
        composeRule.setContent {
            ClarityLensTheme {
                MainDashboardScreen(
                    prefs = UserGatewayPreferences(
                        jurisdiction = JurisdictionOption.US_DMV_CORRIDOR,
                        usageMode = UsageMode.PERSONAL,
                        hasAgreed = true
                    ),
                    inputs = MediaInputs(),
                    enabledModules = AnalysisFocusModule.entries.toSet(),
                    analysisState = AnalysisUiState.Idle,
                    lastReport = null,
                    isRecordingAudio = false,
                    recordingSeconds = 0,
                    onChangeJurisdiction = {},
                    onAddPersonPhotos = {},
                    onRemovePersonPhoto = {},
                    onSetVideoUri = {},
                    onSetAudioUri = { _, _, _ -> },
                    onSetPlatePhotoUri = {},
                    onPlateNumberChange = {},
                    onPlateJurisdictionChange = {},
                    onContextNotesChange = {},
                    onToggleModule = {},
                    onStartVoiceRecording = {},
                    onStopVoiceRecording = {},
                    onClearAll = {},
                    onDismissError = {},
                    onRunAnalysis = {},
                    onOpenLastReport = {}
                )
            }
        }

        composeRule.onNodeWithTag("dashboard_upload_media_button").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_record_audio_button").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_search_plate_button").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_lazy_column")
            .performScrollToNode(hasTestTag("placeholder_behavioral_card"))
        composeRule.onNodeWithTag("placeholder_behavioral_card").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_lazy_column")
            .performScrollToNode(hasTestTag("placeholder_plate_card"))
        composeRule.onNodeWithTag("placeholder_plate_card").assertIsDisplayed()
    }

    @Test
    fun `camera capture screen prompts for camera permission when ungranted`() {
        composeRule.setContent {
            ClarityLensTheme {
                CameraCaptureScreen(
                    target = CameraCaptureTarget.PERSON_PHOTO,
                    onImageCaptured = {},
                    onClose = {}
                )
            }
        }

        composeRule.onNodeWithTag("grant_camera_permission_button").assertIsDisplayed()
    }

    @Test
    fun `repository saves and restores analysis reports in Room database`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, ClarityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = MultimodalAnalysisRepository(context, db.reportDao())
            val sampleReport = AnalysisReport(
                reportId = "RPT12345",
                timestampFormatted = "Nov 22, 2026 • 10:50",
                jurisdiction = JurisdictionOption.US_DMV_CORRIDOR,
                usageMode = UsageMode.LEGAL,
                executiveSummary = "Sample executive summary for Room round-trip test.",
                overallConfidence = "Moderate",
                keyLimitations = listOf("Lighting variation"),
                behavioral = BehavioralSection("Steady gaze", "Relaxed brow", "Neutral lips", "Low load", "Probabilistic indicator", "Interviewing literature"),
                facialStructure = FacialStructureSection("Oval", "Balanced thirds", "Clear jawline", "Anthropometric reference"),
                ancestry = AncestrySection(listOf("East African phenotypic cues"), "Moderate confidence", "Human diversity acknowledgment"),
                medicalScreening = MedicalScreeningSection("None observed", "None observed", "Clinical observation context", "Informational only, not a medical diagnosis."),
                grooming = GroomingSection("Navy blazer", "Classic alignment", "Neat haircut", "Well-kept", "Routine consistency"),
                voice = VoiceSection(true, "Warm baritone", "Steady pace", "Relaxed", "Natural cadence"),
                outfitCost = OutfitCostSection(
                    items = listOf(GarmentEstimate("Navy Blazer", "Outerwear", "Mid-tier", "Amazon", 65.0, 110.0)),
                    totalMinUsd = 65.0,
                    totalMaxUsd = 110.0,
                    shoppingBenchmarkSummary = "Retail benchmark"
                ),
                watchlist = WatchlistSection("No name provided", "No public matches", "None", "Check fbi.gov/wanted"),
                licensePlate = LicensePlateSection("ABC1234", "Maryland", "Public citation rules", "E-ZPass MD portal", "No incident reports", "DPPA limits apply"),
                groundedResearchBrief = "Verified retail range.",
                groundingSources = listOf(GroundingSource("Amazon Apparel", "https://www.amazon.com")),
                searchEntryPointHtml = null,
                metrics = ReportMetrics(
                    behavioralComposureScore = 82,
                    cognitiveLoadScore = 18,
                    facialSymmetryScore = 88,
                    ancestryConfidenceScore = 71,
                    groomingCareScore = 85,
                    styleCoordinationScore = 80,
                    voiceClarityScore = 79,
                    publicRecordConfidenceScore = 76
                )
            )

            runBlocking {
                repo.saveReport(sampleReport)
                val restored = repo.savedReports.first()
                assertEquals(1, restored.size)
                assertEquals("RPT12345", restored.first().reportId)
                assertEquals(JurisdictionOption.US_DMV_CORRIDOR, restored.first().jurisdiction)
                assertEquals("ABC1234", restored.first().licensePlate.detectedPlateNumber)
                assertEquals(1, restored.first().outfitCost.items.size)
                assertEquals(65.0, restored.first().outfitCost.totalMinUsd, 0.01)
                assertEquals(82, restored.first().metrics.behavioralComposureScore)
                assertEquals(88, restored.first().metrics.facialSymmetryScore)

                repo.deleteReport("RPT12345")
                val afterDelete = repo.savedReports.first()
                assertTrue(afterDelete.isEmpty())
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun `report card organizes Gemini analysis into four distinct LazyColumn sections`() {
        val sampleReport = AnalysisReport(
            reportId = "RPT99001",
            timestampFormatted = "Nov 22, 2026 • 10:55",
            jurisdiction = JurisdictionOption.US_DMV_CORRIDOR,
            usageMode = UsageMode.PERSONAL,
            executiveSummary = "Comprehensive visual & public record summary.",
            overallConfidence = "High",
            keyLimitations = listOf("Static image lighting"),
            behavioral = BehavioralSection("Steady gaze", "Relaxed brow", "Neutral lips", "Low load", "Probabilistic cue", "Forensic literature"),
            facialStructure = FacialStructureSection("Oval", "Balanced facial thirds", "Defined jawline", "Anthropometric proportions"),
            ancestry = AncestrySection(listOf("Mediterranean / East African traits"), "Moderate confidence", "Broad human diversity"),
            medicalScreening = MedicalScreeningSection("No atypical craniofacial cues", "No behavioral indicators", "Clinical reference", "Informational only, not a diagnosis."),
            grooming = GroomingSection("Charcoal coat & knitwear", "Modern minimalist", "Neat hair & grooming", "Intentional self-care", "Structured daily routine"),
            voice = VoiceSection(true, "Calm tenor", "Measured pace", "Low tension", "Natural conversational cadence"),
            outfitCost = OutfitCostSection(
                items = listOf(GarmentEstimate("Wool Overcoat", "Outerwear", "Mid-tier", "Amazon", 95.0, 145.0)),
                totalMinUsd = 95.0,
                totalMaxUsd = 145.0,
                shoppingBenchmarkSummary = "Retail apparel benchmark"
            ),
            watchlist = WatchlistSection("Visual + name context", "No FBI Wanted alerts matched", "No public matches", "Verify on fbi.gov/wanted"),
            licensePlate = LicensePlateSection("DC8841", "Washington, D.C.", "DC DMV lookup rules", "Check DC DMV portal", "No incident records", "DPPA compliant"),
            groundedResearchBrief = "Grounded retail pricing and public records checked.",
            groundingSources = listOf(GroundingSource("FBI Wanted", "https://www.fbi.gov/wanted")),
            searchEntryPointHtml = null
        )

        composeRule.setContent {
            ClarityLensTheme {
                ReportCard(report = sampleReport)
            }
        }

        val lazyColumn = composeRule.onNodeWithTag("report_card_lazy_column")
        lazyColumn.assertIsDisplayed()

        lazyColumn.performScrollToNode(hasTestTag("vico_comparative_confidence_chart"))
        composeRule.onNodeWithTag("vico_comparative_confidence_chart").assertIsDisplayed()

        lazyColumn.performScrollToNode(hasTestTag("section_behavioral_indicators"))
        composeRule.onNodeWithTag("section_behavioral_indicators").assertIsDisplayed()
        lazyColumn.performScrollToNode(hasTestTag("chart_behavioral_signals"))
        composeRule.onNodeWithTag("chart_behavioral_signals").assertIsDisplayed()

        lazyColumn.performScrollToNode(hasTestTag("section_facial_anthropometric_notes"))
        composeRule.onNodeWithTag("section_facial_anthropometric_notes").assertIsDisplayed()
        lazyColumn.performScrollToNode(hasTestTag("chart_facial_anthropometric"))
        composeRule.onNodeWithTag("chart_facial_anthropometric").assertIsDisplayed()

        lazyColumn.performScrollToNode(hasTestTag("section_grooming_style_insights"))
        composeRule.onNodeWithTag("section_grooming_style_insights").assertIsDisplayed()
        lazyColumn.performScrollToNode(hasTestTag("chart_grooming_and_cost"))
        composeRule.onNodeWithTag("chart_grooming_and_cost").assertIsDisplayed()

        lazyColumn.performScrollToNode(hasTestTag("section_public_record_summary"))
        composeRule.onNodeWithTag("section_public_record_summary").assertIsDisplayed()
        lazyColumn.performScrollToNode(hasTestTag("chart_public_record_coverage"))
        composeRule.onNodeWithTag("chart_public_record_coverage").assertIsDisplayed()
    }

    @Test
    fun `audio recording studio prompts for microphone permission when ungranted`() {
        composeRule.setContent {
            ClarityLensTheme {
                AudioRecordingStudioContent(
                    hasAudioPermission = false,
                    isRecording = false,
                    isPaused = false,
                    recordingSeconds = 0,
                    amplitudes = emptyList(),
                    attachedAudioUri = null,
                    attachedAudioName = null,
                    onRequestPermission = {},
                    onStartRecording = {},
                    onPauseRecording = {},
                    onResumeRecording = {},
                    onStopRecording = {},
                    onCancelRecording = {},
                    onDiscardAttachedAudio = {},
                    onUploadAudioFile = {},
                    onDone = {}
                )
            }
        }

        composeRule.onNodeWithTag("audio_recording_studio_sheet").assertIsDisplayed()
        composeRule.onNodeWithTag("grant_audio_permission_button").assertIsDisplayed()
        composeRule.onNodeWithTag("audio_studio_upload_file_button").assertIsDisplayed()
    }

    @Test
    fun `audio recording studio displays live waveform and pause stop controls while recording`() {
        var pausedCalled = false
        var stoppedCalled = false

        composeRule.setContent {
            ClarityLensTheme {
                AudioRecordingStudioContent(
                    hasAudioPermission = true,
                    isRecording = true,
                    isPaused = false,
                    recordingSeconds = 12,
                    amplitudes = List(42) { idx -> if (idx % 2 == 0) 0.7f else 0.35f },
                    attachedAudioUri = null,
                    attachedAudioName = null,
                    onRequestPermission = {},
                    onStartRecording = {},
                    onPauseRecording = { pausedCalled = true },
                    onResumeRecording = {},
                    onStopRecording = { stoppedCalled = true },
                    onCancelRecording = {},
                    onDiscardAttachedAudio = {},
                    onUploadAudioFile = {},
                    onDone = {}
                )
            }
        }

        composeRule.onNodeWithTag("audio_timer_text").assertTextEquals("00:12 / 02:00")
        composeRule.onNodeWithTag("audio_waveform_canvas").assertIsDisplayed()
        composeRule.onNodeWithTag("audio_studio_pause_resume_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("audio_studio_stop_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("audio_studio_cancel_button").assertIsDisplayed()
        assertTrue(pausedCalled)
        assertTrue(stoppedCalled)
    }
}
