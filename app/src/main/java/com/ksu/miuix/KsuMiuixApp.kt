package com.ksu.miuix

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ksu.miuix.ui.home.HomeScreen
import com.ksu.miuix.ui.packages.PackagesScreen
import com.ksu.miuix.ui.terminal.TerminalScreen
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Console

@Composable
fun KsuMiuixApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAboutDialog by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = when (selectedTab) {
                    0 -> "KernelSU"
                    1 -> "应用管理"
                    else -> "终端"
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = Icons.Default.Home,
                    label = "首页",
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = Icons.Default.Apps,
                    label = "应用",
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = Icons.Default.Console,
                    label = "终端",
                )
            }
        },
    ) { paddingValues ->
        when (selectedTab) {
            0 -> HomeScreen(paddingValues, onAboutClick = { showAboutDialog = 1 })
            1 -> PackagesScreen(paddingValues)
            2 -> TerminalScreen(paddingValues)
        }
    }

    OverlayDialog(
        show = showAboutDialog == 1,
        title = "关于",
        summary = "KernelSU Miuix\n基于 Miuix 设计语言构建\n\n使用 compose-miuix-ui/miuix 库",
        onDismissRequest = { showAboutDialog = 0 },
    ) {
        Card {
            Text(text = "版本: 1.0.0")
        }
    }
}
