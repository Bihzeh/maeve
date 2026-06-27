package gg.maeve.launcher.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gg.maeve.launcher.ui.LauncherViewModel
import gg.maeve.launcher.ui.components.MaeveButton
import gg.maeve.launcher.ui.components.MaeveCard
import gg.maeve.launcher.ui.components.MaeveProgress
import gg.maeve.launcher.ui.components.PillKind
import gg.maeve.launcher.ui.components.RotatableSkin
import gg.maeve.launcher.ui.components.SectionLabel
import gg.maeve.launcher.ui.components.Spinner
import gg.maeve.launcher.ui.components.StatusPill
import gg.maeve.launcher.ui.components.SymIcon
import gg.maeve.launcher.ui.theme.Maeve
import gg.maeve.launcher.ui.theme.MaeveFonts
import gg.maeve.launcher.update.UpdateState
import gg.maeve.shared.Versions

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        UpdateBanner(vm)
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Hero(vm, Modifier.weight(1.9f).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                WhatsNew()
                ComingSoonSlot(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Hero(vm: LauncherViewModel, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).border(1.dp, Maeve.border, RoundedCornerShape(14.dp))) {
        // Backdrop fills the whole card, cropped as needed.
        Image(painterResource("hero/mc-bg.png"), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        // 50% black overlay to darken the background.
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
        // Player skin (drag to spin), big and knee-cropped, behind the launch bar.
        RotatableSkin(frameCount = 24, modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.74f))
        // Extra bottom gradient so the launch bar stays legible.
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(180.dp)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))))
        // Top: two promoted-server ad cards + the username.
        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdCard("Hypixel Network", "mc.hypixel.net", "42,318 online", Modifier.weight(1f))
                AdCard("CubeCraft Games", "play.cubecraft.net", "11,904 online", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text((vm.session?.username ?: "Player").uppercase(), fontFamily = MaeveFonts.Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 3.sp, color = Color.White)
                Spacer(Modifier.width(8.dp))
                SymIcon("edit", 16.dp, Color.White.copy(alpha = 0.7f))
            }
        }
        // Bottom: split launch button (or download progress while playing).
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
            if (vm.playing) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Maeve.s1.copy(alpha = 0.92f)).border(1.dp, Maeve.border, RoundedCornerShape(14.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Spinner(16)
                        Text(vm.playStatus.ifEmpty { "Preparing…" }, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                    }
                    MaeveProgress(vm.playFraction)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val err = vm.playError; val exit = vm.playExit
                    when {
                        err != null -> StatusPill(err.take(48), PillKind.Failed)
                        exit != null -> StatusPill(exit, PillKind.Neutral)
                        else -> {}
                    }
                    LaunchBar(vm)
                }
            }
        }
    }
}

/** Promoted-server "ad" card (frame 03). Placeholder content; Join is a follow-up. */
@Composable
private fun AdCard(name: String, address: String, online: String, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.42f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Maeve.accentSubtle), contentAlignment = Alignment.Center) {
            Text(name.take(1), fontFamily = MaeveFonts.Display, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Maeve.accentHi)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                SymIcon("verified", 12.dp, Maeve.ember)
                Text("PROMOTED SERVER", color = Maeve.ember, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
            }
            Text(name, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(online, color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(
            Modifier.clip(RoundedCornerShape(8.dp)).background(Maeve.accent.copy(alpha = 0.18f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { /* join — follow-up */ }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            SymIcon("bolt", 15.dp, Maeve.accentHi)
            Text("Join", color = Maeve.accentHi, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/** The split launch button: LAUNCH action + a version/profile dropdown affordance. */
@Composable
private fun LaunchBar(vm: LauncherViewModel) {
    val enabled = vm.session != null && !vm.playing
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().height(72.dp)
            .then(if (enabled) Modifier.shadow(16.dp, shape, ambientColor = Maeve.accent, spotColor = Maeve.accent) else Modifier)
            .clip(shape).background(Brush.horizontalGradient(listOf(Maeve.accentHi, Maeve.accent))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).fillMaxHeight()
                .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.play() }
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SymIcon("play_arrow", 30.dp, Color.White)
            Column {
                Text("LAUNCH", fontFamily = MaeveFonts.Display, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = 1.sp, color = Color.White)
                Text("Maeve Client ${Versions.MINECRAFT}", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelMedium)
            }
        }
        Box(Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.28f)))
        Box(
            Modifier.fillMaxHeight()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { /* profile/version dropdown — follow-up */ }
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center,
        ) { SymIcon("expand_more", 26.dp, Color.White) }
    }
}

@Composable
private fun WhatsNew() {
    MaeveCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("What's new")
            NewsLine("speed", "Sodium + Lithium bundled")
            NewsLine("dashboard", "FPS / coords / keystroke HUD")
            NewsLine("tune", "Fully customizable in-game HUD")
            Spacer(Modifier.height(2.dp))
            Text("Maeve ${buildVersion()}", color = Maeve.text3, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun NewsLine(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SymIcon(icon, 18.dp, Maeve.accentHi)
        Text(text, color = Maeve.text2, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ComingSoonSlot(modifier: Modifier) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Maeve.s1).border(1.dp, Maeve.border, RoundedCornerShape(14.dp)).padding(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Friends")
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SymIcon("group", 22.dp, Maeve.text3)
                Text("Parties & social — coming soon", color = Maeve.text3, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun UpdateBanner(vm: LauncherViewModel) {
    when (val u = vm.update) {
        is UpdateState.Available -> Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Maeve.ember.copy(alpha = 0.10f))
                .border(1.dp, Maeve.ember.copy(alpha = 0.4f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SymIcon("download", 18.dp, Maeve.ember)
            Text("Update available — ${u.info.tag}", color = Maeve.ember, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            MaeveButton("Update now", { vm.applyUpdate() })
        }
        is UpdateState.Working -> Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Maeve.s1)
                .border(1.dp, Maeve.border, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spinner(16); Text(u.status, color = Maeve.text2, style = MaterialTheme.typography.bodyMedium)
        }
        else -> {}
    }
}

private fun buildVersion(): String = gg.maeve.launcher.update.BuildInfo.version
