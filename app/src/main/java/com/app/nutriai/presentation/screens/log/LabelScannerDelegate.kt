package com.app.nutriai.presentation.screens.log

import android.net.Uri
import com.app.nutriai.domain.usecase.ExtractLabelUseCase
import com.app.nutriai.util.ImageCompressor
import com.app.nutriai.util.Constants
import com.app.nutriai.util.Resource
import com.app.nutriai.util.formatMacro
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Handles nutrition label photo scanning via Gemma 4.
 *
 * Extracted from [LogViewModel] to isolate label-scanning concerns:
 * - Image compression and base64 encoding
 * - Multimodal AI request for label reading
 * - Prefilling manual form fields with extracted macros
 *
 * The delegate does NOT own the [MutableStateFlow] — it receives a reference
 * from the ViewModel and updates it in-place.
 */
class LabelScannerDelegate(
    private val extractLabelUseCase: ExtractLabelUseCase,
    private val imageCompressor: ImageCompressor,
    private val uiState: MutableStateFlow<LogUiState>,
    private val coroutineScope: CoroutineScope
) {

    /**
     * Compress and send a label photo to Gemma 4 for nutrition extraction.
     *
     * On success: prefills the manual form with extracted macros and switches to MANUAL_INPUT.
     * On failure: sets [LogUiState.labelExtractionError] so the UI can show a retry option.
     *
     * @param uri        URI of the selected/captured image (content:// or file://)
     * @param sourceType "gallery" or "camera"
     */
    fun onLabelPhotoSelected(uri: Uri, sourceType: String = "gallery") {
        coroutineScope.launch {
            uiState.update {
                it.copy(
                    isExtractingLabel = true,
                    labelExtractionError = null
                )
            }
            try {
                val compressed = imageCompressor.compressAndSave(uri, sourceType)

                when (val result = extractLabelUseCase(compressed.base64, compressed.mimeType)) {
                    is Resource.Success -> {
                        val data = result.data
                        val hasServingWeight = data.servingWeightG != null && data.servingWeightG > 0
                        val w = data.servingWeightG ?: 1.0
                        uiState.update {
                            it.copy(
                                isExtractingLabel = false,
                                labelExtractionError = null,
                                labelPhotoId = compressed.entity.id,
                                inputMode = LogInputMode.MANUAL_INPUT,
                                calories = if (hasServingWeight)
                                    (data.caloriesPerServing / w * Constants.PER_100G_BASE).formatMacro()
                                else data.caloriesPerServing.formatMacro(),
                                protein = if (hasServingWeight)
                                    (data.proteinG / w * Constants.PER_100G_BASE).formatMacro()
                                else data.proteinG.formatMacro(),
                                carbs = if (hasServingWeight)
                                    (data.carbsG / w * Constants.PER_100G_BASE).formatMacro()
                                else data.carbsG.formatMacro(),
                                fat = if (hasServingWeight)
                                    (data.fatG / w * Constants.PER_100G_BASE).formatMacro()
                                else data.fatG.formatMacro(),
                                quantity = if (hasServingWeight) w.formatMacro() else "1",
                                unit = if (hasServingWeight) "g" else "serving"
                            )
                        }
                    }
                    is Resource.Error -> {
                        uiState.update {
                            it.copy(
                                isExtractingLabel = false,
                                labelExtractionError = result.message
                                    ?: "Could not read the label. Please try again or enter values manually."
                            )
                        }
                    }
                    is Resource.Loading -> { /* suspend function — won't reach here */ }
                }
            } catch (e: Exception) {
                uiState.update {
                    it.copy(
                        isExtractingLabel = false,
                        labelExtractionError = "Failed to process the image. Please try again or enter values manually."
                    )
                }
            }
        }
    }

    /** Clear label extraction state (error + photo ID) without affecting form fields. */
    fun clearLabelExtraction() {
        uiState.update {
            it.copy(
                isExtractingLabel = false,
                labelExtractionError = null,
                labelPhotoId = null
            )
        }
    }
}
