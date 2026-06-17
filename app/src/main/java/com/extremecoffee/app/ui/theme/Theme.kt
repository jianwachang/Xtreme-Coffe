package com.extremecoffee.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Palette "espresso" sobria: marroni profondi, ambra calda come accento, teal soft.
private val Espresso     = Color(0xFF15120F)
private val Surface1     = Color(0xFF211C18)
private val Surface2     = Color(0xFF2C2621)
private val Amber        = Color(0xFFE0883C)
private val AmberInk     = Color(0xFF231202)
private val AmberCont    = Color(0xFF3C2A17)
private val OnAmberCont  = Color(0xFFFFD8AE)
private val Teal         = Color(0xFF54B7C2)
private val TealInk      = Color(0xFF00242A)
private val Cream        = Color(0xFFEDE4D8)
private val Muted        = Color(0xFFB7AC9E)
private val Outline      = Color(0xFF4C443B)
private val ErrorRed     = Color(0xFFE57368)

private val CoffeeDark = darkColorScheme(
    primary = Amber,
    onPrimary = AmberInk,
    primaryContainer = AmberCont,
    onPrimaryContainer = OnAmberCont,
    secondary = Teal,
    onSecondary = TealInk,
    secondaryContainer = Color(0xFF14393F),
    onSecondaryContainer = Color(0xFFB6ECF2),
    background = Espresso,
    onBackground = Cream,
    surface = Surface1,
    onSurface = Cream,
    surfaceVariant = Surface2,
    onSurfaceVariant = Muted,
    outline = Outline,
    outlineVariant = Color(0xFF332D27),
    error = ErrorRed,
    onError = Color(0xFF2A0A07),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val AppType = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun ExtremeCoffeeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CoffeeDark,
        shapes = AppShapes,
        typography = AppType,
        content = content
    )
}
