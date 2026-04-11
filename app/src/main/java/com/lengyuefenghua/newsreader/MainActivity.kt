package com.lengyuefenghua.newsreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.androidx.compose.koinViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lengyuefenghua.newsreader.core.navigation.NavRoutes
import com.lengyuefenghua.newsreader.ui.screens.ArticleScreen
import com.lengyuefenghua.newsreader.ui.screens.ArticleReadingContext
import com.lengyuefenghua.newsreader.ui.screens.ArticleReadingSession
import com.lengyuefenghua.newsreader.ui.screens.DebugConsoleScreen
import com.lengyuefenghua.newsreader.ui.screens.DiscoverScreen
import com.lengyuefenghua.newsreader.ui.screens.EditSourceScreen
import com.lengyuefenghua.newsreader.ui.screens.FeedPreviewScreen
import com.lengyuefenghua.newsreader.ui.screens.FavoritesScreen
import com.lengyuefenghua.newsreader.ui.screens.AwesomeRssHubRoutesMarketScreen
import com.lengyuefenghua.newsreader.ui.screens.PlinkMarketScreen
import com.lengyuefenghua.newsreader.ui.screens.PlinkFeedPreviewScreen
import com.lengyuefenghua.newsreader.ui.screens.ProfileScreen
import com.lengyuefenghua.newsreader.ui.screens.QiReaderMarketScreen
import com.lengyuefenghua.newsreader.ui.screens.SettingsScreen
import com.lengyuefenghua.newsreader.ui.screens.SourceManagerScreen
import com.lengyuefenghua.newsreader.ui.screens.StatsScreen
import com.lengyuefenghua.newsreader.ui.screens.TopRssListMarketScreen
import com.lengyuefenghua.newsreader.ui.screens.TimelineScreen
import com.lengyuefenghua.newsreader.ui.screens.Wechat2RssMarketScreen
import com.lengyuefenghua.newsreader.viewmodel.DiscoverViewModel
import com.lengyuefenghua.newsreader.viewmodel.FeedPreviewViewModel
import com.lengyuefenghua.newsreader.viewmodel.PlinkFeedPreviewViewModel
import com.lengyuefenghua.newsreader.viewmodel.SourceViewModel
import com.lengyuefenghua.newsreader.viewmodel.TimelineViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewsReaderApp()
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Timeline : Screen(NavRoutes.TIMELINE, "时间线", Icons.Filled.Home)
    object Sources : Screen(NavRoutes.SOURCES, "订阅", Icons.AutoMirrored.Filled.List)
    object Discover : Screen(NavRoutes.DISCOVER, "发现", Icons.Filled.Public)
    object Profile : Screen(NavRoutes.PROFILE, "我的", Icons.Filled.Person)
}

