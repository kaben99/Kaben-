package com.example.data.local

import com.example.model.AnalysisReport
import com.example.model.AncestrySection
import com.example.model.BehavioralSection
import com.example.model.FacialStructureSection
import com.example.model.GarmentEstimate
import com.example.model.GroomingSection
import com.example.model.GroundingSource
import com.example.model.JurisdictionOption
import com.example.model.LicensePlateSection
import com.example.model.MedicalScreeningSection
import com.example.model.OutfitCostSection
import com.example.model.ReportMetrics
import com.example.model.UsageMode
import com.example.model.VoiceSection
import com.example.model.WatchlistSection
import org.json.JSONArray
import org.json.JSONObject

object ReportJsonSerializer {

    fun toEntity(
        report: AnalysisReport,
        createdAtMillis: Long = System.currentTimeMillis()
    ): ReportEntity {
        val root = JSONObject().apply {
            put("reportId", report.reportId)
            put("timestampFormatted", report.timestampFormatted)
            put("jurisdictionId", report.jurisdiction.id)
            put("usageModeId", report.usageMode.id)
            put("executiveSummary", report.executiveSummary)
            put("overallConfidence", report.overallConfidence)
            put("keyLimitations", JSONArray(report.keyLimitations))

            put("behavioral", JSONObject().apply {
                put("gazeAndBlinking", report.behavioral.gazeAndBlinking)
                put("eyebrowAndForehead", report.behavioral.eyebrowAndForehead)
                put("mouthAndLipMovement", report.behavioral.mouthAndLipMovement)
                put("cognitiveLoadVsNaturalSignals", report.behavioral.cognitiveLoadVsNaturalSignals)
                put("analyticalProbabilityNote", report.behavioral.analyticalProbabilityNote)
                put("researchContext", report.behavioral.researchContext)
            })

            put("facialStructure", JSONObject().apply {
                put("faceShapeClassification", report.facialStructure.faceShapeClassification)
                put("proportionsAndSymmetry", report.facialStructure.proportionsAndSymmetry)
                put("landmarkObservations", report.facialStructure.landmarkObservations)
                put("anthropometricReference", report.facialStructure.anthropometricReference)
            })

            put("ancestry", JSONObject().apply {
                put("traitEstimates", JSONArray(report.ancestry.traitEstimates))
                put("confidenceAndNuance", report.ancestry.confidenceAndNuance)
                put("diversityNote", report.ancestry.diversityNote)
            })

            put("medicalScreening", JSONObject().apply {
                put("craniofacialObservations", report.medicalScreening.craniofacialObservations)
                put("neurodevelopmentalObservations", report.medicalScreening.neurodevelopmentalObservations)
                put("clinicalReferenceContext", report.medicalScreening.clinicalReferenceContext)
                put("nonDiagnosticDisclaimer", report.medicalScreening.nonDiagnosticDisclaimer)
            })

            put("grooming", JSONObject().apply {
                put("clothingStyleAndCoordination", report.grooming.clothingStyleAndCoordination)
                put("trendAndPersonalAlignment", report.grooming.trendAndPersonalAlignment)
                put("hairFacialAndMakeupCare", report.grooming.hairFacialAndMakeupCare)
                put("selfCarePresentation", report.grooming.selfCarePresentation)
                put("psychologicalInsights", report.grooming.psychologicalInsights)
            })

            put("voice", JSONObject().apply {
                put("audioAnalyzed", report.voice.audioAnalyzed)
                put("toneAndPitch", report.voice.toneAndPitch)
                put("paceAndPauses", report.voice.paceAndPauses)
                put("vocalTensionAndStyle", report.voice.vocalTensionAndStyle)
                put("communicationCredibilityNote", report.voice.communicationCredibilityNote)
            })

            put("outfitCost", JSONObject().apply {
                val itemsArray = JSONArray()
                report.outfitCost.items.forEach { item ->
                    itemsArray.put(JSONObject().apply {
                        put("itemName", item.itemName)
                        put("category", item.category)
                        put("brandOrStyleTier", item.brandOrStyleTier)
                        put("benchmarkSource", item.benchmarkSource)
                        put("minPriceUsd", item.minPriceUsd)
                        put("maxPriceUsd", item.maxPriceUsd)
                    })
                }
                put("items", itemsArray)
                put("totalMinUsd", report.outfitCost.totalMinUsd)
                put("totalMaxUsd", report.outfitCost.totalMaxUsd)
                put("shoppingBenchmarkSummary", report.outfitCost.shoppingBenchmarkSummary)
            })

            put("watchlist", JSONObject().apply {
                put("queryBasis", report.watchlist.queryBasis)
                put("fbiAndPublicRecordSummary", report.watchlist.fbiAndPublicRecordSummary)
                put("potentialMatchesOrLeads", report.watchlist.potentialMatchesOrLeads)
                put("verificationGuidance", report.watchlist.verificationGuidance)
            })

            put("licensePlate", JSONObject().apply {
                put("detectedPlateNumber", report.licensePlate.detectedPlateNumber)
                put("detectedJurisdiction", report.licensePlate.detectedJurisdiction)
                put("dcMdVaSummary", report.licensePlate.dcMdVaSummary)
                put("parkingAndTollsNote", report.licensePlate.parkingAndTollsNote)
                put("trafficAndIncidentNote", report.licensePlate.trafficAndIncidentNote)
                put("officialPortalRequirement", report.licensePlate.officialPortalRequirement)
            })

            put("groundedResearchBrief", report.groundedResearchBrief)

            val sourcesArray = JSONArray()
            report.groundingSources.forEach { src ->
                sourcesArray.put(JSONObject().apply {
                    put("title", src.title)
                    put("uri", src.uri)
                })
            }
            put("groundingSources", sourcesArray)
            report.searchEntryPointHtml?.let { put("searchEntryPointHtml", it) }
            put("metrics", JSONObject().apply {
                put("behavioralComposureScore", report.metrics.behavioralComposureScore)
                put("cognitiveLoadScore", report.metrics.cognitiveLoadScore)
                put("facialSymmetryScore", report.metrics.facialSymmetryScore)
                put("ancestryConfidenceScore", report.metrics.ancestryConfidenceScore)
                put("groomingCareScore", report.metrics.groomingCareScore)
                put("styleCoordinationScore", report.metrics.styleCoordinationScore)
                put("voiceClarityScore", report.metrics.voiceClarityScore)
                put("publicRecordConfidenceScore", report.metrics.publicRecordConfidenceScore)
            })
        }

        val costRange = if (report.outfitCost.items.isNotEmpty()) {
            "$${report.outfitCost.totalMinUsd.toInt()}–$${report.outfitCost.totalMaxUsd.toInt()} USD"
        } else {
            "N/A"
        }

        return ReportEntity(
            reportId = report.reportId,
            createdAtMillis = createdAtMillis,
            timestampFormatted = report.timestampFormatted,
            jurisdictionId = report.jurisdiction.id,
            usageModeId = report.usageMode.id,
            executiveSummary = report.executiveSummary,
            overallConfidence = report.overallConfidence,
            plateNumberSummary = report.licensePlate.detectedPlateNumber,
            estimatedCostRange = costRange,
            payloadJson = root.toString()
        )
    }

