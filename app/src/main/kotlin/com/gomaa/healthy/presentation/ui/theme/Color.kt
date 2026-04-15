package com.gomaa.healthy.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// Uber-inspired color palette - Black & White design system

// Primary colors
val UberBlack = Color(0xFF000000)
val PureWhite = Color(0xFFFFFFFF)

// Secondary colors
val BodyGray = Color(0xFF4b4b4b)
val ChipGray = Color(0xFFefefef)
val MutedGray = Color(0xFFafafaf)

// Hover states
val HoverGray = Color(0xFFe2e2e2)
val HoverLight = Color(0xFFf3f3f3)

// Shadows
val ShadowLight = Color(0x1F000000) // rgba(0,0,0,0.12)
val ShadowMedium = Color(0x29000000) // rgba(0,0,0,0.16)
val ShadowPressed = Color(0x14000000) // rgba(0,0,0,0.08)

// Border color
val BorderBlack = Color(0xFF000000)

// Material 3 Color Scheme - Updated to Uber palette
val Primary = UberBlack
val OnPrimary = PureWhite
val PrimaryContainer = ChipGray
val OnPrimaryContainer = UberBlack

val Secondary = BodyGray
val OnSecondary = PureWhite
val SecondaryContainer = ChipGray
val OnSecondaryContainer = UberBlack

val Tertiary = MutedGray
val OnTertiary = PureWhite
val TertiaryContainer = ChipGray
val OnTertiaryContainer = UberBlack

val Error = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF410002)

val Background = PureWhite
val OnBackground = UberBlack
val Surface = PureWhite
val OnSurface = UberBlack
val SurfaceVariant = ChipGray
val OnSurfaceVariant = BodyGray

val Outline = BorderBlack
val OutlineVariant = ChipGray

// Heart rate zone colors (kept from original)
val HeartRateZoneLow = Color(0xFF4CAF50)
val HeartRateZoneMedium = Color(0xFFFFEB3B)
val HeartRateZoneHigh = Color(0xFFFF9800)
val HeartRateZoneVeryHigh = Color(0xFFF44336)
