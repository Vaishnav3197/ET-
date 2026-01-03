package com.Vaishnav.employeetracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryBlueDarkVariant,
    onPrimaryContainer = OnPrimaryLight,
    
    secondary = SecondaryTealDark,
    onSecondary = OnPrimaryDark,
    secondaryContainer = SecondaryTealDarkVariant,
    onSecondaryContainer = OnPrimaryLight,
    
    tertiary = TertiaryOrangeDark,
    onTertiary = OnPrimaryDark,
    
    background = BackgroundDark,
    onBackground = Color(0xFFE1E2E5),
    
    surface = SurfaceDark,
    onSurface = Color(0xFFE1E2E5),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFC4C6C9),
    
    error = ErrorRed,
    onError = OnPrimaryLight,
    
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF43474E)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = Color(0xFF001D36),
    
    secondary = SecondaryTeal,
    onSecondary = OnPrimaryLight,
    secondaryContainer = SecondaryTealLight,
    onSecondaryContainer = Color(0xFF002020),
    
    tertiary = TertiaryOrange,
    onTertiary = OnPrimaryLight,
    
    background = BackgroundLight,
    onBackground = Color(0xFF1A1C1E),
    
    surface = SurfaceLight,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF43474E),
    
    error = ErrorRed,
    onError = OnPrimaryLight,
    
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C7CF)
)

@Composable
fun EmployeeTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to always use our custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}