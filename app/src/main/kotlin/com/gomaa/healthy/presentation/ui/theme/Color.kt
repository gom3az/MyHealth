package com.gomaa.healthy.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// MyHealth Design System - "Health & Energy"
// =============================================================================
// A vibrant, energetic design language reflecting vitality, activity, and wellness.
// No pure black, no shadows - clean flat design with color differentiation.
//
// Color Philosophy:
// - Primary Green (#00C853): Vitality, growth, health - the core brand color
// - Secondary Orange (#FF9100): Energy, motivation, action - for CTAs and highlights  
// - Accent Blue (#2979FF): Trust, reliability, medical credibility
// - Dark surfaces: Deep forest green instead of black (#1B5E20 -> #0D3311)
// =============================================================================

// =============================================================================
// BRAND COLORS - Core identity
// =============================================================================

// Primary: Vibrant Green - vitality, growth, health
val HealthGreen = Color(0xFF00C853)
val HealthGreenLight = Color(0xFF69F0AE)
val HealthGreenDark = Color(0xFF00A843)

// Secondary: Energetic Orange - energy, motivation, action
val EnergyOrange = Color(0xFFFF9100)
val EnergyOrangeLight = Color(0xFFFFAB40)
val EnergyOrangeDark = Color(0xFFFF6D00)

// Tertiary: Trust Blue - reliability, medical credibility
val TrustBlue = Color(0xFF2979FF)
val TrustBlueLight = Color(0xFF82B1FF)
val TrustBlueDark = Color(0xFF2962FF)

// =============================================================================
// DARK SURFACES - Replacing pure black (#000000)
// =============================================================================

// Primary dark surface - Deep forest green (not black!)
val ForestDark = Color(0xFF0D3311)
val ForestMid = Color(0xFF1B5E20)

// Surface for dark sections, footer-like areas
val SurfaceDark = Color(0xFF121F14)

// =============================================================================
// LIGHT SURFACES - Replacing pure white (#ffffff)
// =============================================================================

// Clean backgrounds
val CloudWhite = Color(0xFFFAFAFA)
val SnowWhite = Color(0xFFFFFFFF)

// Subtle surface tints (instead of shadows for depth)
val SurfaceMint = Color(0xFFE8F5E9)      // Light green tint
val SurfacePeach = Color(0xFFFFF3E0)     // Light orange tint  
val SurfaceSky = Color(0xFFE3F2FD)        // Light blue tint
val SurfaceCream = Color(0xFFFFF8E1)      // Light warm tint

// =============================================================================
// TEXT COLORS - High contrast, accessible
// =============================================================================

// Primary text - dark green instead of black
val TextPrimary = Color(0xFF0D3311)
val TextSecondary = Color(0xFF388E3C)
val TextTertiary = Color(0xFF81C784)

// On dark backgrounds - light text
val TextOnDark = Color(0xFFE8F5E9)
val TextOnDarkSecondary = Color(0xFFA5D6A7)

// =============================================================================
// INTERACTIVE STATES
// =============================================================================

// Primary button states
val PrimaryDefault = HealthGreen
val PrimaryHover = HealthGreenDark
val PrimaryPressed = Color(0xFF007A2F)
val PrimaryText = Color(0xFFFFFFFF)

// Secondary button states
val SecondaryDefault = EnergyOrange
val SecondaryHover = EnergyOrangeDark
val SecondaryPressed = Color(0xFFE65100)
val SecondaryText = Color(0xFFFFFFFF)

// Outlined/ghost button backgrounds
val SurfaceInteractive = Color(0xFFF5FFF5)

// Chip/filter backgrounds
val ChipDefault = SurfaceMint
val ChipActive = HealthGreen
val ChipText = TextPrimary
val ChipActiveText = Color(0xFFFFFFFF)

// Hover states
val HoverGreen = Color(0xFFE0F2E0)
val HoverOrange = Color(0xFFFFE0B2)

// =============================================================================
// BORDERS & SEPARATION
// =============================================================================

// Primary border - dark green
val BorderPrimary = Color(0xFF1B5E20)

