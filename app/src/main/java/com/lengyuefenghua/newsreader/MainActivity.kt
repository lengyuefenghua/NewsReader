package com.lengyuefenghua.newsreader

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lengyuefenghua.newsreader.ui.screens.ProfileScreen
import com.lengyuefenghua.newsreader.ui.screens.SourceManagerScreen
import com.lengyuefenghua.newsreader.ui.screens.TimelineScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lengyuefenghua.newsreader.ui.screens.ArticleScreen
import com.lengyuefenghua.newsreader.ui.screens.DebugConsoleScreen
import com.lengyuefenghua.newsreader.ui.screens.EditSourceScreen
import com.lengyuefenghua.newsreader.viewmodel.TimelineViewModel

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
    val items = listOf(Screen.Timeline, Screen.Sources, Screen.Profile)

    Scaffold(
        bottomBar = {
            // 简单的逻辑：只有在主 Tab 页面才显示底部导航
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            // 如果是这三个页面之一，显示 BottomBar
            if (currentRoute == Screen.Timeline.route || currentRoute == Screen.Sources.route || currentRoute == Screen.Profile.route) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                // 点击底部导航时，如果点击的是时间线，重置过滤条件
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
                    onSourceClick = { sourceId ->
                        // [新增] 跳转到特定源的时间线
                        navController.navigate("source_feed/$sourceId")
                    }
                )
            }

            composable(Screen.Profile.route) { ProfileScreen() }
// [新增] 特定源的时间线页面
            composable(
                route = "source_feed/{sourceId}",
                arguments = listOf(navArgument("sourceId") { type = NavType.IntType })
            ) { backStackEntry ->
                val sourceId = backStackEntry.arguments?.getInt("sourceId") ?: -1

                // 激活 VM 中的源过滤器
                LaunchedEffect(sourceId) {
                    if (sourceId != -1) {
                        timelineViewModel.showSource(sourceId)
                    }
                }
                DisposableEffect(Unit) {
                    onDispose {
                        timelineViewModel.resetSourceFilter()
                    }
                }
                // 监听当前显示的源名称
                val currentSourceTitle by timelineViewModel.currentSourceName.collectAsState()

                TimelineScreen(
                    viewModel = timelineViewModel,
                    title = currentSourceTitle ?: "加载中...",
                    onArticleClick = { url ->
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        navController.navigate("article/$encodedUrl")
                    }
                )
            }
            composable(
                route = "article/{url}",
                arguments = listOf(navArgument("url") { type = NavType.StringType })
            ) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: ""
                // 注意：这里我们依靠 ViewModel 的 StateFlow 缓存获取文章，
                // 如果应用被系统回收重启，articles 可能为空。
                // 生产环境应考虑由 Room 根据 ID 重新加载单条数据。
                val article = timelineViewModel.getArticleByUrl(url)

                ArticleScreen(
                    article = article,
                    onBack = { navController.popBackStack() },
                    onMarkRead = {
                        article?.let { timelineViewModel.markAsRead(it.id) }
                    },
                    onToggleFavorite = {
                        article?.let { timelineViewModel.toggleFavorite(it) }
                    },
                    onEditSource = { sourceName ->
                        // 异步查找源ID并跳转
                        timelineViewModel.findSourceIdAndEdit(sourceName) { id ->
                            navController.navigate("source_edit?id=$id")
                        }
                    }
                )
            }

            composable(
                route = "source_edit?id={id}",
                arguments = listOf(navArgument("id") {
                    type = NavType.IntType
                    defaultValue = -1
                })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: -1
                EditSourceScreen(
                    sourceId = id,
                    onBack = { navController.popBackStack() },
                    onSave = { navController.popBackStack() },
                    onDebug = { json -> navController.navigate("debug_console/$json") }
                )
            }

            composable(
                route = "debug_console/{json}",
                arguments = listOf(navArgument("json") { type = NavType.StringType })
            ) { backStackEntry ->
                val json = backStackEntry.arguments?.getString("json") ?: ""
                DebugConsoleScreen(sourceJson = json, onBack = { navController.popBackStack() })
            }
        }
    }
}