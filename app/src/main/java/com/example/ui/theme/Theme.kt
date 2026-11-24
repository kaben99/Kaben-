package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val ClarityDarkColorScheme = darkColorScheme(
    primary = LuminousCyan,
    onPrimary = MidnightNavy,
    primaryContainer = CyanContainerDark,
    onPrimaryContainer = CyanContainerLight,
    secondary = AmberGold,
    onSecondary = MidnightNavy,
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = AmberContainerLight,
    tertiary = EmeraldAccent,
    onTertiary = MidnightNavy,
    background = DeepSlate,
    onBackground = FrostWhite,
    surface = SlateSurface,
    onSurface = FrostWhite,
    surfaceVariant = SlateSurfaceElevated,
    onSurfaceVariant = MistGray,
    outline = SlateOutline,
    error = CoralAlert,
    onError = MidnightNavy
)

private val ClarityLightColorScheme = lightColorScheme(
    primary = DeepCyan,
    onPrimary = Color.White,
    primaryContainer = CyanContainerLight,
    onPrimaryContainer = MidnightNavy,
    secondary = DeepAmber,
    onSecondary = Color.White,
    secondaryContainer = AmberContainerLight,
    onSecondaryContainer = MidnightNavy,
    tertiary = EmeraldDeep,
    onTertiary = Color.White,
    background = PorcelainBg,
    onBackground = InkPrimary,
    surface = PorcelainSurface,
    onSurface = InkPrimary,
    surfaceVariant = PorcelainSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = LightOutline,
    error = CoralDeep,
    onError = Color.White
)

private val ClarityShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ClarityLensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ClarityDarkColorScheme
        else -> ClarityLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ClarityTypography,
        shapes = ClarityShapes,
        content = content
    )
}
