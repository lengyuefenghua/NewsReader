package com.lengyuefenghua.newsreader.core.di

import com.lengyuefenghua.newsreader.viewmodel.*
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    // TimelineViewModel - 已重构，使用 NewsRepository 和 SettingsManager
    viewModel { TimelineViewModel(get(), get()) }

    // EditSourceViewModel - 需要 SourceDao
    viewModel { (sourceId: Int) -> EditSourceViewModel(get()) }

    // FavoritesViewModel - 需要 Application (暂时保持,后续改造)
    viewModel { FavoritesViewModel(get()) }

    // ProfileViewModel - 需要 Application (暂时保持,后续改造)
    viewModel { ProfileViewModel(get()) }

    // SettingsViewModel - 需要 Application (暂时保持,后续改造)
    viewModel { SettingsViewModel(get()) }

    // SourceViewModel - 需要 Application (暂时保持,后续改造)
    viewModel { SourceViewModel(get()) }

    // StatsViewModel - 需要 Application (暂时保持,后续改造)
    viewModel { StatsViewModel(get()) }
}
