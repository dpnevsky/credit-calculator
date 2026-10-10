package com.example.creditcalculatormobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AppLightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = CardWhite,
    primaryContainer = NavbarBlue,
    onPrimaryContainer = CardWhite,
    secondary = PrimaryBlueDark,
    onSecondary = CardWhite,
    background = PageBackground,
    onBackground = TextPrimary,
    surface = CardWhite,
    onSurface = TextPrimary,
    surfaceVariant = PageBackground,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    error = ErrorRed,
)

private val AppDarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = CardWhite,
    primaryContainer = NavbarBlue,
    onPrimaryContainer = CardWhite,
    secondary = PrimaryBlueDark,
    onSecondary = CardWhite,
    background = ColorTokens.DarkBackground,
    onBackground = CardWhite,
    surface = ColorTokens.DarkSurface,
    onSurface = CardWhite,
    surfaceVariant = ColorTokens.DarkSurfaceVariant,
    onSurfaceVariant = ColorTokens.DarkOnSurfaceVariant,
    outline = ColorTokens.DarkOutline,
    error = ErrorRed,
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

@Composable
fun CreditCalculatorMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        AppDarkColorScheme
    } else {
        AppLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

private object ColorTokens {
    val DarkBackground = androidx.compose.ui.graphics.Color(0xFF0F172A)
    val DarkSurface = androidx.compose.ui.graphics.Color(0xFF111827)
    val DarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF1F2937)
    val DarkOnSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFCBD5E1)
    val DarkOutline = androidx.compose.ui.graphics.Color(0xFF334155)
}
