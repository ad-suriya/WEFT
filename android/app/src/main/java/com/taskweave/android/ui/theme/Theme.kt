package com.taskweave.android.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val EditorialColors = lightColorScheme(
    primary = Brand.Orange,
    onPrimary = Brand.White,
    primaryContainer = Color(0xFFF7E4DD),
    onPrimaryContainer = Brand.Ink,
    secondary = Brand.Green,
    onSecondary = Brand.White,
    secondaryContainer = Color(0xFFDFEBE7),
    onSecondaryContainer = Brand.Ink,
    tertiary = Brand.Purple,
    onTertiary = Brand.White,
    tertiaryContainer = Color(0xFFE7E3F7),
    onTertiaryContainer = Brand.Ink,
    background = Brand.Cream,
    onBackground = Brand.Ink,
    surface = Brand.Surface,
    onSurface = Brand.Ink,
    surfaceVariant = Brand.Cream,
    onSurfaceVariant = Brand.Ink,
    surfaceContainer = Brand.Surface,
    surfaceContainerHigh = Brand.Surface,
    surfaceContainerLow = Brand.Cream,
    outline = Brand.Ink,
    outlineVariant = Color(0x331A1A1A),
    error = Brand.Orange,
    onError = Brand.White,
    errorContainer = Color(0xFFF7E4DD),
    onErrorContainer = Brand.Ink,
    scrim = Brand.Ink,
)

/** No rounded corners anywhere — everything is square. */
private val Square = RoundedCornerShape(0.dp)
private val SquareShapes = Shapes(
    extraSmall = Square,
    small = Square,
    medium = Square,
    large = Square,
    extraLarge = Square,
)

@Composable
fun TaskWeaveTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Brand.Cream.toArgb()
            window.navigationBarColor = Brand.Surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }
    MaterialTheme(
        colorScheme = EditorialColors,
        typography = TaskWeaveTypography,
        shapes = SquareShapes,
        content = content,
    )
}
