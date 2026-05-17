package com.ksu.miuix.ui.packages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.ksu.miuix.shell.PackageInfo
import com.ksu.miuix.shell.Shell

private val ItemPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PackagesScreen(paddingValues: PaddingValues) {
    var packages by remember { mutableStateOf<List<PackageInfo>>(emptyList()) }
    var filteredPackages by remember { mutableStateOf<List<PackageInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableIntStateOf(0) }
    var selectedPackage by remember { mutableStateOf<PackageInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

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
        Card(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                placeholder = { Text("搜索应用...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            FilterChip(selected = filterMode == 0, onClick = { filterMode = 0 }, label = { Text("全部") })
            FilterChip(selected = filterMode == 1, onClick = { filterMode = 1 }, label = { Text("用户") })
            FilterChip(selected = filterMode == 2, onClick = { filterMode = 2 }, label = { Text("系统") })
        }

        Text(
            text = "应用列表 (${filteredPackages.size})",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )

        if (isLoading) {
            LoadingItem()
        } else if (filteredPackages.isEmpty()) {
            EmptyItem()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                items(filteredPackages.size, key = { index -> filteredPackages[index].packageName }) { index ->
                    val pkg = filteredPackages[index]
                    PackageItem(pkg, onClick = { selectedPackage = pkg })
                }
            }
        }
    }

    selectedPackage?.let { pkg ->
        AlertDialog(
            onDismissRequest = { selectedPackage = null },
            title = { Text(pkg.appLabel) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    InfoLine("版本", "${pkg.versionName} (${pkg.versionCode})")
                    InfoLine("UID", "${pkg.uid}")
                    InfoLine("类型", if (pkg.isSystem) "系统应用" else "用户应用")
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch { Shell.exec("am force-stop ${pkg.packageName}") }
                    selectedPackage = null
                }) { Text("强制停止") }
            },
            dismissButton = {
                TextButton(onClick = { selectedPackage = null }) { Text("关闭") }
            },
        )
    }
}

@Composable
private fun PackageItem(pkg: PackageInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(ItemPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Card(
                modifier = Modifier.size(40.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = pkg.appLabel.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(text = pkg.appLabel, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = buildString {
                        append(pkg.packageName)
                        append(" · v${pkg.versionName}")
                        if (pkg.isSystem) append(" · 系统")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LoadingItem() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(ItemPadding),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(text = "正在加载应用列表...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyItem() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = "没有找到匹配的应用",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = "$label: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}
