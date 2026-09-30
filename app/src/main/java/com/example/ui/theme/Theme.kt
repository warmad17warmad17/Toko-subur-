package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val TokoMakmurColorScheme = lightColorScheme(
    primary = MakmurBluePrimary,
    onPrimary = MakmurBlueOnPrimary,
    primaryContainer = MakmurBlueContainer,
    onPrimaryContainer = MakmurOnBlueContainer,
    secondary = MakmurTealSecondary,
    onSecondary = MakmurTealOnSecondary,
    secondaryContainer = MakmurTealContainer,
    onSecondaryContainer = MakmurOnTealContainer,
    tertiary = MakmurAmberWarning,
    onTertiary = MakmurBlueOnPrimary,
    tertiaryContainer = MakmurAmberContainer,
    onTertiaryContainer = MakmurOnAmberContainer,
    background = MakmurBackground,
    onBackground = MakmurOnBackground,
    surface = MakmurSurface,
    onSurface = MakmurOnSurface,
    surfaceVariant = MakmurSurfaceVariant,
    onSurfaceVariant = MakmurOnSurfaceVariant,
    outline = MakmurOutline,
    outlineVariant = MakmurOutlineVariant,
    error = MakmurRedError,
    onError = MakmurBlueOnPrimary,
    errorContainer = MakmurRedContainer,
    onErrorContainer = MakmurOnRedContainer
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    // User requested explicit light theme ("Tema aplikasi Terang")
    MaterialTheme(
        colorScheme = TokoMakmurColorScheme,
        typography = Typography,
        content = content
    )
}
