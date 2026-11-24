package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.configureAppCheck
import com.example.ui.AnalysisUiState
import com.example.ui.JurisdictionSelectionScreen
import com.example.ui.MainDashboardScreen
import com.example.ui.MainViewModel
import com.example.ui.ReportScreen
import com.example.ui.theme.ClarityLensTheme

private enum class AppDestination {
    JURISDICTION_SELECTION,
    DASHBOARD,
    REPORT
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            ClarityLensTheme {
                Surface {
                    val mainViewModel: MainViewModel = viewModel()
                    val prefs by mainViewModel.gatewayPrefs.collectAsStateWithLifecycle()
                    val showGatewayOverride by mainViewModel.showGatewayOverride.collectAsStateWithLifecycle()
                    val inputs by mainViewModel.mediaInputs.collectAsStateWithLifecycle()
                    val enabledModules by mainViewModel.enabledModules.collectAsStateWithLifecycle()
                    val analysisState by mainViewModel.analysisState.collectAsStateWithLifecycle()
                    val lastReport by mainViewModel.lastReport.collectAsStateWithLifecycle()
                    val savedReports by mainViewModel.savedReports.collectAsStateWithLifecycle()
                    val isRecordingAudio by mainViewModel.isRecordingAudio.collectAsStateWithLifecycle()
                    val isRecordingPaused by mainViewModel.isRecordingPaused.collectAsStateWithLifecycle()
                    val recordingSeconds by mainViewModel.recordingSeconds.collectAsStateWithLifecycle()
                    val waveformAmplitudes by mainViewModel.audioWaveformAmplitudes.collectAsStateWithLifecycle()

                    val destination = when {
                        !prefs.hasAgreed || showGatewayOverride -> AppDestination.JURISDICTION_SELECTION
                        analysisState is AnalysisUiState.Success -> AppDestination.REPORT
                        else -> AppDestination.DASHBOARD
                    }

                    Crossfade(
                        targetState = destination,
                        label = "ClarityLensNavigation"
                    ) { screen ->
                        when (screen) {
                            AppDestination.JURISDICTION_SELECTION -> {
                                JurisdictionSelectionScreen(
                                    initialJurisdiction = prefs.jurisdiction,
                                    initialUsageMode = prefs.usageMode,
                                    onAgreeAndEnter = { jurisdiction, mode ->
                                        mainViewModel.agreeAndEnter(jurisdiction, mode)
                                    }
                                )
                            }

                            AppDestination.DASHBOARD -> {
                                MainDashboardScreen(
                                    prefs = prefs,
                                    inputs = inputs,
                                    enabledModules = enabledModules,
                                    analysisState = analysisState,
                                    lastReport = lastReport,
                                    savedReports = savedReports,
                                    isRecordingAudio = isRecordingAudio,
                                    isRecordingPaused = isRecordingPaused,
                                    recordingSeconds = recordingSeconds,
                                    waveformAmplitudes = waveformAmplitudes,
                                    onChangeJurisdiction = mainViewModel::reopenGateway,
                                    onAddPersonPhotos = mainViewModel::addPersonPhotos,
                                    onRemovePersonPhoto = mainViewModel::removePersonPhoto,
                                    onSetVideoUri = mainViewModel::setVideoUri,
                                    onSetAudioUri = mainViewModel::setAudioUri,
                                    onSetPlatePhotoUri = mainViewModel::setPlatePhotoUri,
                                    onPlateNumberChange = mainViewModel::updatePlateNumber,
                                    onPlateJurisdictionChange = mainViewModel::updatePlateJurisdiction,
                                    onContextNotesChange = mainViewModel::updateContextNotes,
                                    onToggleModule = mainViewModel::toggleFocusModule,
                                    onStartVoiceRecording = mainViewModel::startVoiceRecording,
                                    onPauseVoiceRecording = mainViewModel::pauseVoiceRecording,
                                    onResumeVoiceRecording = mainViewModel::resumeVoiceRecording,
                                    onStopVoiceRecording = mainViewModel::stopVoiceRecording,
                                    onCancelVoiceRecording = mainViewModel::cancelVoiceRecording,
                                    onClearAll = mainViewModel::clearAllInputs,
                                    onDismissError = mainViewModel::dismissError,
                                    onRunAnalysis = mainViewModel::runAnalysis,
                                    onOpenLastReport = mainViewModel::reopenLastReport,
                                    onOpenSavedReport = mainViewModel::openSavedReport,
                                    onDeleteSavedReport = mainViewModel::deleteSavedReport,
                                    onClearReportHistory = mainViewModel::clearReportHistory
                                )
                            }

                            AppDestination.REPORT -> {
                                val report = (analysisState as? AnalysisUiState.Success)?.report
                                if (report != null) {
                                    ReportScreen(
                                        report = report,
                                        exportText = mainViewModel.buildExportText(report),
                                        onBackToStudio = mainViewModel::returnToStudioFromReport
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
