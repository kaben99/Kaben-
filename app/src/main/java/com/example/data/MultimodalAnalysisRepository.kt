package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
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
import com.example.model.PlateJurisdictionChoice
import com.example.model.ReportMetrics
import com.example.model.UsageMode
import com.example.model.VoiceSection
import com.example.model.WatchlistSection
import com.example.util.AudioPayload
import com.example.util.MediaProcessor
import com.example.data.local.ClarityDatabase
import com.example.data.local.ReportDao
import com.example.data.local.ReportJsonSerializer
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "MultimodalRepo"
private const val MODEL_NAME = "gemini-3.8-flash"

class MultimodalAnalysisRepository(
    private val context: Context,
    private val reportDao: ReportDao = ClarityDatabase.getInstance(context).reportDao()
) {

    val savedReports: Flow<List<AnalysisReport>> = reportDao.observeAllReports()
        .map { entities ->
            entities.mapNotNull { entity ->
                runCatching { ReportJsonSerializer.toDomain(entity) }
                    .onFailure { Log.e(TAG, "Failed to deserialize saved report ${entity.reportId}", it) }
                    .getOrNull()
            }
        }
        .flowOn(Dispatchers.Default)

    suspend fun saveReport(report: AnalysisReport) = withContext(Dispatchers.IO) {
        reportDao.insertReport(ReportJsonSerializer.toEntity(report))
    }

    suspend fun deleteReport(reportId: String) = withContext(Dispatchers.IO) {
        reportDao.deleteReportById(reportId)
    }

    suspend fun clearReportHistory() = withContext(Dispatchers.IO) {
        reportDao.clearAllReports()
    }

    private val reportSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "executiveSummary" to Schema.string("Concise 2-4 sentence overall synthesis across all provided media."),
                "overallConfidence" to Schema.string("High, Moderate, or Preliminary/Limited with brief reason."),
                "keyLimitations" to Schema.array(
                    items = Schema.string(),
                    description = "2-4 analytical limitations based on lighting, angles, missing audio, or public-record scope."
                ),
                "behavioral" to Schema.obj(
                    properties = mapOf(
                        "gazeAndBlinking" to Schema.string("Eye gaze direction, contact, and blink observations across frames or photos."),
                        "eyebrowAndForehead" to Schema.string("Eyebrow position, furrow, or forehead tension indicators."),
                        "mouthAndLipMovement" to Schema.string("Lip compression, smile authenticity, micro-tension, or mouth posture."),
                        "cognitiveLoadVsNaturalSignals" to Schema.string("Behavioral psychology assessment of stress/cognitive load vs relaxed natural cues."),
                        "analyticalProbabilityNote" to Schema.string("Probability-framed summary emphasizing indicators are not definitive lie detection."),
                        "researchContext" to Schema.string("Non-verbal communication and forensic interviewing research context.")
                    )
                ),
                "facialStructure" to Schema.obj(
                    properties = mapOf(
                        "faceShapeClassification" to Schema.string("Primary facial shape category (Oval, Round, Square, Rectangular/Oblong, Heart, Diamond, or N/A)."),
                        "proportionsAndSymmetry" to Schema.string("Upper, middle, and lower facial thirds and bilateral balance observations."),
                        "landmarkObservations" to Schema.string("Jawline, cheekbone prominence, forehead contour, and nasal/chin alignment notes."),
                        "anthropometricReference" to Schema.string("Morphological classification methodology reference.")
                    )
                ),
                "ancestry" to Schema.obj(
                    properties = mapOf(
                        "traitEstimates" to Schema.array(
                            items = Schema.string(),
                            description = "High-level phenotypic regional/ancestral visual estimates with confidence qualifiers."
                        ),
                        "confidenceAndNuance" to Schema.string("Explanation of visual ambiguity, lighting influence, or mixed traits."),
                        "diversityNote" to Schema.string("Acknowledgment of broad human phenotypic diversity and non-determinism.")
                    )
                ),
                "medicalScreening" to Schema.obj(
                    properties = mapOf(
                        "craniofacialObservations" to Schema.string("Informational notes on visible craniofacial features referenced in clinical literature, or 'None observed / No face provided'."),
                        "neurodevelopmentalObservations" to Schema.string("Observational notes on gaze, expression, or posture patterns associated in literature with neurodevelopmental traits (e.g., autism spectrum or Down syndrome traits), or 'None observed'."),
                        "clinicalReferenceContext" to Schema.string("Medical/clinical observation context explaining why visual inspection alone cannot screen or confirm conditions."),
                        "nonDiagnosticDisclaimer" to Schema.string("Explicit statement that this is NOT a medical diagnosis and recommends consulting qualified healthcare professionals.")
                    )
                ),
                "grooming" to Schema.obj(
                    properties = mapOf(
                        "clothingStyleAndCoordination" to Schema.string("Evaluation of outfit category, fit, and color coordination."),
                        "trendAndPersonalAlignment" to Schema.string("Assessment of current fashion trend alignment vs functional/personal style."),
                        "hairFacialAndMakeupCare" to Schema.string("Tailored grooming evaluation: haircut/beard neatness for men, hair styling/makeup/application for women, or age-appropriate care."),
                        "selfCarePresentation" to Schema.string("Overall neatness, hygiene cues, and visible attention to presentation."),
                        "psychologicalInsights" to Schema.string("Behavioral psychology perspective on how grooming/self-care presentation can relate to routine, stress, mood, or lifestyle.")
                    )
                ),
                "voice" to Schema.obj(
                    properties = mapOf(
                        "audioAnalyzed" to Schema.boolean("True if an audio clip was provided and analyzed, false otherwise."),
                        "toneAndPitch" to Schema.string("Vocal tone, pitch warmth/variability, or 'No audio provided'."),
                        "paceAndPauses" to Schema.string("Speaking rate, cadence, hesitation pauses, or filler patterns."),
                        "vocalTensionAndStyle" to Schema.string("Acoustic indicators of calm composure vs vocal strain or tension."),
                        "communicationCredibilityNote" to Schema.string("Communication style summary framed probabilistically without claiming absolute deception detection.")
                    )
                ),
                "outfitCost" to Schema.obj(
                    properties = mapOf(
                        "items" to Schema.array(
                            items = Schema.obj(
                                properties = mapOf(
                                    "itemName" to Schema.string("Specific visible garment, footwear, or accessory."),
                                    "category" to Schema.string("Top, Bottom, Outerwear, Footwear, or Accessory."),
                                    "brandOrStyleTier" to Schema.string("Observed brand or market tier (e.g. Budget, Mid-tier Retail, Premium, Luxury)."),
                                    "benchmarkSource" to Schema.string("Retail benchmark reference (e.g. Amazon / Mass Retail Apparel Benchmark)."),
                                    "minPriceUsd" to Schema.double("Estimated minimum USD price."),
                                    "maxPriceUsd" to Schema.double("Estimated maximum USD price.")
                                )
                            ),
                            description = "List of visible clothing, footwear, and accessory items with estimated USD prices."
                        ),
                        "totalMinUsd" to Schema.double("Sum of minimum estimated USD prices for all visible outfit items."),
                        "totalMaxUsd" to Schema.double("Sum of maximum estimated USD prices for all visible outfit items."),
                        "shoppingBenchmarkSummary" to Schema.string("Summary of how retail/Amazon price benchmarks were estimated.")
                    )
                ),
                "watchlist" to Schema.obj(
                    properties = mapOf(
                        "queryBasis" to Schema.string("What identifying details or visual context were available to query."),
                        "fbiAndPublicRecordSummary" to Schema.string("Summary of whether any public watchlist/FBI check can be performed from inputs."),
                        "potentialMatchesOrLeads" to Schema.string("Any reported public leads or explicit note that no public match was identified."),
                        "verificationGuidance" to Schema.string("Guidance to verify via official public portals such as FBI.gov Wanted lists.")
                    )
                ),
                "licensePlate" to Schema.obj(
                    properties = mapOf(
                        "detectedPlateNumber" to Schema.string("OCR-read plate number from photo or user-entered plate string, or 'None provided'."),
                        "detectedJurisdiction" to Schema.string("Detected or user-selected issuing jurisdiction (e.g. Washington, D.C., Maryland, Virginia)."),
                        "dcMdVaSummary" to Schema.string("Jurisdiction-specific overview across DC, Maryland, and Virginia public citation rules."),
                        "parkingAndTollsNote" to Schema.string("Note on parking citations, HOV, and toll balance lookup channels."),
                        "trafficAndIncidentNote" to Schema.string("Note on publicly reported traffic or incident mentions vs non-public records."),
                        "officialPortalRequirement" to Schema.string("Clear explanation of what requires official DMV/MVA portals under DPPA privacy rules.")
                    )
                ),
                "metrics" to Schema.obj(
                    properties = mapOf(
                        "behavioralComposureScore" to Schema.integer("0-100 estimate of natural calm composure vs visible stress signals (or 50 if N/A)."),
                        "cognitiveLoadScore" to Schema.integer("0-100 estimate of visible cognitive load, hesitation, or facial/micro tension."),
                        "facialSymmetryScore" to Schema.integer("0-100 observational facial proportion/bilateral visual clarity score (or 0 if no face)."),
                        "ancestryConfidenceScore" to Schema.integer("0-100 visual confidence in regional trait estimation (acknowledging diversity; 0 if no face)."),
                        "groomingCareScore" to Schema.integer("0-100 observable grooming, hair/facial care, and self-care presentation index."),
                        "styleCoordinationScore" to Schema.integer("0-100 clothing fit, color coordination, and outfit coherence index."),
                        "voiceClarityScore" to Schema.integer("0-100 vocal composure & acoustic clarity score (0 if no audio provided)."),
                        "publicRecordConfidenceScore" to Schema.integer("0-100 completeness/confidence of public-record & plate check coverage.")
                    )
                )
            )
        )
    }

    private val structuredModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = reportSchema
                temperature = 0.25f
            }
        )
    }

    private val groundedSearchModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            tools = listOf(Tool.googleSearch())
        )
    }

    suspend fun runDeepAnalysis(
        inputs: MediaInputs,
        jurisdiction: JurisdictionOption,
        usageMode: UsageMode,
        enabledModules: Set<AnalysisFocusModule>,
        onStageUpdate: (String) -> Unit
    ): AnalysisReport {
        onStageUpdate("Preparing photos, video frames & audio...")
        val personBitmaps = mutableListOf<Bitmap>()
        for (uri in inputs.personPhotos.take(5)) {
            MediaProcessor.decodeScaledBitmap(context, uri)?.let { personBitmaps.add(it) }
        }
        val videoFrames = inputs.videoUri?.let { uri ->
            MediaProcessor.extractVideoKeyframes(context, uri, maxFrames = 4)
        }.orEmpty()
        val plateBitmap = inputs.platePhotoUri?.let { uri ->
            MediaProcessor.decodeScaledBitmap(context, uri)
        }
        val audioPayload: AudioPayload? = inputs.audioUri?.let { uri ->
            MediaProcessor.readAudioPayload(context, uri)
        }

        val allVisualCount = personBitmaps.size + videoFrames.size + (if (plateBitmap != null) 1 else 0)
        if (allVisualCount == 0 && audioPayload == null &&
            inputs.plateNumberInput.isBlank() && inputs.subjectContextNotes.isBlank()
        ) {
            throw IllegalArgumentException(
                "Please upload at least one photo, video, voice clip, license plate, or context note."
            )
        }

        onStageUpdate("Running multimodal vision, voice & behavioral analysis...")
        val promptText = buildStructuredPrompt(
            inputs = inputs,
            jurisdiction = jurisdiction,
            usageMode = usageMode,
            enabledModules = enabledModules,
            personPhotoCount = personBitmaps.size,
            videoFrameCount = videoFrames.size,
            hasPlatePhoto = plateBitmap != null,
            hasAudio = audioPayload != null
        )

        val structuredResponseText = withTimeout(60_000L) {
            val response = structuredModel.generateContent(
                content {
                    text(promptText)
                    personBitmaps.forEachIndexed { idx, bmp ->
                        text("Person Photo Angle ${idx + 1}:")
                        image(bmp)
                    }
                    videoFrames.forEachIndexed { idx, bmp ->
                        text("Extracted Video Keyframe ${idx + 1}:")
                        image(bmp)
                    }
                    plateBitmap?.let { bmp ->
                        text("License Plate Photo:")
                        image(bmp)
                    }
                    audioPayload?.let { audio ->
                        text("Voice/Audio Clip (${audio.displayName}):")
                        inlineData(audio.bytes, audio.mimeType)
                    }
                }
            )
            response.text ?: throw IllegalStateException("Analysis model returned an empty response.")
        }

        val parsed = parseStructuredJson(
            jsonText = structuredResponseText,
            jurisdiction = jurisdiction,
            usageMode = usageMode
        )

        onStageUpdate("Cross-referencing Amazon pricing, public records & DC/MD/VA sources...")
        val groundedPrompt = buildGroundedSearchPrompt(
            report = parsed,
            inputs = inputs,
            jurisdiction = jurisdiction
        )

        val groundedResult = withTimeout(60_000L) {
            groundedSearchModel.generateContent(
                content {
                    text(groundedPrompt)
                    plateBitmap?.let { bmp ->
                        text("Reference License Plate Photo:")
                        image(bmp)
                    }
                }
            )
        }

        val groundedText = groundedResult.text?.trim().orEmpty()
        val groundingMeta = groundedResult.candidates.firstOrNull()?.groundingMetadata
        val sources = groundingMeta?.groundingChunks?.mapNotNull { chunk ->
            val web = chunk.web
            val uri = web?.uri
            if (!uri.isNullOrBlank()) {
                GroundingSource(
                    title = web.title?.takeIf { it.isNotBlank() } ?: uri,
                    uri = uri
                )
            } else {
                null
            }
        }?.distinctBy { it.uri }.orEmpty()

        val renderedSearchHtml = groundingMeta?.searchEntryPoint?.renderedContent

        // Recycle temporary bitmaps cleanly
        personBitmaps.forEach { if (!it.isRecycled) it.recycle() }
        videoFrames.forEach { if (!it.isRecycled) it.recycle() }
        plateBitmap?.let { if (!it.isRecycled) it.recycle() }

        val finalReport = parsed.copy(
            groundedResearchBrief = groundedText.ifBlank {
                "No additional live web results were returned for the current query parameters."
            },
            groundingSources = sources,
            searchEntryPointHtml = renderedSearchHtml
        )

        saveReport(finalReport)
        return finalReport
    }

    private fun buildStructuredPrompt(
        inputs: MediaInputs,
        jurisdiction: JurisdictionOption,
        usageMode: UsageMode,
        enabledModules: Set<AnalysisFocusModule>,
        personPhotoCount: Int,
        videoFrameCount: Int,
        hasPlatePhoto: Boolean,
        hasAudio: Boolean
    ): String {
        val focusList = enabledModules.joinToString(", ") { it.label }
        val plateState = if (inputs.plateJurisdiction == PlateJurisdictionChoice.AUTO) {
            "Auto-detect from plate design or context"
        } else {
            inputs.plateJurisdiction.label
        }

        return """
You are an objective, thorough multimodal analytical researcher producing a structured multi-module report.
User Jurisdiction Context: ${jurisdiction.displayName} (${jurisdiction.complianceSummary})
Usage Mode: ${usageMode.displayName} (${usageMode.subtitle})
Active Focus Modules: $focusList

Inputs Provided:
- Person Photos: $personPhotoCount
- Video Keyframes: $videoFrameCount
- Voice/Audio Clip Provided: $hasAudio
- License Plate Photo Provided: $hasPlatePhoto
- Entered Plate Number: ${inputs.plateNumberInput.ifBlank { "None" }}
- Selected Plate Jurisdiction: $plateState
- Additional Subject / Context Notes: ${inputs.subjectContextNotes.ifBlank { "None" }}

Strict Scientific, Legal & Grounding Rules:
1. Never invent observations for media types not provided. If no person photo/video is provided, clearly state "No person visual media provided" in visual modules. If no audio is provided, set audioAnalyzed=false and note "No audio provided".
2. Behavioral / Deception Signals: Frame all eye gaze, blinking, brow tension, mouth movement, and vocal hesitation observations through behavioral psychology and non-verbal communication research as indicators of cognitive load, stress, or natural expression—explicitly state these are probabilistic indicators, NEVER absolute proof of truthfulness or deception.
3. Facial Shape & Ancestry: Classify visible facial proportions/landmarks objectively; frame visual regional/ancestry estimates as high-level phenotypic approximations while acknowledging human diversity.
4. Medical & Neurodevelopmental Visual Screening Notes: Only mention visible traits referenced in clinical literature (e.g., craniofacial or behavioral cues sometimes discussed in connection with Down syndrome or autism spectrum observations) when visually relevant, always clearly framed as non-diagnostic informational notes and advising consultation with qualified clinicians.
5. Grooming, Fashion & Psychology: Assess outfit coordination, trend alignment, hair/beard care (for men) or hair styling/makeup/presentation (for women) or age-appropriate care, plus evidence-informed psychological insights on how self-care habits relate to routine, stress, and mood.
6. Clothing & Footwear Cost Estimation: Identify every visible top, bottom, outerwear, footwear, and accessory item. Provide realistic USD retail/Amazon benchmark price ranges (minPriceUsd, maxPriceUsd) and compute accurate sums in totalMinUsd and totalMaxUsd. If no outfit is visible, return an empty items list and 0.0 totals.
7. Watchlist / FBI Check & License Plate Lookup: Note clearly what identifying details or plate characters are observed (read exact plate characters via OCR if a plate photo is shown). Clearly distinguish public search scope vs official government/DMV/MVA portals (DC, Maryland, Virginia DPPA rules). Do not claim official affiliation with any government agency.
        """.trimIndent()
    }

    private fun buildGroundedSearchPrompt(
        report: AnalysisReport,
        inputs: MediaInputs,
        jurisdiction: JurisdictionOption
    ): String {
        val garmentNames = report.outfitCost.items.joinToString("; ") {
            "${it.itemName} (${it.brandOrStyleTier})"
        }.ifBlank { "None identified" }
        val plate = inputs.plateNumberInput.ifBlank { report.licensePlate.detectedPlateNumber }
        val plateJurisdiction = if (inputs.plateJurisdiction != PlateJurisdictionChoice.AUTO) {
            inputs.plateJurisdiction.label
        } else {
            report.licensePlate.detectedJurisdiction
        }

        return """
Using Google Search, verify and summarize publicly available web context for the following items (only search what is relevant and explicitly note when no public matches exist):

1. Retail & Amazon Shopping Price Check:
   Visible garments/footwear identified: $garmentNames
   Check current typical Amazon / major retail price ranges in USD for comparable items and summarize any adjustments or benchmark links.

2. Public Record & FBI Wanted Check:
   User Context / Identifying Details: ${inputs.subjectContextNotes.ifBlank { "No name or identifying text provided." }}
   If a name, alias, or specific public case reference was provided, search public web records and FBI.gov public Wanted listings for any publicly reported mentions or lack of matches. If no identifying text was provided, briefly confirm that public watchlist checks require identifying details or official law-enforcement tools and cannot be run from an anonymous photo alone.

3. License Plate Public Check & DC / Maryland / Virginia Portal Guidance:
   Plate Number: $plate
   Plate Jurisdiction: $plateJurisdiction
   User App Jurisdiction: ${jurisdiction.displayName}
   Search for:
   - Any publicly reported news, incident, or open public-record mentions associated with that plate string (if a real plate was entered/detected), explicitly stating if none are found.
   - Accurate, current public portal procedures for checking unpaid parking tickets, HOV/toll violations (E-ZPass MD / E-ZPass VA / DC DMV), and traffic citations in Washington, D.C., Maryland, and Virginia, clearly noting DPPA restrictions on private owner records.

Keep the response structured with clear headings and bullet points. Do not fabricate matches or URLs.
        """.trimIndent()
    }

    private fun parseStructuredJson(
        jsonText: String,
        jurisdiction: JurisdictionOption,
        usageMode: UsageMode
    ): AnalysisReport {
        val root = JSONObject(jsonText)
        val behavioralObj = root.getJSONObject("behavioral")
        val faceObj = root.getJSONObject("facialStructure")
        val ancestryObj = root.getJSONObject("ancestry")
        val medicalObj = root.getJSONObject("medicalScreening")
        val groomingObj = root.getJSONObject("grooming")
        val voiceObj = root.getJSONObject("voice")
        val outfitObj = root.getJSONObject("outfitCost")
        val watchlistObj = root.getJSONObject("watchlist")
        val plateObj = root.getJSONObject("licensePlate")

        val limitationsArr = root.getJSONArray("keyLimitations")
        val limitations = (0 until limitationsArr.length()).map { limitationsArr.getString(it) }

        val traitArr = ancestryObj.getJSONArray("traitEstimates")
        val traitEstimates = (0 until traitArr.length()).map { traitArr.getString(it) }

        val itemsArr = outfitObj.getJSONArray("items")
        val garments = (0 until itemsArr.length()).map { i ->
            val obj = itemsArr.getJSONObject(i)
            GarmentEstimate(
                itemName = obj.getString("itemName"),
                category = obj.getString("category"),
                brandOrStyleTier = obj.getString("brandOrStyleTier"),
                benchmarkSource = obj.getString("benchmarkSource"),
                minPriceUsd = obj.optDouble("minPriceUsd", 0.0),
                maxPriceUsd = obj.optDouble("maxPriceUsd", 0.0)
            )
        }

        val computedMin = if (garments.isNotEmpty()) garments.sumOf { it.minPriceUsd } else outfitObj.optDouble("totalMinUsd", 0.0)
        val computedMax = if (garments.isNotEmpty()) garments.sumOf { it.maxPriceUsd } else outfitObj.optDouble("totalMaxUsd", 0.0)

        val timestamp = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date())

        return AnalysisReport(
            reportId = UUID.randomUUID().toString().take(8).uppercase(Locale.US),
            timestampFormatted = timestamp,
            jurisdiction = jurisdiction,
            usageMode = usageMode,
            executiveSummary = root.getString("executiveSummary"),
            overallConfidence = root.getString("overallConfidence"),
            keyLimitations = limitations,
            behavioral = BehavioralSection(
                gazeAndBlinking = behavioralObj.getString("gazeAndBlinking"),
                eyebrowAndForehead = behavioralObj.getString("eyebrowAndForehead"),
                mouthAndLipMovement = behavioralObj.getString("mouthAndLipMovement"),
                cognitiveLoadVsNaturalSignals = behavioralObj.getString("cognitiveLoadVsNaturalSignals"),
                analyticalProbabilityNote = behavioralObj.getString("analyticalProbabilityNote"),
                researchContext = behavioralObj.getString("researchContext")
            ),
            facialStructure = FacialStructureSection(
                faceShapeClassification = faceObj.getString("faceShapeClassification"),
                proportionsAndSymmetry = faceObj.getString("proportionsAndSymmetry"),
                landmarkObservations = faceObj.getString("landmarkObservations"),
                anthropometricReference = faceObj.getString("anthropometricReference")
            ),
            ancestry = AncestrySection(
                traitEstimates = traitEstimates,
                confidenceAndNuance = ancestryObj.getString("confidenceAndNuance"),
                diversityNote = ancestryObj.getString("diversityNote")
            ),
            medicalScreening = MedicalScreeningSection(
                craniofacialObservations = medicalObj.getString("craniofacialObservations"),
                neurodevelopmentalObservations = medicalObj.getString("neurodevelopmentalObservations"),
                clinicalReferenceContext = medicalObj.getString("clinicalReferenceContext"),
                nonDiagnosticDisclaimer = medicalObj.getString("nonDiagnosticDisclaimer")
            ),
            grooming = GroomingSection(
                clothingStyleAndCoordination = groomingObj.getString("clothingStyleAndCoordination"),
                trendAndPersonalAlignment = groomingObj.getString("trendAndPersonalAlignment"),
                hairFacialAndMakeupCare = groomingObj.getString("hairFacialAndMakeupCare"),
                selfCarePresentation = groomingObj.getString("selfCarePresentation"),
                psychologicalInsights = groomingObj.getString("psychologicalInsights")
            ),
            voice = VoiceSection(
                audioAnalyzed = voiceObj.optBoolean("audioAnalyzed", false),
                toneAndPitch = voiceObj.getString("toneAndPitch"),
                paceAndPauses = voiceObj.getString("paceAndPauses"),
                vocalTensionAndStyle = voiceObj.getString("vocalTensionAndStyle"),
                communicationCredibilityNote = voiceObj.getString("communicationCredibilityNote")
            ),
            outfitCost = OutfitCostSection(
                items = garments,
                totalMinUsd = computedMin,
                totalMaxUsd = computedMax,
                shoppingBenchmarkSummary = outfitObj.getString("shoppingBenchmarkSummary")
            ),
            watchlist = WatchlistSection(
                queryBasis = watchlistObj.getString("queryBasis"),
                fbiAndPublicRecordSummary = watchlistObj.getString("fbiAndPublicRecordSummary"),
                potentialMatchesOrLeads = watchlistObj.getString("potentialMatchesOrLeads"),
                verificationGuidance = watchlistObj.getString("verificationGuidance")
            ),
            licensePlate = LicensePlateSection(
                detectedPlateNumber = plateObj.getString("detectedPlateNumber"),
                detectedJurisdiction = plateObj.getString("detectedJurisdiction"),
                dcMdVaSummary = plateObj.getString("dcMdVaSummary"),
                parkingAndTollsNote = plateObj.getString("parkingAndTollsNote"),
                trafficAndIncidentNote = plateObj.getString("trafficAndIncidentNote"),
                officialPortalRequirement = plateObj.getString("officialPortalRequirement")
            ),
            groundedResearchBrief = "",
            groundingSources = emptyList(),
            searchEntryPointHtml = null,
            metrics = root.optJSONObject("metrics")?.let { m ->
                ReportMetrics(
                    behavioralComposureScore = m.optInt("behavioralComposureScore", 70).coerceIn(0, 100),
                    cognitiveLoadScore = m.optInt("cognitiveLoadScore", 30).coerceIn(0, 100),
                    facialSymmetryScore = m.optInt("facialSymmetryScore", 75).coerceIn(0, 100),
                    ancestryConfidenceScore = m.optInt("ancestryConfidenceScore", 60).coerceIn(0, 100),
                    groomingCareScore = m.optInt("groomingCareScore", 75).coerceIn(0, 100),
                    styleCoordinationScore = m.optInt("styleCoordinationScore", 74).coerceIn(0, 100),
                    voiceClarityScore = if (voiceObj.optBoolean("audioAnalyzed", false)) {
                        m.optInt("voiceClarityScore", 72).coerceIn(0, 100)
                    } else 0,
                    publicRecordConfidenceScore = m.optInt("publicRecordConfidenceScore", 65).coerceIn(0, 100)
                )
            } ?: ReportMetrics(
                voiceClarityScore = if (voiceObj.optBoolean("audioAnalyzed", false)) 72 else 0
            )
        )
    }

    fun formatReportForExport(report: AnalysisReport): String = buildString {
        appendLine("CLARITY LENS — MULTIMODAL ANALYTICAL REPORT")
        appendLine("Report ID: ${report.reportId} | ${report.timestampFormatted}")
        appendLine("Jurisdiction: ${report.jurisdiction.displayName}")
        appendLine("Usage Mode: ${report.usageMode.displayName}")
        appendLine("Overall Confidence: ${report.overallConfidence}")
        appendLine()
        appendLine("=== EXECUTIVE SUMMARY ===")
        appendLine(report.executiveSummary)
        appendLine()
        appendLine("=== BEHAVIORAL & NON-VERBAL CUES ===")
        appendLine("Gaze & Blinking: ${report.behavioral.gazeAndBlinking}")
        appendLine("Eyebrow & Forehead: ${report.behavioral.eyebrowAndForehead}")
        appendLine("Mouth & Lips: ${report.behavioral.mouthAndLipMovement}")
        appendLine("Cognitive Load vs Natural: ${report.behavioral.cognitiveLoadVsNaturalSignals}")
        appendLine("Probability Note: ${report.behavioral.analyticalProbabilityNote}")
        appendLine()
        appendLine("=== FACIAL SHAPE & ANTHROPOMETRY ===")
        appendLine("Classification: ${report.facialStructure.faceShapeClassification}")
        appendLine("Proportions: ${report.facialStructure.proportionsAndSymmetry}")
        appendLine("Landmarks: ${report.facialStructure.landmarkObservations}")
        appendLine()
        appendLine("=== ANCESTRY / REGIONAL VISUAL ESTIMATION ===")
        report.ancestry.traitEstimates.forEach { appendLine("- $it") }
        appendLine("Nuance: ${report.ancestry.confidenceAndNuance}")
        appendLine()
        appendLine("=== MEDICAL & NEURODEVELOPMENTAL SCREENING NOTES (NON-DIAGNOSTIC) ===")
        appendLine("Craniofacial: ${report.medicalScreening.craniofacialObservations}")
        appendLine("Neurodevelopmental: ${report.medicalScreening.neurodevelopmentalObservations}")
        appendLine("Disclaimer: ${report.medicalScreening.nonDiagnosticDisclaimer}")
        appendLine()
        appendLine("=== GROOMING, FASHION & PSYCHOLOGICAL INSIGHTS ===")
        appendLine("Style & Colors: ${report.grooming.clothingStyleAndCoordination}")
        appendLine("Grooming Care: ${report.grooming.hairFacialAndMakeupCare}")
        appendLine("Psychological Context: ${report.grooming.psychologicalInsights}")
        appendLine()
        appendLine("=== VOICE & ACOUSTIC ANALYSIS ===")
        appendLine("Tone & Pitch: ${report.voice.toneAndPitch}")
        appendLine("Pace & Pauses: ${report.voice.paceAndPauses}")
        appendLine("Credibility Note: ${report.voice.communicationCredibilityNote}")
        appendLine()
        appendLine("=== CLOTHING & FOOTWEAR COST ESTIMATION ===")
        report.outfitCost.items.forEach {
            appendLine("- ${it.itemName} (${it.category}): $${it.minPriceUsd.toInt()}–$${it.maxPriceUsd.toInt()} [${it.benchmarkSource}]")
        }
        appendLine("Estimated Total: $${report.outfitCost.totalMinUsd.toInt()}–$${report.outfitCost.totalMaxUsd.toInt()} USD")
        appendLine()
        appendLine("=== FBI & PUBLIC WATCHLIST CHECK ===")
        appendLine(report.watchlist.fbiAndPublicRecordSummary)
        appendLine("Leads/Matches: ${report.watchlist.potentialMatchesOrLeads}")
        appendLine()
        appendLine("=== LICENSE PLATE LOOKUP (DC / MD / VA & GENERAL) ===")
        appendLine("Plate: ${report.licensePlate.detectedPlateNumber} (${report.licensePlate.detectedJurisdiction})")
        appendLine(report.licensePlate.dcMdVaSummary)
        appendLine("Parking/Tolls: ${report.licensePlate.parkingAndTollsNote}")
        appendLine()
        appendLine("=== LIVE SEARCH GROUNDING BRIEF ===")
        appendLine(report.groundedResearchBrief)
        if (report.groundingSources.isNotEmpty()) {
            appendLine()
            appendLine("Sources:")
            report.groundingSources.forEach { appendLine("- ${it.title}: ${it.uri}") }
        }
    }
}
