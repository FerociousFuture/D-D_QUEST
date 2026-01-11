package com.example.dnd_quest.ui.theme

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

// Definimos nuestro esquema oscuro personalizado
private val DarkColorScheme = darkColorScheme(
    primary = DnDRed,
    onPrimary = Color.White,
    secondary = DnDGold,
    onSecondary = Color.Black,
    tertiary = DnDGrey,
    background = DnDBlack,
    surface = DnDSurface,
    onBackground = DnDWhite,
    onSurface = DnDWhite,
    surfaceVariant = Color(0xFF2C2C2C), // Para las tarjetas del editor
    onSurfaceVariant = DnDWhite
)

@Composable
fun DND_QUESTTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Desactivamos colores dinámicos para mantener NUESTRO estilo
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Forzamos el tema oscuro siempre o usamos el del sistema si prefieres
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}