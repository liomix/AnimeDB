package com.liomix.animedb.source.anicyk.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liomix.animedb.core.designsystem.theme.MonoTextStyle
import com.liomix.animedb.core.source.SourceTestTab
import com.liomix.animedb.core.ui.component.ApiCatalogRow
import com.liomix.animedb.core.ui.component.ConsoleScaffold
import com.liomix.animedb.core.ui.component.OutputPanel
import com.liomix.animedb.core.ui.component.PillTone
import com.liomix.animedb.core.ui.component.SegmentedControl
import com.liomix.animedb.core.ui.component.StatusPill
import com.liomix.animedb.core.ui.model.OutputMode
import com.liomix.animedb.core.ui.util.copyToClipboard
import com.liomix.animedb.source.anicyk.AniCykConsoleViewModel
import com.liomix.animedb.source.anicyk.CykUiState
import com.liomix.animedb.source.anicyk.data.CykApi
import com.liomix.animedb.source.anicyk.data.CykCatalog
import javax.inject.Inject

/** AniCyk 测试页：HTTP 直连 + 4 节点降级 + AES 解密。 */
class AniCykTestTab @Inject constructor() : SourceTestTab {

    override val order: Int = 1

    override val sourceId: String = "anicyk"

    override val displayName: String = "AniCyk"

    override val subtitle: String = "CYK · HTTP / AES"

    @Composable
    override fun Content(modifier: Modifier) {
        val viewModel: AniCykConsoleViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        AniCykConsoleScreen(
            state = state,
            onSearchType = viewModel::setSearchType,
            onScheduleType = viewModel::setScheduleType,
            onKeyword = viewModel::setKeyword,
            onQuickLoad = viewModel::quickLoad,
            onOpenParams = viewModel::openParams,
            onModeChange = viewModel::setMode,
            onTogglePanel = viewModel::togglePanel,
            onParamsTextChange = viewModel::updateParams,
            onResetParams = viewModel::resetParams,
            onRunFromParams = viewModel::runFromParams,
            onCloseParams = viewModel::closeParams,
            modifier = modifier,
        )
    }
}

@Composable
fun AniCykConsoleScreen(
    state: CykUiState,
    onSearchType: (String) -> Unit,
    onScheduleType: (String) -> Unit,
    onKeyword: (String) -> Unit,
    onQuickLoad: (String) -> Unit,
    onOpenParams: (CykApi) -> Unit,
    onModeChange: (OutputMode) -> Unit,
    onTogglePanel: () -> Unit,
    onParamsTextChange: (String) -> Unit,
    onResetParams: () -> Unit,
    onRunFromParams: () -> Unit,
    onCloseParams: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    ConsoleScaffold(
        panelExpanded = state.panelExpanded,
        modifier = modifier,
        header = {
            CykHeader(
                state = state,
                onSearchType = onSearchType,
                onScheduleType = onScheduleType,
                onKeyword = onKeyword,
                onQuickLoad = onQuickLoad,
            )
        },
        catalog = { catalogModifier ->
            LazyColumn(
                modifier = catalogModifier,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CykCatalog.groups().forEach { (group, apis) ->
                    item(key = "group-$group") {
                        Text(
                            text = group,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                        )
                    }
                    items(apis, key = { it.key }) { api ->
                        ApiCatalogRow(
                            title = api.title,
                            code = api.path,
                            note = api.note,
                            badge = api.method,
                            badgeTone = if (api.isGet) PillTone.Ok else PillTone.Warning,
                            running = state.runningKey == api.key,
                            onOpen = { onOpenParams(api) },
                            onRun = { onQuickLoad(api.key) },
                        )
                    }
                }
            }
        },
        output = { outputModifier ->
            OutputPanel(
                output = state.output,
                mode = state.outputMode,
                onModeChange = onModeChange,
                expanded = state.panelExpanded,
                onToggleExpanded = onTogglePanel,
                onCopy = { context.copyToClipboard("animedb", state.output.json) },
                modifier = outputModifier,
            ) {
                CykStructuredContent(view = state.view)
            }
        },
    )

    state.paramsApi?.let { api ->
        CykParamsSheet(
            api = api,
            text = state.paramsText,
            onTextChange = onParamsTextChange,
            onReset = onResetParams,
            onRun = onRunFromParams,
            onDismiss = onCloseParams,
        )
    }
}

@Composable
private fun CykHeader(
    state: CykUiState,
    onSearchType: (String) -> Unit,
    onScheduleType: (String) -> Unit,
    onKeyword: (String) -> Unit,
    onQuickLoad: (String) -> Unit,
) {
    val apiTone = when {
        state.runningKey != null -> PillTone.Warning
        state.lastHttp in 200..299 -> PillTone.Ok
        state.lastHttp > 0 -> PillTone.Error
        else -> PillTone.Idle
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(
                label = if (state.lastHttp in 200..299) "API · ${state.lastHttp}" else "API",
                tone = apiTone,
                onClick = { onQuickLoad("config") },
            )
            Text(
                text = if (state.lastNode.isBlank()) {
                    "4 个节点 · 匿名只读"
                } else {
                    "${state.nodeLabel} · ${state.lastMs}ms" + if (state.encrypted) " · AES" else ""
                },
                style = MonoTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                maxLines = 1,
            )
            IconButton(onClick = { onQuickLoad("config") }, enabled = !state.busy) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "重新读取配置",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.keyword,
                onValueChange = onKeyword,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = { Text("关键词", style = MaterialTheme.typography.bodyMedium) },
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onQuickLoad("search2") }),
            )
            FilledTonalButton(
                onClick = { onQuickLoad("search2") },
                enabled = !state.busy,
                contentPadding = PaddingValues(horizontal = 18.dp),
            ) {
                Text("搜索", maxLines = 1, softWrap = false)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SegmentedControl(
                options = listOf("默认源", "备用源"),
                selectedIndex = if (state.searchType == "DEFAULT") 0 else 1,
                onSelect = { onSearchType(if (it == 0) "DEFAULT" else "BACKUP") },
                modifier = Modifier.weight(1f),
                height = 32,
            )
            SegmentedControl(
                options = listOf("超清", "樱花"),
                selectedIndex = if (state.scheduleType == "超清") 0 else 1,
                onSelect = { onScheduleType(if (it == 0) "超清" else "樱花") },
                modifier = Modifier.weight(1f),
                height = 32,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                "config" to "应用配置",
                "schedule" to "追番日历",
                "hot" to "热搜词",
                "rule" to "搜索规则",
                "banList" to "屏蔽词",
            ).forEach { (key, label) ->
                AssistChip(
                    onClick = { onQuickLoad(key) },
                    enabled = !state.busy,
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }
        }

        Text(
            text = state.footer,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CykParamsSheet(
    api: CykApi,
    text: String,
    onTextChange: (String) -> Unit,
    onReset: () -> Unit,
    onRun: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = api.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${api.method} ${api.path} · ${api.note.ifBlank { "可编辑参数 JSON 后运行" }}",
                style = MonoTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MonoTextStyle,
                minLines = 4,
                maxLines = 9,
                shape = RoundedCornerShape(12.dp),
            )
            Text(
                text = "GET 参数拼到查询串；POST 参数以表单提交。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onReset) { Text("恢复默认") }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                ) {
                    OutlinedButton(onClick = onRun) { Text("运行") }
                }
            }
        }
    }
}
