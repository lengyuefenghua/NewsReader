package com.lengyuefenghua.newsreader.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lengyuefenghua.newsreader.NewsReaderApplication
import com.lengyuefenghua.newsreader.data.DayReadCount
import com.lengyuefenghua.newsreader.data.ReadStat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as NewsReaderApplication).database.articleDao()
    private val TAG = "StatsViewModel"

    private fun getStartTime(type: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        when (type) {
            1 -> {} // 今日
            2 -> calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek) // 本周
            3 -> calendar.set(Calendar.DAY_OF_MONTH, 1) // 本月
            4 -> calendar.set(Calendar.DAY_OF_YEAR, 1) // 本年
            5 -> calendar.add(Calendar.DAY_OF_YEAR, -6) // 最近 7 天 (图表用)
        }
        return calendar.timeInMillis
    }

    private val now = System.currentTimeMillis()

    val todayStats = dao.getReadStats(getStartTime(1), now)
        .stateIn(viewModelScope, SharingStarted.Lazily, ReadStat(0, 0))
    val weekStats = dao.getReadStats(getStartTime(2), now)
        .stateIn(viewModelScope, SharingStarted.Lazily, ReadStat(0, 0))
    val monthStats = dao.getReadStats(getStartTime(3), now)
        .stateIn(viewModelScope, SharingStarted.Lazily, ReadStat(0, 0))

    // [新增] 年度统计
    val yearStats = dao.getReadStats(getStartTime(4), now)
        .stateIn(viewModelScope, SharingStarted.Lazily, ReadStat(0, 0))
    val totalStats =
        dao.getTotalReadStats().stateIn(viewModelScope, SharingStarted.Lazily, ReadStat(0, 0))

    // [新增] 图表数据流 (最近 7 天)
    val trendData: StateFlow<List<DayReadCount>> = dao.getDailyReadCounts(getStartTime(5))
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val sourceDetailStats = dao.getSourceDetailStats()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // [调试日志] 输出详细统计数据
    init {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        Log.d(TAG, "========== 统计数据调试信息 ==========")

        // 输出当前时间和各时间范围
        val now = System.currentTimeMillis()
        Log.d(TAG, "当前时间: ${dateFormat.format(now)} (${now}ms)")

        val todayStart = getStartTime(1)
        val weekStart = getStartTime(2)
        val monthStart = getStartTime(3)
        val yearStart = getStartTime(4)
        val trendStart = getStartTime(5)

        Log.d(TAG, "今日开始时间: ${dateFormat.format(todayStart)} (${todayStart}ms)")
        Log.d(TAG, "本周开始时间: ${dateFormat.format(weekStart)} (${weekStart}ms)")
        Log.d(TAG, "本月开始时间: ${dateFormat.format(monthStart)} (${monthStart}ms)")
        Log.d(TAG, "本年开始时间: ${dateFormat.format(yearStart)} (${yearStart}ms)")
        Log.d(TAG, "趋势开始时间: ${dateFormat.format(trendStart)} (${trendStart}ms)")

        // 收集统计数据并输出
        viewModelScope.launch {
            Log.d(TAG, "---------- 收集统计数据 ----------")

            // 今日统计
            todayStats.collect { stat ->
                Log.d(TAG, "【今日统计】文章数: ${stat.count}, 阅读时长: ${formatDuration(stat.totalDuration)} (${stat.totalDuration}ms)")
            }

            // 本周统计
            weekStats.collect { stat ->
                Log.d(TAG, "【本周统计】文章数: ${stat.count}, 阅读时长: ${formatDuration(stat.totalDuration)} (${stat.totalDuration}ms)")
            }

            // 本月统计
            monthStats.collect { stat ->
                Log.d(TAG, "【本月统计】文章数: ${stat.count}, 阅读时长: ${formatDuration(stat.totalDuration)} (${stat.totalDuration}ms)")
            }

            // 本年统计
            yearStats.collect { stat ->
                Log.d(TAG, "【本年统计】文章数: ${stat.count}, 阅读时长: ${formatDuration(stat.totalDuration)} (${stat.totalDuration}ms)")
            }

            // 总计统计
            totalStats.collect { stat ->
                Log.d(TAG, "【历史总计】文章数: ${stat.count}, 阅读时长: ${formatDuration(stat.totalDuration)} (${stat.totalDuration}ms)")
            }

            // 趋势数据
            trendData.collect { data ->
                Log.d(TAG, "---------- 最近 7 天趋势数据 ----------")
                if (data.isEmpty()) {
                    Log.d(TAG, "趋势数据为空")
                } else {
                    data.forEach { dayCount ->
                        Log.d(TAG, "日期: ${dayCount.day}, 文章数: ${dayCount.count}")
                    }
                }
            }

            // 订阅源排行
            sourceDetailStats.collect { sources ->
                Log.d(TAG, "---------- 订阅源排行 ----------")
                if (sources.isEmpty()) {
                    Log.d(TAG, "订阅源数据为空")
                } else {
                    sources.forEach { source ->
                        Log.d(TAG, "订阅源: ${source.sourceName}, 已读: ${source.readCount}/${source.totalCount}, 阅读时长: ${formatDuration(source.totalReadDuration)}")
                    }
                }
                Log.d(TAG, "=======================================")
            }
        }
    }

    fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        return if (seconds < 3600) "${seconds / 60}分" else "${seconds / 3600}时${(seconds % 3600) / 60}分"
    }
}