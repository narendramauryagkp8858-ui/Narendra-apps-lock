package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

private val AppLockDarkColorScheme = darkColorScheme(
    primary = RoyalPurple,
    onPrimary = TextPrimary,
    primaryContainer = DeepViolet,
    onPrimaryContainer = SoftLavender,
    secondary = CyanShield,
    onSecondary = ObsidianBlack,
    secondaryContainer = ElevatedNavy,
    onSecondaryContainer = CyanShield,
    tertiary = ElectricViolet,
    onTertiary = TextPrimary,
    background = DeepNavy,
    onBackground = TextPrimary,
    surface = MidnightNavy,
    onSurface = TextPrimary,
    surfaceVariant = CardGlassNavy,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderGlow,
    error = AlertCrimson,
    onError = TextPrimary
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

object AppLockGradients {
    val ScreenBackground = Brush.verticalGradient(
        colors = listOf(
            ObsidianBlack,
            DeepNavy,
            MidnightNavy,
            ColorBlendPurple
        )
    )

    val HeroCardGradient = Brush.linearGradient(
        colors = listOf(
            ElevatedNavy,
            DeepViolet,
            MidnightNavy
        )
    )

    val PurpleShieldGradient = Brush.linearGradient(
        colors = listOf(
            RoyalPurple,
            ElectricViolet
        )
    )

    val CyanPurpleGradient = Brush.linearGradient(
        colors = listOf(
            CyanShield,
            RoyalPurple
        )
    )
}

private val ColorBlendPurple = androidx.compose.ui.graphics.Color(0xFF140D2E)

@Composable
fun NarendraAppLockTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppLockDarkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
