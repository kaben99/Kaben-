package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MultimodalAnalysisRepository
import com.example.data.PreferencesRepository
import com.example.data.UserGatewayPreferences
import com.example.model.AnalysisFocusModule
import com.example.model.AnalysisReport
import com.example.model.JurisdictionOption
import com.example.model.MediaInputs
import com.example.model.PlateJurisdictionChoice
import com.example.model.UsageMode
import com.example.util.MediaProcessor
import com.example.util.VoiceRecorderManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data class Analyzing(val stageMessage: String) : AnalysisUiState
    data class Success(val report: AnalysisReport) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application)
    private val analysisRepo = MultimodalAnalysisRepository(application)
    private val voiceRecorder = VoiceRecorderManager(application)

    val gatewayPrefs: StateFlow<UserGatewayPreferences> = prefsRepo.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = UserGatewayPreferences()
        )

    private val _showGatewayOverride = MutableStateFlow(false)
    val showGatewayOverride: StateFlow<Boolean> = _showGatewayOverride.asStateFlow()

    private val _mediaInputs = MutableStateFlow(MediaInputs())
    val mediaInputs: StateFlow<MediaInputs> = _mediaInputs.asStateFlow()

    private val _enabledModules = MutableStateFlow(AnalysisFocusModule.entries.toSet())
    val enabledModules: StateFlow<Set<AnalysisFocusModule>> = _enabledModules.asStateFlow()

    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    private val _lastReport = MutableStateFlow<AnalysisReport?>(null)
    val lastReport: StateFlow<AnalysisReport?> = _lastReport.asStateFlow()

    val savedReports: StateFlow<List<AnalysisReport>> = analysisRepo.savedReports
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    private val _isRecordingAudio = MutableStateFlow(false)
    val isRecordingAudio: StateFlow<Boolean> = _isRecordingAudio.asStateFlow()

    private val _isRecordingPaused = MutableStateFlow(false)
    val isRecordingPaused: StateFlow<Boolean> = _isRecordingPaused.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()

    private val _audioWaveformAmplitudes = MutableStateFlow<List<Float>>(List(42) { 0.08f })
    val audioWaveformAmplitudes: StateFlow<List<Float>> = _audioWaveformAmplitudes.asStateFlow()

    private var recordingTimerJob: Job? = null
    private var amplitudeSamplingJob: Job? = null

    fun agreeAndEnter(jurisdiction: JurisdictionOption, usageMode: UsageMode) {
        viewModelScope.launch {
            prefsRepo.saveGatewayAgreement(jurisdiction, usageMode, agreed = true)
            _showGatewayOverride.value = false
        }
    }

    fun reopenGateway() {
        _showGatewayOverride.value = true
    }

    fun updateJurisdictionAndMode(jurisdiction: JurisdictionOption, usageMode: UsageMode) {
        viewModelScope.launch {
            prefsRepo.updateJurisdictionAndMode(jurisdiction, usageMode)
        }
    }

    fun addPersonPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _mediaInputs.update { current ->
            val merged = (current.personPhotos + uris).distinct().take(5)
            current.copy(personPhotos = merged)
        }
    }

    fun removePersonPhoto(uri: Uri) {
        _mediaInputs.update { current ->
            current.copy(personPhotos = current.personPhotos.filterNot { it == uri })
        }
    }

    fun setVideoUri(uri: Uri?) {
        _mediaInputs.update { current ->
            current.copy(
                videoUri = uri,
                videoFrameCount = if (uri != null) 4 else 0
            )
        }
    }

    fun setAudioUri(uri: Uri?, displayName: String? = null, isRecorded: Boolean = false) {
        val resolvedName = if (uri != null) {
            displayName ?: MediaProcessor.getFileDisplayName(getApplication(), uri)
        } else {
            null
        }
        _mediaInputs.update { current ->
            current.copy(
                audioUri = uri,
                audioDisplayName = resolvedName,
                isRecordedAudio = isRecorded
            )
        }
    }

    fun setPlatePhotoUri(uri: Uri?) {
        _mediaInputs.update { current ->
            current.copy(platePhotoUri = uri)
        }
    }

    fun updatePlateNumber(plate: String) {
        _mediaInputs.update { current ->
            current.copy(plateNumberInput = plate.uppercase())
        }
    }

    fun updatePlateJurisdiction(choice: PlateJurisdictionChoice) {
        _mediaInputs.update { current ->
            current.copy(plateJurisdiction = choice)
        }
    }

    fun updateContextNotes(notes: String) {
        _mediaInputs.update { current ->
            current.copy(subjectContextNotes = notes)
        }
    }

    fun toggleFocusModule(module: AnalysisFocusModule) {
        _enabledModules.update { current ->
            if (current.contains(module) && current.size > 1) {
                current - module
            } else {
                current + module
            }
        }
    }

    fun startVoiceRecording() {
        try {
            voiceRecorder.startRecording()
            _isRecordingAudio.value = true
            _isRecordingPaused.value = false
            _recordingSeconds.value = 0
            _audioWaveformAmplitudes.value = List(42) { 0.08f }

            recordingTimerJob?.cancel()
            recordingTimerJob = viewModelScope.launch {
                while (_isRecordingAudio.value) {
                    delay(1_000L)
                    if (!_isRecordingPaused.value) {
                        _recordingSeconds.update { it + 1 }
                        if (_recordingSeconds.value >= 120) {
                            stopVoiceRecording()
                            break
                        }
                    }
                }
            }

            amplitudeSamplingJob?.cancel()
            amplitudeSamplingJob = viewModelScope.launch {
                while (_isRecordingAudio.value) {
                    delay(80L)
                    if (!_isRecordingPaused.value) {
                        val sample = voiceRecorder.sampleAmplitude()
                        _audioWaveformAmplitudes.update { current ->
                            (current.drop(1) + sample)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to start microphone recording", e)
            _isRecordingAudio.value = false
            _isRecordingPaused.value = false
            _analysisState.value = AnalysisUiState.Error(
                "Unable to start microphone recording: ${e.localizedMessage ?: "Permission or hardware error."}"
            )
        }
    }

    fun pauseVoiceRecording() {
        if (!_isRecordingAudio.value || _isRecordingPaused.value) return
        if (voiceRecorder.pauseRecording()) {
            _isRecordingPaused.value = true
        }
    }

    fun resumeVoiceRecording() {
        if (!_isRecordingAudio.value || !_isRecordingPaused.value) return
        if (voiceRecorder.resumeRecording()) {
            _isRecordingPaused.value = false
        }
    }

    fun stopVoiceRecording() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        amplitudeSamplingJob?.cancel()
        amplitudeSamplingJob = null
        val duration = _recordingSeconds.value
        _isRecordingAudio.value = false
        _isRecordingPaused.value = false
        val uri = voiceRecorder.stopRecording()
        if (uri != null) {
            setAudioUri(
                uri = uri,
                displayName = "Recorded Voice (${duration}s)",
                isRecorded = true
            )
        }
    }

    fun cancelVoiceRecording() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        amplitudeSamplingJob?.cancel()
        amplitudeSamplingJob = null
        _isRecordingAudio.value = false
        _isRecordingPaused.value = false
        _recordingSeconds.value = 0
        _audioWaveformAmplitudes.value = List(42) { 0.08f }
        voiceRecorder.cancelRecording()
    }

    fun clearAllInputs() {
        if (_isRecordingAudio.value) {
            cancelVoiceRecording()
        }
        _mediaInputs.value = MediaInputs()
        _lastReport.value = null
        _analysisState.value = AnalysisUiState.Idle
    }

    fun dismissError() {
        if (_analysisState.value is AnalysisUiState.Error) {
            _analysisState.value = AnalysisUiState.Idle
        }
    }

    fun returnToStudioFromReport() {
        _analysisState.value = AnalysisUiState.Idle
    }

    fun reopenLastReport() {
        _lastReport.value?.let { report ->
            _analysisState.value = AnalysisUiState.Success(report)
        }
    }

    fun openSavedReport(report: AnalysisReport) {
        _lastReport.value = report
        _analysisState.value = AnalysisUiState.Success(report)
    }

    fun deleteSavedReport(reportId: String) {
        viewModelScope.launch {
            analysisRepo.deleteReport(reportId)
            if (_lastReport.value?.reportId == reportId) {
                _lastReport.value = null
            }
        }
    }

    fun clearReportHistory() {
        viewModelScope.launch {
            analysisRepo.clearReportHistory()
            _lastReport.value = null
        }
    }

    fun runAnalysis() {
        val currentInputs = _mediaInputs.value
        if (!currentInputs.hasAnyInput) {
            _analysisState.value = AnalysisUiState.Error(
                "Please add at least one photo, video, audio clip, license plate, or context note."
            )
            return
        }
        if (_isRecordingAudio.value) {
            stopVoiceRecording()
        }

        val prefs = gatewayPrefs.value
        val activeModules = _enabledModules.value

        viewModelScope.launch {
            _analysisState.value = AnalysisUiState.Analyzing("Preparing multimodal inputs...")
            try {
                val report = analysisRepo.runDeepAnalysis(
                    inputs = _mediaInputs.value,
                    jurisdiction = prefs.jurisdiction,
                    usageMode = prefs.usageMode,
                    enabledModules = activeModules,
                    onStageUpdate = { stage ->
                        _analysisState.value = AnalysisUiState.Analyzing(stage)
                    }
                )
                _lastReport.value = report
                _analysisState.value = AnalysisUiState.Success(report)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Analysis timed out", e)
                _analysisState.value = AnalysisUiState.Error(
                    "Analysis timed out after 60 seconds. Try fewer photos or a shorter clip."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Multimodal analysis failed", e)
                _analysisState.value = AnalysisUiState.Error(
                    e.localizedMessage ?: "Multimodal analysis failed. Please check network connectivity and try again."
                )
            }
        }
    }

    fun buildExportText(report: AnalysisReport): String =
        analysisRepo.formatReportForExport(report)

    override fun onCleared() {
        super.onCleared()
        recordingTimerJob?.cancel()
        amplitudeSamplingJob?.cancel()
        voiceRecorder.stopSilently()
    }
}
