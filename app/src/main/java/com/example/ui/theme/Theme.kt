package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = OnEmeraldPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = TerracottaSecondary,
    onSecondary = OnTerracottaSecondary,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = OnTerracottaContainer,
    tertiary = GoldTertiary,
    onTertiary = OnGoldTertiary,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = OnGoldContainer,
    background = WarmLimestoneBg,
    onBackground = DeepCharcoal,
    surface = CrispSurface,
    onSurface = DeepCharcoal,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = MutedSlate,
    outline = WarmOutline,
    outlineVariant = SubtleOutline,
    error = ErrorCrimson,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = OnEmeraldDarkPrimary,
    primaryContainer = EmeraldDarkContainer,
    onPrimaryContainer = OnEmeraldDarkContainer,
    secondary = TerracottaDarkSecondary,
    onSecondary = OnTerracottaDarkSecondary,
    secondaryContainer = TerracottaDarkContainer,
    onSecondaryContainer = OnTerracottaDarkContainer,
    tertiary = GoldDarkTertiary,
    onTertiary = OnGoldDarkTertiary,
    tertiaryContainer = GoldDarkContainer,
    onTertiaryContainer = OnGoldDarkContainer,
    background = DarkObsidianBg,
    onBackground = OffWhiteText,
    surface = DarkSurface,
    onSurface = OffWhiteText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedSlate,
    outline = DarkOutline,
    outlineVariant = DarkSubtleOutline,
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun KhmerHomeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
