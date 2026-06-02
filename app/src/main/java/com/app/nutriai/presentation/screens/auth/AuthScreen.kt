package com.app.nutriai.presentation.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.nutriai.domain.model.AuthState
import com.app.nutriai.domain.model.MacroGoals
import com.app.nutriai.domain.model.UserProfile
import com.app.nutriai.presentation.theme.NutriAiTheme

/**
 * Auth / Profile screen.
 *
 * Shows one of three states based on [AuthUiState.authState]:
 *  - [AuthState.Loading]         → full-screen loading spinner
 *  - [AuthState.Unauthenticated] → sign-in / sign-up form
 *  - [AuthState.Authenticated]   → profile card with sync controls + sign-out
 *
 * [onNavigateToHome] is called when sign-in or sign-up succeeds, navigating
 * the user back to the Home dashboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onNavigateToHome: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val macroGoals by viewModel.macroGoals.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showGoalsSheet by rememberSaveable { mutableStateOf(false) }
    val goalsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showProfileSheet by rememberSaveable { mutableStateOf(false) }
    val profileSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Collect one-time navigation events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.NavigateToHome -> onNavigateToHome()
                is AuthEvent.SignUpSuccess -> {
                    // Show confirmation snackbar then return to sign-in form
                    snackbarHostState.showSnackbar("Account created! Please sign in.")
                    viewModel.toggleSignUpMode()
                }
            }
        }
    }

    // Show auth errors in snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    // Show sync result (success or failure) in snackbar
    LaunchedEffect(uiState.lastSyncMessage) {
        uiState.lastSyncMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        AuthContent(
            uiState = uiState,
            userProfile = userProfile,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
            onSignIn = viewModel::signIn,
            onSignUp = viewModel::signUp,
            onSignOut = viewModel::signOut,
            onToggleMode = viewModel::toggleSignUpMode,
            onSyncNow = viewModel::syncNow,
            onOpenGoals = { showGoalsSheet = true },
            onOpenProfile = { showProfileSheet = true },
            modifier = Modifier.padding(innerPadding)
        )
    }

    // Macro goals bottom sheet (shown from ProfilePanel regardless of auth state)
    if (showGoalsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGoalsSheet = false },
            sheetState = goalsSheetState
        ) {
            MacroGoalsSheetContent(
                current = macroGoals,
                onSave = { goals ->
                    viewModel.saveMacroGoals(goals)
                    showGoalsSheet = false
                },
                onCancel = { showGoalsSheet = false }
            )
        }
    }

    // AI Recommendations profile setup sheet (Phase R4)
    if (showProfileSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            sheetState = profileSheetState
        ) {
            ProfileSetupSheetContent(
                initialProfile = userProfile,
                onSave = { profile ->
                    viewModel.saveProfile(profile)
                    showProfileSheet = false
                },
                onDismiss = { showProfileSheet = false }
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Pure content composable (Preview-friendly)
// ─────────────────────────────────────────────

@Composable
private fun AuthContent(
    uiState: AuthUiState,
    userProfile: UserProfile = UserProfile(),
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onSignOut: () -> Unit,
    onToggleMode: () -> Unit,
    onSyncNow: () -> Unit,
    onOpenGoals: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = uiState.authState,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "AuthStateTransition"
        ) { authState ->
            when (authState) {
                is AuthState.Loading -> LoadingPanel()
                is AuthState.Unauthenticated -> AuthFormPanel(
                    uiState = uiState,
                    onEmailChange = onEmailChange,
                    onPasswordChange = onPasswordChange,
                    onConfirmPasswordChange = onConfirmPasswordChange,
                    onSignIn = onSignIn,
                    onSignUp = onSignUp,
                    onToggleMode = onToggleMode,
                    onOpenGoals = onOpenGoals
                )
                is AuthState.Authenticated -> ProfilePanel(
                    email = authState.email,
                    isSyncing = uiState.isSyncing,
                    isLoading = uiState.isLoading,
                    userProfile = userProfile,
                    onSyncNow = onSyncNow,
                    onSignOut = onSignOut,
                    onOpenGoals = onOpenGoals,
                    onOpenProfile = onOpenProfile
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Loading panel
// ─────────────────────────────────────────────

@Composable
private fun LoadingPanel() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

// ─────────────────────────────────────────────
//  Sign-In / Sign-Up form
// ─────────────────────────────────────────────

@Composable
private fun AuthFormPanel(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onToggleMode: () -> Unit,
    onOpenGoals: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Header
        Icon(
            imageVector = Icons.Default.Cloud,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (uiState.isSignUpMode) "Create Account" else "Sign In",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Sync your nutrition data across all your devices",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        // Email field
        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        // Password field
        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (uiState.isSignUpMode) ImeAction.Next else ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = {
                    focusManager.clearFocus()
                    if (!uiState.isSignUpMode) onSignIn()
                }
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Confirm password (sign-up only)
        if (uiState.isSignUpMode) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onSignUp()
                    }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(24.dp))

        // Primary action button
        Button(
            onClick = if (uiState.isSignUpMode) onSignUp else onSignIn,
            enabled = (if (uiState.isSignUpMode) uiState.isSignUpValid else uiState.isSignInValid)
                    && !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (uiState.isSignUpMode) "Create Account" else "Sign In",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Toggle sign-in / sign-up
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (uiState.isSignUpMode) "Already have an account?" else "Don't have an account?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onToggleMode) {
                Text(if (uiState.isSignUpMode) "Sign In" else "Sign Up")
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text(
            text = "Your data is always stored locally.\nSign in only if you want cloud backup & cross-device sync.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onOpenGoals,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "  Set Nutrition Goals",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Profile panel (authenticated)
// ─────────────────────────────────────────────

@Composable
private fun ProfilePanel(
    email: String,
    isSyncing: Boolean,
    isLoading: Boolean,
    userProfile: UserProfile = UserProfile(),
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    onOpenGoals: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Signed In",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = email,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(32.dp))

        // Sync card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "  Cloud Sync",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "Your data syncs automatically every 12 hours on Wi-Fi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Manual sync button
                FilledTonalButton(
                    onClick = onSyncNow,
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "  Syncing…",
                            style = MaterialTheme.typography.labelLarge
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "  Sync Now",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Nutrition Goals card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Nutrition Goals",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Set your daily calorie & macro targets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = onOpenGoals) { Text("Edit") }
            }
        }

        Spacer(Modifier.height(12.dp))

        // AI Recommendations profile card (Phase R4)
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "AI Recommendations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (userProfile.isComplete) "Profile configured"
                            else "Set up your dietary preferences",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = onOpenProfile) {
                    Text(if (userProfile.isComplete) "Edit" else "Set Up")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Sign out
        OutlinedButton(
            onClick = onSignOut,
            enabled = !isLoading,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Sign Out")
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Signing out keeps your local data intact.\nYou can continue using the app offline.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────
//  Macro Goals bottom sheet
// ─────────────────────────────────────────────

/**
 * Bottom sheet for editing the user's daily nutrition targets.
 *
 * Pre-fills fields from [current] goals. Validates that all values are positive
 * numbers before calling [onSave].
 *
 * @param current  Currently persisted [MacroGoals] — used to pre-fill the form.
 * @param onSave   Called with the validated new [MacroGoals] when the user saves.
 * @param onCancel Called when the user taps Cancel or dismisses the sheet.
 */
