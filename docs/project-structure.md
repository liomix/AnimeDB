# AnimeDB 工程结构

> 对齐 Mikomi 的工程基线（多模块 + build-logic 约定插件 + Hilt + Navigation 3 + Route/Screen 双组件）。
> 本项目是**数据源测试台**：顶部 Tab 每个对应一个数据源，Tab 内容是该数据源的测试页。

## 1. 依赖方向

```text
app ──────────────► feature:console ─────► core:*
 │                        │
 └──────────────────────► source:anirpb / source:anicyk ──► core:source / core:ui
```

- `app` 只做装配：Application / Activity / NavHost，不 import 任何页面实现。
- `feature:console` 提供 Tab 容器，不认识任何具体数据源。
- `source:*` 一个数据源一个模块：数据客户端 + 接口目录 + 测试页。
- `core:*` 不依赖上层，也不依赖任何具体数据源。

## 2. 模块职责

| 模块 | 职责 |
|------|------|
| `:app` | `AnimeDbApp` / `MainActivity` / `AnimeDbNavHost`；abiFilters + jniLibs 打包 |
| `:core:model` | 领域模型（预留） |
| `:core:common` | `@Dispatcher` 调度器；`formatBytes/formatMs/formatCount/hostLabel/prettyJson` |
| `:core:designsystem` | 色板 / 字体 / `AnimeDbTheme`；`ConsoleColors`（成功、警告、等宽底色） |
| `:core:ui` | 测试台组件：`ConsoleScaffold`、`OutputPanel`、`ApiCatalogRow`、`SegmentedControl`、`StatusPill`、`InfoChip`、`MethodBadge`、`StatTile`、`JsonBlock`、`CoverImage`、剪贴板/打开链接 |
| `:core:navigation` | `ConsoleDestination` / `Navigator` / `EntryProviderInstaller` |
| `:core:network` | 预留（HTTP 基础设施，当前两个数据源各自实现） |
| `:core:source` | `SourceTestTab` 契约 |
| `:feature:console` | 顶部 Tab 容器、空态 |
| `:source:anirpb` | `ZhuqueTransport`（UDS + ZQF1 + AES-GCM）、JNI 桥、33 个 op 目录、解析器、ViewModel、测试页、`jniLibs/arm64-v8a` |
| `:source:anicyk` | `AniCykClient`（HttpURLConnection + 4 节点降级 + AES-CBC）、14 个接口目录、解析器、ViewModel、测试页 |

## 3. Tab 注册机制

```kotlin
// core:source
interface SourceTestTab {
    val order: Int
    val sourceId: String
    val displayName: String
    val subtitle: String
    @Composable fun Content(modifier: Modifier)
}

// source:anirpb/di
@Module @InstallIn(SingletonComponent::class)
abstract class AniRpbModule {
    @Binds @IntoSet abstract fun bind(tab: AniRpbTestTab): SourceTestTab
}
```

`ConsoleViewModel` 注入 `Set<SourceTestTab>`，按 `order` 排序渲染。

## 4. 测试页结构（两个数据源共用）

```
ConsoleScaffold(panelExpanded)
├─ header   ← 状态胶囊 / 频道或关键词 / 快捷运行 / 提示行
├─ catalog  ← LazyColumn：分组 + ApiCatalogRow（点行编辑参数，▶ 一键运行）
└─ output   ← OutputPanel：固定在底部，42% ⇄ 68% 高度切换
              标题 + meta（frame/code/字节/耗时）+ 状态点
              [结构化 | JSON] + 复制
              结构化内容由各数据源自行渲染，未识别的接口回退 JSON
```

- 输出窗口始终在最下方，新报文到达时自动回到顶部。
- 参数编辑走 `ModalBottomSheet`：JSON 文本框 + 恢复默认 + 运行，参数先做 JSON 校验。
- 结构化渲染只覆盖高频接口，其余回退 JSON，新增 op 不需要改解析器。

## 5. AniRpb 链路要点

- `ZhuqueTransport`：`Os.setenv(ZQ_FILES_DIR)` → `loadLibrary(addressing_ffi/zhuque)` → `nativeDeviceContextInit(DeviceContext JSON)` → `nativeSessionKey()` 拿 32B 帧密钥 → UDS `ZQF1` 帧（AES-256-GCM）。
- `libzhuque.so` / `libaddressing_ffi.so` 放在 `source/anirpb/src/main/jniLibs/arm64-v8a/`，`app` 侧 `useLegacyPackaging = true` + `abiFilters = arm64-v8a`。
- JNI 桥 `com.yy.myuko.component.ktor.addressing.ZhuqueSessionNativeLib` 包名/类名不可改。
- **bootstrap**：首次业务调用前原生层要跑寻址 + MMTLS 握手，期间回 `503`（`tunnel-unreachable` / `op dormant`）。ViewModel 会轮询 `type.listActive` 直到 200，单次运行遇到同类 503 也会自动重试最多 3 次。

## 6. AniCyk 链路要点

- 4 个明文 HTTP 节点串行降级，2xx/4xx 即视为节点可用。
- 头部：`app-version`、`appId`、`deviceId`、`imei`、`cvb`（时间戳 Base64）。
- 响应以 `CYK2026` 开头 → AES-256-CBC 解密（key = SHA-256("%ZZJ_ciyuan_2025!@")）；`Dddm` 开头 → Base64 明文。

## 7. 下一步

1. `:core:network` 抽出通用 HTTP 执行引擎与请求录制，供后续 HTTP 类数据源复用。
2. 请求历史（最近 N 次调用）与报文对比。
3. 更多 op 的结构化渲染；把全量探测结果导出为文件。
4. Tab 数量变多后，`ConsoleTopBar` 换 `PrimaryScrollableTabRow`。
