package com.lengyuefenghua.newsreader.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources")
    fun getAllSources(): Flow<List<Source>>

    // [新增] 获取所有订阅源（用于备份）
    @Query("SELECT * FROM sources")
    suspend fun getAllSourcesList(): List<Source>

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun getSourceById(id: Int): Source?

    // 根据名称查找源 ID (用于文章页跳转)
    @Query("SELECT id FROM sources WHERE name = :name LIMIT 1")
    suspend fun getSourceIdByName(name: String): Int?

    // 查询所有订阅源的 URL（用于导入时去重）
    @Query("SELECT url FROM sources")
    suspend fun getAllUrls(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: Source)

    // [新增] 批量插入，用于导入
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sources: List<Source>)

    @Update
    suspend fun update(source: Source)

    @Delete
    suspend fun delete(source: Source)

    // [新增] 批量删除
    @Delete
    suspend fun deleteSources(sources: List<Source>)

    @Query("SELECT COUNT(*) FROM sources")
    fun getSourceCount(): Flow<Int>

    @Query("DELETE FROM sources")
    suspend fun deleteAll()

    // [新增] 删除所有订阅源（用于备份恢复）
    @Query("DELETE FROM sources")
    suspend fun deleteAllSources()
}