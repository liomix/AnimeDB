package com.liomix.animedb.source.anicyk.data

/** AniCyk 测试页的结构化视图模型。 */
sealed interface CykView {

    data object None : CykView

    data object Plain : CykView

    data class Chips(
        val heading: String,
        val items: List<String>,
    ) : CykView

    data class Config(
        val heading: String,
        val tiles: List<Pair<String, String>>,
        val sources: List<String>,
        val hosts: List<String>,
        val hostsTotal: Int,
        val appWeb: String,
        val updateInfo: String?,
    ) : CykView

    data class Days(
        val heading: String,
        val days: List<CykDay>,
    ) : CykView

    data class Rules(
        val heading: String,
        val rows: List<Pair<String, String>>,
    ) : CykView

    data class Links(
        val heading: String,
        val groups: List<CykLinkGroup>,
    ) : CykView

    data class Menu(
        val heading: String,
        val rows: List<CykMenuItem>,
    ) : CykView

    data class Avatars(
        val heading: String,
        val urls: List<String>,
    ) : CykView

    data class Pay(
        val heading: String,
        val flags: String,
        val prices: List<String>,
        val desc: String,
        val intro: String,
    ) : CykView
}

data class CykDay(
    val index: Int,
    val items: List<CykScheduleItem>,
)

data class CykScheduleItem(
    val title: String,
    val cover: String,
    val chapter: String,
    val link: String,
)

data class CykLinkGroup(
    val status: String,
    val links: List<String>,
)

data class CykMenuItem(
    val title: String,
    val desc: String,
    val target: String,
    val vip: Boolean,
)
