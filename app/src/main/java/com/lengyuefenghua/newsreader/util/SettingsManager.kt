package com.lengyuefenghua.newsreader.util

import android.content.Context
import android.content.SharedPreferences
import com.lengyuefenghua.newsreader.ui.common.FilterType

/**
 * 设置管理器
 *
 * 负责管理应用的 SharedPreferences 设置,提供类型安全的 API
 */
class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * 获取默认筛选类型
     *
     * @return 用户设置的默认筛选器,工厂默认为 FilterType.UNREAD
     */
    fun getDefaultFilterType(): FilterType {
        val value = prefs.getString(KEY_DEFAULT_FILTER_TYPE, null)
        return when (value) {
            "ALL" -> FilterType.ALL
            "READ" -> FilterType.READ
            else -> FilterType.UNREAD  // 工厂默认
        }
    }

    /**
     * 设置默认筛选类型
     *
     * @param type 筛选器类型
     */
    fun setDefaultFilterType(type: FilterType) {
        prefs.edit().putString(KEY_DEFAULT_FILTER_TYPE, type.name).commit()
    }

    /**
     * 获取刷新并发数
     *
     * @return 用户设置的并发数，工厂默认为 3
     */
    fun getConcurrentCount(): Int {
        val value = prefs.getInt(KEY_CONCURRENT_COUNT, DEFAULT_CONCURRENT_COUNT)
        // 边界值验证，确保在合理范围内
        return value.coerceIn(MIN_CONCURRENT_COUNT, MAX_CONCURRENT_COUNT)
    }

    /**
     * 设置刷新并发数
     *
     * @param count 并发数（会自动限制在 1-5 范围内）
     */
    fun setConcurrentCount(count: Int) {
        val validCount = count.coerceIn(MIN_CONCURRENT_COUNT, MAX_CONCURRENT_COUNT)
        prefs.edit().putInt(KEY_CONCURRENT_COUNT, validCount).commit()
    }

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_DEFAULT_FILTER_TYPE = "default_filter_type"
        private const val KEY_CONCURRENT_COUNT = "refresh_concurrent_count"

        // 并发数配置常量
        private const val DEFAULT_CONCURRENT_COUNT = 3
        private const val MIN_CONCURRENT_COUNT = 1
        private const val MAX_CONCURRENT_COUNT = 5
    }
}
