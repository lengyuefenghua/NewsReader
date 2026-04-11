package com.lengyuefenghua.newsreader.viewmodel

import android.app.Application
import android.content.Context
import android.icu.text.Transliterator
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.lengyuefenghua.newsreader.NewsReaderApplication
import com.lengyuefenghua.newsreader.data.Article
import com.lengyuefenghua.newsreader.data.NewsRepository
import com.lengyuefenghua.newsreader.data.Source
import com.lengyuefenghua.newsreader.data.SourceStat
import com.lengyuefenghua.newsreader.data.isImportableSource
import com.lengyuefenghua.newsreader.data.normalizeImportedSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale

// [新增] 导入结果数据类
data class ImportResult(
    val imported: Int = 0,      // 新导入的数量
    val skipped: Int = 0,       // 跳过的重复数量
    val updated: Int = 0,       // 覆盖更新的数量
    val error: String? = null   // 错误信息
) {
    val total: Int get() = imported + skipped + updated
    val hasError: Boolean get() = error != null
}

// [新增] 导入策略枚举
enum class ImportStrategy {
    SKIP,   // 跳过重复
    UPDATE  // 覆盖重复
}

// [新增] 包装类，包含源信息和统计信息
data class SourceWithStat(
    val source: Source,
    val total: Int = 0,
    val read: Int = 0
)

data class IndexedSourceWithStat(
    val item: SourceWithStat,
    val groupName: String,
    val sortName: String,
    val indexLetter: String,
    val searchText: String
)

data class SourceGroup(
    val name: String,
    val sources: List<IndexedSourceWithStat>
)

data class SourceManagerUiState(
    val groupedSources: List<SourceGroup> = emptyList(),
    val visibleSources: List<SourceWithStat> = emptyList(),
    val indexLetters: List<String> = emptyList(),
    val availableGroups: List<String> = emptyList()
)

class SourceViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NewsReaderApplication
    private val db = app.database
    private val dao = db.sourceDao()
    private val articleDao = db.articleDao() // 需要操作文章表
    private val repository = NewsRepository(db, app.settingsManager)

    // [修改] 开启 PrettyPrinting 和宽松解析模式
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .setLenient()  // 宽松解析，允许注释、单引号等
        .create()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // [修改] 合并 Source 和 Stat 流
    val sourcesWithStats: StateFlow<List<SourceWithStat>> = combine(
        dao.getAllSources(),
        articleDao.getSourceStatsFlow()
    ) { sources, stats ->
        // 将统计列表转为 Map 方便查找
        val statMap = stats.associateBy { it.sourceName }
        sources.map { source ->
            val stat = statMap[source.name] ?: SourceStat(source.name, 0, 0)
            SourceWithStat(source, stat.totalCount, stat.readCount)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val sourceManagerUiState: StateFlow<SourceManagerUiState> = combine(
        sourcesWithStats,
        _searchQuery
    ) { sources, query ->
        buildSourceManagerUiState(sources, query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SourceManagerUiState()
    )

    fun addSource(source: Source) {
        viewModelScope.launch { dao.insert(source) }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSourceGroup(source: Source, groupName: String) {
        viewModelScope.launch {
            dao.update(source.copy(groupName = groupName.trim()))
        }
    }

    fun renameGroup(oldGroupName: String, newGroupName: String) {
        val normalizedOldGroupName = oldGroupName.trim()
        val normalizedNewGroupName = newGroupName.trim()

        if (
            normalizedNewGroupName.isBlank() ||
            normalizedOldGroupName == normalizedNewGroupName
        ) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            dao.getAllSources().first()
                .filter { source ->
                    if (normalizedOldGroupName == DEFAULT_GROUP_NAME) {
                        source.groupName.trim().isBlank()
                    } else {
                        source.groupName.trim() == normalizedOldGroupName
                    }
                }
                .forEach { source ->
                    dao.update(source.copy(groupName = normalizedNewGroupName))
                }
        }
    }

    fun deleteSource(source: Source) {
        viewModelScope.launch {
            // 先删除该订阅源的所有文章
            articleDao.deleteArticlesBySource(source.name)
            // 再删除订阅源
            dao.delete(source)
        }
    }

    fun deleteSources(sourcesToDelete: List<Source>) {
        viewModelScope.launch {
            sourcesToDelete.forEach { source ->
                // 先删除每个订阅源的所有文章
                articleDao.deleteArticlesBySource(source.name)
            }
            // 再批量删除订阅源
            dao.deleteSources(sourcesToDelete)
        }
    }

    fun syncSource(sourceId: Int, onFinished: () -> Unit = {}) {
        viewModelScope.launch {
            repository.syncSource(sourceId)
            onFinished()
        }
    }

    // [新增] 批量标记已读
    fun markAllAsRead(sourceName: String) {
        viewModelScope.launch {
            articleDao.markSourceAsRead(sourceName)
        }
    }

    // [新增] 批量标记未读
    fun markAllAsUnread(sourceName: String) {
        viewModelScope.launch {
            articleDao.markSourceAsUnread(sourceName)
        }
    }

    // [新增] 清理孤立文章（删除不存在对应订阅源的文章）
    fun cleanupOrphanArticles(onResult: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val deletedCount = articleDao.deleteOrphanArticles()
            Log.d("SourceViewModel", "清理了 $deletedCount 条孤立文章")
            onResult(deletedCount)
        }
    }

    fun exportSourceToJson(source: Source): String = gson.toJson(source)

    fun exportSourcesToJson(sources: List<Source>): String = gson.toJson(sources)

    // [新增] 检测导入内容是否有重复
    suspend fun hasDuplicates(json: String): Boolean {
        val cleanedJson = json.replace('\u00a0', ' ').trim().trimIndent()
        return withContext(Dispatchers.IO) {
            try {
                val validSources = parseSources(cleanedJson)
                if (validSources.isEmpty()) return@withContext false

                val existingUrls = dao.getAllUrls()
                validSources.any { it.url in existingUrls }
            } catch (e: Exception) {
                false
            }
        }
    }

    // [新增] 检查单个 URL 是否已存在
    suspend fun isUrlExists(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            url in dao.getAllUrls()
        }
    }

    suspend fun fetchFeedPreview(url: String): NewsRepository.FeedPreviewResult {
        return repository.fetchFeedPreview(url)
    }

    // [修改] 导入订阅源，支持策略选择
    suspend fun importFromJson(json: String, strategy: ImportStrategy = ImportStrategy.SKIP): ImportResult {
        // 1. 替换不换行空格 (\u00a0) 为标准空格
        // 2. 去除首尾空白
        val cleanedJson = json.replace('\u00a0', ' ').trim().trimIndent()
        Log.d("SourceViewModel", "开始导入 JSON，策略: $strategy，源数据长度: ${cleanedJson.length}")

        return withContext(Dispatchers.IO) {
            try {
                val validSources = parseSources(cleanedJson)
                if (validSources.isEmpty()) {
                    Log.w("SourceViewModel", "解析失败：没有有效的订阅源")
                    return@withContext ImportResult(
                        error = "JSON 格式错误或未包含有效的订阅源数据。\n\n" +
                                "请确保 JSON 格式正确，包含必需字段：name, url\n\n" +
                                "示例格式：\n" +
                                "[{\"name\":\"订阅源名称\",\"url\":\"https://example.com/rss\"}]"
                    )
                }

                Log.d("SourceViewModel", "解析到 ${validSources.size} 个订阅源，准备写入数据库")

                try {
                    // 获取数据库中所有订阅源（用于查找重复）
                    val allExistingSources = dao.getAllSources().first()
                    val existingByUrl = allExistingSources.associateBy { it.url }

                    var importedCount = 0
                    var updatedCount = 0

                    // 根据策略处理
                    when (strategy) {
                        ImportStrategy.SKIP -> {
                            // 跳过重复：只插入不存在的
                            val newSources = validSources
                                .filter { it.url !in existingByUrl.keys }
                                .map { it.prepareForInsert() }

                            if (newSources.isNotEmpty()) {
                                dao.insertAll(newSources)
                                importedCount = newSources.size
                            }

                            Log.d("SourceViewModel", "✓ 跳过模式: 新增 $importedCount 个，跳过 ${validSources.size - newSources.size} 个")
                        }

                        ImportStrategy.UPDATE -> {
                            // 覆盖模式：更新已存在的，插入不存在的
                            val toInsert = mutableListOf<Source>()
                            val toUpdate = mutableListOf<Source>()

                            validSources.forEach { source ->
                                val existing = existingByUrl[source.url]
                                if (existing != null) {
                                    // 更新已存在的（保留原 ID）
                                    toUpdate.add(source.prepareForUpdate(existing.id))
                                } else {
                                    // 插入新的
                                    toInsert.add(source.prepareForInsert())
                                }
                            }

                            if (toInsert.isNotEmpty()) {
                                dao.insertAll(toInsert)
                                importedCount = toInsert.size
                            }

                            toUpdate.forEach { source ->
                                dao.update(source)
                                updatedCount++
                            }

                            Log.d("SourceViewModel", "✓ 覆盖模式: 新增 $importedCount 个，更新 $updatedCount 个")
                        }
                    }

                    ImportResult(
                        imported = importedCount,
                        skipped = if (strategy == ImportStrategy.SKIP) (validSources.size - importedCount) else 0,
                        updated = updatedCount
                    )
                } catch (e: Exception) {
                    Log.e("SourceViewModel", "✗ 导入失败: ${e.message}", e)
                    e.printStackTrace()
                    ImportResult(error = "数据库操作失败：${e.message}")
                }
            } catch (e: Exception) {
                Log.e("SourceViewModel", "JSON 解析失败: ${e.message}", e)
                e.printStackTrace()
                ImportResult(error = "JSON 解析失败：${e.message}\n\n请检查 JSON 格式是否正确")
            }
        }
    }

    // 辅助方法：准备插入的数据
    private fun Source.prepareForInsert() = normalizeImportedSource(this).copy(
        id = 0,
    )

    // 辅助方法：准备更新的数据
    private fun Source.prepareForUpdate(existingId: Int) = this.prepareForInsert().copy(id = existingId)

    private fun buildSourceManagerUiState(
        sources: List<SourceWithStat>,
        query: String
    ): SourceManagerUiState {
        val indexedSources = sources.map(::toIndexedSource)
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())

        val filteredSources = if (normalizedQuery.isBlank()) {
            indexedSources
        } else {
            indexedSources.filter { it.searchText.contains(normalizedQuery) }
        }

        val groupedSources = filteredSources
            .groupBy { it.groupName }
            .toSortedMap(groupComparator)
            .map { (groupName, groupSources) ->
                SourceGroup(
                    name = groupName,
                    sources = groupSources.sortedWith(indexedSourceComparator)
                )
            }

        val indexLetters = filteredSources
            .map { it.indexLetter }
            .distinct()
            .sortedWith(indexLetterComparator)

        val availableGroups = indexedSources
            .map { it.groupName }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(groupNameComparator)

        return SourceManagerUiState(
            groupedSources = groupedSources,
            visibleSources = groupedSources.flatMap { group -> group.sources.map { it.item } },
            indexLetters = indexLetters,
            availableGroups = availableGroups
        )
    }

    private fun toIndexedSource(sourceWithStat: SourceWithStat): IndexedSourceWithStat {
        val normalizedGroupName = sourceWithStat.source.groupName.trim()
        val sortName = normalizeForSearch(sourceWithStat.source.name)
        val displayGroup = normalizedGroupName.ifBlank { DEFAULT_GROUP_NAME }

        return IndexedSourceWithStat(
            item = sourceWithStat,
            groupName = displayGroup,
            sortName = sortName,
            indexLetter = extractIndexLetter(sortName),
            searchText = listOf(
                sourceWithStat.source.name,
                sourceWithStat.source.url,
                displayGroup,
                sortName
            ).joinToString("\n").lowercase(Locale.getDefault())
        )
    }

    private fun normalizeForSearch(text: String): String {
        val transliterated = transliterator.transliterate(text)
        val withoutMarks = Normalizer.normalize(transliterated, Normalizer.Form.NFD)
            .replace(COMBINING_MARKS_REGEX, "")

        return withoutMarks
            .replace(NON_ALPHANUMERIC_REGEX, " ")
            .trim()
            .lowercase(Locale.getDefault())
    }

    private fun extractIndexLetter(sortName: String): String {
        val firstLetter = sortName.firstOrNull { it.isLetter() }
        return firstLetter
            ?.uppercaseChar()
            ?.takeIf { it in 'A'..'Z' }
            ?.toString()
            ?: "#"
    }

    private fun parseSources(json: String): List<Source> {
        val parsedSources = try {
            // 尝试解析为列表
            val listType = object : TypeToken<List<Source>>() {}.type
            val sources = gson.fromJson<List<Source>>(json, listType)
            if (sources != null) {
                Log.d("SourceViewModel", "成功解析 ${sources.size} 个订阅源")
                sources
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.w("SourceViewModel", "列表解析失败，尝试单条解析: ${e.message}")
            try {
                // 尝试解析为单个对象
                val source = gson.fromJson(json, Source::class.java)
                if (source != null) {
                    Log.d("SourceViewModel", "成功解析 1 个订阅源: ${source.name}")
                    listOf(source)
                } else {
                    emptyList()
                }
            } catch (e2: Exception) {
                Log.e("SourceViewModel", "JSON 解析完全失败", e2)
                Log.e("SourceViewModel", "原始 JSON 内容: $json")
                emptyList()
            }
        }

        val normalizedSources = parsedSources.map(::normalizeImportedSource)
        val validSources = normalizedSources.filter(::isImportableSource)
        val skippedCount = normalizedSources.size - validSources.size
        if (skippedCount > 0) {
            Log.w("SourceViewModel", "忽略了 $skippedCount 个缺少 name 或 url 的无效订阅源")
        }
        return validSources
    }

    // [修改] 保存订阅源到用户选择的文件
    suspend fun saveToFile(uri: Uri, context: Context): String = withContext(Dispatchers.IO) {
        try {
            val sources = dao.getAllSources().first()
            if (sources.isEmpty()) {
                return@withContext "没有订阅源可备份"
            }

            val json = exportSourcesToJson(sources)

            // 使用 ContentResolver 写入 URI
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            } ?: return@withContext "无法写入文件"

            Log.d("SourceViewModel", "订阅源已备份到: $uri")
            "成功备份 ${sources.size} 个订阅源"
        } catch (e: Exception) {
            Log.e("SourceViewModel", "备份失败", e)
            "备份失败: ${e.message}"
        }
    }

    companion object {
        const val DEFAULT_GROUP_NAME = "未分组"

        private val transliterator: Transliterator by lazy {
            Transliterator.getInstance("Any-Latin; Latin-ASCII")
        }

        private val COMBINING_MARKS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
        private val NON_ALPHANUMERIC_REGEX = Regex("[^a-zA-Z0-9]+")

        private val indexedSourceComparator =
            compareBy<IndexedSourceWithStat>({ it.indexLetter == "#" }, { it.sortName }, { it.item.source.name.lowercase(Locale.getDefault()) })

        private val groupNameComparator =
            compareBy<String>({ normalizeLabel(it) })

        private val groupComparator = Comparator<String> { left, right ->
            when {
                left == DEFAULT_GROUP_NAME && right != DEFAULT_GROUP_NAME -> -1
                left != DEFAULT_GROUP_NAME && right == DEFAULT_GROUP_NAME -> 1
                else -> groupNameComparator.compare(left, right)
            }
        }

        private val indexLetterComparator = Comparator<String> { left, right ->
            when {
                left == right -> 0
                left == "#" -> 1
                right == "#" -> -1
                else -> left.compareTo(right)
            }
        }

        private fun normalizeLabel(value: String): String =
            Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace(COMBINING_MARKS_REGEX, "")
                .lowercase(Locale.getDefault())
    }
}
