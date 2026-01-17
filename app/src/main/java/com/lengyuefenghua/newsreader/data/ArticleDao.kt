package com.lengyuefenghua.newsreader.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    // 核心：返回 Flow，数据一变，UI 自动变。按发布时间倒序排列。
    @Query("SELECT * FROM articles ORDER BY pubDate DESC")
    fun getAllArticlesFlow(): Flow<List<Article>>

    // 插入文章，如果 ID (URL) 已存在则忽略，避免重复
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<Article>)

    @Query("UPDATE articles SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    // 切换收藏状态
    @Query("UPDATE articles SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)
    // 清空缓存
    @Query("DELETE FROM articles")
    suspend fun clearAll()
}