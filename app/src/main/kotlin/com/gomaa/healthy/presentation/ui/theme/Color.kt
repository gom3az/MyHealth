package com.gomaa.healthy.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// MyHealth Design System - Professional Medical Fitness Theme
// =============================================================================
// A professional, accessible color system for a fitness tracking app.
// - Teal primary: Medical credibility + vitality
// - Amber secondary: Energy, warmth, motivation
// - Full dark theme support
// - WCAG AA compliant contrast ratios
// =============================================================================

// =============================================================================
// PRIMARY - Teal (Medical credibility + vitality)
// =============================================================================
val Primary = Color(0xFF00897B)          // Main teal - #00897B
val PrimaryLight = Color(0xFF4DB6AC)    // Light teal
val PrimaryDark = Color(0xFF00695C)     // Dark teal
val PrimaryContainer = Color(0xFFB2DFDB) // Light teal tint
val OnPrimaryContainer = Color(0xFF00251A)

// =============================================================================
// SECONDARY - Amber (Energy, warmth, positivity)
// =============================================================================
val Secondary = Color(0xFFFF8F00)       // Main amber
val SecondaryLight = Color(0xFFFFB300) // Light amber
val SecondaryDark = Color(0xFFF57C00)  // Dark amber
val SecondaryContainer = Color(0xFFFFE0B2)
val OnSecondaryContainer = Color(0xFF2D1600)

// =============================================================================
// TERTIARY - Light Blue (Supporting, calm)
// =============================================================================
val Tertiary = Color(0xFF039BE5)         // Light blue
val TertiaryLight = Color(0xFF4FC3F7)
val TertiaryDark = Color(0xFF0277BD)
val TertiaryContainer = Color(0xFFE1F5FE)
val OnTertiaryContainer = Color(0xFF001F29)

// =============================================================================
// ERROR - Red (Standard semantics)
// =============================================================================
val Error = Color(0xFFD32F2F)
val ErrorLight = Color(0xFFEF5350)
val ErrorDark = Color(0xFFC62828)
val ErrorContainer = Color(0xFFFFCDD2)
val OnErrorContainer = Color(0xFF410002)

// =============================================================================
// NEUTRAL - Slate/Gray (Professional, calm)
// =============================================================================
val Neutral = Color(0xFF37474F)         // Blue-gray 800
val NeutralLight = Color(0xFF78909C)   // Blue-gray 400
val NeutralDark = Color(0xFF263238)    // Blue-gray 900
val NeutralContainer = Color(0xFFECEFF1)
val OnNeutralContainer = Color(0xFF0F1619)

// =============================================================================
// LIGHT THEME - Surfaces
// =============================================================================
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceContainerLight = Color(0xFFF5F7F8)
val SurfaceContainerLowLight = Color(0xFFFAFDFD)
val SurfaceContainerHighLight = Color(0xFFEEF2F3)
val OnSurfaceLight = Color(0xFF1C1B1F)
val OnSurfaceVariantLight = Color(0xFF49454F)
val OutlineLight = Color(0xFF79747E)
val OutlineVariantLight = Color(0xFFCAC4D0)

// =============================================================================
// DARK THEME - Surfaces
// =============================================================================
val SurfaceDark = Color(0xFF121416)
val SurfaceContainerDark = Color(0xFF1C1D1F)
val SurfaceContainerLowDark = Color(0xFF191A1C)
val SurfaceContainerHighDark = Color(0xFF232527)
val OnSurfaceDark = Color(0xFFE6E1E5)
val OnSurfaceVariantDark = Color(0xFFCAC4D0)
val OutlineDark = Color(0xFF938F99)
val OutlineVariantDark = Color(0xFF49454F)

// =============================================================================
// TEXT - High contrast (WCAG AA)
// =============================================================================
val TextPrimaryLight = Color(0xFF1C1B1F)      // 13.5:1 on white
val TextSecondaryLight = Color(0xFF49454F)    // 7.3:1 on white
val TextTertiaryLight = Color(0xFF79747E)      // 4.6:1 on white (AA for large text)

