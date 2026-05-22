package com.app.nutriai.domain.usecase

import com.app.nutriai.domain.model.ExtractedLabelData
import com.app.nutriai.domain.repository.AiRepository
import com.app.nutriai.util.Resource
import javax.inject.Inject

/**
 * Use case for extracting per-serving nutrition data from a food label photo.
 *
 * Thin wrapper around [AiRepository.extractLabelFromImage] — keeps the ViewModel
 * free of repository dependencies and makes the operation independently testable.
 *
 * Phase 11: Nutrition Label Scanner.
 *
 * @param imageBase64 Base64-encoded JPEG/PNG bytes (no data-URI prefix)
 * @param mimeType    Image MIME type — typically "image/jpeg"
 */
class ExtractLabelUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(
        imageBase64: String,
        mimeType: String = "image/jpeg"
    ): Resource<ExtractedLabelData> {
        return aiRepository.extractLabelFromImage(imageBase64, mimeType)
    }
}
