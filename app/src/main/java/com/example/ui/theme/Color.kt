package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme

// Palestinian Corporate Brand Colors
val PalGreenDark = Color(0xFF104620)
val PalGreenNormal = Color(0xFF1E753C)
val PalGreenLight = Color(0xFF4CAF50)

val PalRedNormal = Color(0xFFCE2029)
val PalRedLight = Color(0xFFEF5350)

val isSystemDarkZone: Boolean
    @Composable
    get() = MaterialTheme.colorScheme.primary == Color(0xFF4CAF50)

val PalBlackDark: Color
    @Composable
    get() = if (isSystemDarkZone) Color(0xFF0A0A0A) else Color(0xFFF7FDF9)

val PalBlackNormal: Color
    @Composable
    get() = if (isSystemDarkZone) Color(0xFF111111) else Color(0xFFFFFFFF)

val PalBlackLight: Color
    @Composable
    get() = if (isSystemDarkZone) Color(0xFF1A1A1A) else Color(0xFFF2F4F3)

val PalWhitePure: Color
    @Composable
    get() = if (isSystemDarkZone) Color(0xFFFFFFFF) else Color(0xFF0A0A0A)

val PalWhiteSoft: Color
    @Composable
    get() = if (isSystemDarkZone) Color(0xFFF7FDF9) else Color(0xFF111111)

val PalWhiteMuted: Color
    @Composable
    get() = if (isSystemDarkZone) Color(0xFF8E8E93) else Color(0xFF555555)

val PalGoldCalligraphy = Color(0xFFFFD700)
