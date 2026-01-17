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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lengyuefenghua.newsreader.ui.screens.ArticleScreen
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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewsReaderApp()
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Timeline : Screen("timeline", "时间线", Icons.Filled.Home)
    object Sources : Screen("sources", "订阅", Icons.AutoMirrored.Filled.List)
    object Profile : Screen("profile", "我的", Icons.Filled.Person)
}

@Composable
fun NewsReaderApp() {
    val navController = rememberNavController()
    val timelineViewModel: TimelineViewModel = viewModel()
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
                    onArticleClick = { url ->
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        navController.navigate("article/$encodedUrl")
                    }
                )
            }

            composable(Screen.Sources.route) {
                SourceManagerScreen(
                    onOpenAdvanced = { navController.navigate("source_edit") },
                    onEditSource = { sourceId -> navController.navigate("source_edit?id=$sourceId") },
                    onSourceClick = { sourceId -> navController.navigate("source_feed/$sourceId") }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onOpenFavorites = { navController.navigate("favorites") },
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenStats = { navController.navigate("stats") } // [新增]
                )
            }

            // [新增] 设置页面
            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
            }

            composable("favorites") {
                FavoritesScreen(
                    onBack = { navController.popBackStack() },
                    onArticleClick = { url ->
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        navController.navigate("article/$encodedUrl")
                    }
                )
            }

            // ... (其他原有路由保持不变)
            composable(
                route = "source_feed/{sourceId}",
                arguments = listOf(navArgument("sourceId") { type = NavType.IntType })
            ) { backStackEntry ->
                val sourceId = backStackEntry.arguments?.getInt("sourceId") ?: -1
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
                    onArticleClick = { url ->
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        navController.navigate("article/$encodedUrl")
                    }
                )
            }
            // [新增] 统计页面
            composable("stats") {
                StatsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                "article/{url}",
                arguments = listOf(navArgument("url") { type = NavType.StringType })
            ) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: ""
                val initialArticle = remember { timelineViewModel.getArticleByUrl(url) }
                val article by timelineViewModel.getArticleFlow(url)
                    .collectAsState(initial = initialArticle)
                ArticleScreen(
                    article = article,
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