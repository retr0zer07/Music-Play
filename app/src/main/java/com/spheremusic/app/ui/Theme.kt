package com.spheremusic.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SphereBackground = Color(0xFF0C0B12)
val SphereSurface = Color(0xFF191720)
val SpherePurple = Color(0xFFBAA0FF)
val SphereMint = Color(0xFFDFF9C1)
val SphereMuted = Color(0xFFADA7BA)

@Composable
fun SphereTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = SpherePurple,
            onPrimary = Color(0xFF22143C),
            secondary = SphereMint,
            background = SphereBackground,
            surface = SphereSurface,
            onBackground = Color(0xFFF4F0FA),
            onSurface = Color(0xFFF4F0FA),
            onSurfaceVariant = SphereMuted,
        ),
        content = content,
    )
}
