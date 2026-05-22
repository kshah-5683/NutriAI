package com.app.nutriai.data.remote.api

import com.app.nutriai.data.remote.dto.GeminiRequest
import com.app.nutriai.data.remote.dto.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface for the Google Gemini API.
 *
 * Uses the `generateContent` endpoint with API key authentication via query parameter.
 * The base URL is set in [com.app.nutriai.di.AppModule] as `@Named("gemini")` Retrofit.
 *
 * Model: gemma-4-26b-a4b-it — Google's open Gemma 4 model (Apache 2.0), hosted on the
 * Gemini API. 26B parameters with Active 4B routing for efficient inference.
 * Supports structured JSON output and 256K context.
 * Future-proof against Gemini model deprecations (2.0 shutdown June 2026, 2.5 eventually).
 */
interface GeminiApiService {

    /**
     * Send a prompt to Gemma 4 via the Gemini API and receive a generated response.
     *
     * @param apiKey Gemini API key passed as query parameter
     * @param request Request body containing prompt content and generation config
     * @return [GeminiResponse] with candidates containing the generated text
     */
    @POST("v1beta/models/gemma-4-26b-a4b-it:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
