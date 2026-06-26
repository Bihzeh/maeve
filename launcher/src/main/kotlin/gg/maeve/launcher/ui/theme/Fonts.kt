package gg.maeve.launcher.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

/**
 * Bundled fonts (OFL), matching the Maeve design system:
 *  - [Display] Space Grotesk — display, wordmark and headings.
 *  - [Sans] Manrope — titles (Bold) and body/UI (Medium).
 *  - [Mono] Geist Mono — codes / numerics.
 * All three are variable (wght axis); weights are requested via FontWeight and the desktop
 * loader selects/synthesizes from the single variable file.
 */
object MaeveFonts {
    val Display: FontFamily = FontFamily(
        Font("fonts/SpaceGrotesk.ttf", FontWeight.Light),
        Font("fonts/SpaceGrotesk.ttf", FontWeight.Normal),
        Font("fonts/SpaceGrotesk.ttf", FontWeight.Medium),
        Font("fonts/SpaceGrotesk.ttf", FontWeight.SemiBold),
        Font("fonts/SpaceGrotesk.ttf", FontWeight.Bold),
    )
    val Sans: FontFamily = FontFamily(
        Font("fonts/Manrope.ttf", FontWeight.Light),
        Font("fonts/Manrope.ttf", FontWeight.Normal),
        Font("fonts/Manrope.ttf", FontWeight.Medium),
        Font("fonts/Manrope.ttf", FontWeight.SemiBold),
        Font("fonts/Manrope.ttf", FontWeight.Bold),
        Font("fonts/Manrope.ttf", FontWeight.ExtraBold),
    )
    val Mono: FontFamily = FontFamily(
        Font("fonts/GeistMono.ttf", FontWeight.Normal),
        Font("fonts/GeistMono.ttf", FontWeight.Medium),
    )
}
