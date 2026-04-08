package de.melobeat.workoutplanner.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val AppDarkColorScheme = darkColorScheme(
    primary                = DarkPrimary,
    onPrimary              = DarkOnPrimary,
    primaryContainer       = DarkPrimaryContainer,
    onPrimaryContainer     = DarkOnPrimaryContainer,
    secondary              = DarkSecondary,
    onSecondary            = DarkOnSecondary,
    secondaryContainer     = DarkSecondaryContainer,
    onSecondaryContainer   = DarkOnSecondaryContainer,
    tertiary               = DarkTertiary,
    onTertiary             = DarkOnTertiary,
    tertiaryContainer      = DarkTertiaryContainer,
    onTertiaryContainer    = DarkOnTertiaryContainer,
    error                  = DarkError,
    errorContainer         = DarkErrorContainer,
    onError                = DarkOnError,
    onErrorContainer       = DarkOnErrorContainer,
    background             = DarkBackground,
    onBackground           = DarkOnBackground,
    surface                = DarkSurface,
    onSurface              = DarkOnSurface,
    surfaceVariant         = DarkSurfaceContainer,
    onSurfaceVariant       = DarkOnSurfaceVariant,
    outline                = DarkOutlineVariant,
    outlineVariant         = DarkOutlineVariant,
    surfaceTint            = DarkSurfaceTint,
)

@Composable
fun WorkoutPlannerTheme(
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // App is dark-only. No light theme.
    // Dynamic color opt-in always uses the dark variant.
    val colorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        AppDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
