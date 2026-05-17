package com.ksu.miuix.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ksu.miuix.shell.KsuStatus
import com.ksu.miuix.shell.Shell

private val ItemPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
private val IconSize = 36.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(paddingValues: PaddingValues, onAboutClick: () -> Unit) {
    var ksuStatus by remember { mutableStateOf<KsuStatus>(KsuStatus.Loading) }
    var kernelVersion by remember { mutableStateOf("") }
    var androidVersion by remember { mutableStateOf("") }
    var deviceModel by remember { mutableStateOf("") }
    var showRebootDialog by remember { mutableIntStateOf(0) }
    var showClearCacheDialog by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        kernelVersion = Shell.su("uname -r").stdout.ifEmpty { "未知" }
        androidVersion = Shell.su("getprop ro.build.version.release").stdout.ifEmpty { "未知" }
        deviceModel = Shell.su("getprop ro.product.model").stdout.ifEmpty { "未知" }

        ksuStatus = Shell.detectKsu()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "状态",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            when (val status = ksuStatus) {
                is KsuStatus.Active -> StatusRow(
                    icon = Icons.Default.Security,
                    title = "KernelSU",
                    summary = status.info,
                    isActive = true,
                )
                is KsuStatus.Inactive -> StatusRow(
                    icon = Icons.Default.Security,
                    title = "KernelSU",
                    summary = if (status.kernelInfo.isNotEmpty()) status.kernelInfo else "无法检测内核信息",
                    isActive = false,
                )
                is KsuStatus.Loading -> StatusRow(
                    icon = Icons.Default.Security,
                    title = "KernelSU",
                    summary = "正在检测...",
                    isLoading = true,
                )
            }
        }

        Text(
            text = "设备信息",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                InfoRow(Icons.Default.Memory, "内核版本", kernelVersion)
                InfoRow(Icons.Default.PhoneAndroid, "Android 版本", androidVersion)
                InfoRow(Icons.Default.PhoneAndroid, "设备型号", deviceModel)
            }
        }

        Text(
            text = "快捷操作",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                ActionRow(Icons.Default.RestartAlt, "重启设备", "执行 reboot 重启系统") { showRebootDialog = 1 }
                ActionRow(Icons.Default.Sync, "重启 SystemUI", "刷新界面而不完全重启") { Shell.exec("pkill -f com.android.systemui") }
                ActionRow(Icons.Default.CleaningServices, "清除应用缓存", "清理 /data/data 下缓存目录") { showClearCacheDialog = 1 }
            }
        }
    }

    if (showRebootDialog == 1) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = 0 },
            title = { Text("确认重启") },
            text = { Text("确定要立即重启设备吗？所有未保存的数据将会丢失。") },
            confirmButton = {
                Button(onClick = { Shell.exec("reboot"); showRebootDialog = 0 }) { Text("重启") }
            },
            dismissButton = {
                TextButton(onClick = { showRebootDialog = 0 }) { Text("取消") }
            },
        )
    }

    if (showClearCacheDialog == 1) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = 0 },
            title = { Text("确认清除缓存") },
            text = { Text("确定要清除所有应用的缓存数据吗？部分应用可能需要重新加载数据。") },
            confirmButton = {
                Button(onClick = { Shell.exec("rm -rf /data/data/*/cache/* 2>/dev/null"); showClearCacheDialog = 0 }) { Text("清除") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = 0 }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    title: String,
    summary: String,
    isActive: Boolean = false,
    isLoading: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(ItemPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(IconSize))
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isLoading) {
            Text(text = "--", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Icon(
                imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Security,
                contentDescription = if (isActive) "已激活" else "未激活",
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, summary: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(ItemPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(IconSize))
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = summary.ifEmpty { "加载中..." }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(ItemPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(IconSize))
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
