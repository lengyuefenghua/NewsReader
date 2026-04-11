package com.lengyuefenghua.newsreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lengyuefenghua.newsreader.BuildConfig
import com.lengyuefenghua.newsreader.R
import com.lengyuefenghua.newsreader.viewmodel.ProfileViewModel

private data class AcknowledgementItem(
    val title: String,
    val links: List<String>
)

private val acknowledgementItems = listOf(
    AcknowledgementItem(
        title = "开源阅读",
        links = listOf("https://github.com/gedoor/legado")
    ),
    AcknowledgementItem(
        title = "GeneralNewsExtractor",
        links = listOf("https://github.com/GeneralNewsExtractor/GeneralNewsExtracto")
    ),
    AcknowledgementItem(
        title = "Readability.js",
        links = listOf(
            "https://github.com/mozilla/readability",
            "http://code.arc90.com/readability/"
        )
    ),
    AcknowledgementItem(
        title = "Boilerplate detection using shallow text features",
        links = listOf("https://dl.acm.org/doi/10.1145/1718487.1718542")
    )
)

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onOpenFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit
) {
    val count by viewModel.sourceCount.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAcknowledgementDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 头部区域 (保持不变)
        Box(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Person,
                    null,
                    modifier = Modifier.size(80.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("NewsReader 用户", style = MaterialTheme.typography.titleLarge)
                Text(
                    "已订阅 $count 个来源",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        HorizontalDivider()

        SettingItem(Icons.Default.Favorite, "我的收藏", "查看已收藏的文章", onOpenFavorites, descContent = stringResource(R.string.desc_button_favorites))
        SettingItem(Icons.Default.BarChart, "阅读统计", "查看阅读时长与数据", onOpenStats, descContent = stringResource(R.string.desc_button_stats))
        SettingItem(Icons.Default.Settings, "设置", "自动更新、缓存管理", onOpenSettings, descContent = stringResource(R.string.desc_button_settings))
        SettingItem(
            Icons.Default.Info,
            "关于",
            "查看版本号、构建日期和作者信息",
            { showAboutDialog = true },
            descContent = "打开关于"
        )
        SettingItem(
            Icons.Default.Info,
            "致敬开源",
            "查看开源项目与论文来源",
            { showAcknowledgementDialog = true },
            descContent = stringResource(R.string.desc_button_open_source)
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showAcknowledgementDialog) {
        AcknowledgementDialog(onDismiss = { showAcknowledgementDialog = false })
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AboutInfoRow(label = "版本号", value = BuildConfig.VERSION_NAME)
                AboutInfoRow(label = "构建日期", value = BuildConfig.BUILD_DATE)
                AboutInfoRow(label = "作者", value = "冷月风华")
                AboutInfoRow(
                    label = "GitHub",
                    value = "https://github.com/lengyuefenghua/NewsReader",
                    onClick = { uriHandler.openUri("https://github.com/lengyuefenghua/NewsReader") }
                )
                Text(
                    text = "本项目完全基于OpenCode+GPT开发",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun AboutInfoRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        if (onClick != null) {
            LinkText(text = value, onClick = onClick)
        } else {
            SelectionContainer {
                Text(text = value, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun AcknowledgementDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("致敬开源") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                acknowledgementItems.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = item.title, style = MaterialTheme.typography.titleSmall)
                        item.links.forEach { link ->
                            LinkText(
                                text = link,
                                onClick = { uriHandler.openUri(link) },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun LinkText(
    text: String,
    onClick: () -> Unit,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium
) {
    Text(
        text = text,
        style = style,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun SettingItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, descContent: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
            .semantics { contentDescription = descContent },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