@Composable
fun NewsReaderApp() {
    val navController = rememberNavController()
    val timelineViewModel: TimelineViewModel = koinViewModel()
    val sourceViewModel: SourceViewModel = viewModel()
    val discoverViewModel: DiscoverViewModel = viewModel()
    val feedPreviewViewModel: FeedPreviewViewModel = viewModel()
    val plinkFeedPreviewViewModel: PlinkFeedPreviewViewModel = viewModel()
    val previewReturnRoute by feedPreviewViewModel.returnRoute.collectAsState()
    val scope = rememberCoroutineScope()
    // 获取 Prefs Repo
    val application =
        androidx.compose.ui.platform.LocalContext.current.applicationContext as NewsReaderApplication
    val prefs = application.userPreferencesRepository

    // [新增] 自动更新逻辑
    LaunchedEffect(Unit) {
        scope.launch {
            if (prefs.autoUpdateFlow.first()) {
                timelineViewModel.refresh()
            }
        }
    }

    val items = listOf(Screen.Timeline, Screen.Sources, Screen.Discover, Screen.Profile)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute == Screen.Timeline.route || currentRoute == Screen.Sources.route || currentRoute == Screen.Discover.route || currentRoute == Screen.Profile.route) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (screen.route == Screen.Timeline.route) {
                                    timelineViewModel.resetSourceFilter()
                                }
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Timeline.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Timeline.route) {
                TimelineScreen(
                    viewModel = timelineViewModel,
                    title = "时间线",
                    onArticleClick = { url, articles ->
                        ArticleReadingSession.open(
                            ArticleReadingContext(
                                articles = articles,
                                currentUrl = url,
                            )
                        )
                        navController.navigate(NavRoutes.article(url))
                    }
                )
            }

            composable(Screen.Sources.route) {
                SourceManagerScreen(
                    viewModel = sourceViewModel,
                    previewViewModel = feedPreviewViewModel,
                    onOpenAdvanced = { name, url ->
                        navController.navigate(NavRoutes.sourceEdit(name = name, url = url))
                    },
                    onEditSource = { sourceId -> navController.navigate(NavRoutes.sourceEdit(id = sourceId)) },
                    onSourceClick = { sourceId -> navController.navigate(NavRoutes.sourceFeed(sourceId)) },
                    onOpenFeedPreview = { navController.navigate(NavRoutes.FEED_PREVIEW) }
                )
            }

            composable(Screen.Discover.route) {
                DiscoverScreen(
                    onOpenPlink = { navController.navigate(NavRoutes.PLINK_MARKET) },
                    onOpenAwesomeRssHub = { navController.navigate(NavRoutes.AWESOME_RSSHUB_MARKET) },
                    onOpenTopRssList = { navController.navigate(NavRoutes.TOP_RSS_LIST_MARKET) },
                    onOpenWechat2Rss = { navController.navigate(NavRoutes.WECHAT2RSS_MARKET) },
                    onOpenQiReader = { navController.navigate(NavRoutes.QIREADER_MARKET) }
                )
            }

            composable(NavRoutes.PLINK_MARKET) {
                PlinkMarketScreen(
                    discoverViewModel = discoverViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenFeedPreview = { name, url ->
                        navController.navigate(NavRoutes.plinkFeedPreview(name, url))
                    }
                )
            }

            composable(NavRoutes.AWESOME_RSSHUB_MARKET) {
                AwesomeRssHubRoutesMarketScreen(
                    discoverViewModel = discoverViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenFeedPreview = { name, url ->
                        navController.navigate(NavRoutes.awesomeRssHubFeedPreview(name, url))
                    }
                )
            }

            composable(NavRoutes.TOP_RSS_LIST_MARKET) {
                TopRssListMarketScreen(
                    discoverViewModel = discoverViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenFeedPreview = { name, url ->
                        navController.navigate(NavRoutes.topRssListFeedPreview(name, url))
                    }
                )
            }

            composable(NavRoutes.WECHAT2RSS_MARKET) {
                Wechat2RssMarketScreen(
                    discoverViewModel = discoverViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenFeedPreview = { name, url ->
                        navController.navigate(NavRoutes.wechat2RssFeedPreview(name, url))
                    }
                )
            }

            composable(NavRoutes.QIREADER_MARKET) {
                QiReaderMarketScreen(
                    onBack = { navController.popBackStack() },
                    onOpenFeedPreview = { name, url ->
                        navController.navigate(NavRoutes.qiReaderFeedPreview(name, url))
                    }
                )
            }

            composable(
                route = NavRoutes.PLINK_FEED_PREVIEW,
                arguments = listOf(
                    navArgument(NavRoutes.PLINK_FEED_PREVIEW_NAME_ARG) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(NavRoutes.PLINK_FEED_PREVIEW_URL_ARG) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val title = backStackEntry.arguments?.getString(NavRoutes.PLINK_FEED_PREVIEW_NAME_ARG).orEmpty()
                val url = backStackEntry.arguments?.getString(NavRoutes.PLINK_FEED_PREVIEW_URL_ARG).orEmpty()
                PlinkFeedPreviewScreen(
                    title = title,
                    url = url,
                    viewModel = plinkFeedPreviewViewModel,
                    sourceViewModel = sourceViewModel,
                    onBack = {
                        plinkFeedPreviewViewModel.clearPreview()
                        navController.popBackStack()
                    },
                    onArticleClick = { articleUrl, articles ->
                        ArticleReadingSession.open(
                            ArticleReadingContext(
                                articles = articles,
                                currentUrl = articleUrl,
                            )
                        )
                        navController.navigate(NavRoutes.article(articleUrl))
                    },
                    onOpenAdvanced = { name, targetUrl ->
                        navController.navigate(NavRoutes.sourceEdit(name = name, url = targetUrl))
                    },
                    onSubscribed = {
                        plinkFeedPreviewViewModel.clearPreview()
                        navController.popBackStack(NavRoutes.PLINK_MARKET, false)
                    }
                )
            }

            composable(
                route = NavRoutes.AWESOME_RSSHUB_FEED_PREVIEW,
                arguments = listOf(
                    navArgument(NavRoutes.AWESOME_RSSHUB_FEED_PREVIEW_NAME_ARG) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(NavRoutes.AWESOME_RSSHUB_FEED_PREVIEW_URL_ARG) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val title = backStackEntry.arguments?.getString(NavRoutes.AWESOME_RSSHUB_FEED_PREVIEW_NAME_ARG).orEmpty()
                val url = backStackEntry.arguments?.getString(NavRoutes.AWESOME_RSSHUB_FEED_PREVIEW_URL_ARG).orEmpty()
                PlinkFeedPreviewScreen(
                    title = title,
                    url = url,
                    viewModel = plinkFeedPreviewViewModel,
                    sourceViewModel = sourceViewModel,
                    onBack = {
                        plinkFeedPreviewViewModel.clearPreview()
                        navController.popBackStack()
                    },
                    onArticleClick = { articleUrl, articles ->
                        ArticleReadingSession.open(
                            ArticleReadingContext(
                                articles = articles,
                                currentUrl = articleUrl,
                            )
                        )
                        navController.navigate(NavRoutes.article(articleUrl))
                    },
                    onOpenAdvanced = { name, targetUrl ->
                        navController.navigate(NavRoutes.sourceEdit(name = name, url = targetUrl))
                    },
                    onSubscribed = {
                        plinkFeedPreviewViewModel.clearPreview()
                        navController.popBackStack(NavRoutes.AWESOME_RSSHUB_MARKET, false)
                    }
                )
            }

            composable(
                route = NavRoutes.TOP_RSS_LIST_FEED_PREVIEW,
                arguments = listOf(
                    navArgument(NavRoutes.TOP_RSS_LIST_FEED_PREVIEW_NAME_ARG) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(NavRoutes.TOP_RSS_LIST_FEED_PREVIEW_URL_ARG) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val title = backStackEntry.arguments?.getString(NavRoutes.TOP_RSS_LIST_FEED_PREVIEW_NAME_ARG).orEmpty()
                val url = backStackEntry.arguments?.getString(NavRoutes.TOP_RSS_LIST_FEED_PREVIEW_URL_ARG).orEmpty()
                PlinkFeedPreviewScreen(
                    title = title,
                    url = url,
                    viewModel = plinkFeedPreviewViewModel,
                    sourceViewModel = sourceViewModel,
                    onBack = {
                        plinkFeedPreviewViewModel.clearPreview()
                        navController.popBackStack()
                    },
                    onArticleClick = { articleUrl, articles ->
                        ArticleReadingSession.open(
                            ArticleReadingContext(
                                articles = articles,
                                currentUrl = articleUrl,
                            )
                        )
                        navController.navigate(NavRoutes.article(articleUrl))
                    },
                    onOpenAdvanced = { name, targetUrl ->
                        navController.navigate(NavRoutes.sourceEdit(name = name, url = targetUrl))
                    },
                    onSubscribed = {
                        plinkFeedPreviewViewModel.clearPreview()
                        navController.popBackStack(NavRoutes.TOP_RSS_LIST_MARKET, false)
                    }
                )
            }

            composable(
                route = NavRoutes.WECHAT2RSS_FEED_PREVIEW,
                arguments = listOf(
                    navArgument(NavRoutes.WECHAT2RSS_FEED_PREVIEW_NAME_ARG) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(NavRoutes.WECHAT2RSS_FEED_PREVIEW_URL_ARG) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val title = backStackEntry.arguments?.getString(NavRoutes.WECHAT2RSS_FEED_PREVIEW_NAME_ARG).orEmpty()
                val url = backStackEntry.arguments?.getString(NavRoutes.WECHAT2RSS_FEED_PREVIEW_URL_ARG).orEmpty()
                PlinkFeedPreviewScreen(
                    title = title,
                    url = url,
                    viewModel = plinkFeedPreviewViewModel,
                    sourceViewModel = sourceViewModel,
                    onBack = {
                        plinkFeedPreviewViewModel.clearPreview()
                        navController.popBackStack()
                    },
                    onArticleClick = { articleUrl, articles ->
                        ArticleReadingSession.open(
                            ArticleReadingContext(
                                articles = articles,
                                currentUrl = articleUrl,
                            )
                        )
                        navController.navigate(NavRoutes.article(articleUrl))
                    },
                    onOpenAdvanced = { name, targetUrl ->
                        navController.navigate(NavRoutes.sourceEdit(name = name, url = targetUrl))
                    },
                    onSubscribed = {
                        plinkFeedPreviewViewModel.clearPreview()
                        navController.popBackStack(NavRoutes.WECHAT2RSS_MARKET, false)
                    }
                )
            }

            composable(
                route = NavRoutes.QIREADER_FEED_PREVIEW,
                arguments = listOf(
                    navArgument(NavRoutes.QIREADER_FEED_PREVIEW_NAME_ARG) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument(NavRoutes.QIREADER_FEED_PREVIEW_URL_ARG) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val title = backStackEntry.arguments?.getString(NavRoutes.QIREADER_FEED_PREVIEW_NAME_ARG).orEmpty()
                val url = backStackEntry.arguments?.getString(NavRoutes.QIREADER_FEED_PREVIEW_URL_ARG).orEmpty()
                PlinkFeedPreviewScreen(
                    title = title,
                    url = url,
                    viewModel = plinkFeedPreviewViewModel,
                    sourceViewModel = sourceViewModel,
                    onBack = {
                        plinkFeedPreviewViewModel.clearPreview()
                        navController.popBackStack()
                    },
                    onArticleClick = { articleUrl, articles ->
                        ArticleReadingSession.open(
                            ArticleReadingContext(
                                articles = articles,
                                currentUrl = articleUrl,
                            )
                        )
                        navController.navigate(NavRoutes.article(articleUrl))
                    },
                    onOpenAdvanced = { name, targetUrl ->
                        navController.navigate(NavRoutes.sourceEdit(name = name, url = targetUrl))
                    },
                    onSubscribed = {
                        plinkFeedPreviewViewModel.clearPreview()
                        navController.popBackStack(NavRoutes.QIREADER_MARKET, false)
                    }
                )
            }

            composable(NavRoutes.FEED_PREVIEW) {
                FeedPreviewScreen(
                    previewViewModel = feedPreviewViewModel,
                    sourceViewModel = sourceViewModel,
                    onBack = {
                        val targetRoute = previewReturnRoute
                        feedPreviewViewModel.clearPreviewSession()
                        val popped = if (!targetRoute.isNullOrBlank()) {
                            navController.popBackStack(targetRoute, false)
                        } else {
                            false
                        }
                        if (!popped) {
                            navController.popBackStack()
                        }
                    },
                    onArticleClick = { url, articles ->
                        ArticleReadingSession.open(
                            ArticleReadingContext(
                                articles = articles,
                                currentUrl = url,
                            )
                        )
                        navController.navigate(NavRoutes.article(url))
                    },
                    onOpenAdvanced = { name, url ->
                        navController.navigate(NavRoutes.sourceEdit(name = name, url = url))
                    },
                    onSubscribed = {
                        val targetRoute = previewReturnRoute
                        feedPreviewViewModel.clearPreviewSession()
                        val popped = if (!targetRoute.isNullOrBlank()) {
                            navController.popBackStack(targetRoute, false)
                        } else {
                            false
                        }
                        if (!popped) {
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onOpenFavorites = { navController.navigate(NavRoutes.FAVORITES) },
                    onOpenSettings = { navController.navigate(NavRoutes.SETTINGS) },
                    onOpenStats = { navController.navigate(NavRoutes.STATS) }
                )
            }

            // [新增] 设置页面
            composable(NavRoutes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(NavRoutes.FAVORITES) {
                FavoritesScreen(
                    onBack = { navController.popBackStack() },
                    onArticleClick = { url, articles ->
                        ArticleReadingSession.open(
                            ArticleReadingContext(
                                articles = articles,
                                currentUrl = url,
                            )
                        )
                        navController.navigate(NavRoutes.article(url))
                    }
                )
            }

            // ... (其他原有路由保持不变)
            composable(
                route = NavRoutes.SOURCE_FEED,
                arguments = listOf(navArgument(NavRoutes.SOURCE_FEED_ARG) { type = NavType.IntType })
            ) { backStackEntry ->
                val sourceId = backStackEntry.arguments?.getInt(NavRoutes.SOURCE_FEED_ARG) ?: -1
                LaunchedEffect(sourceId) {
                    if (sourceId != -1) timelineViewModel.showSource(sourceId)
                }
                DisposableEffect(Unit) {
                    onDispose { timelineViewModel.resetSourceFilter() }
                }
                val currentSourceTitle by timelineViewModel.currentSourceName.collectAsState()
                TimelineScreen(
                    viewModel = timelineViewModel,
                    title = currentSourceTitle ?: "加载中...",
                    onBack = { navController.popBackStack() },
                    onArticleClick = { url, articles ->
                        ArticleReadingSession.open(
                            ArticleReadingContext(
                                articles = articles,
                                currentUrl = url,
                            )
                        )
                        navController.navigate(NavRoutes.article(url))
                    }
                )
            }
            // [新增] 统计页面
            composable(NavRoutes.STATS) {
                StatsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = NavRoutes.ARTICLE,
                arguments = listOf(navArgument(NavRoutes.ARTICLE_ARG) { type = NavType.StringType })
            ) { backStackEntry ->
                val url = backStackEntry.arguments?.getString(NavRoutes.ARTICLE_ARG) ?: ""
                var isInternalArticleNavigation by remember(url) { mutableStateOf(false) }
                val readingContext = ArticleReadingSession.current
                    ?.takeIf { it.articleUrls.contains(url) }
                    ?.copy(currentUrl = url)
                DisposableEffect(url, readingContext?.currentUrl, readingContext?.previousUrl, readingContext?.nextUrl) {
                    android.util.Log.d(
                        "ArticlePullDebug",
                        "route url=$url sessionCurrent=${ArticleReadingSession.current?.currentUrl} prev=${readingContext?.previousUrl} next=${readingContext?.nextUrl}"
                    )
                    onDispose {
                        android.util.Log.d(
                            "ArticlePullDebug",
                            "dispose url=$url internal=$isInternalArticleNavigation sessionCurrent=${ArticleReadingSession.current?.currentUrl}"
                        )
                    }
                }
                DisposableEffect(url) {
                    onDispose {
                        if (!isInternalArticleNavigation) {
                            android.util.Log.d("ArticlePullDebug", "clear session on dispose url=$url")
                            ArticleReadingSession.clear()
                        }
                    }
                }
                val preview by feedPreviewViewModel.preview.collectAsState()
                val plinkPreview by plinkFeedPreviewViewModel.uiState.collectAsState()
                val previewArticle = preview?.articles?.find { it.url == url }
                    ?: plinkPreview.preview?.articles?.find { it.url == url }
                val initialArticle = remember(url, previewArticle) {
                    timelineViewModel.getArticleByUrl(url) ?: previewArticle
                }
                val article by timelineViewModel.getArticleFlow(url)
                    .collectAsState(initial = initialArticle)
                val displayedArticle = article ?: previewArticle
                ArticleScreen(
                    article = displayedArticle,
                    previousTitle = readingContext?.previousTitle,
                    nextTitle = readingContext?.nextTitle,
                    previousUrl = readingContext?.previousUrl,
                    nextUrl = readingContext?.nextUrl,
                    onBack = { navController.popBackStack() },
                    onMarkRead = { displayedArticle?.let { timelineViewModel.markAsRead(it.id) } },
                    onToggleFavorite = { displayedArticle?.let { timelineViewModel.toggleFavorite(it) } },
                    onEditSource = { sourceName ->
                        timelineViewModel.findSourceIdAndEdit(sourceName) { id ->
                            navController.navigate(NavRoutes.sourceEdit(id = id))
                        }
                    },
                    // [新增] 计时回调
                    onUpdateReadDuration = { id, duration ->
                        timelineViewModel.updateReadDuration(id, duration)
                    },
                    onOpenAdjacentArticle = { targetUrl ->
                        android.util.Log.d(
                            "ArticlePullDebug",
                            "onOpenAdjacentArticle current=$url target=$targetUrl sessionBefore=${ArticleReadingSession.current?.currentUrl}"
                        )
                        isInternalArticleNavigation = true
                        ArticleReadingSession.moveTo(targetUrl)
                        android.util.Log.d(
                            "ArticlePullDebug",
                            "sessionAfterMove=${ArticleReadingSession.current?.currentUrl} popping current=$url"
                        )
                        navController.popBackStack()
                        android.util.Log.d("ArticlePullDebug", "navigating target=$targetUrl")
                        navController.navigate(NavRoutes.article(targetUrl))
                    }
                )
            }

            composable(
                route = NavRoutes.SOURCE_EDIT,
                arguments = listOf(
                    navArgument(NavRoutes.SOURCE_EDIT_ID_ARG) { type = NavType.IntType; defaultValue = -1 },
                    navArgument(NavRoutes.SOURCE_EDIT_NAME_ARG) { type = NavType.StringType; defaultValue = "" },
                    navArgument(NavRoutes.SOURCE_EDIT_URL_ARG) { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt(NavRoutes.SOURCE_EDIT_ID_ARG) ?: -1
                val initialName = backStackEntry.arguments?.getString(NavRoutes.SOURCE_EDIT_NAME_ARG).orEmpty()
                val initialUrl = backStackEntry.arguments?.getString(NavRoutes.SOURCE_EDIT_URL_ARG).orEmpty()
                EditSourceScreen(
                    sourceId = id,
                    initialName = initialName,
                    initialUrl = initialUrl,
                    onBack = { navController.popBackStack() },
                    onSave = {
                        val previousRoute = navController.previousBackStackEntry?.destination?.route
                        if (id == -1 && initialUrl.isNotBlank()) {
                            when (previousRoute) {
                                NavRoutes.PLINK_FEED_PREVIEW -> {
                                    plinkFeedPreviewViewModel.clearPreview()
                                    navController.popBackStack(NavRoutes.PLINK_MARKET, false)
                                }

                                NavRoutes.AWESOME_RSSHUB_FEED_PREVIEW -> {
                                    plinkFeedPreviewViewModel.clearPreview()
                                    navController.popBackStack(NavRoutes.AWESOME_RSSHUB_MARKET, false)
                                }

                                NavRoutes.TOP_RSS_LIST_FEED_PREVIEW -> {
                                    plinkFeedPreviewViewModel.clearPreview()
                                    navController.popBackStack(NavRoutes.TOP_RSS_LIST_MARKET, false)
                                }

                                NavRoutes.WECHAT2RSS_FEED_PREVIEW -> {
                                    plinkFeedPreviewViewModel.clearPreview()
                                    navController.popBackStack(NavRoutes.WECHAT2RSS_MARKET, false)
                                }

                                NavRoutes.QIREADER_FEED_PREVIEW -> {
                                    plinkFeedPreviewViewModel.clearPreview()
                                    navController.popBackStack(NavRoutes.QIREADER_MARKET, false)
                                }

                                else -> {
                                    feedPreviewViewModel.clearPreviewSession()
                                    navController.popBackStack(Screen.Sources.route, false)
                                }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onDebug = { json -> navController.navigate("debug_console/$json") })
            }

            composable(
                "debug_console/{json}",
                arguments = listOf(navArgument("json") { type = NavType.StringType })
            ) { backStackEntry ->
                val json = backStackEntry.arguments?.getString("json") ?: ""
                DebugConsoleScreen(sourceJson = json, onBack = { navController.popBackStack() })
            }
        }
    }
}
