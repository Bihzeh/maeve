package gg.maeve.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gg.maeve.launcher.ui.components.StatusPill
import gg.maeve.launcher.ui.components.PillKind
import gg.maeve.launcher.ui.components.SymIcon
import gg.maeve.launcher.ui.theme.Maeve

/** Reserved-space placeholder for roadmap screens (Cosmetics P3, Friends P4). */
@Composable
fun ComingSoonScreen(icon: String, title: String, blurb: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.width(360.dp),
        ) {
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(Maeve.accentSubtle),
                contentAlignment = Alignment.Center,
            ) { SymIcon(icon, 36.dp, Maeve.accentHi) }
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            StatusPill("Coming soon", PillKind.UpdateAvailable, showDot = false)
            Text(blurb, style = MaterialTheme.typography.bodyMedium, color = Maeve.text2, textAlign = TextAlign.Center)
        }
    }
}
