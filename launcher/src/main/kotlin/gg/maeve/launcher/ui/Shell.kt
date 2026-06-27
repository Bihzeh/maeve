package gg.maeve.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gg.maeve.launcher.ui.chrome.WindowChrome
import gg.maeve.launcher.ui.components.MaeveIconButton
import gg.maeve.launcher.ui.components.MaeveMark
import gg.maeve.launcher.ui.components.SkinAvatar
import gg.maeve.launcher.ui.components.SymIcon
import gg.maeve.launcher.ui.screens.ComingSoonScreen
import gg.maeve.launcher.ui.screens.HomeScreen
import gg.maeve.launcher.ui.screens.ModsScreen
import gg.maeve.launcher.ui.screens.SettingsScreen
import gg.maeve.launcher.ui.screens.SignInScreen
import gg.maeve.launcher.ui.theme.Maeve
import gg.maeve.launcher.ui.theme.MaeveFonts

@Composable
fun Shell(vm: LauncherViewModel, chrome: WindowChrome = WindowChrome.Preview) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        chrome.dragWrapper { TitleBar(chrome) }
        if (vm.session == null) {
            Box(Modifier.weight(1f).fillMaxWidth()) { SignInScreen(vm) }
        } else {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                IconRail(vm)
                Box(Modifier.width(1.dp).fillMaxHeight().background(Maeve.s2))
                Box(Modifier.weight(1f).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    when (vm.screen) {
                        Screen.MODS -> ModsScreen(vm)
                        Screen.COSMETICS -> ComingSoonScreen(
                            "auto_awesome", "Cosmetics",
                            "Cloaks, wings, hats and pets — ownership-first, never a pushy store. Coming in a later update.",
                        )
                        Screen.FRIENDS -> ComingSoonScreen(
                            "group", "Friends & Parties",
                            "Friends list, presence and parties to play together. Landing in a future update.",
                        )
                        Screen.SETTINGS -> SettingsScreen(vm)
                        else -> HomeScreen(vm)
                    }
                }
            }
        }
    }
}

@Composable
private fun TitleBar(chrome: WindowChrome) {
    Row(
        Modifier.fillMaxWidth().height(44.dp).background(Maeve.bg2).padding(start = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(24.dp).clip(RoundedCornerShape(7.dp))
                .background(Brush.linearGradient(listOf(Maeve.accent, Maeve.accentLo))),
            contentAlignment = Alignment.Center,
        ) { MaeveMark(size = 16.dp, color = Color.White, gem = Color.White) }
        Spacer(Modifier.width(10.dp))
        Text(
            "MAEVE", fontFamily = MaeveFonts.Display, fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.weight(1f))
        MaeveIconButton("remove", chrome.onMinimize, iconSize = 18.dp)
        MaeveIconButton("crop_square", chrome.onToggleMaximize, iconSize = 14.dp)
        MaeveIconButton("close", chrome.onClose, danger = true, iconSize = 18.dp)
    }
}

@Composable
private fun IconRail(vm: LauncherViewModel) {
    Column(
        Modifier.width(76.dp).fillMaxHeight().background(Maeve.bg2).padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RailItem("stadia_controller", vm.screen == Screen.HOME) { vm.screen = Screen.HOME }
            RailItem("extension", vm.screen == Screen.MODS) { vm.screen = Screen.MODS }
            RailItem("auto_awesome", vm.screen == Screen.COSMETICS) { vm.screen = Screen.COSMETICS }
            RailItem("group", vm.screen == Screen.FRIENDS) { vm.screen = Screen.FRIENDS }
        }
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RailItem("settings", vm.screen == Screen.SETTINGS) { vm.screen = Screen.SETTINGS }
            SkinAvatar(sizeDp = 38.dp, online = vm.session != null)
        }
    }
}

@Composable
private fun RailItem(icon: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
        if (selected) {
            Box(Modifier.align(Alignment.CenterStart).width(3.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(Maeve.accent))
        }
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                .background(if (selected) Maeve.accentSubtle else if (hovered) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { SymIcon(icon, 24.dp, if (selected) Maeve.accentHi else Maeve.text3) }
    }
}
