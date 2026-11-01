package com.liomix.animedb.source.anirpb

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
import com.liomix.animedb.source.anirpb.data.RpbApi
import com.liomix.animedb.source.anirpb.data.RpbCatalog
import com.liomix.animedb.source.anirpb.data.RpbParser
import com.liomix.animedb.source.anirpb.data.RpbProbeRow
import com.liomix.animedb.source.anirpb.data.RpbView
import com.liomix.animedb.source.anirpb.transport.ZhuqueTransport
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * AniRpb 测试台状态机：链路自检 → 单接口运行 → 全量探测。
 *
 * 传输层是阻塞 API（UDS + AES-GCM），所有调用都在 IO 线程，UI 只观察 [state]。
 */
@HiltViewModel
class AniRpbConsoleViewModel @Inject constructor(
    private val transport: ZhuqueTransport,
    @Dispatcher(AnimeDbDispatchers.IO) private val io: CoroutineDispatcher,
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _state = MutableStateFlow(RpbUiState(date = dateFormat.format(Date())))
    val state: StateFlow<RpbUiState> = _state.asStateFlow()

    private var exchangeToken: String = ""
    private val probeBodies = mutableMapOf<String, String>()

    @Volatile
    private var stopProbe = false

    init {
        selfCheck()
    }

    // ------------------------------------------------------------------
    // 链路自检
    // ------------------------------------------------------------------

    fun selfCheck() {
        val current = _state.value
        if (current.booting || current.busy) return

        _state.update { it.copy(booting = true, bootError = null, footer = "初始化 libzhuque / ESDK 链路…") }

        viewModelScope.launch {
            val outcome = withContext(io) {
                runCatching {
                    transport.initialize()
                    val key = transport.awaitSessionKey(KEY_TIMEOUT_MS)
                        ?: error("session key 未就绪（检查 so / ABI / ZQ_FILES_DIR）")
                    // 原生层要跑 bootstrap（寻址 + MMTLS 握手）才肯放行业务 op，
                    // 这期间 frame 会是 503（tunnel-unreachable / dormant），轮询等待即可。
                    var channels = transport.channels()
                    var attempt = 0
                    while (channels.optInt("frameStatus", -1) != 200 && attempt < BOOTSTRAP_ATTEMPTS) {
                        attempt++
                        Thread.sleep(BOOTSTRAP_INTERVAL_MS)
                        channels = transport.channels()
                    }
                    CheckResult(
                        keySize = key.size,
                        frame = channels.optInt("frameStatus", -1),
                        body = channels.optString("body"),
                    )
                }
            }

            outcome.onSuccess { result ->
                val ids = parseChannelIds(result.body)
                _state.update {
                    it.copy(
                        libReady = true,
                        keyReady = true,
                        udsReady = result.frame == 200,
                        booting = false,
                        bootError = null,
                        channelIds = ids.ifEmpty { it.channelIds },
                        footer = "链路就绪 · key ${result.keySize}B · frame ${result.frame}",
                    )
                }
                if (_state.value.output.isIdle) run(RpbCatalog.apis.first())
            }.onFailure { error ->
                val message = "${error.javaClass.simpleName}: ${error.message}"
                _state.update {
                    it.copy(
                        booting = false,
                        keyReady = false,
                        udsReady = false,
                        bootError = message,
                        footer = "链路初始化失败",
                        output = ConsoleOutput(
                            title = "链路自检",
                            meta = "deviceContextInit / sessionKey 失败",
                            error = message,
                            status = OutputStatus.Failed,
                        ),
                        view = RpbView.None,
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // 控制项
    // ------------------------------------------------------------------

    fun selectChannel(channel: RpbChannel) {
        _state.update { it.copy(channel = channel, footer = "频道 · ${channel.label}") }
    }

    fun setDate(date: String) {
        _state.update { it.copy(date = date, footer = "更新表日期 · $date") }
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

    // ------------------------------------------------------------------
    // 参数面板
    // ------------------------------------------------------------------

    fun openParams(api: RpbApi) {
        _state.update {
            it.copy(paramsApi = api, paramsText = resolveParams(api))
        }
    }

    fun updateParams(text: String) {
        _state.update { it.copy(paramsText = text) }
    }

    fun resetParams() {
        _state.update { current ->
            val api = current.paramsApi ?: return@update current
            current.copy(paramsText = resolveParams(api))
        }
    }

    fun closeParams() {
        _state.update { it.copy(paramsApi = null) }
    }

    fun runFromParams() {
        val current = _state.value
        val api = current.paramsApi ?: return
        val text = current.paramsText.trim()
        val valid = runCatching { JSONObject(text) }.isSuccess
        if (!valid) {
            _state.update { it.copy(footer = "参数不是合法 JSON") }
            return
        }
        closeParams()
        run(api, text)
    }

    // ------------------------------------------------------------------
    // 单接口
    // ------------------------------------------------------------------

    fun run(api: RpbApi, paramsOverride: String? = null) {
        val current = _state.value
        if (current.busy) {
            _state.update { it.copy(footer = "上一个请求还没结束") }
            return
        }
        val params = paramsOverride ?: resolveParams(api)

        _state.update {
            it.copy(
                runningOp = api.op,
                footer = "请求 ${api.op} …",
                output = it.output.copy(
                    title = api.title,
                    meta = "请求中 · ${api.op}",
                    error = null,
                    status = OutputStatus.Running,
                ),
            )
        }

        viewModelScope.launch {
            val started = SystemClock.elapsedRealtime()
            val outcome = withContext(io) {
                runCatching {
                    transport.ensureInitialized()
                    var finalParams = params
                    if (api.op == "resolve.exchange") finalParams = ensureExchangeToken(finalParams)
                    var response = transport.semantic(api.op, finalParams, attempts = 2)
                    // bootstrap 未完成时原生层回 503，再等一拍通常就好了。
                    var bootstrapRetry = 0
                    while (
                        response.optInt("frameStatus", -1) != 200 &&
                        bootstrapRetry < BOOTSTRAP_RETRY &&
                        isBootstrapPending(response.optString("body"))
                    ) {
                        bootstrapRetry++
                        Thread.sleep(BOOTSTRAP_INTERVAL_MS)
                        response = transport.semantic(api.op, finalParams, attempts = 1)
                    }
                    response
                }
            }
            val elapsed = SystemClock.elapsedRealtime() - started

            outcome.onSuccess { response ->
                val frame = response.optInt("frameStatus", -1)
                val body = response.optString("body")
                val parsed = runCatching { JSONObject(body) }.getOrNull()
                val code = parsed?.optInt("code", -1) ?: -1
                val message = parsed?.optString("msg").orEmpty()
                val bytes = body.toByteArray(Charsets.UTF_8).size
                val tokenMatch = response.optBoolean("tokenMatch", false)

                if (api.op == "resolve.qualities") cacheExchangeToken(body)
                if (api.op == "type.listActive" || api.op == "home.load") {
                    val ids = parseChannelIds(body)
                    if (ids.isNotEmpty()) _state.update { it.copy(channelIds = ids) }
                }

                _state.update { current ->
                    current.copy(
                        runningOp = null,
                        keyReady = true,
                        udsReady = frame == 200,
                        output = ConsoleOutput(
                            title = api.title,
                            meta = buildString {
                                append("frame $frame")
                                append(" · code $code")
                                append(" · ${formatBytes(bytes)}")
                                append(" · ${formatMs(elapsed)}")
                                if (tokenMatch) append(" · token ✓")
                            },
                            json = body,
                            error = if (frame == 200) null else message.ifBlank { "frame=$frame 请求失败" },
                            status = if (frame == 200) OutputStatus.Ok else OutputStatus.Failed,
                        ),
                        view = RpbParser.parse(
                            op = api.op,
                            body = body,
                            channelLabel = current.channel.label,
                            fallbackDate = current.date,
                        ),
                        footer = "${api.op} · ${formatMs(elapsed)} · ${formatBytes(bytes)} · code=$code",
                    )
                }
            }.onFailure { error ->
                val message = "${error.javaClass.simpleName}: ${error.message}"
                _state.update {
                    it.copy(
                        runningOp = null,
                        output = ConsoleOutput(
                            title = api.title,
                            meta = "${formatMs(elapsed)} · 请求失败",
                            error = message,
                            status = OutputStatus.Failed,
                        ),
                        view = RpbView.None,
                        footer = "${api.op} 失败 · ${formatMs(elapsed)}",
                    )
                }
            }
        }
    }

    fun runQuick(op: String) {
        RpbCatalog.apis.firstOrNull { it.op == op }?.let(::run)
    }

    // ------------------------------------------------------------------
    // 全量探测
    // ------------------------------------------------------------------

    fun runAll() {
        val current = _state.value
        if (current.busy) {
            _state.update { it.copy(footer = "已有请求在运行") }
            return
        }
        stopProbe = false
        probeBodies.clear()

        _state.update {
            it.copy(
                probing = true,
                probeDone = 0,
                probeTotal = RpbCatalog.apis.size,
                footer = "全量探测准备中…",
                output = ConsoleOutput(
                    title = "全量探测",
                    meta = "0/${RpbCatalog.apis.size}",
                    status = OutputStatus.Running,
                ),
                view = RpbView.Probes(rows = emptyList(), okCount = 0),
            )
        }

        viewModelScope.launch {
            withContext(io) { runCatching { transport.ensureInitialized() } }
            val rows = mutableListOf<RpbProbeRow>()

            for ((index, api) in RpbCatalog.apis.withIndex()) {
                if (stopProbe) break
                _state.update {
                    it.copy(
                        probeDone = index,
                        footer = "全量 ${index + 1}/${RpbCatalog.apis.size} · ${api.op}",
                    )
                }

                val started = SystemClock.elapsedRealtime()
                var frame = -1
                var code = -1
                var bytes = 0
                var errorText = ""

                val outcome = withContext(io) {
                    runCatching {
                        var params = resolveParams(api)
                        if (api.op == "resolve.exchange") params = ensureExchangeToken(params)
                        transport.semantic(api.op, params, attempts = 1)
                    }
                }
                outcome.onSuccess { response ->
                    frame = response.optInt("frameStatus", -1)
                    val body = response.optString("body")
                    bytes = body.toByteArray(Charsets.UTF_8).size
                    code = runCatching { JSONObject(body) }.getOrNull()?.optInt("code", -1) ?: -1
                    probeBodies[api.op] = body
                    if (api.op == "type.listActive" || api.op == "home.load") {
                        val ids = parseChannelIds(body)
                        if (ids.isNotEmpty()) _state.update { it.copy(channelIds = ids) }
                    }
                    if (api.op == "resolve.qualities") cacheExchangeToken(body)
                }.onFailure { error ->
                    errorText = "${error.javaClass.simpleName}: ${error.message}"
                }

                rows += RpbProbeRow(
                    op = api.op,
                    title = api.title,
                    frame = frame,
                    code = code,
                    bytes = bytes,
                    ms = SystemClock.elapsedRealtime() - started,
                    error = errorText,
                )
                _state.update {
                    it.copy(
                        view = RpbView.Probes(
                            rows = rows.toList(),
                            okCount = rows.count(RpbProbeRow::ok),
                        ),
                    )
                }
            }

            val snapshot = rows.toList()
            _state.update {
                it.copy(
                    probing = false,
                    runningOp = null,
                    probeDone = snapshot.size,
                    footer = "全量完成 · ${snapshot.count { row -> row.frame == 200 }}/${snapshot.size} 可用",
                    output = it.output.copy(
                        title = "全量探测",
                        meta = "${snapshot.count { row -> row.code == 200 }}/${snapshot.size} code=200",
                        status = OutputStatus.Ok,
                    ),
                    view = RpbView.Probes(
                        rows = snapshot,
                        okCount = snapshot.count(RpbProbeRow::ok),
                    ),
                )
            }
        }
    }

    fun stopAll() {
        stopProbe = true
    }

    /** 点击探测结果行：把该 op 的报文放回输出窗口。 */
    fun openProbeRow(row: RpbProbeRow) {
        val body = probeBodies[row.op] ?: return
        _state.update {
            it.copy(
                outputMode = OutputMode.Json,
                output = ConsoleOutput(
                    title = row.title,
                    meta = "${row.op} · frame ${row.frame} · code ${row.code} · ${formatBytes(row.bytes)}",
                    json = body,
                    status = if (row.ok) OutputStatus.Ok else OutputStatus.Failed,
                ),
                view = RpbParser.parse(
                    op = row.op,
                    body = body,
                    channelLabel = it.channel.label,
                    fallbackDate = it.date,
                ),
            )
        }
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    private fun resolveParams(api: RpbApi): String {
        val current = _state.value
        return RpbCatalog.resolve(
            template = api.template,
            channelId = current.channelId,
            date = current.date,
            exchange = exchangeToken,
        )
    }

    private fun ensureExchangeToken(params: String): String {
        val json = JSONObject(params)
        val existing = json.optString("exchangeToken")
        if (existing.isNotBlank() && existing != "{EXCHANGE}") return params

        val qualitiesParams = JSONObject()
            .put("videoId", json.optString("videoId", RpbCatalog.SAMPLE_VIDEO))
            .put("code", json.optString("code", RpbCatalog.SAMPLE_CODE))
            .put("episodeNum", json.optInt("episodeNum", 1))
            .put("caps", json.optJSONArray("caps") ?: JSONArray().put("v2ts"))
            .put("playbackScene", json.optString("playbackScene", "normal"))

        val qualities = transport.semantic("resolve.qualities", qualitiesParams.toString(), attempts = 2)
        cacheExchangeToken(qualities.optString("body"))
        if (exchangeToken.isBlank()) error("qualities 没有返回 exchangeToken")
        json.put("exchangeToken", exchangeToken)
        return json.toString()
    }

    private fun cacheExchangeToken(body: String) {
        runCatching {
            val token = JSONObject(body).getJSONObject("data").optString("exchangeToken")
            if (token.isNotBlank()) exchangeToken = token
        }
    }

    private fun parseChannelIds(body: String): Map<RpbChannel, String> {
        val types = runCatching {
            JSONObject(body).getJSONObject("data").getJSONArray("types")
        }.getOrNull() ?: return emptyMap()

        val ids = mutableMapOf<RpbChannel, String>()
        for (i in 0 until types.length()) {
            val type = types.optJSONObject(i) ?: continue
            when (type.optString("name")) {
                "日漫" -> ids[RpbChannel.Jp] = type.optString("id")
                "国漫" -> ids[RpbChannel.Cn] = type.optString("id")
            }
        }
        return ids
    }

    private data class CheckResult(val keySize: Int, val frame: Int, val body: String)

    private fun isBootstrapPending(body: String): Boolean =
        body.contains("dormant") ||
            body.contains("bootstrap") ||
            body.contains("tunnel-unreachable")

    private companion object {
        const val KEY_TIMEOUT_MS = 45_000L
        const val BOOTSTRAP_ATTEMPTS = 10
        const val BOOTSTRAP_RETRY = 3
        const val BOOTSTRAP_INTERVAL_MS = 1_200L
    }
}
