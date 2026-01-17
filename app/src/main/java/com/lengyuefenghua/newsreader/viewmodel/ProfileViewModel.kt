package com.lengyuefenghua.newsreader.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
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

    // 打开 GitHub 项目主页 (模拟 "关于" 功能)
    fun openProjectLink() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/gedoor/legado"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }
}