@Composable
private fun MacroGoalsSheetContent(
    current: MacroGoals,
    onSave: (MacroGoals) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var calories by rememberSaveable {
        mutableStateOf(current.calorieGoal.toInt().toString())
    }
    var protein by rememberSaveable {
        mutableStateOf(current.proteinGoal.toInt().toString())
    }
    var carbs by rememberSaveable {
        mutableStateOf(current.carbsGoal.toInt().toString())
    }
    var fat by rememberSaveable {
        mutableStateOf(current.fatGoal.toInt().toString())
    }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .imePadding()
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Daily Nutrition Goals",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "These targets drive the progress arcs on the Home screen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        // Calorie goal (full width)
        OutlinedTextField(
            value = calories,
            onValueChange = { calories = it; errorMessage = null },
            label = { Text("Daily Calories (kcal)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage != null && calories.toDoubleOrNull()?.let { it <= 0 } == true
        )
        Spacer(Modifier.height(12.dp))

        // Protein + Carbs row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = protein,
                onValueChange = { protein = it; errorMessage = null },
                label = { Text("Protein (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = carbs,
                onValueChange = { carbs = it; errorMessage = null },
                label = { Text("Carbs (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))

        // Fat goal (half width)
        OutlinedTextField(
            value = fat,
            onValueChange = { fat = it; errorMessage = null },
            label = { Text("Fat (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.5f)
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(24.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) { Text("Cancel") }
            Button(
                onClick = {
                    val kcal = calories.toDoubleOrNull()
                    val prot = protein.toDoubleOrNull()
                    val carb = carbs.toDoubleOrNull()
                    val f    = fat.toDoubleOrNull()
                    if (kcal == null || kcal <= 0 ||
                        prot == null || prot < 0 ||
                        carb == null || carb < 0 ||
                        f    == null || f    < 0) {
                        errorMessage = "All values must be positive numbers"
                        return@Button
                    }
                    onSave(MacroGoals(kcal, prot, carb, f))
                },
                modifier = Modifier.weight(1f)
            ) { Text("Save Goals") }
        }
    }
}

// ─────────────────────────────────────────────
//  Previews
// ─────────────────────────────────────────────

@Preview(showBackground = true, name = "Sign In Form")
@Composable
private fun AuthScreenSignInPreview() {
    NutriAiTheme {
        AuthContent(
            uiState = AuthUiState(authState = AuthState.Unauthenticated),
            onEmailChange = {}, onPasswordChange = {}, onConfirmPasswordChange = {},
            onSignIn = {}, onSignUp = {}, onSignOut = {}, onToggleMode = {}, onSyncNow = {}
        )
    }
}

@Preview(showBackground = true, name = "Sign Up Form")
@Composable
private fun AuthScreenSignUpPreview() {
    NutriAiTheme {
        AuthContent(
            uiState = AuthUiState(authState = AuthState.Unauthenticated, isSignUpMode = true),
            onEmailChange = {}, onPasswordChange = {}, onConfirmPasswordChange = {},
            onSignIn = {}, onSignUp = {}, onSignOut = {}, onToggleMode = {}, onSyncNow = {}
        )
    }
}

@Preview(showBackground = true, name = "Profile (authenticated)")
@Composable
private fun AuthScreenProfilePreview() {
    NutriAiTheme {
        AuthContent(
            uiState = AuthUiState(
                authState = AuthState.Authenticated("uuid-123", "user@example.com")
            ),
            onEmailChange = {}, onPasswordChange = {}, onConfirmPasswordChange = {},
            onSignIn = {}, onSignUp = {}, onSignOut = {}, onToggleMode = {}, onSyncNow = {}
        )
    }
}
