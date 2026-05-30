package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CosmicDarkColorScheme = darkColorScheme(
    primary = CosmicBlue,
    onPrimary = SolidWhite,
    secondary = NeonPeach,
    onSecondary = SpaceDark,
    tertiary = FlareGold,
    background = SpaceDark,
    onBackground = LightSlate,
    surface = SpaceCard,
    onSurface = LightSlate,
    error = CriticRed,
    onError = SolidWhite
)

private val CosmicLightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = SolidWhite,
    secondary = NeonPeach,
    onSecondary = SolidWhite,
    tertiary = FlareGold,
    background = LightSlate,
    onBackground = DarkText,
    surface = SolidWhite,
    onSurface = DarkText,
    error = CriticRed,
    onError = SolidWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted artistic color scheme primarily!
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) CosmicDarkColorScheme else CosmicLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
