package com.lengyuefenghua.newsreader.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// [新增] 用于接收统计查询结果的数据类
data class SourceStat(
    val sourceName: String,
    val totalCount: Int,
    val readCount: Int
)

// [新增] 统计结果 DTO
data class ReadStat(
    val count: Int,
    val totalDuration: Long
)

// [新增] 订阅源详细统计 DTO
data class SourceDetailStat(
    val sourceName: String,
    val totalCount: Int,
    val readCount: Int,
    val totalReadDuration: Long
)

data class DayReadCount(
    val day: String, // 格式如 "MM-dd"
    val count: Int
)

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY pubDate DESC")
    fun getAllArticlesFlow(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE sourceName = :sourceName ORDER BY pubDate DESC")
    fun getArticlesBySourceFlow(sourceName: String): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE isFavorite = 1 ORDER BY pubDate DESC")
    fun getFavoriteArticlesFlow(): Flow<List<Article>>

    // [新增] 搜索收藏文章 (标题或摘要)
    @Query("SELECT * FROM articles WHERE isFavorite = 1 AND (title LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%') ORDER BY pubDate DESC")
    fun searchFavoriteArticles(query: String): Flow<List<Article>>

    // [新增] 统计每个源的文章数和已读数
    @Query("SELECT sourceName, COUNT(*) as totalCount, SUM(CASE WHEN isRead = 1 THEN 1 ELSE 0 END) as readCount FROM articles GROUP BY sourceName")
    fun getSourceStatsFlow(): Flow<List<SourceStat>>

    @Query("SELECT * FROM articles WHERE url = :url LIMIT 1")
    fun getArticleFlow(url: String): Flow<Article?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<Article>)

    // [修改] 标记已读时，如果之前未读，则记录当前时间戳
    @Query("UPDATE articles SET isRead = 1, readTimestamp = CASE WHEN readTimestamp = 0 THEN :timestamp ELSE readTimestamp END WHERE id = :id")
    suspend fun markAsRead(id: String, timestamp: Long = System.currentTimeMillis())

    // [新增] 更新阅读时长 (累加)
    @Query("UPDATE articles SET readDuration = readDuration + :duration WHERE id = :id")
    suspend fun updateReadDuration(id: String, duration: Long)

    // [新增] 获取指定时间范围内的阅读统计 (基于首次阅读时间)
    @Query("SELECT COUNT(*) as count, SUM(readDuration) as totalDuration FROM articles WHERE isRead = 1 AND readTimestamp >= :startTime AND readTimestamp <= :endTime")
    fun getReadStats(startTime: Long, endTime: Long): Flow<ReadStat>

    // [新增] 获取所有时间的总阅读统计
    @Query("SELECT COUNT(*) as count, SUM(readDuration) as totalDuration FROM articles WHERE isRead = 1")
    fun getTotalReadStats(): Flow<ReadStat>

    // [新增] 获取每个源的详细阅读情况
    @Query("SELECT sourceName, COUNT(*) as totalCount, SUM(CASE WHEN isRead = 1 THEN 1 ELSE 0 END) as readCount, SUM(readDuration) as totalReadDuration FROM articles GROUP BY sourceName ORDER BY totalReadDuration DESC")
    fun getSourceDetailStats(): Flow<List<SourceDetailStat>>

    // [新增] 批量标记某个源为已读
    @Query("UPDATE articles SET isRead = 1 WHERE sourceName = :sourceName")
    suspend fun markSourceAsRead(sourceName: String)

    // [新增] 批量标记某个源为未读
    @Query("UPDATE articles SET isRead = 0 WHERE sourceName = :sourceName")
    suspend fun markSourceAsUnread(sourceName: String)

    @Query("UPDATE articles SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    // [新增] 批量取消收藏 (用于多选删除)
    @Query("UPDATE articles SET isFavorite = 0 WHERE id IN (:ids)")
    suspend fun removeFavorites(ids: List<String>)

    @Query("DELETE FROM articles")
    suspend fun clearAll()

    // [新增] 缓存清理：保留最新的 limit 条，且不删除收藏的文章
    @Query(
        """
        DELETE FROM articles 
        WHERE isFavorite = 0 
        AND id NOT IN (
            SELECT id FROM articles 
            WHERE isFavorite = 0 
            ORDER BY pubDate DESC 
            LIMIT :limit
        )
    """
    )
    suspend fun cleanupCache(limit: Int)

    // [新增] 获取最近 N 天每天的阅读量统计
    @Query(
        """
        SELECT strftime('%m-%d', datetime(readTimestamp / 1000, 'unixepoch', 'localtime')) as day, 
               COUNT(*) as count 
        FROM articles 
        WHERE isRead = 1 AND readTimestamp >= :startTime
        GROUP BY day 
        ORDER BY readTimestamp ASC
    """
    )
    fun getDailyReadCounts(startTime: Long): Flow<List<DayReadCount>>
}