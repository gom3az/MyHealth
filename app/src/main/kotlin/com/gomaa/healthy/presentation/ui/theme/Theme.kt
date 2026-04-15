package com.gomaa.healthy.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Uber Typography Scale - Full hierarchy for MaterialTheme.typography
val UberTypography = Typography(
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
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp, // 1.33
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp, // 1.40
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
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
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp, // 1.43
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 20.sp, // 1.67
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp, // 1.45
        letterSpacing = 0.5.sp
    )
)

// Uber-inspired Light Color Scheme - all colors from Theme, not static references
private val UberColorScheme = lightColorScheme(
    primary = UberBlack,           // #000000 - main actions
    onPrimary = PureWhite,          // #ffffff - text on primary
    primaryContainer = ChipGray,   // #efefef
    onPrimaryContainer = UberBlack,

    secondary = BodyGray,           // #4b4b4b - secondary text
    onSecondary = PureWhite,        // #ffffff
    secondaryContainer = ChipGray, // #efefef
    onSecondaryContainer = UberBlack,

    tertiary = MutedGray,           // #afafaf
    onTertiary = PureWhite,
    tertiaryContainer = ChipGray,
    onTertiaryContainer = UberBlack,

    error = Error,                  // Keep standard red
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    background = PureWhite,         // #ffffff - screen backgrounds
    onBackground = UberBlack,       // #000000 - main text
    surface = PureWhite,            // #ffffff - card backgrounds
    onSurface = UberBlack,          // #000000 - text on surface
    surfaceVariant = ChipGray,     // #efefef - chip backgrounds
    onSurfaceVariant = BodyGray,   // #4b4b4b - secondary text

    outline = BorderBlack,          // #000000 - borders
    outlineVariant = ChipGray
)

@Composable
fun HealthTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = UberColorScheme,
        typography = UberTypography,
        content = content
    )
}
