package studio.anatomy.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import studio.anatomy.app.R

data class AnatomyColors(
    val ink: Color = Color(0xFF060A0E),
    val inkDeep: Color = Color(0xFF04070A),
    val surface: Color = Color(0xFF0B1218),
    val surfaceElevated: Color = Color(0xFF101A21),
    val surfaceHigh: Color = Color(0xFF15222B),
    val line: Color = Color(0xFF20313A),
    val lineSoft: Color = Color(0xFF17242C),
    val accent: Color = Color(0xFF6EF2DF),
    val accentDim: Color = Color(0xFF2B7C76),
    val accentDeep: Color = Color(0xFF123B3A),
    val amber: Color = Color(0xFFF2B25C),
    val text: Color = Color(0xFFE9F1EF),
    val textSecondary: Color = Color(0xFF93A7A9),
    val textTertiary: Color = Color(0xFF5F7377),
    val danger: Color = Color(0xFFFF7A6B),
    val paper: Color = Color(0xFFEDF2ED),
    val onAccent: Color = Color(0xFF03201E),
    val isDark: Boolean = true,
    // The 3D viewports stay dark in both appearances; these colors never change.
    val stageInk: Color = Color(0xFF060A0E),
    val stageSurface: Color = Color(0xFF0D161B),
    val stageLine: Color = Color(0xFF26363D),
    val stageText: Color = Color(0xFFE9F1EF),
    val stageTextSecondary: Color = Color(0xFF93A7A9),
    val stageAccent: Color = Color(0xFF6EF2DF),
)

val LightAnatomyColors = AnatomyColors(
    ink = Color(0xFFF3EFE6),
    inkDeep = Color(0xFFE9E3D5),
    surface = Color(0xFFFFFDF8),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFEDE8DC),
    line = Color(0xFFD3CCBC),
    lineSoft = Color(0xFFE6E1D5),
    accent = Color(0xFF0B6B61),
    accentDim = Color(0xFF5F9F97),
    accentDeep = Color(0xFF083E3A),
    amber = Color(0xFFB5681C),
    text = Color(0xFF13211F),
    textSecondary = Color(0xFF4D5C58),
    textTertiary = Color(0xFF75827D),
    danger = Color(0xFFB84330),
    paper = Color(0xFFF4F1E8),
    onAccent = Color(0xFFFFFFFF),
    isDark = false,
)

val DarkAnatomyColors = AnatomyColors()

val LocalAnatomyColors = staticCompositionLocalOf { LightAnatomyColors }

val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Light),
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold),
)

val Manrope = FontFamily(
    Font(R.font.manrope, FontWeight.Light),
    Font(R.font.manrope, FontWeight.Normal),
    Font(R.font.manrope, FontWeight.Medium),
    Font(R.font.manrope, FontWeight.SemiBold),
    Font(R.font.manrope, FontWeight.Bold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Light),
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono, FontWeight.Bold),
)

object AnatomyType {
    val display = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 46.sp,
        lineHeight = 46.sp,
        letterSpacing = (-1.6).sp,
    )
    val displaySmall = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.9).sp,
    )
    val headline = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp,
    )
    val title = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp,
    )
    val body = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 15.5.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.05.sp,
    )
    val bodySmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.05.sp,
    )
    val mono = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 1.1.sp,
    )
    val monoLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        letterSpacing = 1.0.sp,
    )
    val monoMicro = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.8.sp,
    )
    val numeric = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Light,
        fontSize = 28.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.6).sp,
    )
}

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF6EF2DF),
    onPrimary = Color(0xFF03201E),
    primaryContainer = Color(0xFF113B39),
    onPrimaryContainer = Color(0xFFC9FFF7),
    secondary = Color(0xFFF2B25C),
    onSecondary = Color(0xFF2A1A06),
    secondaryContainer = Color(0xFF3B2A12),
    onSecondaryContainer = Color(0xFFFFE2B7),
    tertiary = Color(0xFF91A8FF),
    onTertiary = Color(0xFF0A1535),
    background = Color(0xFF060A0E),
    onBackground = Color(0xFFE9F1EF),
    surface = Color(0xFF0B1218),
    onSurface = Color(0xFFE9F1EF),
    surfaceVariant = Color(0xFF142028),
    onSurfaceVariant = Color(0xFF93A7A9),
    outline = Color(0xFF2B3E47),
    outlineVariant = Color(0xFF1B2A32),
    error = Color(0xFFFF7A6B),
    onError = Color(0xFF2B0804),
    inverseSurface = Color(0xFFE9F1EF),
    inverseOnSurface = Color(0xFF0B1218),
    scrim = Color(0xFF000000),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0B6B61),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1EDE7),
    onPrimaryContainer = Color(0xFF073B36),
    secondary = Color(0xFFB5681C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5E3CC),
    onSecondaryContainer = Color(0xFF4A2B07),
    tertiary = Color(0xFF3F5B8C),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF3EFE6),
    onBackground = Color(0xFF13211F),
    surface = Color(0xFFFFFDF8),
    onSurface = Color(0xFF13211F),
    surfaceVariant = Color(0xFFEDE8DC),
    onSurfaceVariant = Color(0xFF4D5C58),
    outline = Color(0xFFD3CCBC),
    outlineVariant = Color(0xFFE6E1D5),
    error = Color(0xFFB84330),
    onError = Color(0xFFFFFFFF),
    inverseSurface = Color(0xFF13211F),
    inverseOnSurface = Color(0xFFF3EFE6),
    scrim = Color(0xFF000000),
)

@Composable
fun AnatomyTheme(
    darkTheme: Boolean = false,
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkAnatomyColors else LightAnatomyColors
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalAnatomyColors provides colors,
        LocalDensity provides Density(density.density, density.fontScale * textScale),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = Typography(
                displayLarge = AnatomyType.display,
                displayMedium = AnatomyType.displaySmall,
                headlineLarge = AnatomyType.headline,
                titleLarge = AnatomyType.title,
                titleMedium = AnatomyType.title.copy(fontSize = 16.sp),
                bodyLarge = AnatomyType.body,
                bodyMedium = AnatomyType.body,
                bodySmall = AnatomyType.bodySmall,
                labelLarge = AnatomyType.monoLarge,
                labelMedium = AnatomyType.mono,
                labelSmall = AnatomyType.monoMicro,
            ),
            content = content,
        )
    }
}
