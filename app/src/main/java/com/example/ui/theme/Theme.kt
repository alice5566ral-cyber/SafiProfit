package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class SemanticFinanceColors(
    val profit: Color,
    val profitContainer: Color,
    val loss: Color,
    val lossContainer: Color,
    val pending: Color,
    val pendingContainer: Color,
    val cardBorder: Color
)

private val LightSemanticColors = SemanticFinanceColors(
    profit = ProfitGreenLight,
    profitContainer = ProfitGreenContainerLight,
    loss = LossRoseLight,
    lossContainer = LossRoseContainerLight,
    pending = PendingAmberLight,
    pendingContainer = PendingAmberContainerLight,
    cardBorder = OutlineLight
)

private val DarkSemanticColors = SemanticFinanceColors(
    profit = ProfitGreenDark,
    profitContainer = ProfitGreenContainerDark,
    loss = LossRoseDark,
    lossContainer = LossRoseContainerDark,
    pending = PendingAmberDark,
    pendingContainer = PendingAmberContainerDark,
    cardBorder = OutlineDark
)

val LocalFinanceColors = staticCompositionLocalOf { LightSemanticColors }

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryEmeraldDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerMint,
    secondary = SecondarySlateDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryAmberDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = LossRoseDark,
    errorContainer = LossRoseContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryEmerald,
    onPrimary = OnPrimaryWhite,
    primaryContainer = PrimaryContainerMint,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondarySlate,
    onSecondary = OnSecondaryWhite,
    secondaryContainer = SecondaryContainerSlate,
    onSecondaryContainer = OnSecondaryContainerSlate,
    tertiary = TertiaryGold,
    tertiaryContainer = TertiaryContainerAmber,
    onTertiaryContainer = OnTertiaryContainerAmber,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = LossRoseLight,
    errorContainer = LossRoseContainerLight
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SafiProfitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalFinanceColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = SafiProfitTheme(darkTheme = darkTheme, content = content)
