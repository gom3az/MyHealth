package com.gomaa.healthy.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// =============================================================================
// MyHealth Typography Scale - Clean, modern, accessible
// =============================================================================
// Font: System default sans-serif (Roboto on Android)
// Keeping the hierarchy from the original but with improved readability
// =============================================================================

val HealthTypography = Typography(
    // Display - Hero headlines
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
        lineHeight = 64.sp, // 1.23
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp, // 1.22
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp, // 1.25
        letterSpacing = 0.sp
    ),

    // Headlines - Section titles
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp, // 1.33
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp, // 1.40
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp, // 1.33
        letterSpacing = 0.sp
    ),

    // Titles - Cards and features
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp, // 1.33
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp, // 1.50
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp, // 1.43
        letterSpacing = 0.1.sp
    ),

    // Body - Main content
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp, // 1.50
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp, // 1.43
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp, // 1.33
        letterSpacing = 0.4.sp
    ),

    // Labels - Buttons, chips, navigation
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp, // 1.43
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp, // 1.33
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp, // 1.45
        letterSpacing = 0.5.sp
    )
)

// =============================================================================
// MyHealth Color Scheme - Health & Energy Theme
// =============================================================================
//
// Design Principles:
// - NO pure black (#000000) - replaced with ForestDark (#0D3311)
// - NO shadows - use color differentiation and borders instead
// - Vibrant green as primary - health, vitality, growth
// - Energetic orange for secondary - energy, motivation
// - Trust blue for tertiary - reliability, medical credibility
// =============================================================================

private val HealthEnergyColorScheme = lightColorScheme(
    // Primary - Health Green
    primary = md_primary,                    // #00C853
    onPrimary = md_onPrimary,                // #FFFFFF
    primaryContainer = md_primaryContainer,  // #E8F5E9
    onPrimaryContainer = md_onPrimaryContainer, // #0D3311

    // Secondary - Energy Orange  
    secondary = md_secondary,               // #FF9100
    onSecondary = md_onSecondary,           // #FFFFFF
    secondaryContainer = md_secondaryContainer, // #FFF3E0
    onSecondaryContainer = md_onSecondaryContainer, // #3E2723

    // Tertiary - Trust Blue
    tertiary = md_tertiary,                  // #2979FF
    onTertiary = md_onTertiary,             // #FFFFFF
    tertiaryContainer = md_tertiaryContainer, // #E3F2FD
    onTertiaryContainer = md_onTertiaryContainer, // #0D47A1

    // Error
    error = md_error,                        // #BA1A1A
    onError = md_onError,                   // #FFFFFF
    errorContainer = md_errorContainer,     // #FFCDD2
    onErrorContainer = md_onErrorContainer, // #410002

    // Background & Surface
    background = md_background,              // #FAFAFA
    onBackground = md_onBackground,         // #0D3311
    surface = md_surface,                   // #FFFFFF
    onSurface = md_onSurface,               // #0D3311
    surfaceVariant = md_surfaceVariant,     // #E8F5E9
    onSurfaceVariant = md_onSurfaceVariant, // #388E3C

    // Outline
    outline = md_outline,                    // #C8E6C9
    outlineVariant = md_outlineVariant,     // #E8F5E9

    // Inverse (for dark surfaces if needed)
    inverseSurface = md_inverseSurface,      // #0D3311
    inverseOnSurface = md_inverseOnSurface, // #E8F5E9
    inversePrimary = md_inversePrimary,     // #69F0AE

    // Surface tint & containers
    surfaceTint = md_surfaceTint,            // #00C853
    surfaceContainerLowest = md_surfaceContainerLowest, // #FFFFFF
    surfaceContainerLow = md_surfaceContainerLow,     // #F1F8E9
    surfaceContainer = md_surfaceContainer,           // #EDFAE8
    surfaceContainerHigh = md_surfaceContainerHigh, // #E8F5DE
    surfaceContainerHighest = md_surfaceContainerHighest, // #EFF7EC

    // Scrim
    scrim = md_scrim                        // #000000
)

@Composable
fun HealthTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HealthEnergyColorScheme,
        typography = HealthTypography,
        content = content
    )
}