package com.ksu.miuix.ui.theme

import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.rememberMiuixThemeController

@Composable
fun AppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val controller = rememberMiuixThemeController(darkTheme = darkTheme)
    MiuixTheme(controller = controller, content = content)
}
