package com.lengyuefenghua.newsreader.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lengyuefenghua.newsreader.data.NewsRepository
import com.lengyuefenghua.newsreader.viewmodel.DiscoverUiState
import com.lengyuefenghua.newsreader.viewmodel.DiscoverViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlinkMarketScreen(
    discoverViewModel: DiscoverViewModel,
    onBack: () -> Unit,
    onOpenFeedPreview: (String, String) -> Unit
) {
    val state by discoverViewModel.plinkState.collectAsState()
    LaunchedEffect(Unit) {
        discoverViewModel.loadPlinkFeeds()
    }

    FeedCatalogMarketScreen(
        title = "Plink",
        sourceUrl = NewsRepository.PLINK_URL,
        state = state,
        onBack = onBack,
        onRefresh = { discoverViewModel.loadPlinkFeeds(forceRefresh = true) },
        onOpenFeedPreview = onOpenFeedPreview
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AwesomeRssHubRoutesMarketScreen(
    discoverViewModel: DiscoverViewModel,
    onBack: () -> Unit,
    onOpenFeedPreview: (String, String) -> Unit
) {
    val state by discoverViewModel.awesomeRssHubState.collectAsState()
    LaunchedEffect(Unit) {
        discoverViewModel.loadAwesomeRssHubFeeds()
    }

    FeedCatalogMarketScreen(
        title = "Awesome RSSHub Routes",
        sourceUrl = NewsRepository.AWESOME_RSSHUB_ROUTES_URL,
        state = state,
        onBack = onBack,
        onRefresh = { discoverViewModel.loadAwesomeRssHubFeeds(forceRefresh = true) },
        onOpenFeedPreview = onOpenFeedPreview
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopRssListMarketScreen(
    discoverViewModel: DiscoverViewModel,
    onBack: () -> Unit,
    onOpenFeedPreview: (String, String) -> Unit
) {
    val state by discoverViewModel.topRssListState.collectAsState()
    LaunchedEffect(Unit) {
        discoverViewModel.loadTopRssListFeeds()
    }

    FeedCatalogMarketScreen(
        title = "Top RSS List",
        sourceUrl = NewsRepository.TOP_RSS_LIST_URL,
        state = state,
        onBack = onBack,
        onRefresh = { discoverViewModel.loadTopRssListFeeds(forceRefresh = true) },
        onOpenFeedPreview = onOpenFeedPreview
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Wechat2RssMarketScreen(
    discoverViewModel: DiscoverViewModel,
    onBack: () -> Unit,
    onOpenFeedPreview: (String, String) -> Unit
) {
    val state by discoverViewModel.wechat2RssState.collectAsState()
    LaunchedEffect(Unit) {
        discoverViewModel.loadWechat2RssFeeds()
    }

    FeedCatalogMarketScreen(
        title = "Wechat2RSS",
        sourceUrl = NewsRepository.WECHAT2RSS_URL,
        state = state,
        onBack = onBack,
        onRefresh = { discoverViewModel.loadWechat2RssFeeds(forceRefresh = true) },
        onOpenFeedPreview = onOpenFeedPreview
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedCatalogMarketScreen(
    title: String,
    sourceUrl: String,
    state: DiscoverUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenFeedPreview: (String, String) -> Unit
) {
    var collapsedGroups by rememberSaveable { mutableStateOf(listOf<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading && state.groups.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null && state.groups.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = state.error ?: "加载失败",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = onRefresh) {
                            Text("重试")
                        }
                    }
                }
            }

            state.groups.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无可用订阅源")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        end = 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = sourceUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    state.groups.forEach { group ->
                        val isCollapsed = group.name in collapsedGroups

                        item(key = "group_${group.name}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        collapsedGroups = if (isCollapsed) {
                                            collapsedGroups - group.name
                                        } else {
                                            collapsedGroups + group.name
                                        }
                                    }
                                    .padding(top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = if (isCollapsed) "展开分组" else "收起分组"
                                )
                            }
                        }

                        if (!isCollapsed) {
                            itemsIndexed(
                                items = group.feeds,
                                key = { index, feed -> "${group.name}:${feed.url}:$index" }
                            ) { _, feed ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onOpenFeedPreview(feed.name, feed.url)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = feed.name,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = feed.url,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "divider_${group.name}") {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
