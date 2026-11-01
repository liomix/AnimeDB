package com.liomix.animedb.source.anicyk.data

import org.json.JSONObject

/** 参数面板里可改的运行上下文。 */
data class CykQuery(
    val searchType: String = "DEFAULT",
    val scheduleType: String = "超清",
    val keyword: String = "火影",
    val page: Int = 0,
)

/** AniCyk 的匿名可直连接口。 */
data class CykApi(
    val key: String,
    val title: String,
    val group: String,
    val method: String,
    val path: String,
    val note: String = "",
    val params: (CykQuery) -> JSONObject = { JSONObject() },
) {
    val isGet: Boolean get() = method.equals("GET", ignoreCase = true)
}

object CykCatalog {

    val apis: List<CykApi> = listOf(
        // ===== 配置 / 运营 =====
        CykApi("config", "应用配置", "配置 / 运营", "GET", "/mobile/config",
            note = "hosts / source / prices / 更新信息",
            params = { JSONObject().put("version", AniCykClient.APP_VERSION.toInt()) }),
        CykApi("payConfig", "付费配置", "配置 / 运营", "GET", "/payConfig",
            note = "会员价格与活动文案"),
        CykApi("mineItems", "我的菜单", "配置 / 运营", "GET", "/mineItems",
            note = "8 个菜单项与跳转配置"),
        CykApi("error", "服务状态", "配置 / 运营", "GET", "/error",
            note = "主 API 健康检查"),
        CykApi("discountInfo", "优惠信息", "配置 / 运营", "GET", "/discountInfo",
            note = "当前活动（可能为空对象）"),

        // ===== 搜索 =====
        CykApi("hot", "热搜词", "搜索", "GET", "/search/hot",
            note = "20 个热门搜索词",
            params = { query -> JSONObject().put("type", query.searchType) }),
        CykApi("associate", "搜索联想", "搜索", "POST", "/search/associate",
            note = "根据关键词返回 8 条联想",
            params = { query ->
                JSONObject().put("keyword", query.keyword).put("type", query.searchType)
            }),
        CykApi("rule", "搜索规则", "搜索", "GET", "/search/rule",
            note = "DEFAULT / BACKUP / VIP / MINE"),
        CykApi("search2", "搜索入口", "搜索", "POST", "/search2",
            note = "返回各源站搜索页地址，page 从 0 开始",
            params = { query ->
                JSONObject()
                    .put("type", query.searchType)
                    .put("key", query.keyword)
                    .put("page", query.page)
            }),
        CykApi("cache", "搜索缓存", "搜索", "GET", "/search/cache",
            note = "当前通常返回空数组",
            params = { query ->
                JSONObject()
                    .put("source", query.searchType)
                    .put("key", query.keyword)
                    .put("page", 1)
            }),

        // ===== 日历 / 规则 =====
        CykApi("schedule", "追番日历", "日历 / 规则", "GET", "/mobile/schedule",
            note = "超清 7 天 / 樱花 11 天",
            params = { query -> JSONObject().put("type", query.scheduleType) }),
        CykApi("parseRule", "解析规则", "日历 / 规则", "GET", "/parseRule",
            note = "urlRules / otherRules"),

        // ===== 素材 / 其他 =====
        CykApi("banList", "屏蔽词", "素材 / 其他", "GET", "/banList",
            note = "127 个内容屏蔽词"),
        CykApi("gifAvatar", "GIF 头像", "素材 / 其他", "GET", "/getGifAvatar",
            note = "20 个头像素材"),
    )

    fun groups(apis: List<CykApi> = this.apis): List<Pair<String, List<CykApi>>> =
        apis.groupBy { it.group }.map { (group, items) -> group to items }
}
