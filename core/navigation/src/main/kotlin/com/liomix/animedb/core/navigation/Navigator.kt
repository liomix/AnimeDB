package com.liomix.animedb.core.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.scopes.ActivityRetainedScoped

/** 处理导航事件（前进 / 返回），更新导航状态。 */
@ActivityRetainedScoped
class Navigator(startDestination: NavKey) {

    val backStack: SnapshotStateList<NavKey> = mutableStateListOf(startDestination)

    /** 压入二级目的地（如请求详情）。 */
    fun goTo(destination: NavKey) {
        backStack.add(destination)
    }

    /**
     * 返回上一页。
     *
     * 返回栈必须至少保留一个目的地（`NavDisplay` 不接受空栈）；
     * 根页面的返回交给系统处理。
     *
     * @return 是否消费了本次返回（false 表示已在根页面）。
     */
    fun goBack(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeLastOrNull()
        return true
    }
}
