package com.liomix.animedb.source.anirpb.data

/** AniRpb 测试页的结构化视图模型。 */
sealed interface RpbView {

    data object None : RpbView

    /** 无结构化渲染，回退到 JSON。 */
    data object Plain : RpbView

    data class VideoFeed(
        val heading: String,
        val items: List<VideoItem>,
        val footer: String? = null,
    ) : RpbView

    data class Calendar(
        val heading: String,
        val groups: List<CalendarGroup>,
    ) : RpbView

    data class Channels(
        val heading: String,
        val rows: List<ChannelRow>,
    ) : RpbView

    data class Probes(
        val rows: List<RpbProbeRow>,
        val okCount: Int,
    ) : RpbView
}

data class VideoItem(
    val rank: Int,
    val title: String,
    val cover: String,
    val badge: String,
    val meta: String,
    val trailing: String,
)

data class CalendarGroup(
    val time: String,
    val items: List<CalendarItem>,
)

data class CalendarItem(
    val title: String,
    val cover: String,
    val update: String,
    val episode: String,
    val channel: String,
)

data class ChannelRow(
    val name: String,
    val id: String,
    val code: String,
)

data class RpbProbeRow(
    val op: String,
    val title: String,
    val frame: Int,
    val code: Int,
    val bytes: Int,
    val ms: Long,
    val error: String = "",
) {
    val ok: Boolean get() = error.isBlank() && frame == 200
}
