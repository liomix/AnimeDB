package com.liomix.animedb.feature.console

import androidx.lifecycle.ViewModel
import com.liomix.animedb.core.source.SourceTestTab
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 测试台外壳：只负责「有哪些数据源 Tab」与「选中了哪一个」。
 *
 * 各数据源的测试状态由各自模块持有（后续接入时再下沉），
 * 这里保持无状态，避免容器成为数据源逻辑的汇聚点。
 */
@HiltViewModel
class ConsoleViewModel @Inject constructor(
    sourceTestTabs: Set<@JvmSuppressWildcards SourceTestTab>,
) : ViewModel() {

    val tabs: List<SourceTestTab> = sourceTestTabs.sortedBy(SourceTestTab::order)

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()

    fun selectTab(index: Int) {
        if (index in tabs.indices) _selectedIndex.value = index
    }
}
