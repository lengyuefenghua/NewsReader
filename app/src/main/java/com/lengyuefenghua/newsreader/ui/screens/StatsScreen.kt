package com.lengyuefenghua.newsreader.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lengyuefenghua.newsreader.data.DayReadCount
import com.lengyuefenghua.newsreader.data.ReadStat
import com.lengyuefenghua.newsreader.data.SourceDetailStat
import com.lengyuefenghua.newsreader.viewmodel.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit, viewModel: StatsViewModel = viewModel()) {
    val today by viewModel.todayStats.collectAsState()
    val week by viewModel.weekStats.collectAsState()
    val month by viewModel.monthStats.collectAsState()
    val year by viewModel.yearStats.collectAsState()
    val total by viewModel.totalStats.collectAsState()
    val trendData by viewModel.trendData.collectAsState()
    val sourceDetails by viewModel.sourceDetailStats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("阅读数据分析", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                16.dp,
                innerPadding.calculateTopPadding() + 16.dp,
                16.dp,
                32.dp
            )
        ) {
            // 1. 阅读趋势图 (Canvas 手绘)
            item {
                Text("最近 7 天阅读趋势", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                ReadingTrendChart(trendData)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 2. 统计卡片网格 (包含年度统计)
            item {
                Text("时段概览", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                StatRow(listOf("今日" to today, "本周" to week), viewModel)
                Spacer(modifier = Modifier.height(12.dp))
                StatRow(listOf("本月" to month, "本年" to year), viewModel) // [集成年度统计]
                Spacer(modifier = Modifier.height(12.dp))

                // 总计卡片 (通栏)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoGraph,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("历史总阅读量", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${total?.count ?: 0} 篇文章 / ${viewModel.formatDuration(total?.totalDuration ?: 0L)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. 订阅源排行
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text("订阅源排行", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(sourceDetails) { item ->
                SourceRankRow(item, viewModel)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

/**
 * Canvas 手绘折线趋势图
 */
@Composable
fun ReadingTrendChart(data: List<DayReadCount>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.3f
            )
        )
    ) {
        if (data.size < 2) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "数据收集积累中...",
                    color = labelColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            Canvas(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                val width = size.width
                val height = size.height
                val maxCount = (data.maxOf { it.count }.coerceAtLeast(5)).toFloat()

                val spaceX = width / (data.size - 1)
                val points = data.mapIndexed { index, dayReadCount ->
                    Offset(
                        x = index * spaceX,
                        y = height - (dayReadCount.count / maxCount * height)
                    )
                }

                // 绘制折线路径
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        // 使用三阶贝塞尔曲线让线条更平滑 (可选，这里使用直线)
                        lineTo(points[i].x, points[i].y)
                    }
                }

                drawPath(path = path, color = primaryColor, style = Stroke(width = 3.dp.toPx()))

                // 绘制数据点
                points.forEach { point ->
                    drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = point)
                }
            }
        }
    }
}

@Composable
fun StatRow(
    items: List<Pair<String, ReadStat?>>,
    viewModel: StatsViewModel
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                        alpha = 0.7f
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(item.first, style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${item.second?.count ?: 0} 篇",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        viewModel.formatDuration(item.second?.totalDuration ?: 0L),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (index < items.size - 1) Spacer(modifier = Modifier.width(12.dp))
        }
    }
}

@Composable
fun SourceRankRow(item: SourceDetailStat, viewModel: StatsViewModel) {
    // 计算阅读百分比
    val progress =
        if (item.totalCount > 0) item.readCount.toFloat() / item.totalCount.toFloat() else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                item.sourceName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = viewModel.formatDuration(item.totalReadDuration),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // 进度条显示已读比例
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.weight(1f).height(6.dp),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${item.readCount}/${item.totalCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}