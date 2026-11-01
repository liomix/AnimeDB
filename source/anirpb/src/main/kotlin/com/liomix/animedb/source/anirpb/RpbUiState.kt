package com.liomix.animedb.source.anirpb

import com.liomix.animedb.core.ui.model.ConsoleOutput
import com.liomix.animedb.core.ui.model.OutputMode
import com.liomix.animedb.source.anirpb.data.RpbApi
import com.liomix.animedb.source.anirpb.data.RpbCatalog
import com.liomix.animedb.source.anirpb.data.RpbProbeRow
import com.liomix.animedb.source.anirpb.data.RpbView

enum class RpbChannel(val label: String) {
    Jp("日漫"),
    Cn("国漫"),
}

data class RpbUiState(
    val libReady: Boolean = false,
    val keyReady: Boolean = false,
    val udsReady: Boolean = false,
    val booting: Boolean = false,
    val bootError: String? = null,
    val channel: RpbChannel = RpbChannel.Jp,
    val channelIds: Map<RpbChannel, String> = mapOf(
        RpbChannel.Jp to RpbCatalog.JP_CHANNEL_ID,
        RpbChannel.Cn to RpbCatalog.CN_CHANNEL_ID,
    ),
    val date: String = "",
    val query: String = "",
    val paramsApi: RpbApi? = null,
    val paramsText: String = "",
    val runningOp: String? = null,
    val probing: Boolean = false,
    val probeDone: Int = 0,
    val probeTotal: Int = 0,
    val output: ConsoleOutput = ConsoleOutput(),
    val outputMode: OutputMode = OutputMode.Structured,
    val panelExpanded: Boolean = false,
    val view: RpbView = RpbView.None,
    val footer: String = "正在准备链路…",
) {
    val channelId: String get() = channelIds[channel] ?: RpbCatalog.JP_CHANNEL_ID

    val busy: Boolean get() = runningOp != null || probing

    val booted: Boolean get() = libReady && keyReady && udsReady
}
