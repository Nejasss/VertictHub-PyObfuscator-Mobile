package com.verticthub.pyobfuscator.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.verticthub.pyobfuscator.design.tokens.DarkStudioColors
import com.verticthub.pyobfuscator.design.tokens.LightStudioColors
import com.verticthub.pyobfuscator.design.tokens.StudioTypography

data class ExtendedStudioPalette(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceSoft: Color,
    val primary: Color,
    val primaryDark: Color,
    val onPrimary: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val codeBackground: Color,
)

val LocalStudioPalette = staticCompositionLocalOf {
    ExtendedStudioPalette(
        background = DarkStudioColors.Background,
        surface = DarkStudioColors.Surface,
        surfaceElevated = DarkStudioColors.SurfaceElevated,
        surfaceSoft = DarkStudioColors.SurfaceSoft,
        primary = DarkStudioColors.Primary,
        primaryDark = DarkStudioColors.PrimaryDark,
        onPrimary = DarkStudioColors.OnPrimary,
        success = DarkStudioColors.Success,
        warning = DarkStudioColors.Warning,
        error = DarkStudioColors.Error,
        textPrimary = DarkStudioColors.TextPrimary,
        textSecondary = DarkStudioColors.TextSecondary,
        divider = DarkStudioColors.Divider,
        codeBackground = DarkStudioColors.CodeBackground,
    )
}

enum class ThemeMode { DARK, LIGHT, SYSTEM }

@Composable
fun VertictStudioTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val extended = if (useDark) {
        ExtendedStudioPalette(
            DarkStudioColors.Background, DarkStudioColors.Surface, DarkStudioColors.SurfaceElevated, DarkStudioColors.SurfaceSoft,
            DarkStudioColors.Primary, DarkStudioColors.PrimaryDark, DarkStudioColors.OnPrimary, DarkStudioColors.Success,
            DarkStudioColors.Warning, DarkStudioColors.Error, DarkStudioColors.TextPrimary, DarkStudioColors.TextSecondary,
            DarkStudioColors.Divider, DarkStudioColors.CodeBackground,
        )
    } else {
        ExtendedStudioPalette(
            LightStudioColors.Background, LightStudioColors.Surface, LightStudioColors.SurfaceElevated, LightStudioColors.SurfaceSoft,
            LightStudioColors.Primary, LightStudioColors.PrimaryDark, LightStudioColors.OnPrimary, LightStudioColors.Success,
            LightStudioColors.Warning, LightStudioColors.Error, LightStudioColors.TextPrimary, LightStudioColors.TextSecondary,
            LightStudioColors.Divider, LightStudioColors.CodeBackground,
        )
    }
    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = extended.primary, onPrimary = extended.onPrimary, secondary = extended.primaryDark,
            background = extended.background, onBackground = extended.textPrimary, surface = extended.surface,
            onSurface = extended.textPrimary, surfaceVariant = extended.surfaceElevated, onSurfaceVariant = extended.textSecondary,
            outline = extended.divider, error = extended.error,
        )
    } else {
        lightColorScheme(
            primary = extended.primary, onPrimary = extended.onPrimary, secondary = extended.primaryDark,
            background = extended.background, onBackground = extended.textPrimary, surface = extended.surface,
            onSurface = extended.textPrimary, surfaceVariant = extended.surfaceElevated, onSurfaceVariant = extended.textSecondary,
            outline = extended.divider, error = extended.error,
        )
    }
    CompositionLocalProvider(LocalStudioPalette provides extended) {
        MaterialTheme(colorScheme = colorScheme, typography = StudioTypography, content = content)
    }
}
