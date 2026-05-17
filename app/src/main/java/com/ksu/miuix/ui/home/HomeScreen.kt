package com.ksu.miuix.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ksu.miuix.shell.Shell
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync

private val ItemPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
private val IconSize = 36.dp

@Composable
fun HomeScreen(paddingValues: PaddingValues, onAboutClick: () -> Unit) {
    var ksuStatus by remember { mutableStateOf<KsuStatus>(KsuStatus.Loading) }
    var kernelVersion by remember { mutableStateOf("") }
    var androidVersion by remember { mutableStateOf("") }
    var deviceModel by remember { mutableStateOf("") }
    var showRebootDialog by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        kernelVersion = Shell.su("uname -r").stdout.ifEmpty { "未知" }
        androidVersion = Shell.su("getprop ro.build.version.release").stdout.ifEmpty { "未知" }
        deviceModel = Shell.su("getprop ro.product.model").stdout.ifEmpty { "未知" }

        ksuStatus = if (kernelVersion.contains("KSU")) {
            KsuStatus.Active(kernelVersion)
        } else {
            KsuStatus.Inactive(kernelVersion)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        SmallTitle(text = "状态")

        Card {
            when (val status = ksuStatus) {
                is KsuStatus.Active -> StatusRow(
                    icon = Icons.Default.Security,
                    title = "KernelSU",
                    summary = status.kernelInfo,
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

        SmallTitle(text = "设备信息")

        Card {
            InfoRow(Icons.Default.Memory, "内核版本", kernelVersion)
            InfoRow(Icons.Default.PhoneAndroid, "Android 版本", androidVersion)
            InfoRow(Icons.Default.PhoneAndroid, "设备型号", deviceModel)
        }

        SmallTitle(text = "快捷操作")

        Card {
            ActionRow(Icons.Default.RestartAlt, "重启设备", "执行 reboot 重启系统") { showRebootDialog = 1 }
            ActionRow(Icons.Default.Sync, "重启 SystemUI", "刷新界面而不完全重启") { Shell.exec("pkill -f com.android.systemui") }
            ActionRow(Icons.Default.CleaningServices, "清除应用缓存", "清理 /data/data 下缓存目录") { Shell.exec("rm -rf /data/data/*/cache/* 2>/dev/null") }
        }
    }

    OverlayDialog(
        show = showRebootDialog == 1,
        title = "确认重启",
        summary = "确定要立即重启设备吗？所有未保存的数据将会丢失。",
        onDismissRequest = { showRebootDialog = 0 },
    ) {
        TextButton(text = "取消", onClick = { showRebootDialog = 0 })
        Button(onClick = { Shell.exec("reboot"); showRebootDialog = 0 }) { Text(text = "重启") }
    }
}

@Composable
private fun StatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            Text(text = title, style = MiuixTheme.textStyles.body1)
            Text(text = summary, style = MiuixTheme.textStyles.body2)
        }
        if (isLoading) {
            Text(text = "--", style = MiuixTheme.textStyles.body2)
        } else {
            Switch(checked = isActive, onCheckedChange = null, enabled = false)
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, summary: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(ItemPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(IconSize))
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(text = title, style = MiuixTheme.textStyles.body1)
            Text(text = summary.ifEmpty { "加载中..." }, style = MiuixTheme.textStyles.body2)
        }
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    Card(cornerRadius = 0.dp, insideMargin = PaddingValues(0.dp), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(ItemPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(IconSize))
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(text = title, style = MiuixTheme.textStyles.body1)
                Text(text = summary, style = MiuixTheme.textStyles.body2)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.disabledOnSurface,
            )
        }
    }
}

private sealed class KsuStatus {
    data object Loading : KsuStatus()
    data class Active(val kernelInfo: String) : KsuStatus()
    data class Inactive(val kernelInfo: String) : KsuStatus()
}
