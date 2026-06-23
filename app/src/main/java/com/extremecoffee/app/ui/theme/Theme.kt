package com.extremecoffee.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.extremecoffee.app.R

/* ---------- Tipografia: DM Serif Display (titoli) + Poppins (interfaccia) ---------- */
private val Serif = FontFamily(Font(R.font.dm_serif_display, FontWeight.Normal))
private val Sans = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

private val AppType = Typography(
    displayLarge  = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 52.sp, lineHeight = 56.sp),
    displayMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 46.sp),
    displaySmall  = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 38.sp),
    headlineLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium= TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 27.sp, lineHeight = 33.sp),
    headlineSmall = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Normal, fontSize = 23.sp, lineHeight = 29.sp),
    titleLarge    = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium   = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    titleSmall    = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge     = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium    = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall     = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge    = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium   = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
    labelSmall    = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.4.sp),
)

/* ---------- Schema CHIARO (crema + arancio brand) ---------- */
private val CoffeeLight = lightColorScheme(
    primary = Color(0xFFE8772E),            onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFCE3CE),   onPrimaryContainer = Color(0xFF5A2E10),
    secondary = Color(0xFFC85F1C),          onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF4E6D2), onSecondaryContainer = Color(0xFF5A4632),
    tertiary = Color(0xFFD9A24B),           onTertiary = Color(0xFF3A2418),
    tertiaryContainer = Color(0xFFF6E7C9),  onTertiaryContainer = Color(0xFF50380F),
    background = Color(0xFFFBF4EA),         onBackground = Color(0xFF2A1A0F),
    surface = Color(0xFFFBF4EA),            onSurface = Color(0xFF2A1A0F),
    surfaceVariant = Color(0xFFFFFFFF),     onSurfaceVariant = Color(0xFF9C8770),
    outline = Color(0xFFE7D6BD),            outlineVariant = Color(0xFFF0E2CF),
    error = Color(0xFFB23B2E),              onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFBE9E5),     onErrorContainer = Color(0xFF7A271E),
)

/* ---------- Schema SCURO (espresso) ---------- */
private val CoffeeDark = darkColorScheme(
    primary = Color(0xFFF0923F),            onPrimary = Color(0xFF2A1306),
    primaryContainer = Color(0xFF4A2E16),   onPrimaryContainer = Color(0xFFFFD8AE),
    secondary = Color(0xFFE0883C),          onSecondary = Color(0xFF231202),
    secondaryContainer = Color(0xFF3C2A17), onSecondaryContainer = Color(0xFFFFD8AE),
    tertiary = Color(0xFFE6C079),           onTertiary = Color(0xFF3A2A0F),
    tertiaryContainer = Color(0xFF4A3A1A),  onTertiaryContainer = Color(0xFFF6E7C9),
    background = Color(0xFF241309),         onBackground = Color(0xFFF6EADA),
    surface = Color(0xFF2A1A0F),            onSurface = Color(0xFFF6EADA),
    surfaceVariant = Color(0xFF33200F),     onSurfaceVariant = Color(0xFFC2A98F),
    outline = Color(0xFF4A331F),            outlineVariant = Color(0xFF3A2418),
    error = Color(0xFFE57368),              onError = Color(0xFF2A0A07),
    errorContainer = Color(0xFF4A2420),     onErrorContainer = Color(0xFFFFD9D3),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun ExtremeCoffeeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) CoffeeDark else CoffeeLight,
        shapes = AppShapes,
        typography = AppType,
        content = content
    )
}
