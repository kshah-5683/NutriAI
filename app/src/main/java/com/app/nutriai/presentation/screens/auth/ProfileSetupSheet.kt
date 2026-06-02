package com.app.nutriai.presentation.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.app.nutriai.domain.model.UserProfile

// ─── Option constants ────────────────────────────────────────────────────────

private val GENDER_OPTIONS = listOf(
    "male" to "Male",
    "female" to "Female",
    "other" to "Other",
    "prefer_not_to_say" to "Prefer not to say"
)

private val DIET_OPTIONS = listOf(
    "vegetarian" to "Vegetarian",
    "veg_eggs" to "Veg + Eggs",
    "non_veg" to "Non-Vegetarian",
    "pescatarian" to "Pescatarian",
    "vegan" to "Vegan"
)

private val WEIGHT_GOAL_OPTIONS = listOf(
    "lose" to "Lose",
    "maintain" to "Maintain",
    "gain" to "Gain"
)

private val CUISINE_OPTIONS = listOf(
    "Indian", "South Indian", "Maharashtrian", "Gujarati",
    "Italian", "French", "Mexican", "Japanese",
    "Mediterranean", "Chinese", "Thai", "Korean"
)

private val ALLERGY_OPTIONS = listOf(
    "Gluten", "Dairy", "Nuts", "Soy",
    "Shellfish", "Eggs", "Fish", "Sesame"
)

/** Max length for custom cuisine/allergy entries — matches server-side sanitizeProfileEntry. */
private const val MAX_ENTRY_LENGTH = 40

/**
 * Strips HTML, markdown, control chars and truncates — client-side mirror of
 * the server-side sanitizeProfileEntry in recommend-meals/index.ts.
 */
private fun sanitizeEntry(raw: String): String =
    raw.replace(Regex("<[^>]*>"), "")
        .replace(Regex("[#*_~`\\[\\]{}()|\\\\]"), "")
        .replace(Regex("[\\x00-\\x1F\\x7F]"), "")
        .trim()
        .take(MAX_ENTRY_LENGTH)

// ─── Sheet content composable ────────────────────────────────────────────────

