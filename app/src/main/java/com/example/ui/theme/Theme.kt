package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(
    val id: String,
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val surfaceColor: Color,
    val backgroundColor: Color
) {
    NEON_OBSIDIAN(
        id = "neon_obsidian",
        displayName = "بنفش نئونی (Neon Obsidian)",
        primaryColor = Color(0xFF8B5CF6),
        secondaryColor = Color(0xFF06B6D4),
        surfaceColor = Color(0xFF141923),
        backgroundColor = Color(0xFF0B0E14)
    ),
    CYAN_MATRIX(
        id = "cyan_matrix",
        displayName = "فیروزه‌ای سایبر (Cyan Matrix)",
        primaryColor = Color(0xFF06B6D4),
        secondaryColor = Color(0xFF10B981),
        surfaceColor = Color(0xFF0F1E24),
        backgroundColor = Color(0xFF071014)
    ),
    EMERALD_SPOTIFY(
        id = "emerald_spotify",
        displayName = "سبز اسپاتیفای (Spotify Emerald)",
        primaryColor = Color(0xFF1DB954),
        secondaryColor = Color(0xFF10B981),
        surfaceColor = Color(0xFF121E17),
        backgroundColor = Color(0xFF0A120D)
    ),
    SUNSET_FIRE(
        id = "sunset_fire",
        displayName = "غروب آتشین (Sunset Fire)",
        primaryColor = Color(0xFFF43F5E),
        secondaryColor = Color(0xFFFB923C),
        surfaceColor = Color(0xFF201318),
        backgroundColor = Color(0xFF120A0E)
    ),
    MIDNIGHT_BLUE(
        id = "midnight_blue",
        displayName = "آبی شبانه (Midnight Blue)",
        primaryColor = Color(0xFF3B82F6),
        secondaryColor = Color(0xFF60A5FA),
        surfaceColor = Color(0xFF111827),
        backgroundColor = Color(0xFF0B0F19)
    );

    companion object {
        fun fromId(id: String): AppThemeMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NEON_OBSIDIAN
        }
    }
}

fun getAppColorScheme(themeMode: AppThemeMode): ColorScheme {
    return darkColorScheme(
        primary = themeMode.primaryColor,
        onPrimary = Color.White,
        primaryContainer = themeMode.primaryColor.copy(alpha = 0.35f),
        onPrimaryContainer = Color(0xFFE9D5FF),
        secondary = themeMode.secondaryColor,
        onSecondary = Color.Black,
        secondaryContainer = themeMode.secondaryColor.copy(alpha = 0.25f),
        onSecondaryContainer = Color(0xFFCFFAFE),
        tertiary = SunsetRose,
        onTertiary = Color.White,
        background = themeMode.backgroundColor,
        onBackground = TextPrimary,
        surface = themeMode.surfaceColor,
        onSurface = TextPrimary,
        surfaceVariant = ObsidianSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        outline = ObsidianBorder,
        outlineVariant = Color(0xFF334155)
    )
}

@Composable
fun LyricEchoTheme(
    themeMode: AppThemeMode = AppThemeMode.NEON_OBSIDIAN,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = getAppColorScheme(themeMode),
        typography = Typography,
        content = content
    )
}
