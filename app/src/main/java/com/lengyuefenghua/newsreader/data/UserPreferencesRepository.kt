package com.lengyuefenghua.newsreader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val AUTO_UPDATE = booleanPreferencesKey("auto_update")
        val CACHE_LIMIT = intPreferencesKey("cache_limit")
        const val DEFAULT_CACHE_LIMIT = 1000
    }

    val autoUpdateFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_UPDATE] ?: false
        }

    val cacheLimitFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[CACHE_LIMIT] ?: DEFAULT_CACHE_LIMIT
        }

    suspend fun setAutoUpdate(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_UPDATE] = enabled
        }
    }

    suspend fun setCacheLimit(limit: Int) {
        context.dataStore.edit { preferences ->
            preferences[CACHE_LIMIT] = limit
        }
    }
}