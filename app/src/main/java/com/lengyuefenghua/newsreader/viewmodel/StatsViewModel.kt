package com.lengyuefenghua.newsreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lengyuefenghua.newsreader.NewsReaderApplication
import com.lengyuefenghua.newsreader.data.DayReadCount
import com.lengyuefenghua.newsreader.data.ReadStat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as NewsReaderApplication).database.articleDao()

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

    val todayStats = dao.getReadStats(getStartTime(1), now).stateIn(viewModelScope, SharingStarted.Lazily, ReadStat(0, 0))
    val weekStats = dao.getReadStats(getStartTime(2), now).stateIn(viewModelScope, SharingStarted.Lazily, ReadStat(0, 0))
    val monthStats = dao.getReadStats(getStartTime(3), now).stateIn(viewModelScope, SharingStarted.Lazily, ReadStat(0, 0))
    // [新增] 年度统计
    val yearStats = dao.getReadStats(getStartTime(4), now).stateIn(viewModelScope, SharingStarted.Lazily, ReadStat(0, 0))
    val totalStats = dao.getTotalReadStats().stateIn(viewModelScope, SharingStarted.Lazily, ReadStat(0, 0))

    // [新增] 图表数据流 (最近 7 天)
    val trendData: StateFlow<List<DayReadCount>> = dao.getDailyReadCounts(getStartTime(5))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sourceDetailStats = dao.getSourceDetailStats().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        return if (seconds < 3600) "${seconds / 60}分" else "${seconds / 3600}时${(seconds % 3600) / 60}分"
    }
}