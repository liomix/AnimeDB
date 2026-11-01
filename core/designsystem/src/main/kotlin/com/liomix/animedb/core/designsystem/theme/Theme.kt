package com.liomix.animedb.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F6FA),
    surfaceContainer = Color(0xFFEEF1F6),
    surfaceContainerHigh = Color(0xFFE8EBF1),
    surfaceContainerHighest = Color(0xFFE1E5EC),
    outline = OutlineLight,
    outlineVariant = Color(0xFFE0E4EA),
    error = ErrorLight,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = Color(0xFF0B0B0D),
    surfaceContainerLow = Color(0xFF141416),
    surfaceContainer = Color(0xFF1A1A1D),
    surfaceContainerHigh = Color(0xFF212125),
    surfaceContainerHighest = Color(0xFF2A2A2E),
    outline = OutlineDark,
    outlineVariant = Color(0xFF2E3136),
    error = ErrorDark,
)

/** 材质主题之外的语义色：只服务于测试台的状态表达。 */
@Immutable
data class ConsoleColors(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val monoSurface: Color,
)

private val LightConsoleColors = ConsoleColors(
    success = SuccessLight,
    successContainer = SuccessContainerLight,
    warning = WarningLight,
    warningContainer = WarningContainerLight,
    monoSurface = MonoSurfaceLight,
)

private val DarkConsoleColors = ConsoleColors(
    success = SuccessDark,
    successContainer = SuccessContainerDark,
    warning = WarningDark,
    warningContainer = WarningContainerDark,
    monoSurface = MonoSurfaceDark,
)

val LocalConsoleColors = staticCompositionLocalOf { LightConsoleColors }

object AnimeDbTheme {

    val consoleColors: ConsoleColors
        @Composable
        @ReadOnlyComposable
        get() = LocalConsoleColors.current
}

/**
 * AnimeDB 主题：不启用动态取色，保证测试台里同一份报文在日夜模式下对比一致。
 */
@Composable
fun AnimeDbTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AnimeDbTypography,
    ) {
        CompositionLocalProvider(
            LocalConsoleColors provides if (darkTheme) DarkConsoleColors else LightConsoleColors,
            // 非 Surface 容器里的 Text / Icon 也要跟随主题，避免夜间变黑。
            LocalContentColor provides colorScheme.onBackground,
            content = content,
        )
    }
}
