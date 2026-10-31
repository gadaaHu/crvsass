package com.qcafr.gatherapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── CompositionLocals for semantic color tokens ─────────────────────────────

data class AppColors(
    val bgPrimary: Color,
    val bgSecondary: Color,
    val bgCard: Color,
    val borderSubtle: Color,
    val textPrimary: Color,   // was TextWhite
    val textMuted: Color,
    val textDim: Color,
    val isDark: Boolean
)

val LocalAppColors = compositionLocalOf {
    AppColors(
        bgPrimary    = DarkBgPrimary,
        bgSecondary  = DarkBgSecondary,
        bgCard       = DarkBgCard,
        borderSubtle = DarkBorderSubtle,
        textPrimary  = DarkTextWhite,
        textMuted    = DarkTextMuted,
        textDim      = DarkTextDim,
        isDark       = true
    )
}

// Convenience extension so call-sites stay identical to old Color.kt globals
val MaterialTheme.appColors: AppColors
    @Composable get() = LocalAppColors.current

private val DarkColorScheme = darkColorScheme(
    primary           = PrimaryIndigo,
    onPrimary         = DarkTextWhite,
    secondary         = AccentEmerald,
    onSecondary       = DarkTextWhite,
    tertiary          = AccentCyan,
    background        = DarkBgPrimary,
    onBackground      = DarkTextWhite,
    surface           = DarkBgCard,
    onSurface         = DarkTextWhite,
    surfaceVariant    = DarkBgSecondary,
    onSurfaceVariant  = DarkTextMuted,
    outline           = DarkBorderSubtle
)

private val LightColorScheme = lightColorScheme(
    primary           = PrimaryIndigo,
    onPrimary         = Color.White,
    secondary         = AccentEmerald,
    onSecondary       = Color.White,
    tertiary          = AccentCyan,
    background        = LightBgPrimary,
    onBackground      = LightTextWhite,
    surface           = LightBgCard,
    onSurface         = LightTextWhite,
    surfaceVariant    = LightBgSecondary,
    onSurfaceVariant  = LightTextMuted,
    outline           = LightBorderSubtle
)

@Composable
fun CRVSTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val appColors = if (darkTheme) {
        AppColors(
            bgPrimary    = DarkBgPrimary,
            bgSecondary  = DarkBgSecondary,
            bgCard       = DarkBgCard,
            borderSubtle = DarkBorderSubtle,
            textPrimary  = DarkTextWhite,
            textMuted    = DarkTextMuted,
            textDim      = DarkTextDim,
            isDark       = true
        )
    } else {
        AppColors(
            bgPrimary    = LightBgPrimary,
            bgSecondary  = LightBgSecondary,
            bgCard       = LightBgCard,
            borderSubtle = LightBorderSubtle,
            textPrimary  = LightTextWhite,
            textMuted    = LightTextMuted,
            textDim      = LightTextDim,
            isDark       = false
        )
    }

    // Keep legacy top-level color vars in sync so existing screens still compile
    BgPrimary    = appColors.bgPrimary
    BgSecondary  = appColors.bgSecondary
    BgCard       = appColors.bgCard
    BorderSubtle = appColors.borderSubtle
    TextWhite    = appColors.textPrimary
    TextMuted    = appColors.textMuted
    TextDim      = appColors.textDim

    val view = LocalView.current
    if (!view.isInEditMode) {
        val statusBarColor = colorScheme.background.toArgb()
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = statusBarColor
            window.navigationBarColor = statusBarColor
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}

@Composable
fun AskGatherTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    CRVSTheme(darkTheme = darkTheme, content = content)
}
