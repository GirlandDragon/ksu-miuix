package com.ksu.miuix

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ksu.miuix.ui.home.HomeScreen
import com.ksu.miuix.ui.packages.PackagesScreen
import com.ksu.miuix.ui.terminal.TerminalScreen

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KsuMiuixApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAboutDialog by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (selectedTab) {
                            0 -> "KernelSU"
                            1 -> "应用管理"
                            else -> "终端"
                        }
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("首页") },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                    label = { Text("应用") },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("终端") },
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

    if (showAboutDialog == 1) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = 0 },
            title = { Text("关于") },
            text = {
                Text(
                    "KernelSU MD3E\n基于 Material 3 Expressive 设计语言构建\n\n使用 androidx.compose.material3 库"
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showAboutDialog = 0 }) {
                    Text("确定")
                }
            },
        )
    }
}
