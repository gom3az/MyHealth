package com.gomaa.healthy.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// MyHealth Design System - "Health & Energy" Theme
// =============================================================================
// A vibrant, energetic design that reflects health, vitality, and wellness.
// - Health Green primary: Vitality, growth, activity
// - Energy Orange secondary: Energy, motivation, action
// - Full dark theme support with forest tones
// - WCAG AA compliant contrast ratios
// =============================================================================

// =============================================================================
// PRIMARY - Health Green (Vitality, growth, health, activity)
// =============================================================================
val Primary = Color(0xFF00C853)          // Health Green
val PrimaryLight = Color(0xFF69F0AE)      // Light green - highlights
val PrimaryDark = Color(0xFF00A843)      // Dark green - hover
val PrimaryPressed = Color(0xFF007A2F)   // Pressed state
val PrimaryContainer = Color(0xFFE8F5E9) // Subtle tint - containers
val OnPrimary = Color(0xFFFFFFFF)        // White text on primary
val OnPrimaryContainer = Color(0xFF0D3311)

// =============================================================================
// SECONDARY - Energy Orange (Energy, motivation, action)
// =============================================================================
val Secondary = Color(0xFFFF9100)        // Energy Orange
val SecondaryLight = Color(0xFFFFAB40)  // Light orange - active
val SecondaryDark = Color(0xFFFF6D00)   // Dark orange - hover
val SecondaryPressed = Color(0xFFE65100) // Pressed state
val SecondaryContainer = Color(0xFFFFF3E0) // Subtle tint
val OnSecondary = Color(0xFFFFFFFF)      // White text on secondary
val OnSecondaryContainer = Color(0xFF2D1600)

// =============================================================================
// TERTIARY - Trust Blue (Reliability, medical credibility)
// =============================================================================
val Tertiary = Color(0xFF2979FF)         // Trust Blue
val TertiaryLight = Color(0xFF82B1FF)   // Light blue
val TertiaryDark = Color(0xFF2962FF)    // Dark blue
val TertiaryContainer = Color(0xFFE3F2FD)
val OnTertiary = Color(0xFFFFFFFF)
val OnTertiaryContainer = Color(0xFF001F29)

// =============================================================================
// ERROR - Red (Standard semantics)
// =============================================================================
val Error = Color(0xFFD32F2F)
val ErrorLight = Color(0xFFEF5350)
val ErrorDark = Color(0xFFC62828)
val ErrorContainer = Color(0xFFFFCDD2)
val OnError = Color(0xFFFFFFFF)
val OnErrorContainer = Color(0xFF410002)

// =============================================================================
// SURFACE COLORS - Light Theme
// =============================================================================
val SurfaceLight = Color(0xFFFFFFFF)          // Snow White - cards
val SurfaceContainerLight = Color(0xFFF5F5F5) // Cloud White - page bg
val SurfaceContainerLowLight = Color(0xFFFAFAFA)
val SurfaceContainerHighLight = Color(0xFFEEEEEE)
val OnSurfaceLight = Color(0xFF0D3311)        // Forest Dark
val OnSurfaceVariantLight = Color(0xFF388E3C)
val OutlineLight = Color(0xFFC8E6C9)
val OutlineVariantLight = Color(0xFFE8F5E9)

// =============================================================================
// SURFACE COLORS - Dark Theme
// =============================================================================
val SurfaceDark = Color(0xFF1A2E1A)          // Forest Dark variant
val SurfaceContainerDark = Color(0xFF0D3311) // Primary dark
val SurfaceContainerLowDark = Color(0xFF121F14)
val SurfaceContainerHighDark = Color(0xFF1B5E20)
val OnSurfaceDark = Color(0xFFE8F5E9)
val OnSurfaceVariantDark = Color(0xFFA5D6A7)
val OutlineDark = Color(0xFF1B5E20)
val OutlineVariantDark = Color(0xFF2E7D32)

// =============================================================================
// TEXT COLORS - High contrast (WCAG AA)
// =============================================================================
val TextPrimaryLight = Color(0xFF0D3311)      // Forest Dark - 11.4:1 on #F5F5F5
val TextSecondaryLight = Color(0xFF388E3C)    // Medium green
val TextTertiaryLight = Color(0xFF81C784)    // Light green - disabled

val TextPrimaryDark = Color(0xFFE8F5E9)      // Light mint
val TextSecondaryDark = Color(0xFFA5D6A7)   // Medium mint
val TextTertiaryDark = Color(0xFF81C784)     // Light green

// =============================================================================
// INVERSE (Light on dark, dark on light)
// =============================================================================
val InverseSurface = Color(0xFFE8F5E9)
val InverseOnSurface = Color(0xFF0D3311)
val InversePrimary = Color(0xFF69F0AE)

// =============================================================================
// STATUS & FEEDBACK
// =============================================================================
val StatusSuccess = Color(0xFF2E7D32)
val StatusSuccessLight = Color(0xFFC8E6C9)
val StatusWarning = Color(0xFFF57C00)
val StatusWarningLight = Color(0xFFFFE0B2)
val StatusError = Color(0xFFD32F2F)
val StatusErrorLight = Color(0xFFFFCDD2)
val StatusInfo = Color(0xFF1976D2)
val StatusInfoLight = Color(0xFFBBDEFB)

// =============================================================================
// HEART RATE ZONES - Accessible on both themes
// =============================================================================
val ZoneFatBurn = Color(0xFF2E7D32)        // Dark green - Fat Burn
val ZoneCardio = Color(0xFFF9A825)         // Dark yellow - Cardio (dark text)
val ZonePeak = Color(0xFFEF6C00)           // Dark orange - Peak
val ZoneMax = Color(0xFFC62828)            // Dark red - Max

// Light variants (for subtle highlights)
val ZoneFatBurnLight = Color(0xFF4CAF50)
val ZoneCardioLight = Color(0xFFFFCA28)
val ZonePeakLight = Color(0xFFFF9100)
val ZoneMaxLight = Color(0xFFEF5350)

// =============================================================================
// ACTIVITY METRICS
// =============================================================================
val MetricSteps = Primary                 // Health Green
val MetricStepsContainer = PrimaryLight
val MetricCalories = Secondary            // Energy Orange
val MetricCaloriesContainer = SecondaryLight
val MetricDistance = Tertiary             // Trust Blue
val MetricDistanceContainer = TertiaryLight
val MetricDuration = Color(0xFF7B1FA2)    // Purple
val MetricDurationContainer = Color(0xFFBA68C8)

// =============================================================================
// INTERACTIVE STATES
// =============================================================================
val InteractiveDefault = Primary
val InteractiveHover = PrimaryDark
val InteractivePressed = PrimaryPressed
val InteractiveDisabled = Color(0xFFC8E6C9)
val InteractiveSecondary = Secondary

// =============================================================================
// BORDERS
// =============================================================================
val BorderPrimary = Color(0xFF1B5E20)     // Strong borders
val BorderSubtle = Color(0xFFC8E6C9)      // Card borders
val BorderLight = Color(0xFFE8F5E9)       // Subtle separation
val Divider = Color(0xFFDCEDC8)

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
