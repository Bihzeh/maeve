package gg.snell.launcher

import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import gg.snell.launcher.ui.LauncherViewModel
import gg.snell.launcher.ui.LogWindow
import gg.snell.launcher.ui.Shell
import gg.snell.launcher.ui.chrome.WindowChrome
import gg.snell.launcher.ui.components.Brand
import gg.snell.launcher.ui.theme.SnellTheme
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.Toolkit

fun main() = application {
    val state = rememberWindowState(width = 1280.dp, height = 800.dp, position = WindowPosition(Alignment.Center))
    val scope = rememberCoroutineScope()
    val vm = remember { LauncherViewModel(scope) }
    Window(
        onCloseRequest = ::exitApplication,
        state = state,
        undecorated = true,   // custom Obsidian chrome; edge-resize still works (UndecoratedWindowResizer)
        resizable = true,
        title = "Snell",
        icon = painterResource(Brand.APP_ICON),
    ) {
        LaunchedEffect(Unit) { window.minimumSize = Dimension(1000, 640) }
        val chrome = remember {
            WindowChrome(
                onMinimize = { state.isMinimized = true },
                onToggleMaximize = {
                    if (state.placement == WindowPlacement.Maximized) {
                        state.placement = WindowPlacement.Floating
                    } else {
                        // The window is undecorated, so MAXIMIZED_BOTH would otherwise cover the OS
                        // taskbar. Constrain it to the current monitor's work area (bounds minus screen
                        // insets) so it maximizes to the taskbar like a normal app. Recomputed per toggle
                        // so it follows the window across monitors.
                        val gc = window.graphicsConfiguration
                        val b = gc.bounds
                        val ins = Toolkit.getDefaultToolkit().getScreenInsets(gc)
                        window.maximizedBounds = Rectangle(
                            b.x + ins.left, b.y + ins.top,
                            b.width - ins.left - ins.right, b.height - ins.top - ins.bottom,
                        )
                        state.placement = WindowPlacement.Maximized
                    }
                },
                onClose = { exitApplication() },
                dragWrapper = { content -> WindowDraggableArea { content() } },
            )
        }
        SnellTheme {
            LaunchedEffect(Unit) { vm.checkForUpdates() }
            Shell(vm, chrome)
        }
    }

    if (vm.showLogWindow && vm.logWindowOpen) {
        LogWindow(vm) { vm.logWindowOpen = false }
    }
}
