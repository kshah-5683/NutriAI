package com.app.nutriai.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.app.nutriai.presentation.navigation.NutriAiNavHost
import com.app.nutriai.presentation.theme.NutriAiTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point of the app. Uses @AndroidEntryPoint to allow
 * Hilt to inject dependencies into this Activity and its composables.
 *
 * Phase 8 Pre-work II: Registers a [DefaultLifecycleObserver] that triggers a
 * throttled foreground pull via [ForegroundSyncViewModel] whenever the app
 * comes back to the foreground (at most once per 5 minutes).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val foregroundSyncViewModel: ForegroundSyncViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutriAiTheme {
                NutriAiNavHost()
            }
        }

        // Trigger incremental pull when the app comes to the foreground.
        // Throttled to once per 5 minutes inside ForegroundSyncViewModel.
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                foregroundSyncViewModel.pullIfThrottled()
            }
        })
    }
}
