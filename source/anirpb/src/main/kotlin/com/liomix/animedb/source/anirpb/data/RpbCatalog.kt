package com.liomix.animedb.source.anirpb.data

/** AniRpb 的一个语义 op（只读接口）。 */
data class RpbApi(
    val op: String,
    val title: String,
    val group: String,
    val template: String,
    val note: String = "",
)

/**
 * AniRpb 可直连的只读 op 目录。
 *
 * 参数模板里的变量在运行时替换：
 * `{CHANNEL}` 频道 id、`{VIDEO}` 示例视频、`{CODE}` 线路 code、
 * `{DATE}` 更新表日期、`{EXCHANGE}` 换源令牌（自动获取）。
 */
object RpbCatalog {

    const val SAMPLE_VIDEO = "366541396852445184"
    const val SAMPLE_CODE = "newup-jp"

    /** 兜底频道 id（运行时会被 type.listActive 的实际值覆盖）。 */
    const val JP_CHANNEL_ID = "316207249407696896"
    const val CN_CHANNEL_ID = "316207250154283008"

    val apis: List<RpbApi> = listOf(
        // ===== 内容发现 =====
        RpbApi("home.load", "首页模块 · 热播榜", "内容发现",
            """{"categoryId":"{CHANNEL}","pageIndex":1,"pageSize":20}""",
            "频道 id 跟随顶部 日漫 / 国漫 切换"),
        RpbApi("home.recommend", "猜你喜欢", "内容发现",
            """{"categoryId":"{CHANNEL}","pageIndex":1,"pageSize":20,"tags":[],"videoId":""}""",
            "按频道返回推荐内容"),
        RpbApi("video.categoryList", "分类整榜", "内容发现",
            """{"pageIndex":1,"pageSize":20,"categoryId":"{CHANNEL}","filters":[],"sort":"hottest","billing":"","seriesMerge":0}""",
            "sort 可改为 latest / hottest / score / discuss"),
        RpbApi("type.listActive", "频道 / 类型", "内容发现",
            """{"pageIndex":1,"pageSize":100,"parentId":null,"parentCode":null,"withChannel":null,"dataId":null}""",
            "运行时动态解析日漫 / 国漫频道 id"),
        RpbApi("calendar.videos", "更新表", "内容发现",
            """{"playDate":"{DATE}","pageIndex":1,"pageSize":20}""",
            "日期跟随顶部选择，也可在参数里改写"),
        RpbApi("search.videos", "视频搜索", "内容发现",
            """{"categoryId":"","keyword":"火影","pageIndex":1,"pageSize":20,"seriesMerge":0,"tags":""}""",
            "可在参数里替换 keyword"),
        RpbApi("search.suggest", "搜索联想", "内容发现",
            """{"keyword":"火影","pageSize":10}""",
            "联想词与搜索建议"),
        RpbApi("search.config", "搜索页配置", "内容发现", "{}", "搜索入口与运营配置"),
        RpbApi("filter.dimensions", "筛选维度", "内容发现",
            """{"channelId":"{CHANNEL}"}""",
            "返回分类 / 地区 / 年份等筛选项"),
        RpbApi("banner.list", "首页轮播图", "内容发现", "{}", "Banner 素材与跳转配置"),
        RpbApi("guide.listActive", "新手引导", "内容发现", "{}", "引导页开关与素材"),
        RpbApi("splash.active", "开屏配置", "内容发现", "{}", "开屏广告 / 活动"),
        RpbApi("ui.config", "UI 配置", "内容发现", "{}", "模板、皮肤与字体"),
        RpbApi("ui.version", "UI 版本", "内容发现", "{}", "UI 资源版本号"),

        // ===== 详情 / 播放 =====
        RpbApi("video.detail", "视频详情", "详情 / 播放",
            """{"videoId":"{VIDEO}"}""",
            "示例视频 id 可在参数编辑中替换"),
        RpbApi("video.viewerCount", "在看人数", "详情 / 播放",
            """{"videoId":"{VIDEO}"}""",
            "实时观看人数"),
        RpbApi("resolve.episodes", "线路 / 集数", "详情 / 播放",
            """{"videoId":"{VIDEO}","code":"{CODE}"}""",
            "code 来自详情 player[].playerCode"),
        RpbApi("resolve.qualities", "清晰度 / 播放令牌", "详情 / 播放",
            """{"videoId":"{VIDEO}","code":"{CODE}","episodeNum":1,"caps":["v2ts"],"playbackScene":"normal"}""",
            "返回 exchangeToken，可用于下一步解析"),
        RpbApi("resolve.exchange", "换源解析", "详情 / 播放",
            """{"videoId":"{VIDEO}","code":"{CODE}","episodeNum":1,"quality":"1080p","exchangeToken":"{EXCHANGE}","adToken":"","caps":["v2ts"],"playbackScene":"normal"}""",
            "exchangeToken 为空时自动先调 qualities 获取"),

        // ===== 社区互动 =====
        RpbApi("comment.config", "评论配置", "社区互动", "{}", "评论开关与金币门槛"),
        RpbApi("comment.list", "评论列表", "社区互动",
            """{"videoId":"{VIDEO}","episodeNum":1,"pageIndex":1,"pageSize":20,"sortBy":""}""",
            "按集读取评论"),
        RpbApi("review.summary", "评分汇总", "社区互动",
            """{"videoId":"{VIDEO}"}""",
            "综合评分 / 用户评分 / 评价数"),
        RpbApi("review.list", "长评列表", "社区互动",
            """{"videoId":"{VIDEO}","pageIndex":1,"pageSize":20,"sort":""}""",
            "长评与评分内容"),
        RpbApi("episode.likeCount", "单集点赞数", "社区互动",
            """{"videoId":"{VIDEO}","episodeNum":1}""",
            "指定集数点赞数"),
        RpbApi("engagement.counts", "互动计数", "社区互动",
            """{"videoIds":["{VIDEO}"]}""",
            "批量读取点赞 / 收藏计数"),

        // ===== 会员 / 运营 =====
        RpbApi("vip.plans", "会员套餐", "会员 / 运营", "{}", "会员价格与权益套餐"),
        RpbApi("vip.serverTime", "服务端时间", "会员 / 运营", "{}", "毫秒时间戳，可校准客户端"),
        RpbApi("membership.catalog", "会员等级目录", "会员 / 运营", "{}", "等级与权益列表"),
        RpbApi("bubbleSkin.catalog", "弹幕气泡皮肤", "会员 / 运营", "{}", "气泡皮肤素材"),
        RpbApi("welfare.hub", "福利中心", "会员 / 运营", "{}", "福利活动聚合"),
        RpbApi("notice.list", "公告列表", "会员 / 运营", "{}", "站内公告"),
        RpbApi("base.agreement", "协议 / 隐私", "会员 / 运营", "{}", "用户协议与隐私政策"),
        RpbApi("config.resolve", "解析配置", "会员 / 运营", "{}", "客户端解析 KV 配置"),
    )

    /** 按 group 保持原始顺序分组。 */
    fun groups(apis: List<RpbApi> = this.apis): List<Pair<String, List<RpbApi>>> =
        apis.groupBy { it.group }.map { (group, items) -> group to items }

    fun resolve(
        template: String,
        channelId: String,
        video: String = SAMPLE_VIDEO,
        code: String = SAMPLE_CODE,
        date: String,
        exchange: String = "",
    ): String = template
        .replace("{CHANNEL}", channelId)
        .replace("{VIDEO}", video)
        .replace("{CODE}", code)
        .replace("{DATE}", date)
        .replace("{EXCHANGE}", exchange)
}
