# AnimeDB 工程结构

> 对齐 Mikomi 的工程基线（多模块 + build-logic 约定插件 + Hilt + Navigation 3 + Route/Screen 双组件）。
> 本项目是**数据源测试台**：顶部 Tab 每个对应一个数据源，Tab 内容是该数据源的测试页。

## 1. 依赖方向

```text
app ──────────────► feature:console ─────► core:*
 │                        │
 └──────────────────────► source:anirpb / source:anicyk ──► core:source
```

- `app` 只做装配：Application / Activity / NavHost，不 import 任何页面实现。
- `feature:console` 提供 Tab 容器，不认识任何具体数据源。
- `source:*` 一个数据源一个模块，同时承载「数据源实现」与「该数据源的测试页」。
- `core:*` 不依赖上层，也不依赖任何具体数据源。

## 2. 模块职责

| 模块 | 职责 | 当前状态 |
|------|------|----------|
| `:app` | `AnimeDbApp` / `MainActivity` / `AnimeDbNavHost` | 已完成装配 |
| `:core:model` | 领域模型（Anime / Episode / 报文体等） | 空模块，待接入 |
| `:core:common` | `@Dispatcher` 调度器、通用扩展 | 空模块，待接入 |
| `:core:designsystem` | 色板 / 字体 / `AnimeDbTheme` | 已完成 |
| `:core:ui` | 共享 Compose 组件（`SourcePlaceholder`） | 占位组件 |
| `:core:navigation` | `ConsoleDestination` / `Navigator` / `EntryProviderInstaller` | 已完成 |
| `:core:network` | OkHttp / Retrofit / 报文抓取 | 空模块，待接入 |
| `:core:source` | 数据源测试页契约 `SourceTestTab` | 已完成契约 |
| `:feature:console` | 顶部 Tab 容器 + 空态 | 已完成 |
| `:source:anirpb` | AniRpb 数据源 + 测试页 | 占位 |
| `:source:anicyk` | AniCyk 数据源 + 测试页 | 占位 |

## 3. Tab 注册机制

顶部 Tab 不写死在容器里，而是由各数据源模块通过 Hilt 多重绑定注入：

```kotlin
// core:source
interface SourceTestTab {
    val order: Int
    val sourceId: String
    val displayName: String
    val subtitle: String
    @Composable fun Content(modifier: Modifier)
}

// source:anirpb
@Module @InstallIn(SingletonComponent::class)
abstract class AniRpbModule {
    @Binds @IntoSet abstract fun bind(tab: AniRpbTestTab): SourceTestTab
}
```

`:feature:console` 的 `ConsoleViewModel` 注入 `Set<SourceTestTab>`，按 `order` 排序渲染。
由此：**新增数据源 = 新增一个模块，容器与 app 都不用改**。

## 4. 导航

- `Navigator`（`@ActivityRetainedScoped`）持有 `SnapshotStateList<NavKey>` 返回栈，`NavDisplay` 直接消费。
- 顶级目的地只有 `ConsoleDestination`；数据源切换是页面内 Tab 状态，不占用返回栈。
- 二级页面（如请求详情、报文对比）后续以 `@Serializable` 路由压栈，并在对应 `:source:*` 或
  `:feature:*` 模块内注册。

## 5. 下一步（占位待实现）

1. `:core:source` 增加接口探针模型：`SourceProbe` / `ProbeRequest` / `ProbeResult`（状态、耗时、原始报文）。
2. `:core:network` 增加 HTTP 执行引擎与请求录制，供 HTTP 类数据源复用。
3. `:source:anirpb` 接入 `ZhuqueTransport`（原生加密通道），测试页展示握手 / op / 解密后 JSON。
4. `:source:anicyk` 确认协议与鉴权后接入，测试页与 AniRpb 同构。
5. 数据源数量变多后，`ConsoleTopBar` 的 `TabRow` 换成 `PrimaryScrollableTabRow`。
