# AnimeDB

动漫数据源测试台。顶部导航栏每个 Tab 对应一个数据源，每个 Tab 页就是该数据源的测试页：选接口 → 改参数 → 运行 → 底部输出窗口看结构化结果或原始报文。

## 技术栈

与 [Mikomi](https://github.com/liomix/Mikomi) 一致的版本基线：

| 类别 | 选型 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.4.21 |
| 构建 | AGP / Gradle | 9.2.1 / 9.4.1 |
| UI | Jetpack Compose + Material 3 | BOM 2026.02.01 |
| 导航 | Jetpack Navigation 3（类型安全路由） | 1.2.0 |
| 依赖注入 | Hilt（+ KSP） | 2.60.1 |
| 图片 | Coil 3 | 3.6.3 |
| 异步 | Coroutines + Flow | 1.10.1 |
| 构建规范 | build-logic 约定插件 | — |
| SDK | compileSdk 37 / minSdk 24 / targetSdk 36 / arm64-v8a | — |

## 已接入数据源

| Tab | 链路 | 测试能力 |
|-----|------|----------|
| **AniRpb** | `libzhuque.so` 原生加密通道（ESDK/MMTLS，非 HTTP） | 33 个只读语义 op；链路自检（LIB/KEY/UDS）、日漫/国漫频道切换、更新表日期选择、单接口参数编辑运行、全量探测（逐 op 结果表，点行看报文） |
| **AniCyk** | CYK 主 API 直连（明文 HTTP，AES 解密） | 14 个匿名接口；4 节点自动降级、GET/POST 参数编辑、搜索源与日历源切换、响应解密标记 |

两个 Tab 各自持有状态，切换不丢上次结果。

## 界面结构

```
┌──────────────────────────────────┐
│ AniRpb            AniCyk         │  顶栏 = 数据源 Tab
├──────────────────────────────────┤
│ ● LIB  ● KEY  ● UDS      ⚗  ↻    │  链路状态 / 全量探测 / 自检
│ [日漫 | 国漫]       2026-11-01    │  频道 & 日期
│ 🔍 过滤接口名称或 op              │  接口过滤
│ 热播榜 整榜 更新表 频道           │  快捷运行
├──────────────────────────────────┤
│ 内容发现                          │
│  首页模块·热播榜  home.load   ▶   │  接口目录（点行编辑参数）
│  猜你喜欢        home.recommend ▶ │
├──────────────────────────────────┤
│ 首页模块·热播榜  frame 200 · …    │  输出窗口（可上拉展开）
│ [结构化 | JSON]       149182 字符  │
│ 1 黑色五叶草 第二季  138.9万       │
└──────────────────────────────────┘
```

## 模块结构

```
AnimeDB/
├─ app/                 装配入口：Application / Activity / NavHost，jniLibs 打包配置
├─ build-logic/         约定插件（animedb.android.application/library/compose/hilt
│                       /serialization/core/feature/source）
├─ core/
│  ├─ model/            领域模型（暂无内容）
│  ├─ common/           调度器限定符 + 格式化（字节/耗时/JSON 美化）
│  ├─ designsystem/     色板 / 字体 / AnimeDbTheme / 测试台语义色
│  ├─ ui/               测试台通用组件：ConsoleScaffold、OutputPanel、ApiCatalogRow、
│  │                    SegmentedControl、StatusPill、JsonBlock、CoverImage…
│  ├─ navigation/       ConsoleDestination / Navigator / EntryProviderInstaller
│  ├─ network/          （预留）
│  └─ source/           SourceTestTab 契约（Tab 注册点）
├─ feature/console/     顶部 Tab 容器 + 空态
└─ source/
   ├─ anirpb/           Zhuque 传输层 + 33 个 op 目录 + 测试页 + jniLibs/arm64-v8a
   └─ anicyk/           CYK 客户端 + 14 个接口目录 + 测试页
```

## 新增一个数据源

1. 新建 `source/<name>/` 模块（`plugins { id("animedb.android.source") }`），并在 `settings.gradle.kts` 里 `include(":source:<name>")`。
2. 写接口目录（`data/<Name>Catalog.kt`）与可选的结构化解析器。
3. 实现 `SourceTestTab`（`order` / `sourceId` / `displayName` / `subtitle` / `Content`），`Content` 里用 `:core:ui` 的 `ConsoleScaffold` + `OutputPanel` 拼页面。
4. Hilt `@Binds @IntoSet` 注册，并在 `app/build.gradle.kts` 加 `implementation(project(":source:<name>"))`。

Tab 会自动出现，容器与 app 无需改动。

## 构建

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> AniRpb 的原生 so 只有 `arm64-v8a`，务必用 arm64 真机；模拟器会报 `UnsatisfiedLinkError`。
> AniCyk 走明文 HTTP 节点，Manifest 已开启 `usesCleartextTraffic`。
