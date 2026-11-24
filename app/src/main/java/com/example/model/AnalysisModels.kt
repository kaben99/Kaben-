package com.example.model

import android.net.Uri

enum class JurisdictionOption(
    val id: String,
    val displayName: String,
    val complianceSummary: String
) {
    US_DMV_CORRIDOR(
        id = "us_dmv",
        displayName = "United States — DC / Maryland / Virginia",
        complianceSummary = "Includes DC DMV, Maryland MVA/E-ZPass, Virginia DMV/E-ZPass public citation context and US DPPA privacy boundaries."
    ),
    US_OTHER(
        id = "us_other",
        displayName = "United States — Other States",
        complianceSummary = "Operates under general US public-record & DPPA informational boundaries; outputs are analytical indicators only."
    ),
    ETHIOPIA(
        id = "ethiopia",
        displayName = "Ethiopia",
        complianceSummary = "Open global analytical access; local privacy and Ethiopian federal/regional legal review applies when used professionally."
    ),
    UK(
        id = "uk",
        displayName = "United Kingdom",
        complianceSummary = "Subject to UK GDPR & Data Protection Act awareness; observational notes are non-diagnostic and non-evidentiary."
    ),
    EU(
        id = "eu",
        displayName = "European Union (GDPR)",
        complianceSummary = "EU GDPR transparency context; biometric & visual estimates are high-level analytical indicators only."
    ),
    UAE(
        id = "uae",
        displayName = "United Arab Emirates",
        complianceSummary = "UAE federal privacy context; use strictly for lawful research, personal review, or authorized commercial analysis."
    ),
    CANADA(
        id = "canada",
        displayName = "Canada",
        complianceSummary = "PIPEDA-aware informational context; visual, voice, and public lookup insights require authorized verification."
    ),
    GLOBAL_OTHER(
        id = "global",
        displayName = "Other Country / International",
        complianceSummary = "Worldwide open-access mode; user is responsible for ensuring compliance with local data and privacy laws."
    );

    companion object {
        fun fromId(id: String?): JurisdictionOption =
            entries.find { it.id == id } ?: US_DMV_CORRIDOR
    }
}

enum class UsageMode(
    val id: String,
    val displayName: String,
    val subtitle: String
) {
    BUSINESS(
        id = "business",
        displayName = "Business",
        subtitle = "Commercial retail, fashion & communication review"
    ),
    LEGAL(
        id = "legal",
        displayName = "Legal",
        subtitle = "Investigative cues, citations & public record leads"
    ),
    PERSONAL(
        id = "personal",
        displayName = "Personal",
        subtitle = "Everyday visual, style, voice & plate lookup"
    );

    companion object {
        fun fromId(id: String?): UsageMode =
            entries.find { it.id == id } ?: PERSONAL
    }
}

enum class AnalysisFocusModule(
    val id: String,
    val label: String
) {
    BEHAVIORAL_FACE("behavioral_face", "Face, Eye & Truth Signals"),
    SHAPE_ANCESTRY("shape_ancestry", "Facial Shape & Ancestry"),
    MEDICAL_NOTES("medical_notes", "Medical & Neuro Notes"),
    GROOMING_PSYCH("grooming_psych", "Grooming & Psychology"),
    VOICE_ACOUSTIC("voice_acoustic", "Voice & Acoustic Tone"),
    OUTFIT_COST("outfit_cost", "Outfit & Shoe Pricing"),
    WATCHLIST_FBI("watchlist_fbi", "FBI & Public Watchlist"),
    LICENSE_PLATE("license_plate", "License Plate (DC/MD/VA)")
}

enum class PlateJurisdictionChoice(val label: String) {
    AUTO("Auto-detect"),
    DC("Washington, D.C."),
    MARYLAND("Maryland"),
    VIRGINIA("Virginia"),
    OTHER("Other US / Intl")
}

