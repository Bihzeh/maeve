package gg.maeve.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gg.maeve.launcher.ui.LauncherViewModel
import gg.maeve.launcher.ui.components.ButtonVariant
import gg.maeve.launcher.ui.components.MaeveButton
import gg.maeve.launcher.ui.components.MaeveMark
import gg.maeve.launcher.ui.components.Spinner
import gg.maeve.launcher.ui.theme.Maeve
import gg.maeve.launcher.ui.theme.MaeveFonts
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun SignInScreen(vm: LauncherViewModel) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.width(380.dp).padding(24.dp)) {
            MaeveMark(size = 56.dp)
            Text("MAEVE", fontFamily = MaeveFonts.Display, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = 4.sp, color = MaterialTheme.colorScheme.onBackground)

            val prompt = vm.prompt
            when {
                prompt != null -> {
                    var copied by remember { mutableStateOf(false) }
                    LaunchedEffect(copied) { if (copied) { delay(1600); copied = false } }
                    Text("Go to ${prompt.verificationUri} and enter this code", color = Maeve.text2, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                    Box(Modifier.fillMaxWidth().border(1.dp, Maeve.border, RoundedCornerShape(12.dp)).background(Maeve.elevated, RoundedCornerShape(12.dp)).padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                        Text(prompt.userCode, fontFamily = MaeveFonts.Mono, fontSize = 26.sp, letterSpacing = 4.sp, color = MaterialTheme.colorScheme.onBackground)
                    }
                    AnimatedVisibility(visible = copied, enter = fadeIn() + slideInVertically { -it / 2 }, exit = fadeOut()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.background(Maeve.success.copy(alpha = 0.16f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text("\u2713", color = Maeve.success, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Text("Copied to clipboard", color = Maeve.success, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MaeveButton("Open link", { open(prompt.verificationUri) }, Modifier.weight(1f), fillWidth = true)
                        MaeveButton(if (copied) "Copied!" else "Copy code", { copy(prompt.userCode); copied = true }, Modifier.weight(1f), variant = ButtonVariant.Secondary, fillWidth = true)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spinner(16); Text("Waiting for you to sign in\u2026", color = Maeve.text3, style = MaterialTheme.typography.bodySmall)
                    }
                    MaeveButton("Cancel", { vm.cancelSignIn() }, variant = ButtonVariant.Ghost, fillWidth = true)
                }
                vm.signInError != null -> {
                    Text("Couldn't sign in", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text(vm.signInError!!, color = Maeve.text2, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                    MaeveButton("Try again", { vm.signIn() }, fillWidth = true)
                }
                vm.signInBusy -> {
                    Spinner(28); Text("Confirming with Microsoft…", color = Maeve.text2)
                }
                else -> {
                    Text("Sign in to play", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    MaeveButton("Sign in with Microsoft", { vm.signIn() }, fillWidth = true)
                    Text("Official Microsoft login · we never see your password", color = Maeve.text3, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
                    if (vm.devMode) {
                        MaeveButton("Continue offline (dev)", { vm.continueOffline() }, variant = ButtonVariant.Ghost, fillWidth = true)
                    }
                }
            }
        }
    }
}

private fun open(uri: String) = runCatching {
    val d = java.awt.Desktop.getDesktop(); d.browse(java.net.URI(uri))
}

private fun copy(text: String) = runCatching {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}
