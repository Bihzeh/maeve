package gg.maeve.launcher.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

/**
 * Maeve's unified type family: **Poppins** (OFL) across the whole launcher and the in-game HUD,
 * with hierarchy expressed via weight + size (see the Material3 Typography in MaeveTheme).
 * Geist Mono is kept for codes / numerics.
 */
object MaeveFonts {
    val Poppins: FontFamily = FontFamily(
        Font("fonts/Poppins-Regular.ttf", FontWeight.Normal),
        Font("fonts/Poppins-Medium.ttf", FontWeight.Medium),
        Font("fonts/Poppins-SemiBold.ttf", FontWeight.SemiBold),
        Font("fonts/Poppins-Bold.ttf", FontWeight.Bold),
    )
    val Mono: FontFamily = FontFamily(
        Font("fonts/GeistMono.ttf", FontWeight.Normal),
        Font("fonts/GeistMono.ttf", FontWeight.Medium),
    )
}
