package com.ksu.miuix.ui.packages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import coil.compose.AsyncImage
import com.ksu.miuix.shell.PackageInfo
import com.ksu.miuix.shell.Shell
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.DeleteSweep

private val ItemPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)

@Composable
fun PackagesScreen(paddingValues: PaddingValues) {
    var packages by remember { mutableStateOf<List<PackageInfo>>(emptyList()) }
    var filteredPackages by remember { mutableStateOf<List<PackageInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableIntStateOf(0) }
    var selectedPackage by remember { mutableStateOf<PackageInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        val pkgNames = Shell.getPackages()
        packages = pkgNames.mapNotNull { name ->
            runCatching { Shell.getPackageInfo(name) }.getOrNull()
        }.sortedBy { it.appLabel.lowercase() }
        filteredPackages = packages
        isLoading = false
    }

    LaunchedEffect(searchQuery, filterMode) {
        filteredPackages = packages.filter { pkg ->
            val matchesSearch = searchQuery.isEmpty() ||
                pkg.packageName.contains(searchQuery, ignoreCase = true) ||
                pkg.appLabel.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filterMode) {
                0 -> true
                1 -> !pkg.isSystem
                else -> pkg.isSystem
            }
            matchesSearch && matchesFilter
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
        Card {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Default.Apps, contentDescription = null, tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                Text(
                    text = if (searchQuery.isEmpty()) "搜索应用..." else searchQuery,
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                    style = MiuixTheme.textStyles.body1,
                    color = if (searchQuery.isEmpty()) MiuixTheme.colorScheme.disabledOnSurface else MiuixTheme.colorScheme.onSurface,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            FilterChip(label = "全部", selected = filterMode == 0, onClick = { filterMode = 0 })
            FilterChip(label = "用户", selected = filterMode == 1, onClick = { filterMode = 1 })
            FilterChip(label = "系统", selected = filterMode == 2, onClick = { filterMode = 2 })
        }

        SmallTitle(text = "应用列表 (${filteredPackages.size})")

        if (isLoading) {
            LoadingItem()
        } else if (filteredPackages.isEmpty()) {
            EmptyItem()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                items(filteredPackages.size) { index ->
                    val pkg = filteredPackages[index]
                    PackageItem(pkg, onClick = { selectedPackage = pkg })
                }
            }
        }
    }

    selectedPackage?.let { pkg ->
        OverlayDialog(
            show = true,
            title = pkg.appLabel,
            summary = "${pkg.packageName} · v${pkg.versionName}",
            onDismissRequest = { selectedPackage = null },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InfoLine("版本", "${pkg.versionName} (${pkg.versionCode})")
                InfoLine("UID", "${pkg.uid}")
                InfoLine("类型", if (pkg.isSystem) "系统应用" else "用户应用")
            }
            TextButton(text = "关闭", onClick = { selectedPackage = null })
            Button(onClick = {
                Shell.exec("am force-stop ${pkg.packageName}")
                selectedPackage = null
            }) { Text(text = "强制停止") }
            Button(onClick = {
                Shell.exec("pm clear ${pkg.packageName}")
                selectedPackage = null
            }) { Text(text = "清除数据") }
        }
    }
}

@Composable
private fun PackageItem(pkg: PackageInfo, onClick: () -> Unit) {
    Card(cornerRadius = 0.dp, insideMargin = PaddingValues(0.dp), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(ItemPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = "https://play.google.com/store/apps/details?id=${pkg.packageName}",
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(text = pkg.appLabel, style = MiuixTheme.textStyles.body1)
                Text(text = buildString {
                    append(pkg.packageName)
                    append(" · v${pkg.versionName}")
                    if (pkg.isSystem) append(" · 系统")
                }, style = MiuixTheme.textStyles.body2)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.disabledOnSurface,
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        cornerRadius = 999.dp,
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = if (selected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurfaceVariantActions,
        )
    }
}

@Composable
private fun LoadingItem() {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(ItemPadding),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(text = "正在加载应用列表...", style = MiuixTheme.textStyles.body2)
        }
    }
}

@Composable
private fun EmptyItem() {
    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(imageVector = Icons.Default.Apps, contentDescription = null, tint = MiuixTheme.colorScheme.disabledOnSurface)
            Text(text = "没有找到匹配的应用", style = MiuixTheme.textStyles.body2, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = "$label: ", style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        Text(text = value, style = MiuixTheme.textStyles.body2)
    }
}
