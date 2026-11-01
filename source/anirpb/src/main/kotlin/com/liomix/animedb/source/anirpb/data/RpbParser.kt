package com.liomix.animedb.source.anirpb.data

import com.liomix.animedb.core.common.format.formatCount
import org.json.JSONArray
import org.json.JSONObject

/**
 * 报文 → 结构化视图。
 *
 * 只对高频接口做结构化，其余返回 [RpbView.Plain] 由输出窗口回退到 JSON，
 * 这样新增 op 不需要改解析器。
 */
object RpbParser {

    fun parse(op: String, body: String, channelLabel: String, fallbackDate: String): RpbView {
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return RpbView.Plain
        return when (op) {
            "home.load" -> parseHome(json, channelLabel)
            "video.categoryList" -> parseVideoList(json, "分类整榜")
            "search.videos" -> parseVideoList(json, "搜索结果")
            "home.recommend" -> parseVideoList(json, "猜你喜欢")
            "calendar.videos" -> parseCalendar(json, fallbackDate)
            "type.listActive" -> parseChannels(json)
            else -> RpbView.Plain
        }
    }

    private fun parseHome(json: JSONObject, channelLabel: String): RpbView {
        val data = json.optJSONObject("data") ?: return RpbView.Plain
        val modules = data.optJSONArray("list") ?: return RpbView.Plain
        var hot: JSONObject? = null
        for (i in 0 until modules.length()) {
            val module = modules.optJSONObject(i) ?: continue
            if (module.optString("k") == "hot") {
                hot = module
                break
            }
            if (hot == null && (module.optJSONArray("items")?.length() ?: 0) > 0) hot = module
        }
        val module = hot ?: return RpbView.Plain
        val items = module.optJSONArray("items") ?: JSONArray()
        val footer = "模块 ${module.optString("id")} · ${module.optString("layout")} · " +
            "rank=${module.optJSONObject("displayFields")?.optBoolean("rank")}"
        return RpbView.VideoFeed(
            heading = "$channelLabel · ${module.optString("name")} · ${items.length()} 条",
            items = videoItems(items, withRank = true),
            footer = footer,
        )
    }

    private fun parseVideoList(json: JSONObject, heading: String): RpbView {
        val data = json.optJSONObject("data") ?: return RpbView.Plain
        val videos = data.optJSONArray("videos") ?: data.optJSONArray("items") ?: return RpbView.Plain
        val total = data.optJSONObject("pageinfo")?.optInt("total", 0) ?: 0
        val suffix = if (total > 0) " · 共 $total 部" else ""
        return RpbView.VideoFeed(
            heading = "$heading · ${videos.length()} 条$suffix",
            items = videoItems(videos, withRank = true),
        )
    }

    private fun videoItems(array: JSONArray, withRank: Boolean): List<VideoItem> = buildList {
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            add(
                VideoItem(
                    rank = if (withRank) i + 1 else 0,
                    title = item.optString("title"),
                    cover = item.optString("coverUrl"),
                    badge = item.optString("customText").ifBlank { item.optString("updateSchedule") },
                    meta = "更新至 ${item.optInt("currentEpisodes")} 集 · ${item.optDouble("score", 0.0)} 分",
                    trailing = formatCount(item.optLong("playCount")),
                ),
            )
        }
    }

    private fun parseCalendar(json: JSONObject, fallbackDate: String): RpbView {
        val data = json.optJSONObject("data") ?: return RpbView.Plain
        val groups = data.optJSONArray("data") ?: return RpbView.Plain
        val channelNames = mutableMapOf<String, String>()
        data.optJSONArray("channels")?.let { channels ->
            for (i in 0 until channels.length()) {
                val channel = channels.optJSONObject(i) ?: continue
                channelNames[channel.optString("channelId")] = channel.optString("name")
            }
        }

        var total = 0
        val parsed = mutableListOf<CalendarGroup>()
        for (i in 0 until groups.length()) {
            val group = groups.optJSONObject(i) ?: continue
            val items = group.optJSONArray("videoCalendarData") ?: continue
            total += items.length()
            val rows = buildList {
                for (k in 0 until items.length()) {
                    val video = items.optJSONObject(k) ?: continue
                    add(
                        CalendarItem(
                            title = video.optString("videoName"),
                            cover = video.optString("coverUrl"),
                            update = video.optString("updateDesc"),
                            episode = video.optString("episodeNum"),
                            channel = channelNames[video.optString("channelId")].orEmpty(),
                        ),
                    )
                }
            }
            if (rows.isNotEmpty()) {
                parsed += CalendarGroup(time = group.optString("updateTime"), items = rows)
            }
        }
        val date = groups.optJSONObject(0)?.optString("playDate").orEmpty().ifBlank { fallbackDate }
        return RpbView.Calendar(heading = "$date · $total 部更新", groups = parsed)
    }

    private fun parseChannels(json: JSONObject): RpbView {
        val types = json.optJSONObject("data")?.optJSONArray("types") ?: return RpbView.Plain
        val rows = buildList {
            for (i in 0 until types.length()) {
                val type = types.optJSONObject(i) ?: continue
                if (type.optString("parentId") != "0") continue
                add(
                    ChannelRow(
                        name = type.optString("name"),
                        id = type.optString("id"),
                        code = type.optString("code"),
                    ),
                )
            }
        }
        return RpbView.Channels(heading = "频道 / 类型（${types.length()}）", rows = rows)
    }
}
