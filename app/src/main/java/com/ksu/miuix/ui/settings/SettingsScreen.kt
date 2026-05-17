package com.ksu.miuix.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL

private const val APP_VERSION_URL = "https://raw.githubusercontent.com/GirlandDragon/ksu-miuix/main/app_version"
private const val CURRENT_VERSION = "1.0.0"
private const val REQUEST_TIMEOUT_MS = 10_000L

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
                        text = when (val r = result) {
                            null -> "点击检查 GitHub 仓库是否有新版本"
                            is UpdateResult.NewVersion -> "发现新版本 ${r.version}"
                            is UpdateResult.Latest -> "已是最新版本"
                            is UpdateResult.Error -> "检查失败"
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

private suspend fun checkUpdate(): UpdateResult = withContext(Dispatchers.IO) {
    try {
        val rawBody = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
            fetchRemoteText(APP_VERSION_URL)
        } ?: return@withContext UpdateResult.Error("连接超时 (${REQUEST_TIMEOUT_MS / 1000}s)，请检查网络")

        val remoteVersion = rawBody.trim()
        if (remoteVersion.isBlank()) {
            return@withContext UpdateResult.Error("服务器返回空内容，请稍后重试")
        }

        val current = parseSemanticVersion(CURRENT_VERSION)
        val remote = parseSemanticVersion(remoteVersion)

        if (current == null || remote == null) {
            return@withContext if (remoteVersion != CURRENT_VERSION) {
                UpdateResult.NewVersion(remoteVersion)
            } else {
                UpdateResult.Latest
            }
        }

        if (remote > current) {
            UpdateResult.NewVersion(remoteVersion)
        } else {
            UpdateResult.Latest
        }
    } catch (e: java.net.SocketTimeoutException) {
        UpdateResult.Error("网络超时，请检查网络连接后重试")
    } catch (e: java.net.UnknownHostException) {
        UpdateResult.Error("无法解析域名，请检查网络或 DNS 设置")
    } catch (e: java.net.ConnectException) {
        UpdateResult.Error("无法连接服务器，请检查网络连接")
    } catch (e: javax.net.ssl.SSLException) {
        UpdateResult.Error("SSL 连接失败，请检查系统时间或网络环境")
    } catch (e: Exception) {
        UpdateResult.Error(e.message ?: "未知错误，请稍后重试")
    }
}

private fun fetchRemoteText(urlString: String): String {
    val url = URL(urlString)
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = REQUEST_TIMEOUT_MS.toInt()
    connection.readTimeout = REQUEST_TIMEOUT_MS.toInt()
    connection.setRequestProperty("Accept", "text/plain")
    connection.setRequestProperty("User-Agent", "KSU-MD3E/$CURRENT_VERSION")

    return try {
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}")
        }
        connection.inputStream.bufferedReader().readText().trim()
    } finally {
        connection.disconnect()
    }
}

data class SemanticVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int {
        val m = major.compareTo(other.major)
        if (m != 0) return m
        val n = minor.compareTo(other.minor)
        if (n != 0) return n
        return patch.compareTo(other.patch)
    }
}

private fun parseSemanticVersion(version: String): SemanticVersion? {
    val parts = version.trim().removePrefix("v").removePrefix("V").split(".")
    if (parts.isEmpty()) return null
    val major = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return null
    val minor = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
    val patch = parts.getOrNull(2)?.trim()?.toIntOrNull() ?: 0
    return SemanticVersion(major, minor, patch)
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
