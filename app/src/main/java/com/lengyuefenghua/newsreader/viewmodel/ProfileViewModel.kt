package com.lengyuefenghua.newsreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lengyuefenghua.newsreader.NewsReaderApplication
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as NewsReaderApplication).database.sourceDao()

    // 实时监控订阅数量
    val sourceCount: StateFlow<Int> = dao.getSourceCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // 清空所有数据
    fun clearAllData() {
        viewModelScope.launch {
            dao.deleteAll()
        }
    }
}