    fun toDomain(entity: ReportEntity): AnalysisReport {
        val root = JSONObject(entity.payloadJson)
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

        val sourcesArr = root.optJSONArray("groundingSources") ?: JSONArray()
        val sources = (0 until sourcesArr.length()).map { i ->
            val obj = sourcesArr.getJSONObject(i)
            GroundingSource(
                title = obj.optString("title", ""),
                uri = obj.optString("uri", "")
            )
        }.filter { it.uri.isNotBlank() }

        val searchHtml = if (root.has("searchEntryPointHtml") && !root.isNull("searchEntryPointHtml")) {
            root.getString("searchEntryPointHtml")
        } else {
            null
        }

        return AnalysisReport(
            reportId = root.optString("reportId", entity.reportId),
            timestampFormatted = root.optString("timestampFormatted", entity.timestampFormatted),
            jurisdiction = JurisdictionOption.fromId(root.optString("jurisdictionId", entity.jurisdictionId)),
            usageMode = UsageMode.fromId(root.optString("usageModeId", entity.usageModeId)),
            executiveSummary = root.optString("executiveSummary", entity.executiveSummary),
            overallConfidence = root.optString("overallConfidence", entity.overallConfidence),
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
                totalMinUsd = outfitObj.optDouble("totalMinUsd", 0.0),
                totalMaxUsd = outfitObj.optDouble("totalMaxUsd", 0.0),
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
            groundedResearchBrief = root.optString("groundedResearchBrief", ""),
            groundingSources = sources,
            searchEntryPointHtml = searchHtml,
            metrics = root.optJSONObject("metrics")?.let { m ->
                ReportMetrics(
                    behavioralComposureScore = m.optInt("behavioralComposureScore", 74).coerceIn(0, 100),
                    cognitiveLoadScore = m.optInt("cognitiveLoadScore", 26).coerceIn(0, 100),
                    facialSymmetryScore = m.optInt("facialSymmetryScore", 80).coerceIn(0, 100),
                    ancestryConfidenceScore = m.optInt("ancestryConfidenceScore", 64).coerceIn(0, 100),
                    groomingCareScore = m.optInt("groomingCareScore", 78).coerceIn(0, 100),
                    styleCoordinationScore = m.optInt("styleCoordinationScore", 75).coerceIn(0, 100),
                    voiceClarityScore = m.optInt("voiceClarityScore", 0).coerceIn(0, 100),
                    publicRecordConfidenceScore = m.optInt("publicRecordConfidenceScore", 60).coerceIn(0, 100)
                )
            } ?: ReportMetrics()
        )
    }
}
