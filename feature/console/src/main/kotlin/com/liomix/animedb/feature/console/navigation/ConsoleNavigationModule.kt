package com.liomix.animedb.feature.console.navigation

import com.liomix.animedb.core.navigation.ConsoleDestination
import com.liomix.animedb.core.navigation.EntryProviderInstaller
import com.liomix.animedb.feature.console.ConsoleRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
object ConsoleNavigationModule {

    @Provides
    @IntoSet
    fun provideEntryProviderInstaller(): EntryProviderInstaller = {
        entry<ConsoleDestination> {
            ConsoleRoute()
        }
    }
}
