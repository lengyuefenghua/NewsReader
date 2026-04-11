package com.lengyuefenghua.newsreader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lengyuefenghua.newsreader.data.NewsRepository

private data class DiscoverPlugin(
    val title: String,
    val subtitle: String,
    val description: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onOpenPlink: () -> Unit,
    onOpenAwesomeRssHub: () -> Unit,
    onOpenTopRssList: () -> Unit,
    onOpenWechat2Rss: () -> Unit,
    onOpenQiReader: () -> Unit
) {
    val plugins = listOf(
        DiscoverPlugin(
            title = "Plink",
            subtitle = NewsRepository.PLINK_URL,
            description = "快速添加现有订阅市场中的 RSS 源",
            onClick = onOpenPlink
        ),
        DiscoverPlugin(
            title = "Awesome RSSHub Routes",
            subtitle = NewsRepository.AWESOME_RSSHUB_ROUTES_URL,
            description = "导入官方 RSS 与 RSSHub 路由目录中的公开订阅源",
            onClick = onOpenAwesomeRssHub
        ),
        DiscoverPlugin(
            title = "Top RSS List",
            subtitle = NewsRepository.TOP_RSS_LIST_URL,
            description = "浏览 ifeed 热门订阅榜单里的公开 RSS 源",
            onClick = onOpenTopRssList
        ),
        DiscoverPlugin(
            title = "Wechat2RSS",
            subtitle = NewsRepository.WECHAT2RSS_URL,
            description = "浏览 Wechat2RSS 整理的公开公众号 RSS 列表",
            onClick = onOpenWechat2Rss
        ),
        DiscoverPlugin(
            title = "QiReader",
            subtitle = NewsRepository.QIREADER_DISCOVER_URL,
            description = "登录后在 QiReader 的标签和搜索页里发现订阅源，并进入预览后订阅",
            onClick = onOpenQiReader
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("发现") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "订阅市场插件",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(plugins, key = { it.title }) { plugin ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = plugin.onClick)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = plugin.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = plugin.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = plugin.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