// Subtle borders for cards
val BorderSubtle = Color(0xFFC8E6C9)
val BorderLight = Color(0xFFE8F5E9)

// Divider color
val Divider = Color(0xFFDCEDC8)

// =============================================================================
// HEART RATE ZONES - Keeping existing, improving visibility
// =============================================================================

val HeartRateZoneLow = Color(0xFF4CAF50)      // Green - Fat Burn
val HeartRateZoneMedium = Color(0xFFFFEB3B)  // Yellow - Cardio
val HeartRateZoneHigh = Color(0xFFFF9800)    // Orange - Peak
val HeartRateZoneVeryHigh = Color(0xFFF44336) // Red - Max

// Improved zone colors with better contrast
val ZoneFatBurn = Color(0xFF2E7D32)          // Darker green
val ZoneCardio = Color(0xFFF9A825)            // Darker yellow
val ZonePeak = Color(0xFFEF6C00)             // Darker orange
val ZoneMax = Color(0xFFC62828)              // Darker red

// =============================================================================
// ACTIVITY METRICS COLORS
// =============================================================================

// Steps
val StepsColor = HealthGreen
val StepsColorLight = HealthGreenLight

// Calories  
val CaloriesColor = EnergyOrange
val CaloriesColorLight = EnergyOrangeLight

// Distance
val DistanceColor = TrustBlue
val DistanceColorLight = TrustBlueLight

// Duration/Time
val DurationColor = Color(0xFF7B1FA2)        // Purple

// =============================================================================
// STATUS & FEEDBACK
// =============================================================================

val Success = Color(0xFF2E7D32)
val SuccessLight = Color(0xFFC8E6C9)
val Warning = Color(0xFFF57C00)
val WarningLight = Color(0xFFFFE0B2)
val Error = Color(0xFFD32F2F)
val ErrorLight = Color(0xFFFFCDD2)
val Info = Color(0xFF1976D2)
val InfoLight = Color(0xFFBBDEFB)

// =============================================================================
// MATERIAL 3 COLOR SCHEME - Health & Energy Theme
// =============================================================================

// Primary: Health Green - main brand color
val md_primary = HealthGreen
val md_onPrimary = Color(0xFFFFFFFF)
val md_primaryContainer = SurfaceMint
val md_onPrimaryContainer = ForestMid

// Secondary: Energy Orange - CTAs and highlights
val md_secondary = EnergyOrange
val md_onSecondary = Color(0xFFFFFFFF)
val md_secondaryContainer = SurfacePeach
val md_onSecondaryContainer = Color(0xFF3E2723)

// Tertiary: Trust Blue - supporting actions
val md_tertiary = TrustBlue
val md_onTertiary = Color(0xFFFFFFFF)
val md_tertiaryContainer = SurfaceSky
val md_onTertiaryContainer = Color(0xFF0D47A1)

// Error states
val md_error = Color(0xFFBA1A1A)
val md_onError = Color(0xFFFFFFFF)
val md_errorContainer = ErrorLight
val md_onErrorContainer = Color(0xFF410002)

// Background & Surface - Light theme
val md_background = CloudWhite
val md_onBackground = TextPrimary
val md_surface = SnowWhite
val md_onSurface = TextPrimary
val md_surfaceVariant = SurfaceMint
val md_onSurfaceVariant = TextSecondary

// Outline & dividers
val md_outline = BorderSubtle
val md_outlineVariant = BorderLight

// Inverse colors for dark surfaces
val md_inverseSurface = ForestDark
val md_inverseOnSurface = TextOnDark
val md_inversePrimary = HealthGreenLight

// Scrim for overlays
val md_scrim = Color(0xFF000000)

// Surface tint for elevation feel (replacing shadow)
val md_surfaceTint = HealthGreen
val md_surfaceContainerLowest = SnowWhite
val md_surfaceContainerLow = Color(0xFFF1F8E9)
val md_surfaceContainer = Color(0xFFEDFAE8)
val md_surfaceContainerHigh = Color(0xFFE8F5DE)
val md_surfaceContainerHighest = Color(0xFFEFF7EC)