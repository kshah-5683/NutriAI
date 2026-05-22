package com.app.nutriai.presentation.theme

import androidx.compose.ui.graphics.Color

// ─── Primary — Forest Green ────────────────────────────────────────────────
// Deep olive/forest green used for buttons, FAB, top-app-bar content, links
val ForestGreen      = Color(0xFF2D5A27)
val ForestGreenLight = Color(0xFFA8D99C)  // light-theme primaryContainer on dark
val ForestGreenDark  = Color(0xFF1B3D17)  // darker variant for pressed states

// ─── Primary Container — Sage ──────────────────────────────────────────────
// Soft sage used for card surfaces, selected chip backgrounds
val Sage     = Color(0xFFD4E8C8)
val SageDark = Color(0xFF3A4F34)          // dark-theme primaryContainer

// ─── Secondary — Lime ──────────────────────────────────────────────────────
// Bright lime for progress rings, badges, nutrition indicators
val Lime     = Color(0xFF6B9B37)
val LimeLight = Color(0xFFC0E17E)         // lighter lime for dark-theme secondary
val LimeDark = Color(0xFF3D6B1F)

// ─── Secondary Container — Pale Lime ──────────────────────────────────────
val PaleLime     = Color(0xFFE8F5E1)
val PaleLimeDark = Color(0xFF2A3D24)

// ─── Tertiary — Amber ──────────────────────────────────────────────────────
// Warm amber for calorie highlights, tertiary badges, active accent
val Amber      = Color(0xFFF9A825)
val AmberLight = Color(0xFFFFD95A)
val AmberDark  = Color(0xFFB27A00)

// ─── Tertiary Container — Pale Amber ──────────────────────────────────────
val PaleAmber     = Color(0xFFFFF3D6)
val PaleAmberDark = Color(0xFF3D3420)

// ─── Neutrals — Warm Cream (organic, not cool-gray) ───────────────────────
val Charcoal    = Color(0xFF1A1C18)  // primary text color in light theme
val MidGreen    = Color(0xFF3C4A37)  // secondary text / on-surface-variant (light)
val Cream       = Color(0xFFFEFBF3)  // light-theme background
val CreamSurface = Color(0xFFFFFFFF) // light-theme surface (cards) — pure white for clear contrast on cream bg
val SageGray    = Color(0xFFEEF2E6)  // surfaceVariant — subtle tinted bg for chips/tabs
val WarmGray    = Color(0xFFC8C8BC)  // disabled / outline

// ─── Dark-theme neutral surfaces ──────────────────────────────────────────
val DarkForestBg = Color(0xFF0F1A0D)  // dark-theme background
val DarkSurface  = Color(0xFF1E2A1C)  // dark-theme surface
val DarkVariant  = Color(0xFF2A3828)  // dark-theme surfaceVariant
val OnDarkText   = Color(0xFFE0E8DC)  // primary text on dark backgrounds

// ─── Semantic ──────────────────────────────────────────────────────────────
val ErrorRed      = Color(0xFFBA1A1A)
val ErrorRedLight = Color(0xFFFFB4AB)

// ─── Macro colors — nature-inspired palette ────────────────────────────────
// Used in progress arcs, chips, and nutrition cards throughout the app.
// Names are kept identical so all 6 import sites pick up new values automatically.
val CalorieColor = Color(0xFFE8673C)  // Tomato-orange  (warm energy)
val ProteinColor = Color(0xFF2E8B7A)  // Teal-green     (avocado / clean protein)
val CarbsColor   = Color(0xFFD4A017)  // Honey-amber    (grain / carbs)
val FatColor     = Color(0xFF8E5BA2)  // Fig-purple     (rich, distinct)
