package com.liomix.animedb.source.anicyk.di

import com.liomix.animedb.core.source.SourceTestTab
import com.liomix.animedb.source.anicyk.ui.AniCykTestTab
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** 向测试台注册 AniCyk 的 Tab。 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AniCykModule {

    @Binds
    @IntoSet
    abstract fun bindSourceTestTab(tab: AniCykTestTab): SourceTestTab
}
