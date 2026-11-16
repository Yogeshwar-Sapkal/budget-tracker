package com.yogeshwar.budgettracker.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF0B6E4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFF3EA),
    onPrimaryContainer = Color(0xFF0B6E4F),
    tertiaryContainer = Color(0xFFFFF1D0),
    onTertiaryContainer = Color(0xFF8A5A00),
    errorContainer = Color(0xFFFDE4E4),
    onErrorContainer = Color(0xFFA4262C),
    background = Color(0xFFF6F7F9),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF4CC79B),
    onPrimary = Color(0xFF07261B),
    primaryContainer = Color(0xFF12372B),
    onPrimaryContainer = Color(0xFF4CC79B),
    tertiaryContainer = Color(0xFF3A2C0D),
    onTertiaryContainer = Color(0xFFF0B64A),
    errorContainer = Color(0xFF401A1A),
    onErrorContainer = Color(0xFFFF8C8C),
)

@Composable
fun BudgetTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