/**
 * Bottom sheet content for setting up the AI Recommendations user profile.
 *
 * Takes an initial [UserProfile], renders a form, and calls [onSave] with
 * the updated profile. The parent ViewModel handles persistence.
 *
 * Phase R4: Part of the AI Recommendations infrastructure.
 *
 * @param initialProfile The current persisted profile — used to pre-fill form fields.
 * @param onSave Called with the validated updated [UserProfile] when the user saves.
 * @param onDismiss Called when the user dismisses without saving.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileSetupSheetContent(
    initialProfile: UserProfile,
    onSave: (UserProfile) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Local form state — initialized from the profile
    var enabled by rememberSaveable { mutableStateOf(initialProfile.recommendationsEnabled) }
    var age by rememberSaveable { mutableStateOf(initialProfile.age?.toString() ?: "") }
    var gender by rememberSaveable { mutableStateOf(initialProfile.gender ?: "") }
    var weightKg by rememberSaveable { mutableStateOf(initialProfile.weightKg?.toString() ?: "") }
    var weightGoal by rememberSaveable { mutableStateOf(initialProfile.weightGoal ?: "") }
    var dietType by rememberSaveable { mutableStateOf(initialProfile.dietType ?: "") }
    var cuisines by rememberSaveable { mutableStateOf(initialProfile.cuisinePreferences.toSet()) }
    var allergies by rememberSaveable { mutableStateOf(initialProfile.allergies.toSet()) }
    var customCuisineInput by rememberSaveable { mutableStateOf("") }
    var customAllergyInput by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
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
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Set Up AI Recommendations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Personalize your meal suggestions with your dietary preferences.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        // Enable toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Enable AI Recommendations",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Switch(
                checked = enabled,
                onCheckedChange = { enabled = it }
            )
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // Profile fields — animated visibility based on toggle
        AnimatedVisibility(
            visible = enabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Age
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age") },
                    suffix = { Text("years") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Gender — dropdown
                DropdownField(
                    label = "Gender",
                    selectedValue = gender,
                    options = GENDER_OPTIONS,
                    onSelect = { gender = it }
                )

                // Weight
                OutlinedTextField(
                    value = weightKg,
                    onValueChange = { weightKg = it },
                    label = { Text("Weight") },
                    suffix = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Weight Goal — chips
                Text(
                    text = "Weight Goal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WEIGHT_GOAL_OPTIONS.forEach { (value, label) ->
                        FilterChip(
                            selected = weightGoal == value,
                            onClick = { weightGoal = if (weightGoal == value) "" else value },
                            label = { Text(label) }
                        )
                    }
                }

                // Diet Type — dropdown
                DropdownField(
                    label = "Diet Type",
                    selectedValue = dietType,
                    options = DIET_OPTIONS,
                    onSelect = { dietType = it }
                )

                // Cuisine Preferences — multi-select chips + custom input
                Text(
                    text = "Preferred Cuisines",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Predefined options
                    CUISINE_OPTIONS.forEach { cuisine ->
                        FilterChip(
                            selected = cuisine in cuisines,
                            onClick = {
                                cuisines = if (cuisine in cuisines) cuisines - cuisine
                                else cuisines + cuisine
                            },
                            label = { Text(cuisine) }
                        )
                    }
                    // Custom cuisines not in predefined list
                    cuisines.filter { it !in CUISINE_OPTIONS }.forEach { custom ->
                        FilterChip(
                            selected = true,
                            onClick = { cuisines = cuisines - custom },
                            label = { Text(custom) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove $custom",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
                // Add custom cuisine input
                OutlinedTextField(
                    value = customCuisineInput,
                    onValueChange = { if (it.length <= MAX_ENTRY_LENGTH) customCuisineInput = it },
                    label = { Text("Add other cuisine") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val sanitized = sanitizeEntry(customCuisineInput)
                            if (sanitized.isNotEmpty() && cuisines.none { it.equals(sanitized, ignoreCase = true) }) {
                                cuisines = cuisines + sanitized
                            }
                            customCuisineInput = ""
                        }
                    ),
                    trailingIcon = {
                        if (customCuisineInput.isNotBlank()) {
                            IconButton(onClick = {
                                val sanitized = sanitizeEntry(customCuisineInput)
                                if (sanitized.isNotEmpty() && cuisines.none { it.equals(sanitized, ignoreCase = true) }) {
                                    cuisines = cuisines + sanitized
                                }
                                customCuisineInput = ""
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add cuisine")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Allergies — multi-select chips + custom input
                Text(
                    text = "Allergies & Restrictions",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Predefined options
                    ALLERGY_OPTIONS.forEach { allergy ->
                        FilterChip(
                            selected = allergy in allergies,
                            onClick = {
                                allergies = if (allergy in allergies) allergies - allergy
                                else allergies + allergy
                            },
                            label = { Text(allergy) }
                        )
                    }
                    // Custom allergies not in predefined list
                    allergies.filter { it !in ALLERGY_OPTIONS }.forEach { custom ->
                        FilterChip(
                            selected = true,
                            onClick = { allergies = allergies - custom },
                            label = { Text(custom) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove $custom",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
                // Add custom allergy input
                OutlinedTextField(
                    value = customAllergyInput,
                    onValueChange = { if (it.length <= MAX_ENTRY_LENGTH) customAllergyInput = it },
                    label = { Text("Add other allergy") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val sanitized = sanitizeEntry(customAllergyInput)
                            if (sanitized.isNotEmpty() && allergies.none { it.equals(sanitized, ignoreCase = true) }) {
                                allergies = allergies + sanitized
                            }
                            customAllergyInput = ""
                        }
                    ),
                    trailingIcon = {
                        if (customAllergyInput.isNotBlank()) {
                            IconButton(onClick = {
                                val sanitized = sanitizeEntry(customAllergyInput)
                                if (sanitized.isNotEmpty() && allergies.none { it.equals(sanitized, ignoreCase = true) }) {
                                    allergies = allergies + sanitized
                                }
                                customAllergyInput = ""
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add allergy")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // Save button
        Button(
            onClick = {
                val profile = UserProfile(
                    age = age.toIntOrNull(),
                    gender = gender.ifBlank { null },
                    weightKg = weightKg.toDoubleOrNull(),
                    weightGoal = weightGoal.ifBlank { null },
                    dietType = dietType.ifBlank { null },
                    cuisinePreferences = cuisines.toList(),
                    allergies = allergies.toList(),
                    recommendationsEnabled = enabled
                )
                onSave(profile)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Profile")
        }
    }
}

// ─── Dropdown helper ─────────────────────────────────────────────────────────

/**
 * Reusable dropdown field backed by [ExposedDropdownMenuBox].
 *
 * @param label TextField label text.
 * @param selectedValue Currently selected value (stored key, e.g. "male").
 * @param options List of (value, displayLabel) pairs.
 * @param onSelect Called with the selected value key.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val displayText = options.firstOrNull { it.first == selectedValue }?.second ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, displayLabel) ->
                DropdownMenuItem(
                    text = { Text(displayLabel) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