val TextPrimaryDark = Color(0xFFE6E1E5)         // 12.8:1 on #121416
val TextSecondaryDark = Color(0xFFCAC4D0)     // 6.2:1 on #121416
val TextTertiaryDark = Color(0xFF938F99)       // 4.5:1 on #121416 (AA borderline)

// =============================================================================
// INVERSE (Light on dark, dark on light)
// =============================================================================
val InverseSurface = Color(0xFF313033)
val InverseOnSurface = Color(0xFFF4EFF4)
val InversePrimary = Color(0xFF4DB6AC)

// =============================================================================
// STATUS & FEEDBACK
// =============================================================================
val StatusSuccess = Color(0xFF2E7D32)
val StatusSuccessLight = Color(0xFFC8E6C9)
val StatusWarning = Color(0xFFEF6C00)
val StatusWarningLight = Color(0xFFFFE0B2)
val StatusError = Color(0xFFD32F2F)
val StatusErrorLight = Color(0xFFFFCDD2)
val StatusInfo = Color(0xFF1565C0)
val StatusInfoLight = Color(0xFFBBDEFB)

// =============================================================================
// HEART RATE ZONES - Accessible on both themes
// =============================================================================
val ZoneFatBurn = Color(0xFF388E3C)        // Green - Fat Burn
val ZoneCardio = Color(0xFFF9A825)        // Amber - Cardio
val ZonePeak = Color(0xFFFF6D00)           // Orange - Peak
val ZoneMax = Color(0xFFD32F2F)            // Red - Max

// Light variants (for dark theme backgrounds)
val ZoneFatBurnLight = Color(0xFF4CAF50)
val ZoneCardioLight = Color(0xFFFFCA28)
val ZonePeakLight = Color(0xFFFF9100)
val ZoneMaxLight = Color(0xFFEF5350)

// =============================================================================
// ACTIVITY METRICS
// =============================================================================
val MetricSteps = Primary
val MetricStepsContainer = PrimaryContainer
val MetricCalories = Secondary
val MetricCaloriesContainer = SecondaryContainer
val MetricDistance = Tertiary
val MetricDistanceContainer = TertiaryContainer
val MetricDuration = Color(0xFF7B1FA2)     // Purple
val MetricDurationContainer = Color(0xFFE1BEE7)

// =============================================================================
// INTERACTIVE STATES
// =============================================================================
val InteractiveDefault = Primary
val InteractiveHover = Color(0xFFB2DFDB)
val InteractivePressed = Color(0xFF80CBC4)
val InteractiveDisabled = Color(0xFFBDBDBD)
val InteractiveSecondary = Secondary

// =============================================================================
// BORDERS
// =============================================================================
val BorderLight = Color(0xFFE0E0E0)
val BorderDark = Color(0xFF424242)

// Backward compatibility aliases
val HeartRateZoneLow = ZoneFatBurnLight
val HeartRateZoneMedium = ZoneCardioLight
val HeartRateZoneHigh = ZonePeakLight
val HeartRateZoneVeryHigh = ZoneMaxLight

val HealthGreen = Primary
val HealthGreenLight = PrimaryLight
val HealthGreenDark = PrimaryDark

val EnergyOrange = Secondary
val EnergyOrangeLight = SecondaryLight
val EnergyOrangeDark = SecondaryDark

val TrustBlue = Tertiary
val TrustBlueLight = TertiaryLight
val TrustBlueDark = TertiaryDark

val TextPrimary = TextPrimaryLight
val TextSecondary = TextSecondaryLight
val TextOnDark = TextPrimaryDark
val TextOnDarkSecondary = TextSecondaryDark

val SurfaceMint = PrimaryContainer
val SurfacePeach = SecondaryContainer
val SurfaceSky = TertiaryContainer

val Success = StatusSuccess
val Warning = StatusWarning
val StatusErrorAlias = StatusError
val Info = StatusInfo