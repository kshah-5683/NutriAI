package com.app.nutriai.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.WorkManager
import com.app.nutriai.BuildConfig
import com.app.nutriai.data.local.preferences.AuthPreferences
import com.app.nutriai.data.remote.api.SupabaseAuthApiService
import com.app.nutriai.data.remote.api.SupabaseDbApiService
import com.app.nutriai.data.remote.api.SupabaseEdgeFunctionService
import com.app.nutriai.data.remote.auth.SupabaseAuthenticator
import com.app.nutriai.util.Constants
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

// Top-level DataStore delegate — must be outside any class to avoid creating
// multiple DataStore instances.  Hilt provides this via [provideAuthDataStore].
private val Context.authDataStore by preferencesDataStore(name = Constants.AUTH_PREFS_NAME)

/**
 * Hilt module providing Supabase networking and DataStore dependencies.
 *
 * Responsibilities:
 *  - [DataStore<Preferences>] for auth session persistence.
 *  - `@Named("supabase")` [OkHttpClient] with:
 *      • `apikey` header (Supabase anon key) on every request.
 *      • `Authorization: Bearer <token>` header when a session exists.
 *      • [Proxy.NO_PROXY] to bypass Walmart corporate proxy.
 *  - `@Named("supabase")` [Retrofit] pointing at [BuildConfig.SUPABASE_URL].
 *  - [SupabaseAuthApiService] and [SupabaseDbApiService] instances.
 *  - [WorkManager] singleton for WorkManager injection in non-Hilt workers.
 *
 * If [BuildConfig.SUPABASE_URL] is empty (key not set in local.properties),
 * the Retrofit instance uses a placeholder URL.  Auth calls will fail with a
 * network error rather than a crash — the UI surfaces this as an error message.
 *
 * Phase 6: added [androidx.hilt.work.HiltWorkerFactory] support via
 * [dagger.hilt.android.components.ServiceComponent] — requires
 * [com.app.nutriai.NutriAiApplication] to call
 * `HiltWorkerFactory.setWorkerFactory(...)` (handled by
 * `@HiltAndroidApp` annotation automatically).
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    // ─── DataStore ───────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideAuthDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.authDataStore

    // ─── OkHttp client ───────────────────────────────────────────────────

    /**
     * Dedicated OkHttpClient for all Supabase requests.
     *
     * Interceptor adds two headers on every call:
     *  - `apikey`: Supabase anon key (always required by PostgREST and GoTrue).
     *  - `Authorization`: JWT bearer token when the user is signed in.
     *
     * The JWT is read synchronously via [AuthPreferences.getCachedToken].
     * The cache is populated immediately when [AuthPreferences.saveSession] is
     * called (before the coroutine scope returns), so the interceptor always
     * sees the latest token.
     */
    @Provides
    @Singleton
    @Named("supabase")
    fun provideSupabaseOkHttpClient(
        authPreferences: AuthPreferences,
        authenticator: SupabaseAuthenticator
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY)
            .authenticator(authenticator)
            .addInterceptor { chain ->
                val original: Request = chain.request()
                val builder = original.newBuilder()
                    .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .header("Content-Type", "application/json")

                // Add JWT if a session exists
                authPreferences.getCachedToken()?.let { token ->
                    builder.header("Authorization", "Bearer $token")
                }

                chain.proceed(builder.build())
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // ─── JSON ────────────────────────────────────────────────────────────

    /**
     * Supabase-specific Json instance.
     *
     * Key difference from the app-wide [Json] (AppModule): [explicitNulls] is
     * **true** so nullable fields serialize as `"key": null` rather than being
     * omitted from the object entirely.
     *
     * PostgREST requires every object in a batch upsert array to have the exact
     * same set of keys — PGRST102 "All object keys must match". The shared Json
     * uses `explicitNulls = false` for Gemini (which rejects explicit nulls), so
     * Supabase needs its own instance to avoid stripping nullable fields like
     * `brand`, `deleted_at`, and `external_api_id` from some rows but not others.
     */
    @Provides
    @Singleton
    @Named("supabase")
    fun provideSupabaseJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = true   // required: PostgREST "All object keys must match"
    }

    // ─── Retrofit ────────────────────────────────────────────────────────

    @Provides
    @Singleton
    @Named("supabase")
    fun provideSupabaseRetrofit(
        @Named("supabase") okHttpClient: OkHttpClient,
        @Named("supabase") json: Json
    ): Retrofit {
        // Use a safe fallback URL if SUPABASE_URL is not configured
        val baseUrl = BuildConfig.SUPABASE_URL
            .let { if (it.isBlank()) "https://placeholder.supabase.co" else it }
            .trimEnd('/') + "/"

        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    // ─── API services ────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideSupabaseAuthApiService(
        @Named("supabase") retrofit: Retrofit
    ): SupabaseAuthApiService = retrofit.create(SupabaseAuthApiService::class.java)

    @Provides
    @Singleton
    fun provideSupabaseDbApiService(
        @Named("supabase") retrofit: Retrofit
    ): SupabaseDbApiService = retrofit.create(SupabaseDbApiService::class.java)

    /**
     * Phase R2: Edge Function service for AI recommendations.
     * Reuses the same `@Named("supabase")` Retrofit instance (same base URL, auth headers).
     * Edge Functions live at `/functions/v1/{name}` — same Supabase project URL.
     */
    @Provides
    @Singleton
    fun provideSupabaseEdgeFunctionService(
        @Named("supabase") retrofit: Retrofit
    ): SupabaseEdgeFunctionService = retrofit.create(SupabaseEdgeFunctionService::class.java)

    // ─── WorkManager ─────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager = WorkManager.getInstance(context)
}
