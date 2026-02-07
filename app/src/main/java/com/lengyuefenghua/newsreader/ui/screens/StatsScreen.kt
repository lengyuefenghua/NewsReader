package com.lengyuefenghua.newsreader.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.lengyuefenghua.newsreader.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lengyuefenghua.newsreader.data.DayReadCount
import com.lengyuefenghua.newsreader.data.ReadStat
import com.lengyuefenghua.newsreader.data.SourceDetailStat
import com.lengyuefenghua.newsreader.viewmodel.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onBack: () -> Unit, viewModel: StatsViewModel = viewModel()) {
    val context = LocalContext.current
    val today by viewModel.todayStats.collectAsState()
    val week by viewModel.weekStats.collectAsState()
    val month by viewModel.monthStats.collectAsState()
    val year by viewModel.yearStats.collectAsState()
    val total by viewModel.totalStats.collectAsState()
    val trendData by viewModel.trendData.collectAsState()
    val sourceDetails by viewModel.sourceDetailStats.collectAsState()

    // Pre-load content description strings
    val descStatCard = context.getString(R.string.desc_stat_card)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("阅读数据分析", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.desc_back)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = descStatCard },
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
 * Canvas 手绘折线趋势图（带坐标轴）
 * X轴始终显示最近 7 天（从今天往前）
 */
@Composable
fun ReadingTrendChart(data: List<DayReadCount>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.outline

    // [调试日志] 输出接收到的数据
    android.util.Log.d("ReadingTrendChart", "接收到的数据量: ${data.size}")
    data.forEach { android.util.Log.d("ReadingTrendChart", "  日期: ${it.day}, 数量: ${it.count}") }

    // 生成最近 7 天的日期（从今天往前）
    // 使用 remember 并添加 data 作为 key，让 data 变化时重新计算
    val last7Days = remember(data.size) {
        android.util.Log.d("ReadingTrendChart", "重新计算 last7Days，data.size = ${data.size}")
        val days = mutableListOf<Pair<String, Int>>() // Pair(日期字符串, count)
        val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        for (i in 6 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis() - i * 24 * 60 * 60 * 1000
            val dateStr = dateFormat.format(calendar.time)

            // 查找该日期的数据
            val count = data.find { it.day == dateStr }?.count ?: 0
            android.util.Log.d("ReadingTrendChart", "  日期 $dateStr -> 数量 $count")
            days.add(dateStr to count)
        }
        days.toList()
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.3f
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 图表区域
            Canvas(
                modifier = Modifier.fillMaxSize().padding(horizontal = 50.dp, vertical = 16.dp).padding(bottom = 30.dp)
            ) {
                val width = size.width
                val height = size.height
                val maxCount = (last7Days.maxOf { it.second }.coerceAtLeast(5)).toFloat()

                val spaceX = width / (last7Days.size - 1)
                val points = last7Days.mapIndexed { index, pair ->
                    Offset(
                        x = index * spaceX,
                        y = height - (pair.second / maxCount * height)
                    )
                }

                // 绘制网格线（水平）
                for (i in 1..4) {
                    val y = height - (i.toFloat() / 4) * height
                    drawLine(
                        color = labelColor.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 绘制折线路径
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }

                drawPath(path = path, color = primaryColor, style = Stroke(width = 3.dp.toPx()))

                // 绘制数据点
                points.forEach { point ->
                    drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = point)
                }
            }

            // X轴标签（日期）- 固定在底部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 50.dp, end = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                last7Days.forEach { (dateStr, _) ->
                    Text(
                        dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Y轴标签（数量）
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 8.dp, top = 16.dp, bottom = 46.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val maxCount = (last7Days.maxOf { it.second }.coerceAtLeast(5)).toFloat()
                val ySteps = 4
                for (i in 0..ySteps) {
                    val value = ((maxCount / ySteps) * (ySteps - i)).toInt()
                    Text(
                        value.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
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