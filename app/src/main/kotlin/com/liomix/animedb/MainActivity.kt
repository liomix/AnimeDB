package com.liomix.animedb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.liomix.animedb.core.designsystem.theme.AnimeDbTheme
import com.liomix.animedb.core.navigation.EntryProviderInstaller
import com.liomix.animedb.core.navigation.Navigator
import com.liomix.animedb.navigation.AnimeDbNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var entryProviderInstallers: Set<@JvmSuppressWildcards EntryProviderInstaller>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnimeDbTheme {
                AnimeDbNavHost(
                    navigator = navigator,
                    entryProviderInstallers = entryProviderInstallers,
                )
            }
        }
    }
}
