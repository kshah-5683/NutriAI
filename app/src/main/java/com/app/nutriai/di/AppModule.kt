package com.app.nutriai.di

import com.app.nutriai.BuildConfig
import com.app.nutriai.data.remote.api.FoodDataCentralApiService
import com.app.nutriai.data.remote.api.GeminiApiService
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
 * Phase 1: OkHttp, Retrofit instances (Gemini), JSON.
 * Phase 4: GeminiApiService wired via @Named("gemini") Retrofit.
 * Phase 5.5: FoodDataCentralApiService wired via @Named("fdc") Retrofit with
 *            a dedicated OkHttpClient. API key injected via @Named("fdcApiKey").
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Phase 11: `explicitNulls = false` is required because [GeminiPart] now has nullable
     * `text` and `inlineData` fields. Without this flag, nullable fields with null values
     * are serialized as `"key": null`, which the Gemini API rejects (it expects absent keys,
     * not explicit nulls). Setting `explicitNulls = false` omits null fields entirely.
     *
     * This is safe for response deserialization: `ignoreUnknownKeys = true` is already set,
     * and all nullable response fields have `= null` defaults, so missing keys deserialize
     * to null correctly.
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
     * General-purpose OkHttpClient used by the Gemini Retrofit instance.
     *
     * Uses [Proxy.NO_PROXY] to bypass system/corporate proxy settings entirely.
     * On MDM-managed or corporate devices, the OS may have a proxy configured
     * (e.g. proxy-intlho.wal-mart.com:8080) that is only reachable on VPN.
     * OkHttp's default behaviour is to honour the system proxy — this causes
     * UnknownHostException when VPN is off, even if the device has working
     * internet via mobile data or Wi-Fi.
     *
     * generativelanguage.googleapis.com is a public endpoint; it must be
     * reached directly, not through a corporate proxy.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
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
    @Named("gemini")
    fun provideGeminiRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(Constants.GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
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
     * Creates [GeminiApiService] from the @Named("gemini") Retrofit instance.
     * Used by [com.app.nutriai.data.repository.AiRepositoryImpl] for food parsing.
     */
    @Provides
    @Singleton
    fun provideGeminiApiService(
        @Named("gemini") retrofit: Retrofit
    ): GeminiApiService {
        return retrofit.create(GeminiApiService::class.java)
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
