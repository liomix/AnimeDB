package com.liomix.animedb.source.anicyk

import com.liomix.animedb.core.ui.model.ConsoleOutput
import com.liomix.animedb.core.ui.model.OutputMode
import com.liomix.animedb.source.anicyk.data.CykApi
import com.liomix.animedb.source.anicyk.data.CykView

data class CykUiState(
    val searchType: String = "DEFAULT",
    val scheduleType: String = "超清",
    val keyword: String = "火影",
    val page: Int = 0,
    val query: String = "",
    val runningKey: String? = null,
    val lastNode: String = "",
    val lastHttp: Int = 0,
    val lastMs: Long = 0,
    val encrypted: Boolean = false,
    val output: ConsoleOutput = ConsoleOutput(),
    val outputMode: OutputMode = OutputMode.Structured,
    val panelExpanded: Boolean = false,
    val view: CykView = CykView.None,
    val paramsApi: CykApi? = null,
    val paramsText: String = "",
    val footer: String = "4 个节点 · 匿名只读接口",
) {
    val busy: Boolean get() = runningKey != null

    val nodeLabel: String
        get() = lastNode.removePrefix("http://").ifBlank { "未连接" }
}
