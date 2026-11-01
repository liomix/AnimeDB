# AnimeDB

动漫数据源测试台。顶部导航栏每个 Tab 对应一个数据源，每个 Tab 页就是该数据源的测试页。

## 技术栈

与 [Mikomi](https://github.com/liomix/Mikomi) 完全一致的版本基线：

| 类别 | 选型 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.4.21 |
| 构建 | AGP / Gradle | 9.2.1 / 9.4.1 |
| UI | Jetpack Compose + Material 3 | BOM 2026.02.01 |
| 导航 | Jetpack Navigation 3（类型安全路由） | 1.2.0 |
| 依赖注入 | Hilt（+ KSP） | 2.60.1 |
| 网络 | OkHttp + kotlinx.serialization | 4.12.0 / 1.11.0 |
| 异步 | Coroutines + Flow | 1.10.1 |
| 构建规范 | build-logic 约定插件 | — |
| SDK | compileSdk 37 / minSdk 24 / targetSdk 36 | — |

## 模块结构

```
AnimeDB/
├─ app/                  # 装配入口：Application / Activity / NavHost
├─ build-logic/          # 约定插件（animedb.android.*）
├─ core/
│  ├─ model/             # 领域模型（纯 Kotlin，暂无内容）
│  ├─ common/            # 调度器 / 通用工具（暂无内容）
│  ├─ designsystem/      # 主题：色板 / 字体 / AnimeDbTheme
│  ├─ ui/                # 共享 Compose 组件（占位面板等）
│  ├─ navigation/        # 路由 / Navigator / EntryProviderInstaller
│  ├─ network/           # HTTP 基础设施（暂无内容）
│  └─ source/            # 数据源测试页契约 SourceTestTab
├─ feature/
│  └─ console/           # 顶部 Tab 容器：一源一 Tab，内容来自各数据源模块
└─ source/
   ├─ anirpb/            # AniRpb：数据源实现 + 测试页
   └─ anicyk/            # AniCyk：数据源实现 + 测试页
```

依赖方向：`app → feature/source → core`，`core` 不依赖上层。

## 新增一个数据源

1. 新建 `source/<name>/` 模块，`plugins { id("animedb.android.source") }`，`settings.gradle.kts` 里 `include(":source:<name>")`。
2. 实现 `SourceTestTab`（`order` / `sourceId` / `displayName` / `subtitle` / `Content`）。
3. 用 Hilt `@Binds @IntoSet` 注册，并在 `app/build.gradle.kts` 加上 `implementation(project(":source:<name>"))`。

Tab 会自动出现在顶部导航栏，`feature:console` 与 `app` 无需改动。

## 构建

```bash
./gradlew :app:assembleDebug
```

> 当前 AniRpb / AniCyk 两个 Tab 均为占位页，数据源接入逻辑尚未实现。
