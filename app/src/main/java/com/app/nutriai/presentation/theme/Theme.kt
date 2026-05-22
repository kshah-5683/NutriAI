package com.app.nutriai.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// ─── Light Color Scheme — "Forest & Cream" ────────────────────────────────
// Warm cream backgrounds with deep forest-green primary, lime secondary,
// and amber tertiary. Inspired by organic/nature health-app aesthetics.
private val LightColorScheme = lightColorScheme(
    // Primary — deep forest green (buttons, FAB, active nav items, links)
    primary            = ForestGreen,
    onPrimary          = Cream,
    primaryContainer   = Sage,
    onPrimaryContainer = ForestGreenDark,

    // Secondary — bright lime (progress arcs, badges, accents)
    secondary            = Lime,
    onSecondary          = Cream,
    secondaryContainer   = PaleLime,
    onSecondaryContainer = LimeDark,

    // Tertiary — warm amber (calorie ring, highlights)
    tertiary            = Amber,
    onTertiary          = Charcoal,
    tertiaryContainer   = PaleAmber,
    onTertiaryContainer = AmberDark,

    // Backgrounds & surfaces — warm cream, not clinical white
    background        = Cream,
    onBackground      = Charcoal,
    surface           = CreamSurface,
    onSurface         = Charcoal,
    surfaceVariant    = SageGray,
    onSurfaceVariant  = MidGreen,

    // Outlines
    outline        = WarmGray,
    outlineVariant = SageGray,

    // Semantic
    error   = ErrorRed,
    onError = Cream
)

// ─── Dark Color Scheme — "Dark Forest" ────────────────────────────────────
// Deep forest backgrounds with light sage greens and lime accents.
private val DarkColorScheme = darkColorScheme(
    // Primary — lighter green that pops on dark backgrounds
    primary            = ForestGreenLight,
    onPrimary          = ForestGreenDark,
    primaryContainer   = SageDark,
    onPrimaryContainer = ForestGreenLight,

    // Secondary — lime light
    secondary            = LimeLight,
    onSecondary          = LimeDark,
    secondaryContainer   = PaleLimeDark,
    onSecondaryContainer = LimeLight,

    // Tertiary — amber light
    tertiary            = AmberLight,
    onTertiary          = AmberDark,
    tertiaryContainer   = PaleAmberDark,
    onTertiaryContainer = AmberLight,

    // Backgrounds & surfaces — deep forest, not pure black
    background        = DarkForestBg,
    onBackground      = OnDarkText,
    surface           = DarkSurface,
    onSurface         = OnDarkText,
    surfaceVariant    = DarkVariant,
    onSurfaceVariant  = ForestGreenLight,

    // Outlines
    outline        = MidGreen,
    outlineVariant = DarkVariant,

    // Semantic
    error   = ErrorRedLight,
    onError = ForestGreenDark
)

// ─── Shapes — organic rounded corners ─────────────────────────────────────
// Slightly larger radii than Material3 defaults for a softer, organic feel.
private val NutriAiShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small      = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium     = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large      = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
)

/**
 * NutriAI Material3 theme — "Forest & Cream" design system.
 *
 * Dynamic color is intentionally **disabled** so the brand palette is always
 * consistent regardless of the user's wallpaper on Android 12+.
 * This can be re-exposed as a user preference in a future settings screen.
 */
@Composable
fun NutriAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = NutriAiTypography,
        shapes      = NutriAiShapes,
        content     = content
    )
}
