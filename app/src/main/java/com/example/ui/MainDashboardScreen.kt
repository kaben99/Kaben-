package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.data.UserGatewayPreferences
import com.example.model.AnalysisFocusModule
import com.example.model.AnalysisReport
import com.example.model.MediaInputs
import com.example.model.PlateJurisdictionChoice
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainDashboardScreen(
    prefs: UserGatewayPreferences,
    inputs: MediaInputs,
    enabledModules: Set<AnalysisFocusModule>,
    analysisState: AnalysisUiState,
    lastReport: AnalysisReport?,
    savedReports: List<AnalysisReport> = emptyList(),
    isRecordingAudio: Boolean,
    isRecordingPaused: Boolean = false,
    recordingSeconds: Int,
    waveformAmplitudes: List<Float> = emptyList(),
    onChangeJurisdiction: () -> Unit,
    onAddPersonPhotos: (List<Uri>) -> Unit,
    onRemovePersonPhoto: (Uri) -> Unit,
    onSetVideoUri: (Uri?) -> Unit,
    onSetAudioUri: (Uri?, String?, Boolean) -> Unit,
    onSetPlatePhotoUri: (Uri?) -> Unit,
    onPlateNumberChange: (String) -> Unit,
    onPlateJurisdictionChange: (PlateJurisdictionChoice) -> Unit,
    onContextNotesChange: (String) -> Unit,
    onToggleModule: (AnalysisFocusModule) -> Unit,
    onStartVoiceRecording: () -> Unit,
    onPauseVoiceRecording: () -> Unit = {},
    onResumeVoiceRecording: () -> Unit = {},
    onStopVoiceRecording: () -> Unit,
    onCancelVoiceRecording: () -> Unit = {},
    onClearAll: () -> Unit,
    onDismissError: () -> Unit,
    onRunAnalysis: () -> Unit,
    onOpenLastReport: () -> Unit,
    onOpenSavedReport: (AnalysisReport) -> Unit = {},
    onDeleteSavedReport: (String) -> Unit = {},
    onClearReportHistory: () -> Unit = {}
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var activeCameraTarget by remember { mutableStateOf<CameraCaptureTarget?>(null) }
    var showMediaSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showAudioStudioSheet by remember { mutableStateOf(false) }
    var showPlatePanel by remember { mutableStateOf(false) }
    var audioPermissionDeniedNote by remember { mutableStateOf(false) }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        onAddPersonPhotos(uris)
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onSetVideoUri(uri)
    }

    val platePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onSetPlatePhotoUri(uri)
    }

    val audioFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onSetAudioUri(uri, null, false)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            audioPermissionDeniedNote = false
            onStartVoiceRecording()
        } else {
            audioPermissionDeniedNote = true
        }
    }

    activeCameraTarget?.let { target ->
        CameraCaptureScreen(
            target = target,
            onImageCaptured = { capturedUri ->
                when (target) {
                    CameraCaptureTarget.PERSON_PHOTO -> onAddPersonPhotos(listOf(capturedUri))
                    CameraCaptureTarget.LICENSE_PLATE -> onSetPlatePhotoUri(capturedUri)
                }
                activeCameraTarget = null
            },
            onClose = { activeCameraTarget = null }
        )
        return
    }

    val isAnalyzing = analysisState is AnalysisUiState.Analyzing

    if (showMediaSheet) {
        ModalBottomSheet(onDismissRequest = { showMediaSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Upload Image or Video",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Choose photos from your gallery/social media, capture directly with the camera, or upload a video.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        showMediaSheet = false
                        multiPhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("pick_photos_button")
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Select Multi-Angle Photos (${inputs.personPhotos.size}/5)")
                }

                FilledTonalButton(
                    onClick = {
                        showMediaSheet = false
                        activeCameraTarget = CameraCaptureTarget.PERSON_PHOTO
                    },
                    enabled = inputs.personPhotos.size < 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("take_photo_button")
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Take Photo with Camera")
                }

                OutlinedButton(
                    onClick = {
                        showMediaSheet = false
                        videoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("pick_video_button")
                ) {
                    Icon(Icons.Filled.VideoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (inputs.videoUri != null) "Replace Video" else "Upload Video Clip")
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(onDismissRequest = { showHistorySheet = false }) {
            SavedHistorySheetContent(
                savedReports = savedReports,
                onOpenReport = { report ->
                    showHistorySheet = false
                    onOpenSavedReport(report)
                },
                onDeleteReport = onDeleteSavedReport,
                onClearHistory = onClearReportHistory
            )
            Spacer(Modifier.navigationBarsPadding().height(12.dp))
        }
    }

    if (showAudioStudioSheet) {
        ModalBottomSheet(onDismissRequest = { showAudioStudioSheet = false }) {
            val permissionGrantedNow = hasAudioPermission || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            AudioRecordingStudioContent(
                hasAudioPermission = permissionGrantedNow,
                isRecording = isRecordingAudio,
                isPaused = isRecordingPaused,
                recordingSeconds = recordingSeconds,
                amplitudes = waveformAmplitudes,
                attachedAudioUri = inputs.audioUri,
                attachedAudioName = inputs.audioDisplayName,
                onRequestPermission = {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onStartRecording = {
                    if (permissionGrantedNow) {
                        onStartVoiceRecording()
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onPauseRecording = onPauseVoiceRecording,
                onResumeRecording = onResumeVoiceRecording,
                onStopRecording = onStopVoiceRecording,
                onCancelRecording = {
                    onCancelVoiceRecording()
                    showAudioStudioSheet = false
                },
                onDiscardAttachedAudio = { onSetAudioUri(null, null, false) },
                onUploadAudioFile = {
                    showAudioStudioSheet = false
                    audioFilePicker.launch(arrayOf("audio/*"))
                },
                onDone = { showAudioStudioSheet = false }
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "${prefs.jurisdiction.displayName} • ${prefs.usageMode.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier.testTag("history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "View saved analysis history"
                        )
                    }
                    IconButton(
                        onClick = onChangeJurisdiction,
                        modifier = Modifier.testTag("change_jurisdiction_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Public,
                            contentDescription = "Change jurisdiction or usage mode"
                        )
                    }
                    if (inputs.hasAnyInput) {
                        IconButton(
                            onClick = onClearAll,
                            enabled = !isAnalyzing,
                            modifier = Modifier.testTag("clear_inputs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Clear all uploaded media"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onRunAnalysis,
                        enabled = inputs.hasAnyInput && !isAnalyzing,
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .testTag("run_analysis_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAnalyzing) {
                                "Analyzing Multimodal Signals..."
                            } else {
                                "Run Deep Multimodal Analysis"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize()
                    .testTag("dashboard_lazy_column"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status / Error Feedback
                if (analysisState is AnalysisUiState.Analyzing) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = analysisState.stageMessage,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }

                if (analysisState is AnalysisUiState.Error) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = analysisState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = onDismissError) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }

                // Primary Dashboard Action Hub
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Multimodal Intake Dashboard",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Capture or upload photos, video, speech, or a license plate to populate the analytical report cards below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 1. Upload Image/Video
                            Button(
                                onClick = { showMediaSheet = true },
                                enabled = !isAnalyzing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 54.dp)
                                    .testTag("dashboard_upload_media_button"),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Filled.CloudUpload, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Upload Image/Video",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 2. Record Audio
                                FilledTonalButton(
                                    onClick = {
                                        hasAudioPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        showAudioStudioSheet = true
                                    },
                                    enabled = !isAnalyzing,
                                    colors = if (isRecordingAudio) {
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        )
                                    } else {
                                        ButtonDefaults.filledTonalButtonColors()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 52.dp)
                                        .testTag("dashboard_record_audio_button"),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(
                                        imageVector = if (isRecordingAudio) Icons.Filled.Stop else Icons.Filled.Mic,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = when {
                                            isRecordingAudio && isRecordingPaused -> "Paused (${recordingSeconds}s)"
                                            isRecordingAudio -> "Mic Live (${recordingSeconds}s)"
                                            else -> "Record Audio"
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // 3. Search License Plate
                                OutlinedButton(
                                    onClick = {
                                        showPlatePanel = true
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(2)
                                        }
                                    },
                                    enabled = !isAnalyzing,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 52.dp)
                                        .testTag("dashboard_search_plate_button"),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Search License Plate",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Quick Audio File Upload & Permission Fallback
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { audioFilePicker.launch(arrayOf("audio/*")) },
                                    enabled = !isAnalyzing && !isRecordingAudio,
                                    modifier = Modifier.testTag("upload_audio_button")
                                ) {
                                    Icon(Icons.Filled.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Or upload audio file")
                                }
                                if (lastReport != null) {
                                    TextButton(
                                        onClick = onOpenLastReport,
                                        modifier = Modifier.testTag("view_full_report_button")
                                    ) {
                                        Text("Open Full Report #${lastReport.reportId}")
                                    }
                                } else if (savedReports.isNotEmpty()) {
                                    TextButton(
                                        onClick = { showHistorySheet = true },
                                        modifier = Modifier.testTag("open_history_link_button")
                                    ) {
                                        Text("Saved History (${savedReports.size})")
                                    }
                                }
                            }

                            if (audioPermissionDeniedNote) {
                                Text(
                                    text = "Microphone permission was declined. Use 'Or upload audio file' above.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            // Attached Media Previews
                            if (inputs.personPhotos.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(inputs.personPhotos, key = { it.toString() }) { uri ->
                                        Box(
                                            modifier = Modifier
                                                .size(84.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        ) {
                                            AsyncImage(
                                                model = uri,
                                                contentDescription = "Selected person photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            IconButton(
                                                onClick = { onRemovePersonPhoto(uri) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(26.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                                        CircleShape
                                                    )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Remove photo",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(visible = inputs.videoUri != null) {
                                AttachedChipRow(
                                    label = "Video attached • Keyframes ready",
                                    onRemove = { onSetVideoUri(null) }
                                )
                            }

                            AnimatedVisibility(visible = inputs.audioUri != null) {
                                AttachedChipRow(
                                    label = "Audio ready: ${inputs.audioDisplayName ?: "Voice clip"}",
                                    onRemove = { onSetAudioUri(null, null, false) }
                                )
                            }
                        }
                    }
                }

                // License Plate & Subject Context Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            width = if (showPlatePanel) 1.5.dp else 1.dp,
                            color = if (showPlatePanel) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            }
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DirectionsCar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = "License Plate & Subject Context",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = {
                                        platePhotoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    enabled = !isAnalyzing,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("pick_plate_photo_button")
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Plate Photo")
                                }

                                OutlinedButton(
                                    onClick = { activeCameraTarget = CameraCaptureTarget.LICENSE_PLATE },
                                    enabled = !isAnalyzing,
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("snap_plate_photo_button")
                                ) {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Snap Plate")
                                }
                            }

                            AnimatedVisibility(visible = inputs.platePhotoUri != null) {
                                inputs.platePhotoUri?.let { uri ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = "Selected license plate image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        IconButton(
                                            onClick = { onSetPlatePhotoUri(null) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(30.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Remove plate photo")
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = inputs.plateNumberInput,
                                onValueChange = onPlateNumberChange,
                                label = { Text("License Plate Number (DC / MD / VA / General)") },
                                placeholder = { Text("e.g., ABC-1234 or 7XY8901") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("plate_number_input")
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PlateJurisdictionChoice.entries.forEach { choice ->
                                    FilterChip(
                                        selected = inputs.plateJurisdiction == choice,
                                        onClick = { onPlateJurisdictionChange(choice) },
                                        label = { Text(choice.label) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = inputs.subjectContextNotes,
                                onValueChange = onContextNotesChange,
                                label = { Text("Identifying Details / Context (Optional)") },
                                placeholder = {
                                    Text("Name/alias for public FBI/record check, interview context, or garment notes")
                                },
                                minLines = 2,
                                maxLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("context_notes_input")
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Focus Modules",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AnalysisFocusModule.entries.forEach { module ->
                                    FilterChip(
                                        selected = enabledModules.contains(module),
                                        onClick = { onToggleModule(module) },
                                        label = { Text(module.label, style = MaterialTheme.typography.bodySmall) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Analytical Report Card Placeholders Section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Analytical Report Cards",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (lastReport != null) "Report #${lastReport.reportId}" else "9 Modules",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                val cardSpecs = buildDashboardReportCards(
                    hasVisualMedia = inputs.personPhotos.isNotEmpty() || inputs.videoUri != null,
                    hasAudio = inputs.audioUri != null,
                    hasPlateOrContext = inputs.platePhotoUri != null || inputs.plateNumberInput.isNotBlank(),
                    hasSubjectContext = inputs.subjectContextNotes.isNotBlank(),
                    lastReport = lastReport
                )

                items(cardSpecs, key = { it.tag }) { spec ->
                    AnalyticalReportPlaceholderCard(
                        spec = spec,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = if (lastReport != null) onOpenLastReport else null
                    )
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun AttachedChipRow(
    label: String,
    onRemove: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove media")
            }
        }
    }
}
