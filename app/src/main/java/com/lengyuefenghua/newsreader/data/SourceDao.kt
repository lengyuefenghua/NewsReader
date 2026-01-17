package com.lengyuefenghua.newsreader.data

import androidx.room.*

import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources")
    fun getAllSources(): Flow<List<Source>>

    // 根据 ID 获取单条数据 (用于编辑回显)
    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun getSourceById(id: Int): Source?

    @Insert
    suspend fun insert(source: Source)

    // 更新数据
    @Update
    suspend fun update(source: Source)

    //删除数据
    @Delete
    suspend fun delete(source: Source)
    // 查询获取订阅源数量
    @Query("SELECT COUNT(*) FROM sources")
    fun getSourceCount(): Flow<Int>

    //    清空所有数据
    @Query("DELETE FROM sources")
    suspend fun deleteAll()
}