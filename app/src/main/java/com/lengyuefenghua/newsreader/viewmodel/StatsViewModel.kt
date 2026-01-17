package com.lengyuefenghua.newsreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lengyuefenghua.newsreader.NewsReaderApplication
import com.lengyuefenghua.newsreader.data.ReadStat
import com.lengyuefenghua.newsreader.data.SourceDetailStat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as NewsReaderApplication).database.articleDao()

    private fun getStartOfDay(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private val today = LocalDate.now()
    private val startOfToday = getStartOfDay(today)
    private val startOfWeek = getStartOfDay(today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)))
    private val startOfMonth = getStartOfDay(today.withDayOfMonth(1))
    private val startOfYear = getStartOfDay(today.withDayOfYear(1))
    private val now = System.currentTimeMillis()

    val todayStats: StateFlow<ReadStat?> = dao.getReadStats(startOfToday, now)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val weekStats: StateFlow<ReadStat?> = dao.getReadStats(startOfWeek, now)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val monthStats: StateFlow<ReadStat?> = dao.getReadStats(startOfMonth, now)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val yearStats: StateFlow<ReadStat?> = dao.getReadStats(startOfYear, now)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val totalStats: StateFlow<ReadStat?> = dao.getTotalReadStats()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val sourceDetailStats: StateFlow<List<SourceDetailStat>> = dao.getSourceDetailStats()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}