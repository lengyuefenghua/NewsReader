package com.lengyuefenghua.newsreader.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lengyuefenghua.newsreader.data.Source
import com.lengyuefenghua.newsreader.data.SourceDao
import kotlinx.coroutines.launch


class EditSourceViewModel(private val sourceDao: SourceDao) : ViewModel() {

    // --- 表单状态 (使用 mutableStateOf 以便 Compose 监听) ---
    var name by mutableStateOf("")
    var url by mutableStateOf("")

    // 0=RSS/混合, 1=完全自定义
    var selectedTab by mutableIntStateOf(0)

    // RSS 增强
    var useAutoExtract by mutableStateOf(false)
    var ruleContent by mutableStateOf("")

    // 自定义爬虫配置
    var requestMethod by mutableStateOf(false) // 0=HTTP, 1=WebView
    var enablePcUserAgent by mutableStateOf(false) // 是否模拟PC
    var ruleList by mutableStateOf("")
    var ruleTitle by mutableStateOf("")
    var ruleLink by mutableStateOf("")
    var ruleSummary by mutableStateOf("")
    var ruleImage by mutableStateOf("")

    // 记录正在编辑的 ID (-1 表示新增)
    private var currentId: Int = -1
    private var isDataLoaded = false


    // [核心修复 1]：真正从数据库加载数据
    fun loadSourceIfNeed(id: Int) {
        // 如果是新增(-1) 或者 数据已经加载过了，就直接跳过
        if (id == -1 || isDataLoaded) {
            currentId = id
            return
        }

        viewModelScope.launch {
            // 调用刚刚在 Dao 里写的方法
            val source = sourceDao.getSourceById(id)
            if (source != null) {
                // 拿到数据后，填入输入框
                currentId = source.id
                name = source.name
                url = source.url

                // 恢复模式选择
                selectedTab = if (source.isCustom) 1 else 0

                // 恢复 RSS 增强选项
                useAutoExtract = source.useAutoExtract
                ruleContent = source.ruleContent

                // 恢复自定义规则
                requestMethod = source.requestMethod
                enablePcUserAgent = source.enablePcUserAgent
                ruleList = source.ruleList
                ruleTitle = source.ruleTitle
                ruleLink = source.ruleLink
                ruleSummary = source.ruleSummary
                ruleImage = source.ruleImage
            }
            // 标记已加载，防止页面刷新时被重置
            isDataLoaded = true
        }
    }

    // --- 构建 Source 对象 (用于调试或保存) ---
    fun buildSource(): Source {
        return Source(
            id = if (currentId == -1) 0 else currentId,
            name = name,
            url = url,
            isCustom = (selectedTab == 1),
            requestMethod = requestMethod,
            enablePcUserAgent = enablePcUserAgent,
            ruleList = ruleList,
            ruleTitle = ruleTitle,
            ruleLink = ruleLink,
            ruleSummary = ruleSummary,
            ruleImage = ruleImage,
            ruleContent = ruleContent,
            useAutoExtract = useAutoExtract
        )
    }

    // --- 保存到数据库 ---
    fun saveSource() {
        viewModelScope.launch {
            val source = buildSource()
            if (currentId == -1 || currentId == 0) {
                // ID 不存在，说明是新增
                sourceDao.insert(source)
            } else {
                // ID 存在，说明是编辑，调用 update
                sourceDao.update(source)
            }
        }
    }
}

// --- ViewModel 工厂 (用于注入 DAO) ---
class EditSourceViewModelFactory(private val dao: SourceDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditSourceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditSourceViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}