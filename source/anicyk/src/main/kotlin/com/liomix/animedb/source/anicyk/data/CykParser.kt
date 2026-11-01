package com.liomix.animedb.source.anicyk.data

import com.liomix.animedb.core.common.format.prettyJson
import com.liomix.animedb.core.common.format.strings
import org.json.JSONArray
import org.json.JSONObject

/** 报文 → 结构化视图；未识别的接口回退 JSON。 */
object CykParser {

    fun parse(key: String, body: String): CykView {
        val trimmed = body.trim()
        return when (key) {
            "config" -> jsonObject(trimmed)?.let(::config) ?: CykView.Plain
            "payConfig" -> jsonObject(trimmed)?.let(::pay) ?: CykView.Plain
            "schedule" -> jsonArray(trimmed)?.let(::schedule) ?: CykView.Plain
            "hot" -> jsonArray(trimmed)?.let { CykView.Chips("热搜词 · ${it.length()} 条", it.strings()) } ?: CykView.Plain
            "associate" -> jsonArray(trimmed)?.let { CykView.Chips("搜索联想 · ${it.length()} 条", it.strings()) } ?: CykView.Plain
            "banList" -> jsonArray(trimmed)?.let { CykView.Chips("屏蔽词 · ${it.length()} 条", it.strings()) } ?: CykView.Plain
            "rule" -> jsonArray(trimmed)?.let(::rules) ?: CykView.Plain
            "search2" -> jsonArray(trimmed)?.let(::links) ?: CykView.Plain
            "mineItems" -> jsonArray(trimmed)?.let(::menu) ?: CykView.Plain
            "gifAvatar" -> jsonArray(trimmed)?.let(::avatars) ?: CykView.Plain
            else -> CykView.Plain
        }
    }

    private fun jsonObject(body: String): JSONObject? =
        runCatching { JSONObject(body) }.getOrNull()

    private fun jsonArray(body: String): JSONArray? =
        runCatching { JSONArray(body) }.getOrNull()

    private fun config(json: JSONObject): CykView {
        val hosts = json.strings("hosts")
        val sources = json.strings("source")
        val prices = json.strings("prices")
        val ips = json.optJSONArray("ips")?.length() ?: 0
        val updateInfo = json.optJSONObject("updateInfo")
        return CykView.Config(
            heading = "应用配置 · ${AniCykClient.APP_ID}",
            tiles = listOf(
                hosts.size.toString() to "源站",
                sources.size.toString() to "首页源",
                prices.size.toString() to "价格档",
                ips.toString() to "IP 映射",
            ),
            sources = sources,
            hosts = hosts,
            hostsTotal = hosts.size,
            appWeb = json.optString("appWeb"),
            updateInfo = updateInfo?.let { prettyJson(it.toString()) },
        )
    }

    private fun pay(json: JSONObject): CykView = CykView.Pay(
        heading = "付费配置",
        flags = "礼包 ${if (json.optBoolean("isGift")) "开启" else "关闭"} · " +
            "微信 ${if (json.optBoolean("haveWx")) "支持" else "不支持"}",
        prices = json.strings("prices"),
        desc = json.optString("priceDesc"),
        intro = json.optString("vipIntro"),
    )

    private fun schedule(array: JSONArray): CykView {
        val days = buildList {
            for (i in 0 until array.length()) {
                val day = array.optJSONArray(i) ?: continue
                val items = buildList {
                    for (k in 0 until day.length()) {
                        val item = day.optJSONObject(k) ?: continue
                        add(
                            CykScheduleItem(
                                title = item.optString("title"),
                                cover = item.optString("cover"),
                                chapter = item.optString("chapter"),
                                link = item.optString("link"),
                            ),
                        )
                    }
                }
                add(CykDay(index = i + 1, items = items))
            }
        }
        val total = days.sumOf { it.items.size }
        return CykView.Days(
            heading = "追番日历 · ${days.size} 天 · $total 部",
            days = days,
        )
    }

    private fun rules(array: JSONArray): CykView {
        val rows = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(item.optString("type") to item.optString("name"))
            }
        }
        return CykView.Rules(heading = "搜索规则 · ${rows.size} 条", rows = rows)
    }

    private fun links(array: JSONArray): CykView {
        val groups = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val urls = buildList {
                    item.optJSONArray("links")?.let { list ->
                        for (k in 0 until list.length()) {
                            list.optString(k).takeIf { it.isNotBlank() }?.let(::add)
                        }
                    }
                    if (isEmpty()) {
                        item.optString("link").takeIf { it.isNotBlank() }?.let(::add)
                    }
                }
                add(
                    CykLinkGroup(
                        status = item.optString("status").ifBlank { "SUCCESS" },
                        links = urls,
                    ),
                )
            }
        }
        val count = groups.sumOf { it.links.size }
        return CykView.Links(heading = "搜索入口 · $count 个链接", groups = groups)
    }

    private fun menu(array: JSONArray): CykView {
        val rows = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(
                    CykMenuItem(
                        title = item.optString("title"),
                        desc = item.optString("desc"),
                        target = item.optString("url").ifBlank { item.optString("scheme") },
                        vip = item.optInt("vip") == 1,
                    ),
                )
            }
        }
        return CykView.Menu(heading = "我的菜单 · ${rows.size} 项", rows = rows)
    }

    private fun avatars(array: JSONArray): CykView {
        val urls = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                item.optString("url").takeIf { it.isNotBlank() }?.let(::add)
            }
        }
        return CykView.Avatars(heading = "GIF 头像 · ${urls.size} 个", urls = urls)
    }
}
