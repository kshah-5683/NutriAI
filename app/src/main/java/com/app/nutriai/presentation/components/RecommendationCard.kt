package com.app.nutriai.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.nutriai.domain.model.MealType
import com.app.nutriai.domain.model.Recommendation
import com.app.nutriai.domain.model.RecommendationSource
import com.app.nutriai.presentation.theme.CalorieColor
import com.app.nutriai.presentation.theme.CarbsColor
import com.app.nutriai.presentation.theme.FatColor
import com.app.nutriai.presentation.theme.ProteinColor
import java.net.URLEncoder
import kotlin.math.roundToInt

/**
 * AI meal recommendation card for the Home screen.
 *
 * Shows up to 3 visible recommendations from a buffer of 5.
 * Dismiss X button removes one and slides in the next from the buffer.
 * Handles both catalog and internet sources.
 *
 * Phase R2: Android Home Screen Recommendations.
 */
@Composable
fun RecommendationCard(
    recommendations: List<Recommendation>,
    isLoading: Boolean,
    error: String?,
    nextMeal: MealType?,
    missedMeals: List<MealType>,
    addedToFoods: Set<String>,
    onAddToMyFoods: (Recommendation) -> Unit,
    modifier: Modifier = Modifier
) {
    var dismissedIndices by remember { mutableStateOf(setOf<Int>()) }

    // Reset dismissed state when nextMeal changes (new meal slot)
    LaunchedEffect(nextMeal) {
        dismissedIndices = emptySet()
    }

    // Visible recommendations: filter dismissed, show max 3
    val visibleRecs = recommendations
        .mapIndexed { i, rec -> i to rec }
        .filter { (i, _) -> i !in dismissedIndices }
        .take(3)

    // Don't render anything if empty + not loading + no error
    if (!isLoading && error == null && recommendations.isEmpty() && missedMeals.isEmpty()) {
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("✨", fontSize = 16.sp)
                    Text(
                        text = if (nextMeal != null) "${nextMeal.label} Suggestions"
                               else "Suggested for you",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (nextMeal != null) {
                    Text(
                        text = nextMeal.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Missed meals banner
            if (missedMeals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You haven't logged ${
                        missedMeals.joinToString(" or ") { it.label.lowercase() }
                    } today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            // Loading state
            if (isLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                ShimmerPlaceholder()
            }

            // Error state — non-blocking inline message
            if (error != null && !isLoading) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Couldn't load suggestions. Pull to refresh to try again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            // Recommendations list
            if (!isLoading && visibleRecs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                visibleRecs.forEachIndexed { displayIdx, (origIdx, rec) ->
                    if (displayIdx > 0) {
                        Divider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    RecommendationItem(
                        rec = rec,
                        featured = displayIdx == 0,
                        isAdded = rec.name in addedToFoods,
                        onAddToMyFoods = { onAddToMyFoods(rec) },
                        onDismiss = { dismissedIndices = dismissedIndices + origIdx }
                    )
                }
            }

            // All dismissed — subtle empty message
            if (!isLoading && recommendations.isNotEmpty() && visibleRecs.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No more suggestions for now.",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ─── Single recommendation item ──────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecommendationItem(
    rec: Recommendation,
    featured: Boolean,
    isAdded: Boolean,
    onAddToMyFoods: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var recipeExpanded by remember { mutableStateOf(false) }

    val qtyLabel = if (rec.suggestedQuantity > 1)
        " (x${rec.suggestedQuantity.roundToInt()})" else ""

    Column {
        // Name + dismiss button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = rec.name + qtyLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss ${rec.name}",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Reason (featured items only)
        if (featured && rec.reason.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = rec.reason,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Macro pills row
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MacroPill("${rec.calories.roundToInt()} kcal", CalorieColor)
            MacroPill("${rec.protein.roundToInt()}g P", ProteinColor)
            MacroPill("${rec.carbs.roundToInt()}g C", CarbsColor)
            MacroPill("${rec.fat.roundToInt()}g F", FatColor)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Source-specific content
        when (rec.source) {
            RecommendationSource.CATALOG -> {
                Text(
                    text = "From: Your Catalog",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp
                )
            }

            RecommendationSource.INTERNET -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AI Suggestion",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "~Estimated macros~",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                // Expandable recipe text
                if (rec.recipeText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { recipeExpanded = !recipeExpanded },
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Text(
                            text = if (recipeExpanded) "Hide Recipe" else "View Recipe",
                            fontSize = 12.sp
                        )
                        Icon(
                            imageVector = if (recipeExpanded) Icons.Default.ExpandLess
                                          else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    AnimatedVisibility(
                        visible = recipeExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        FormattedRecipeText(text = rec.recipeText)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons: YouTube, Google, Add to My Foods
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val searchTerm = rec.searchQuery ?: "${rec.name} recipe"
                    val encodedQuery = URLEncoder.encode(searchTerm, "UTF-8")

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")
                            )
                            context.startActivity(intent)
                        },
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("YouTube", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/search?q=$encodedQuery")
                            )
                            context.startActivity(intent)
                        },
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Google", fontSize = 12.sp)
                    }

                    AddToMyFoodsButton(
                        isAdded = isAdded,
                        onAdd = onAddToMyFoods
                    )
                }
            }
        }
    }
}

// ─── "Add to My Foods" button ────────────────────────────────────────────

@Composable
private fun AddToMyFoodsButton(
    isAdded: Boolean,
    onAdd: () -> Unit
) {
    var loading by remember { mutableStateOf(false) }

    // Reset loading when isAdded flips to true (success signal from ViewModel)
    LaunchedEffect(isAdded) {
        if (isAdded) loading = false
    }

    Button(
        onClick = {
            if (!loading && !isAdded) {
                loading = true
                onAdd()
            }
        },
        enabled = !loading && !isAdded,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        modifier = Modifier.height(32.dp),
        colors = if (isAdded) ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) else ButtonDefaults.buttonColors()
    ) {
        when {
            isAdded -> {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Added", fontSize = 12.sp)
            }
            loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Adding...", fontSize = 12.sp)
            }
            else -> {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add to My Foods", fontSize = 12.sp)
            }
        }
    }
}

// ─── Macro pill ──────────────────────────────────────────────────────────

@Composable
private fun MacroPill(
    text: String,
    color: androidx.compose.ui.graphics.Color
) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun FormattedRecipeText(text: String) {
    val methodRegex = Regex("method\\s*:", RegexOption.IGNORE_CASE)
    val ingredientsRegex = Regex("ingredients\\s*:", RegexOption.IGNORE_CASE)

    val methodMatch = methodRegex.find(text)
    val ingredientsMatch = ingredientsRegex.find(text)

    var ingredients = emptyList<String>()
    var steps = emptyList<String>()
    var fallbackLines = emptyList<String>()

    if (ingredientsMatch != null && methodMatch != null) {
        val ingStart = ingredientsMatch.range.last + 1
        val ingEnd = methodMatch.range.first
        val ingredientsRaw = text.substring(ingStart, ingEnd).trim()
        val methodRaw = text.substring(methodMatch.range.last + 1).trim()

        ingredients = ingredientsRaw
            .replace(Regex("\\.\\s*$"), "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        steps = methodRaw
            .split(Regex("\\.(?:\\s|$)"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    } else if (ingredientsMatch != null) {
        val ingStart = ingredientsMatch.range.last + 1
        val ingredientsRaw = text.substring(ingStart).trim()
        ingredients = ingredientsRaw
            .replace(Regex("\\.\\s*$"), "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    } else {
        fallbackLines = text
            .split(Regex("[.,]"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (fallbackLines.isNotEmpty()) {
            fallbackLines.forEach { line ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        if (ingredients.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Ingredients",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ingredients.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        if (steps.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Method",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                steps.forEach { step ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

// ─── Shimmer loading placeholder ─────────────────────────────────────────

@Composable
private fun ShimmerPlaceholder() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 140.dp, height = 14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Box(
                    modifier = Modifier
                        .size(width = 200.dp, height = 12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .size(width = 56.dp, height = 18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }
        }
    }
}
