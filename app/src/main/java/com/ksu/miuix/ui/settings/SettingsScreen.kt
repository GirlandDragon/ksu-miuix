package com.ksu.miuix.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.net.URL

private const val APP_VERSION_URL = "https://raw.githubusercontent.com/GirlandDragon/ksu-miuix/main/app_version"
private const val CURRENT_VERSION = "1.0.0"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(paddingValues: PaddingValues) {
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<UpdateResult?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            text = "关于",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(ItemPadding)) {
                InfoLine("版本", CURRENT_VERSION)
                InfoLine("设计语言", "Material 3 Expressive")
            }
        }

        Text(
            text = "更新",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (!checking) {
                    checking = true
                    result = null
                    scope.launch {
                        result = checkUpdate()
                        checking = false
                        showResultDialog = true
                    }
                }
            },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(ItemPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(IconSize))
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(text = "检查更新", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = when {
                            checking -> "正在检查..."
                            result != null -> when (result) {
                                is UpdateResult.NewVersion -> "发现新版本 ${result.version}"
                                is UpdateResult.Latest -> "已是最新版本"
                                is UpdateResult.Error -> "检查失败"
                            }
                            else -> "点击检查 GitHub 仓库是否有新版本"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else if (result is UpdateResult.NewVersion) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        if (showResultDialog && result != null) {
            AlertDialog(
                onDismissRequest = { showResultDialog = false },
                title = { Text("检查更新") },
                text = {
                    Text(
                        when (val r = result!!) {
                            is UpdateResult.NewVersion -> "发现新版本 ${r.version}\n\n当前版本: $CURRENT_VERSION\n最新版本: ${r.version}"
                            is UpdateResult.Latest -> "当前版本 $CURRENT_VERSION 已是最新版本。"
                            is UpdateResult.Error -> "检查更新失败:\n\n${r.message}"
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showResultDialog = false }) {
                        Text("确定")
                    }
                },
            )
        }
    }
}

private suspend fun checkUpdate(): UpdateResult = try {
    val remoteVersion = URL(APP_VERSION_URL).readText().trim()
    if (remoteVersion.isBlank()) {
        UpdateResult.Error("服务器返回空内容")
    } else if (remoteVersion != CURRENT_VERSION) {
        UpdateResult.NewVersion(remoteVersion)
    } else {
        UpdateResult.Latest
    }
} catch (e: Exception) {
    UpdateResult.Error(e.message ?: "未知错误")
}

private sealed class UpdateResult {
    data class NewVersion(val version: String) : UpdateResult()
    data object Latest : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = "$label: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

private val ItemPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
private val IconSize = 36.dp
