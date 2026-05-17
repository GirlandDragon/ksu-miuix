package com.ksu.miuix.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ksu.miuix.shell.Shell

private data class TerminalLine(val text: String, val type: LineType = LineType.Output)

private enum class LineType { Output, Error, Input, Info }

private val QuickCommands = listOf(
    "whoami" to "当前用户",
    "id" to "用户ID",
    "df -h" to "磁盘空间",
    "free -m" to "内存信息",
    "getprop ro.product.model" to "设备型号",
    "cat /proc/version" to "内核信息",
    "ls /data/adb/modules/" to "模块列表",
    "cat /data/adb/ksu.json" to "KSU 配置",
)

@Composable
fun TerminalScreen(paddingValues: PaddingValues) {
    val lines = remember { mutableStateListOf<TerminalLine>() }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        lines.add(TerminalLine("KernelSU Terminal — Root Shell", LineType.Info))
        lines.add(TerminalLine("输入命令并按回车执行 (root 权限)", LineType.Info))
        lines.add(TerminalLine(""))
    }

    LaunchedEffect(lines.size) {
        if (lines.size > 0) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A2E))
                    .padding(12.dp),
                reverseLayout = false,
            ) {
                items(lines) { line ->
                    Text(
                        text = line.text.ifEmpty { " " },
                        color = when (line.type) {
                            LineType.Error -> Color(0xFFF12522)
                            LineType.Input -> Color(0xFF4788FF)
                            LineType.Info -> Color(0xFFFFB84D)
                            else -> Color(0xFFE0E0E0)
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row {
                    Text(
                        text = "# ",
                        color = Color(0xFF4788FF),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = if (inputText.isEmpty()) "输入 shell 命令..." else inputText,
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (inputText.isEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QuickCommands.forEach { (cmd, label) ->
                AssistChip(
                    onClick = { inputText = cmd },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }

        Text(
            text = "使用说明",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(ItemPaddingInner), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                HelpItem("Root Shell", "所有命令均以 root 身份执行")
                HelpItem("快捷命令", "点击上方标签快速填入")
                HelpItem("输出", "上方终端区域显示命令结果")
            }
        }
    }
}

@Composable
private fun HelpItem(title: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val ItemPaddingInner = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
