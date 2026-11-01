package com.liomix.animedb.source.anirpb.di

import com.liomix.animedb.core.source.SourceTestTab
import com.liomix.animedb.source.anirpb.AniRpbTestTab
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** 向测试台注册 AniRpb 的 Tab；新增数据源不需要改动 app / console 模块。 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AniRpbModule {

    @Binds
    @IntoSet
    abstract fun bindSourceTestTab(tab: AniRpbTestTab): SourceTestTab
}