data class MediaInputs(
    val personPhotos: List<Uri> = emptyList(),
    val videoUri: Uri? = null,
    val videoFrameCount: Int = 0,
    val audioUri: Uri? = null,
    val audioDisplayName: String? = null,
    val isRecordedAudio: Boolean = false,
    val platePhotoUri: Uri? = null,
    val plateNumberInput: String = "",
    val plateJurisdiction: PlateJurisdictionChoice = PlateJurisdictionChoice.AUTO,
    val subjectContextNotes: String = ""
) {
    val hasAnyInput: Boolean
        get() = personPhotos.isNotEmpty() ||
            videoUri != null ||
            audioUri != null ||
            platePhotoUri != null ||
            plateNumberInput.isNotBlank() ||
            subjectContextNotes.isNotBlank()
}

data class BehavioralSection(
    val gazeAndBlinking: String,
    val eyebrowAndForehead: String,
    val mouthAndLipMovement: String,
    val cognitiveLoadVsNaturalSignals: String,
    val analyticalProbabilityNote: String,
    val researchContext: String
)

data class FacialStructureSection(
    val faceShapeClassification: String,
    val proportionsAndSymmetry: String,
    val landmarkObservations: String,
    val anthropometricReference: String
)

data class AncestrySection(
    val traitEstimates: List<String>,
    val confidenceAndNuance: String,
    val diversityNote: String
)

data class MedicalScreeningSection(
    val craniofacialObservations: String,
    val neurodevelopmentalObservations: String,
    val clinicalReferenceContext: String,
    val nonDiagnosticDisclaimer: String
)

data class GroomingSection(
    val clothingStyleAndCoordination: String,
    val trendAndPersonalAlignment: String,
    val hairFacialAndMakeupCare: String,
    val selfCarePresentation: String,
    val psychologicalInsights: String
)

data class VoiceSection(
    val audioAnalyzed: Boolean,
    val toneAndPitch: String,
    val paceAndPauses: String,
    val vocalTensionAndStyle: String,
    val communicationCredibilityNote: String
)

data class GarmentEstimate(
    val itemName: String,
    val category: String,
    val brandOrStyleTier: String,
    val benchmarkSource: String,
    val minPriceUsd: Double,
    val maxPriceUsd: Double
)

data class OutfitCostSection(
    val items: List<GarmentEstimate>,
    val totalMinUsd: Double,
    val totalMaxUsd: Double,
    val shoppingBenchmarkSummary: String
)

data class WatchlistSection(
    val queryBasis: String,
    val fbiAndPublicRecordSummary: String,
    val potentialMatchesOrLeads: String,
    val verificationGuidance: String
)

data class LicensePlateSection(
    val detectedPlateNumber: String,
    val detectedJurisdiction: String,
    val dcMdVaSummary: String,
    val parkingAndTollsNote: String,
    val trafficAndIncidentNote: String,
    val officialPortalRequirement: String
)

data class GroundingSource(
    val title: String,
    val uri: String
)

data class ReportMetrics(
    val behavioralComposureScore: Int = 74,
    val cognitiveLoadScore: Int = 26,
    val facialSymmetryScore: Int = 80,
    val ancestryConfidenceScore: Int = 64,
    val groomingCareScore: Int = 78,
    val styleCoordinationScore: Int = 75,
    val voiceClarityScore: Int = 0,
    val publicRecordConfidenceScore: Int = 60
)

data class AnalysisReport(
    val reportId: String,
    val timestampFormatted: String,
    val jurisdiction: JurisdictionOption,
    val usageMode: UsageMode,
    val executiveSummary: String,
    val overallConfidence: String,
    val keyLimitations: List<String>,
    val behavioral: BehavioralSection,
    val facialStructure: FacialStructureSection,
    val ancestry: AncestrySection,
    val medicalScreening: MedicalScreeningSection,
    val grooming: GroomingSection,
    val voice: VoiceSection,
    val outfitCost: OutfitCostSection,
    val watchlist: WatchlistSection,
    val licensePlate: LicensePlateSection,
    val groundedResearchBrief: String,
    val groundingSources: List<GroundingSource>,
    val searchEntryPointHtml: String?,
    val metrics: ReportMetrics = ReportMetrics()
)
