package com.lengyuefenghua.newsreader.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lengyuefenghua.newsreader.R
import com.lengyuefenghua.newsreader.ui.common.FilterType
import com.lengyuefenghua.newsreader.ui.common.displayName
import com.lengyuefenghua.newsreader.util.buildDataBackupFileName
import com.lengyuefenghua.newsreader.util.SettingsManager
import com.lengyuefenghua.newsreader.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.abs
import kotlin.math.roundToInt

private val DISCRETE_SLIDER_VALUE_WIDTH = 40.dp

@Composable
private fun DiscreteDotSlider(
    value: Float,
    anchorCount: Int,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape
) {
    val selectedIndex = value.roundToInt().coerceIn(0, anchorCount - 1)
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant

    BoxWithConstraints(
        modifier = modifier.height(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(anchorCount) { index ->
                if (index > 0) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(
                                color = if (index <= selectedIndex) activeColor else inactiveColor,
                                shape = shape
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .width(if (index == selectedIndex) 16.dp else 12.dp)
                        .height(if (index == selectedIndex) 16.dp else 12.dp)
                        .clip(shape)
                        .background(if (index == selectedIndex) activeColor else inactiveColor)
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..(anchorCount - 1).toFloat(),
            steps = anchorCount - 2,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun SettingRow(
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
    settingsManager: SettingsManager = koinInject()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val autoUpdate by viewModel.autoUpdate.collectAsState()
    val cacheLimit by viewModel.cacheLimit.collectAsState()

    var concurrentCount by remember {
        mutableIntStateOf(snapConcurrentCountToAnchor(settingsManager.getConcurrentCount()))
    }
    var sourceTimeoutSeconds by remember {
        mutableIntStateOf(snapSourceTimeoutSecondsToAnchor(settingsManager.getSourceTimeoutSeconds()))
    }
    var defaultFilter by remember { mutableStateOf(settingsManager.getDefaultFilterType()) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val snappedCacheLimit = snapCacheLimitToAnchor(cacheLimit)
    val cacheLimitSliderIndex = cacheLimitAnchorIndex(cacheLimit).toFloat()
    val concurrentSliderIndex = concurrentCountAnchorIndex(concurrentCount).toFloat()
    val sourceTimeoutSliderIndex = sourceTimeoutSecondsAnchorIndex(sourceTimeoutSeconds).toFloat()

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val msg = viewModel.saveBackupToFile(uri, context)
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val data = viewModel.loadBackupFromFile(uri, context)
                    if (data != null) {
                        pendingRestoreUri = uri
                        showRestoreDialog = true
                    } else {
                        Toast.makeText(context, "备份文件格式错误", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "读取失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.desc_back))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { focusManager.clearFocus() }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 自动更新
                SettingRow(label = "自动更新") {
                    Switch(
                        checked = autoUpdate,
                        onCheckedChange = {
                            focusManager.clearFocus()
                            viewModel.setAutoUpdate(it)
                        }
                    )
                }

                // 并发刷新
                SettingRow(label = "并发刷新") {
                    Row(
                        modifier = Modifier.width(220.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "$concurrentCount",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.width(DISCRETE_SLIDER_VALUE_WIDTH)
                        )
                        DiscreteDotSlider(
                            value = concurrentSliderIndex,
                            anchorCount = CONCURRENT_COUNT_ANCHORS.size,
                            onValueChange = { sliderValue ->
                                focusManager.clearFocus()
                                val newCount = concurrentCountFromSliderIndex(sliderValue)
                                if (newCount != concurrentCount) {
                                    concurrentCount = newCount
                                    settingsManager.setConcurrentCount(newCount)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                SettingRow(label = "订阅源更新超时") {
                    Row(
                        modifier = Modifier.width(220.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "${sourceTimeoutSeconds}s",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.width(DISCRETE_SLIDER_VALUE_WIDTH)
                        )
                        DiscreteDotSlider(
                            value = sourceTimeoutSliderIndex,
                            anchorCount = SOURCE_TIMEOUT_SECONDS_ANCHORS.size,
                            onValueChange = { sliderValue ->
                                focusManager.clearFocus()
                                val newTimeout = sourceTimeoutSecondsFromSliderIndex(sliderValue)
                                if (newTimeout != sourceTimeoutSeconds) {
                                    sourceTimeoutSeconds = newTimeout
                                    settingsManager.setSourceTimeoutSeconds(newTimeout)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 默认筛选
                SettingRow(label = "默认筛选") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterType.values().forEach { type ->
                            FilterChip(
                                selected = defaultFilter == type,
                                onClick = {
                                    focusManager.clearFocus()
                                    defaultFilter = type
                                    settingsManager.setDefaultFilterType(type)
                                },
                                label = { Text(type.displayName, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }
                }

                // 文章保留
                SettingRow(label = "文章保留") {
                    Row(
                        modifier = Modifier.width(220.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = snappedCacheLimit.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(DISCRETE_SLIDER_VALUE_WIDTH)
                        )
                        DiscreteDotSlider(
                            value = cacheLimitSliderIndex,
                            anchorCount = CACHE_LIMIT_ANCHORS.size,
                            onValueChange = { sliderValue ->
                                focusManager.clearFocus()
                                viewModel.setCacheLimit(cacheLimitFromSliderIndex(sliderValue))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 清理缓存
                SettingRow(label = "清理缓存") {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.clearCacheNow()
                        },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("清理", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // 数据备份
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            backupLauncher.launch(buildDataBackupFileName())
                        },
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text("备份", style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            restoreLauncher.launch(arrayOf("application/json"))
                        },
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text("恢复", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    // 恢复确认对话框
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("确认恢复") },
            text = { Text("恢复将覆盖所有数据，是否继续？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val uri = pendingRestoreUri ?: return@launch
                            viewModel.loadBackupFromFile(uri, context)?.let {
                                Toast.makeText(context, viewModel.restoreBackup(it), Toast.LENGTH_LONG).show()
                            }
                            showRestoreDialog = false
                            pendingRestoreUri = null
                        }
                    }
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        pendingRestoreUri = null
                    }
                ) { Text("取消") }
            }
        )
    }
}

internal fun syncCacheLimitDraft(currentDraft: String, persistedLimit: Int): String {
    val persistedText = persistedLimit.toString()
    return if (currentDraft == persistedText) currentDraft else persistedText
}

internal val CACHE_LIMIT_ANCHORS = listOf(10, 50, 100, 500, 1000)
internal val CONCURRENT_COUNT_ANCHORS = listOf(1, 2, 3, 4, 5)
internal val SOURCE_TIMEOUT_SECONDS_ANCHORS = listOf(5, 10, 15, 30, 60)

internal fun snapToNearestAnchor(value: Int, anchors: List<Int>): Int {
    return anchors.minByOrNull { abs(it - value) } ?: anchors.first()
}

internal fun snapCacheLimitToAnchor(value: Int): Int {
    return snapToNearestAnchor(value, CACHE_LIMIT_ANCHORS)
}

internal fun cacheLimitAnchorIndex(value: Int): Int {
    return CACHE_LIMIT_ANCHORS.indexOf(snapCacheLimitToAnchor(value)).coerceAtLeast(0)
}

internal fun cacheLimitFromSliderIndex(index: Float): Int {
    val snappedIndex = index.roundToInt().coerceIn(0, CACHE_LIMIT_ANCHORS.lastIndex)
    return CACHE_LIMIT_ANCHORS[snappedIndex]
}

internal fun snapConcurrentCountToAnchor(value: Int): Int {
    return snapToNearestAnchor(value, CONCURRENT_COUNT_ANCHORS)
}

internal fun concurrentCountAnchorIndex(value: Int): Int {
    return CONCURRENT_COUNT_ANCHORS.indexOf(snapConcurrentCountToAnchor(value)).coerceAtLeast(0)
}

internal fun concurrentCountFromSliderIndex(index: Float): Int {
    val snappedIndex = index.roundToInt().coerceIn(0, CONCURRENT_COUNT_ANCHORS.lastIndex)
    return CONCURRENT_COUNT_ANCHORS[snappedIndex]
}

internal fun snapSourceTimeoutSecondsToAnchor(value: Int): Int {
    return snapToNearestAnchor(value, SOURCE_TIMEOUT_SECONDS_ANCHORS)
}

internal fun sourceTimeoutSecondsAnchorIndex(value: Int): Int {
    return SOURCE_TIMEOUT_SECONDS_ANCHORS.indexOf(snapSourceTimeoutSecondsToAnchor(value)).coerceAtLeast(0)
}

internal fun sourceTimeoutSecondsFromSliderIndex(index: Float): Int {
    val snappedIndex = index.roundToInt().coerceIn(0, SOURCE_TIMEOUT_SECONDS_ANCHORS.lastIndex)
    return SOURCE_TIMEOUT_SECONDS_ANCHORS[snappedIndex]
}

internal fun shouldClearCacheLimitFocusOnImeDone(): Boolean = false

internal fun shouldUseOutlinedCacheLimitField(): Boolean = false

internal fun discreteSliderValueWidthDp(): Int = 40

internal fun concurrentCountValueWidthDp(): Int = discreteSliderValueWidthDp()

internal fun cacheLimitValueWidthDp(): Int = discreteSliderValueWidthDp()

internal fun sourceTimeoutValueWidthDp(): Int = discreteSliderValueWidthDp()
