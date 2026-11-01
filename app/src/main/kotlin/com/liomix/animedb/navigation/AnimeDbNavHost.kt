package com.liomix.animedb.navigation

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.liomix.animedb.core.navigation.EntryProviderInstaller
import com.liomix.animedb.core.navigation.Navigator

/**
 * 导航宿主：装配 NavDisplay + 各 feature 通过 Hilt 注入的 entryProvider。
 *
 * app 模块因此不需要 import 任何页面实现，多模块装配点只有 [EntryProviderInstaller]。
 */
@Composable
fun AnimeDbNavHost(
    navigator: Navigator,
    entryProviderInstallers: Set<@JvmSuppressWildcards EntryProviderInstaller>,
    modifier: Modifier = Modifier,
) {
    val navEntryProvider = remember(entryProviderInstallers) {
        entryProvider {
            entryProviderInstallers.forEach { installer ->
                installer()
            }
        }
    }

    val view = LocalView.current
    val onBack: () -> Unit = {
        if (!navigator.goBack()) {
            (view.context as? Activity)?.finish()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NavDisplay(
            backStack = navigator.backStack,
            onBack = onBack,
            entryProvider = navEntryProvider,
        )
    }
}
