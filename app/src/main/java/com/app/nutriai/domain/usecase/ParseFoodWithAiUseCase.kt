package com.app.nutriai.domain.usecase

import com.app.nutriai.domain.model.ParsedFood
import com.app.nutriai.domain.repository.AiRepository
import com.app.nutriai.util.Resource
import javax.inject.Inject

/**
 * Wraps [AiRepository.parseFood] with input validation and error mapping.
 *
 * This UseCase is the single entry point for AI food parsing in the presentation layer.
 * It validates the input before making the API call and provides user-friendly error messages.
 *
 * Usage from ViewModel:
 * ```
 * val result = parseFoodWithAiUseCase("2 slices of toast with butter")
 * when (result) {
 *     is Resource.Success -> // auto-fill form with result.data
 *     is Resource.Error -> // show error, user falls back to manual entry
 *     is Resource.Loading -> // shouldn't happen (suspend function)
 * }
 * ```
 */
class ParseFoodWithAiUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    /**
     * Parse a natural language food description into structured food entities.
     *
     * @param input The user's food description (e.g., "2 eggs and a glass of milk")
     * @return [Resource.Success] with list of [ParsedFood], or [Resource.Error] with message
     */
    suspend operator fun invoke(input: String): Resource<List<ParsedFood>> {
        // Input validation
        val trimmed = input.trim()

        if (trimmed.isBlank()) {
            return Resource.Error("Please describe what you ate.")
        }

        if (trimmed.length < 2) {
            return Resource.Error("Please provide a more detailed food description.")
        }

        if (trimmed.length > 500) {
            return Resource.Error("Food description is too long. Please keep it under 500 characters.")
        }

        return aiRepository.parseFood(trimmed)
    }
}
