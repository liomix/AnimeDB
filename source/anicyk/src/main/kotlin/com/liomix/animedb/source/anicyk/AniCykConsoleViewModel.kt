package com.liomix.animedb.source.anicyk

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.liomix.animedb.core.common.dispatcher.AnimeDbDispatchers
import com.liomix.animedb.core.common.dispatcher.Dispatcher
import com.liomix.animedb.core.common.format.formatBytes
import com.liomix.animedb.core.common.format.formatMs
import com.liomix.animedb.core.ui.model.ConsoleOutput
import com.liomix.animedb.core.ui.model.OutputMode
import com.liomix.animedb.core.ui.model.OutputStatus
import com.liomix.animedb.source.anicyk.data.AniCykClient
import com.liomix.animedb.source.anicyk.data.CykApi
import com.liomix.animedb.source.anicyk.data.CykCatalog
import com.liomix.animedb.source.anicyk.data.CykParser
import com.liomix.animedb.source.anicyk.data.CykQuery
import com.liomix.animedb.source.anicyk.data.CykView
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** AniCyk 测试台：HTTP 直连 + 节点降级 + AES 解密。 */
@HiltViewModel
class AniCykConsoleViewModel @Inject constructor(
    private val client: AniCykClient,
    @Dispatcher(AnimeDbDispatchers.IO) private val io: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(CykUiState())
    val state: StateFlow<CykUiState> = _state.asStateFlow()

    init {
        quickLoad("config")
    }

    private val queryContext: CykQuery
        get() = _state.value.let {
            CykQuery(
                searchType = it.searchType,
                scheduleType = it.scheduleType,
                keyword = it.keyword.ifBlank { "火影" },
                page = it.page,
            )
        }

    fun setSearchType(type: String) {
        _state.update { it.copy(searchType = type, footer = "搜索类型 · $type") }
    }

    fun setScheduleType(type: String) {
        _state.update { it.copy(scheduleType = type, footer = "日历源 · $type") }
    }

    fun setKeyword(keyword: String) {
        _state.update { it.copy(keyword = keyword) }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun setMode(mode: OutputMode) {
        _state.update { it.copy(outputMode = mode) }
    }

    fun togglePanel() {
        _state.update { it.copy(panelExpanded = !it.panelExpanded) }
    }

    fun quickLoad(key: String) {
        CykCatalog.apis.firstOrNull { it.key == key }?.let { run(it) }
    }

    // ------------------------------------------------------------------
    // 参数面板
    // ------------------------------------------------------------------

    fun openParams(api: CykApi) {
        _state.update {
            it.copy(paramsApi = api, paramsText = api.params(queryContext).toString(2))
        }
    }

    fun updateParams(text: String) {
        _state.update { it.copy(paramsText = text) }
    }

    fun resetParams() {
        _state.update { current ->
            val api = current.paramsApi ?: return@update current
            current.copy(paramsText = api.params(queryContext).toString(2))
        }
    }

    fun closeParams() {
        _state.update { it.copy(paramsApi = null) }
    }

    fun runFromParams() {
        val current = _state.value
        val api = current.paramsApi ?: return
        val params = runCatching { JSONObject(current.paramsText.trim()) }.getOrNull()
        if (params == null) {
            _state.update { it.copy(footer = "参数不是合法 JSON") }
            return
        }
        closeParams()
        run(api, params)
    }

    // ------------------------------------------------------------------
    // 请求
    // ------------------------------------------------------------------

    fun run(api: CykApi, paramsOverride: JSONObject? = null) {
        if (_state.value.busy) {
            _state.update { it.copy(footer = "上一个请求还没结束") }
            return
        }
        val params = paramsOverride ?: api.params(queryContext)

        _state.update {
            it.copy(
                runningKey = api.key,
                footer = "${api.method} ${api.path} …",
                output = it.output.copy(
                    title = api.title,
                    meta = "请求中 · ${api.method} ${api.path}",
                    error = null,
                    status = OutputStatus.Running,
                ),
            )
        }

        viewModelScope.launch {
            val started = SystemClock.elapsedRealtime()
            val outcome = withContext(io) {
                runCatching { client.request(api.method, api.path, params) }
            }
            val elapsed = SystemClock.elapsedRealtime() - started

            outcome.onSuccess { result ->
                val bytes = result.body.toByteArray(Charsets.UTF_8).size
                val code = runCatching { JSONObject(result.body) }.getOrNull()?.optInt("code", -1) ?: -1
                val ok = result.httpCode in 200..299

                _state.update {
                    it.copy(
                        runningKey = null,
                        lastNode = result.node,
                        lastHttp = result.httpCode,
                        lastMs = result.ms,
                        encrypted = result.encrypted,
                        output = ConsoleOutput(
                            title = api.title,
                            meta = "${api.method} ${api.path} · HTTP ${result.httpCode} · " +
                                "${formatBytes(bytes)} · ${formatMs(result.ms)}" +
                                if (result.encrypted) " · AES ✓" else "",
                            json = result.body,
                            error = if (ok) null else "HTTP ${result.httpCode} · ${result.body.take(160)}",
                            status = if (ok) OutputStatus.Ok else OutputStatus.Failed,
                        ),
                        view = CykParser.parse(api.key, result.body),
                        footer = "${api.key} · HTTP ${result.httpCode} · ${formatMs(result.ms)}" +
                            if (code >= 0) " · code $code" else "",
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        runningKey = null,
                        lastHttp = 0,
                        output = ConsoleOutput(
                            title = api.title,
                            meta = "${formatMs(elapsed)} · 全部节点失败",
                            error = "${error.javaClass.simpleName}: ${error.message}",
                            status = OutputStatus.Failed,
                        ),
                        view = CykView.None,
                        footer = "${api.key} 失败 · ${formatMs(elapsed)}",
                    )
                }
            }
        }
    }
}
