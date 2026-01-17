package com.lengyuefenghua.newsreader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lengyuefenghua.newsreader.data.ReadStat
import com.lengyuefenghua.newsreader.data.SourceDetailStat
import com.lengyuefenghua.newsreader.viewmodel.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val today by viewModel.todayStats.collectAsState()
    val week by viewModel.weekStats.collectAsState()
    val month by viewModel.monthStats.collectAsState()
    val year by viewModel.yearStats.collectAsState()
    val total by viewModel.totalStats.collectAsState()
    val sourceDetails by viewModel.sourceDetailStats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("阅读统计") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "返回") }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("时间概览", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard(modifier = Modifier.weight(1f), title = "今日", stat = today)
                        StatCard(modifier = Modifier.weight(1f), title = "本周", stat = week)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard(modifier = Modifier.weight(1f), title = "本月", stat = month)
                        StatCard(modifier = Modifier.weight(1f), title = "本年", stat = year)
                    }
                    StatCard(modifier = Modifier.fillMaxWidth(), title = "历史总计", stat = total, isHighlight = true)
                }
            }

            item {
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("订阅源排行", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }

            items(sourceDetails) { item ->
                SourceStatRow(item)
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    stat: ReadStat?,
    isHighlight: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            val count = stat?.count ?: 0
            val duration = stat?.totalDuration ?: 0L

            Text(
                text = "$count 篇",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SourceStatRow(item: SourceDetailStat) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.sourceName, style = MaterialTheme.typography.titleSmall)
            Text(
                "总文章: ${item.totalCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "已读: ${item.readCount}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                formatDuration(item.totalReadDuration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    return when {
        seconds < 60 -> "${seconds}秒"
        seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
        else -> "${seconds / 3600}小时${(seconds % 3600) / 60}分"
    }
}