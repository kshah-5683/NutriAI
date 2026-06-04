package com.app.nutriai.presentation.screens.log

import android.net.Uri
import com.app.nutriai.domain.usecase.ExtractLabelUseCase
import com.app.nutriai.util.ImageCompressor
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
                        // Per-100g conversion and form hints are now pre-computed
                        // by the scan-label Edge Function — no client-side math needed.
                        uiState.update {
                            it.copy(
                                isExtractingLabel = false,
                                labelExtractionError = null,
                                labelPhotoId = compressed.entity.id,
                                inputMode = LogInputMode.MANUAL_INPUT,
                                calories = data.calories.formatMacro(),
                                protein = data.protein.formatMacro(),
                                carbs = data.carbs.formatMacro(),
                                fat = data.fat.formatMacro(),
                                quantity = data.suggestedQuantity.formatMacro(),
                                unit = if (data.suggestedUnit == "grams") "g" else data.suggestedUnit
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
