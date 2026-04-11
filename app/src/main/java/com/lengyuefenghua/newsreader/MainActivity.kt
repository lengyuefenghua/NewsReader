package com.lengyuefenghua.newsreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import com.lengyuefenghua.newsreader.ui.screens.EditSourceScreen
import com.lengyuefenghua.newsreader.ui.screens.FavoritesScreen
import com.lengyuefenghua.newsreader.ui.screens.ProfileScreen
import com.lengyuefenghua.newsreader.ui.screens.SettingsScreen
import com.lengyuefenghua.newsreader.ui.screens.SourceManagerScreen
import com.lengyuefenghua.newsreader.ui.screens.StatsScreen
import com.lengyuefenghua.newsreader.ui.screens.TimelineScreen
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
    object Profile : Screen(NavRoutes.PROFILE, "我的", Icons.Filled.Person)
}

@Composable
fun NewsReaderApp() {
    val navController = rememberNavController()
    val timelineViewModel: TimelineViewModel = koinViewModel()
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

    val items = listOf(Screen.Timeline, Screen.Sources, Screen.Profile)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute == Screen.Timeline.route || currentRoute == Screen.Sources.route || currentRoute == Screen.Profile.route) {
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
                    onOpenAdvanced = { navController.navigate("source_edit") },
                    onEditSource = { sourceId -> navController.navigate("source_edit?id=$sourceId") },
                    onSourceClick = { sourceId -> navController.navigate(NavRoutes.sourceFeed(sourceId)) }
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
                val initialArticle = remember { timelineViewModel.getArticleByUrl(url) }
                val article by timelineViewModel.getArticleFlow(url)
                    .collectAsState(initial = initialArticle)
                ArticleScreen(
                    article = article,
                    previousTitle = readingContext?.previousTitle,
                    nextTitle = readingContext?.nextTitle,
                    previousUrl = readingContext?.previousUrl,
                    nextUrl = readingContext?.nextUrl,
                    onBack = { navController.popBackStack() },
                    onMarkRead = { article?.let { timelineViewModel.markAsRead(it.id) } },
                    onToggleFavorite = { article?.let { timelineViewModel.toggleFavorite(it) } },
                    onEditSource = { sourceName ->
                        timelineViewModel.findSourceIdAndEdit(sourceName) { id ->
                            navController.navigate(
                                "source_edit?id=$id"
                            )
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
                "source_edit?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = -1 })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: -1
                EditSourceScreen(
                    sourceId = id,
                    onBack = { navController.popBackStack() },
                    onSave = { navController.popBackStack() },
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
