package com.app.nutriai.di

import com.app.nutriai.BuildConfig
import com.app.nutriai.data.remote.api.FoodDataCentralApiService
import com.app.nutriai.util.Constants
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton


/**
 * Hilt module providing app-level singleton dependencies.
 * Includes networking (OkHttp, Retrofit), serialization, and API service interfaces.
 *
 * Phase 5.5: FoodDataCentralApiService wired via @Named("fdc") Retrofit with
 *            a dedicated OkHttpClient. API key injected via @Named("fdcApiKey").
 *
 * Note: Gemini API networking (OkHttpClient, @Named("gemini") Retrofit, GeminiApiService)
 * was removed during the Edge Function migration. AI calls now go through
 * SupabaseEdgeFunctionService (provided by SupabaseModule).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Shared JSON configuration for kotlinx.serialization.
     *
     * - `ignoreUnknownKeys = true` — tolerates extra fields in API responses
     * - `isLenient = true` — accepts non-strict JSON (quoted numbers, etc.)
     * - `encodeDefaults = true` — includes default-valued fields in serialized output
     * - `explicitNulls = false` — omits null fields from serialized output (required by
     *   some APIs that reject explicit `"key": null`)
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    /**
     * Dedicated OkHttpClient for USDA FoodData Central.
     *
     * FDC is a public USDA endpoint — must bypass corporate proxy on VPN.
     * Slightly longer timeouts than Gemini because FDC can be slower on
     * large Branded-food queries.
     */
    @Provides
    @Singleton
    @Named("fdc")
    fun provideFdcOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("fdc")
    fun provideFdcRetrofit(
        @Named("fdc") okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(Constants.USDA_FDC_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    /**
     * Creates [FoodDataCentralApiService] from the @Named("fdc") Retrofit instance.
     * Used by [com.app.nutriai.data.repository.NutritionRepositoryImpl] for nutrition lookup.
     */
    @Provides
    @Singleton
    fun provideFdcApiService(
        @Named("fdc") retrofit: Retrofit
    ): FoodDataCentralApiService {
        return retrofit.create(FoodDataCentralApiService::class.java)
    }

    /**
     * Provides the USDA FDC API key from [BuildConfig.USDA_FDC_API_KEY].
     * The value is populated from local.properties at build time.
     * An empty string is allowed — the repository will skip FDC gracefully.
     */
    @Provides
    @Singleton
    @Named("fdcApiKey")
    fun provideFdcApiKey(): String = BuildConfig.USDA_FDC_API_KEY
}
