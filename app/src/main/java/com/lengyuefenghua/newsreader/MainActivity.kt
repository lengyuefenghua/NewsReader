package com.lengyuefenghua.newsreader

import android.os.Bundle
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
import androidx.lifecycle.viewmodel.compose.viewModel // [新增导入]
import com.lengyuefenghua.newsreader.ui.screens.ArticleScreen
import com.lengyuefenghua.newsreader.ui.screens.DebugConsoleScreen
import com.lengyuefenghua.newsreader.ui.screens.EditSourceScreen
import com.lengyuefenghua.newsreader.viewmodel.TimelineViewModel // [新增导入]

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NewsReaderApp()
        }
    }
}

// 定义底部导航的三个目的地
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Timeline : Screen("timeline", "时间线", Icons.Filled.Home)
    object Sources : Screen("sources", "订阅", Icons.AutoMirrored.Filled.List)
    object Profile : Screen("profile", "我的", Icons.Filled.Person)
}

@Composable
fun NewsReaderApp() {
    val navController = rememberNavController()

    // [修复 1] 必须在这里创建 ViewModel，以便在 NavHost 里的不同页面间共享
    val timelineViewModel: TimelineViewModel = viewModel()

    // 底部导航栏要显示的列表
    val items = listOf(Screen.Timeline, Screen.Sources, Screen.Profile)

    Scaffold(
        bottomBar = {
            NavigationBar {
                // 获取当前路由，用来决定哪个图标高亮
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Timeline.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. 时间线页面
            composable(Screen.Timeline.route) {
                TimelineScreen(
                    // [修复 2] 传入上面创建的 viewModel 实例
                    viewModel = timelineViewModel,
                    onArticleClick = { url ->
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        navController.navigate("article/$encodedUrl")
                    }
                )
            }

            // 2. 订阅管理页面 (修复了重复定义的问题)
            composable(Screen.Sources.route) {
                SourceManagerScreen(
                    // 情况 A: 在弹窗里点了“高级模式” -> 跳转到新增页面 (默认 ID -1)
                    onOpenAdvanced = {
                        navController.navigate("source_edit")
                    },
                    // 情况 B: 点击了列表项的“编辑”按钮 -> 跳转到编辑页面 (带 ID)
                    onEditSource = { sourceId ->
                        navController.navigate("source_edit?id=$sourceId")
                    }
                )
            }

            // 3. 我的页面
            composable(Screen.Profile.route) { ProfileScreen() }

            // 4. 文章详情页
            composable(
                route = "article/{url}",
                arguments = listOf(navArgument("url") { type = NavType.StringType })
            ) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: ""

                // [修复 3] 现在 timelineViewModel 已经定义了，可以正常调用
                val article = timelineViewModel.getArticleByUrl(url)

                ArticleScreen(
                    article = article,
                    onBack = { navController.popBackStack() }
                )
            }

            // 5. 编辑/新增页面 (修复了重复定义，只保留这个带参数的版本)
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
                    onDebug = { json ->
                        navController.navigate("debug_console/$json")
                    }
                )
            }

            // 6. 调试控制台页面
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