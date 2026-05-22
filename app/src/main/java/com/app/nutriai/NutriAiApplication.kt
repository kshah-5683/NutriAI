package com.app.nutriai

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.app.nutriai.domain.usecase.CleanupLabelPhotosUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class.
 *
 * @HiltAndroidApp triggers Hilt's code generation and serves as the
 * application-level dependency injection container.
 *
 * Implements [Configuration.Provider] to supply [HiltWorkerFactory] to
 * WorkManager — required for [@HiltWorker]-annotated workers like
 * [com.app.nutriai.work.SyncWorker] to receive Hilt-injected dependencies.
 *
 * The default WorkManager [androidx.startup.Initializer] is disabled in
 * AndroidManifest.xml so this custom configuration is used instead.
 *
 * Phase 6:  Added [Configuration.Provider] + [HiltWorkerFactory] for SyncWorker.
 * Phase 11: Added [onCreate] to trigger label photo TTL cleanup on every launch.
 */
@HiltAndroidApp
class NutriAiApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * Phase 11: Cleans up nutrition label photos older than 10 days.
     * Injected by Hilt — no manual construction needed.
     */
    @Inject
    lateinit var cleanupLabelPhotosUseCase: CleanupLabelPhotosUseCase

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Phase 11: Purge label photos older than 10 days on every app launch.
        // Runs on IO dispatcher in a fire-and-forget coroutine — non-blocking.
        // SupervisorJob ensures a cleanup failure doesn't crash the application.
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            runCatching { cleanupLabelPhotosUseCase() }
        }
    }
}
