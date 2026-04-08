package de.melobeat.workoutplanner.ui.theme

import androidx.compose.ui.graphics.Color

// ── Dark & Deep fixed palette ─────────────────────────────────────────────────

// Surfaces
val DarkBackground      = Color(0xFF0D0D14)  // screen background
val DarkSurface         = Color(0xFF0D0D14)  // same as background
val DarkSurfaceContainer     = Color(0xFF1A1A28)  // cards, elevated surfaces
val DarkSurfaceContainerLow  = Color(0xFF141422)  // nav bar
val DarkSurfaceContainerLowest = Color(0xFF12102A) // stepper inner cards

// Borders / dividers
val DarkOutlineVariant  = Color(0x12FFFFFF)  // rgba(255,255,255,0.07) ≈ 0x12

// Text
val DarkOnBackground    = Color(0xFFEEE8FF)  // primary text (lavender-white)
val DarkOnSurface       = Color(0xFFEEE8FF)
val DarkOnSurfaceVariant = Color(0xFF7A7590) // secondary / supporting text

// Accent
val DarkPrimary         = Color(0xFFD0BCFF)  // active indicators, borders
val DarkPrimaryContainer = Color(0xFF3B2F6B) // tonal button containers, nav indicator pill
val DarkOnPrimary       = Color(0xFF1C1040)
val DarkOnPrimaryContainer = Color(0xFFEEE8FF)

val DarkSecondary       = Color(0xFFFFB0C8)
val DarkSecondaryContainer = Color(0xFF5E2750)
val DarkOnSecondary     = Color(0xFF3E0030)
val DarkOnSecondaryContainer = Color(0xFFFFD8E4)

val DarkTertiary        = Color(0xFFEADDFF)
val DarkTertiaryContainer = Color(0xFF4A3278)
val DarkOnTertiary      = Color(0xFF21005D)
val DarkOnTertiaryContainer = Color(0xFFEADDFF)

val DarkError           = Color(0xFFFFB4AB)
val DarkErrorContainer  = Color(0xFF93000A)
val DarkOnError         = Color(0xFF690005)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkSurfaceTint     = DarkPrimary

// ── Gradient stop colors (used by Brush.linearGradient at call sites) ─────────
val GradientHeroStart   = Color(0xFF4A0080)  // hero banner start
val GradientHeroMid     = Color(0xFF6750A4)  // hero banner mid / CTA start
val GradientHeroEnd     = Color(0xFFB5488A)  // hero banner end / CTA end
val GradientCardStart   = Color(0xFF2D1060)  // active exercise card header strip start
val GradientCardEnd     = Color(0xFF4A2280)  // active exercise card header strip end
