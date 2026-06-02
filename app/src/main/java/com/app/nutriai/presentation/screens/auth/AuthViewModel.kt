package com.app.nutriai.presentation.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.nutriai.data.local.preferences.AuthPreferences
import com.app.nutriai.data.local.preferences.UserPreferences
import com.app.nutriai.domain.model.AuthState
import com.app.nutriai.domain.model.MacroGoals
import com.app.nutriai.domain.model.UserProfile
import com.app.nutriai.domain.usecase.GetAuthStateUseCase
import com.app.nutriai.domain.usecase.SignInUseCase
import com.app.nutriai.domain.usecase.SignOutUseCase
import com.app.nutriai.domain.usecase.SignUpUseCase
import com.app.nutriai.domain.usecase.SyncDataUseCase
import com.app.nutriai.util.Resource
import com.app.nutriai.work.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────
//  UI State
// ─────────────────────────────────────────────

/**
 * Complete UI state for the Auth / Profile screen.
 *
 * When [authState] is [AuthState.Authenticated], the profile view is shown.
 * When [authState] is [AuthState.Unauthenticated], the sign-in/sign-up form is shown.
 */
data class AuthUiState(
    /** Current Supabase auth state — drives which panel is visible. */
    val authState: AuthState = AuthState.Loading,

    // Form fields (sign-in / sign-up)
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",

    /** True while sign-in, sign-up, or sign-out is in progress. */
    val isLoading: Boolean = false,

    /** Error message to display in a Snackbar or inline — cleared when user edits a field. */
    val errorMessage: String? = null,

    /** True when the sign-up form is visible; false for sign-in. */
    val isSignUpMode: Boolean = false,

    /** True while a manual sync is running from the Profile screen. */
    val isSyncing: Boolean = false,

    /** Human-readable result of the last manual sync (success or error message). */
    val lastSyncMessage: String? = null
) {
    /** Sign-in form is valid: non-blank email + non-blank password. */
    val isSignInValid: Boolean
        get() = email.isNotBlank() && password.isNotBlank()

    /** Sign-up form is valid: sign-in fields valid + confirm password matches. */
    val isSignUpValid: Boolean
        get() = isSignInValid && confirmPassword.isNotBlank() && password == confirmPassword
}

// ─────────────────────────────────────────────
//  One-time events
// ─────────────────────────────────────────────

sealed class AuthEvent {
    /** Navigate to the Home screen after successful sign-in. */
    data object NavigateToHome : AuthEvent()

    /** Sign-up completed — UI should show confirmation and return to sign-in form. */
    data object SignUpSuccess : AuthEvent()
}

// ─────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────

/**
 * ViewModel for the Auth / Profile screen.
 *
 * Collects [GetAuthStateUseCase] as a [StateFlow] and exposes it in [uiState].
 * One-time navigation events (sign-in success → Home) are sent via [events].
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val getAuthStateUseCase: GetAuthStateUseCase,
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val syncDataUseCase: SyncDataUseCase,
    private val authPreferences: AuthPreferences,
    private val userPreferences: UserPreferences,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        // Observe auth state from DataStore and update UI accordingly
        viewModelScope.launch {
            getAuthStateUseCase().collect { authState ->
                _uiState.update { it.copy(authState = authState) }
            }
        }
    }

    // ─── Form field updates ──────────────────────────────────────────────

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun toggleSignUpMode() {
        _uiState.update {
            it.copy(
                isSignUpMode = !it.isSignUpMode,
                errorMessage = null,
                password = "",
                confirmPassword = ""
            )
        }
    }

    // ─── Auth actions ────────────────────────────────────────────────────

    fun signIn() {
        val state = _uiState.value
        if (state.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = signInUseCase(state.email, state.password)) {
                is Resource.Success -> {
                    SyncScheduler.schedule(appContext)
                    // Kick off initial sync in the background (don't block navigation)
                    launch { syncDataUseCase() }
                    _events.send(AuthEvent.NavigateToHome)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                is Resource.Loading -> Unit
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun signUp() {
        val state = _uiState.value
        if (state.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = signUpUseCase(state.email, state.password, state.confirmPassword)) {
                is Resource.Success -> {
                    // Sign-up only creates the account — user must sign in separately.
                    // SyncScheduler and initial sync run after sign-in (not here).
                    _events.send(AuthEvent.SignUpSuccess)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                is Resource.Loading -> Unit
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun signOut() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            signOutUseCase()
            SyncScheduler.cancel(appContext)
            // Auth state flow will emit Unauthenticated — UI updates automatically
            _uiState.update { it.copy(isLoading = false, lastSyncMessage = null) }
        }
    }

    // ─── Macro Goals ─────────────────────────────────────────────────────

    /**
     * Persisted daily macro goals — exposed so the goals bottom sheet can
     * read the current values for pre-filling its form fields.
     */
    val macroGoals: StateFlow<MacroGoals> = userPreferences.macroGoalsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MacroGoals()
        )

    /** Persist new macro goals to DataStore. */
    fun saveMacroGoals(goals: MacroGoals) {
        viewModelScope.launch {
            userPreferences.saveMacroGoals(goals)
        }
    }

    // ─── User Profile (Phase R4: AI Recommendations) ─────────────────────

    /**
     * Persisted dietary profile — exposed so the profile setup sheet can
     * read the current values for pre-filling its form fields.
     */
    val userProfile: StateFlow<UserProfile> = userPreferences.profileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserProfile()
        )

    /** Persist updated user profile to Room and trigger Supabase sync. */
    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            userPreferences.saveProfile(profile)
        }
    }

    // ─── Manual sync ─────────────────────────────────────────────────────

    fun syncNow() {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, lastSyncMessage = null) }
            val result = syncDataUseCase()
            val message = when (result) {
                is Resource.Success -> "Sync complete ✓"
                is Resource.Error   -> "Sync failed: ${result.message}"
                is Resource.Loading -> null
            }
            _uiState.update { it.copy(isSyncing = false, lastSyncMessage = message) }
        }
    }
}
