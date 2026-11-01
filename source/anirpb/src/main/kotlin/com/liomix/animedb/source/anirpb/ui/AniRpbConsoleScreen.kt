package com.liomix.animedb.source.anirpb.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.liomix.animedb.core.designsystem.theme.MonoTextStyle
import com.liomix.animedb.core.source.SourceTestTab
import com.liomix.animedb.core.ui.component.ApiCatalogRow
import com.liomix.animedb.core.ui.component.ConsoleScaffold
import com.liomix.animedb.core.ui.component.ConsoleSearchField
import com.liomix.animedb.core.ui.component.InfoChip
import com.liomix.animedb.core.ui.component.OutputPanel
import com.liomix.animedb.core.ui.component.PillTone
import com.liomix.animedb.core.ui.component.SegmentedControl
import com.liomix.animedb.core.ui.component.StatusPill
import com.liomix.animedb.core.ui.model.OutputMode
import com.liomix.animedb.core.ui.util.copyToClipboard
import com.liomix.animedb.source.anirpb.AniRpbConsoleViewModel
import com.liomix.animedb.source.anirpb.RpbChannel
import com.liomix.animedb.source.anirpb.RpbUiState
import com.liomix.animedb.source.anirpb.data.RpbApi
import com.liomix.animedb.source.anirpb.data.RpbCatalog
import com.liomix.animedb.source.anirpb.data.RpbProbeRow
import java.util.Locale
import javax.inject.Inject

/** AniRpb 测试页：Zhuque 原生链路的接口测试台。 */
class AniRpbTestTab @Inject constructor() : SourceTestTab {

    override val order: Int = 0

    override val sourceId: String = "anirpb"

    override val displayName: String = "AniRpb"

    override val subtitle: String = "Zhuque · ESDK / MMTLS"

    @Composable
    override fun Content(modifier: Modifier) {
        val viewModel: AniRpbConsoleViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        AniRpbConsoleScreen(
            state = state,
            onSelfCheck = viewModel::selfCheck,
            onChannel = viewModel::selectChannel,
            onDate = viewModel::setDate,
            onQuery = viewModel::setQuery,
            onRunQuick = viewModel::runQuick,
            onOpenParams = viewModel::openParams,
            onRunAll = viewModel::runAll,
            onStopAll = viewModel::stopAll,
            onModeChange = viewModel::setMode,
            onTogglePanel = viewModel::togglePanel,
            onProbeRowClick = viewModel::openProbeRow,
            onParamsTextChange = viewModel::updateParams,
            onResetParams = viewModel::resetParams,
            onRunFromParams = viewModel::runFromParams,
            onCloseParams = viewModel::closeParams,
            modifier = modifier,
        )
    }
}

@Composable
fun AniRpbConsoleScreen(
    state: RpbUiState,
    onSelfCheck: () -> Unit,
    onChannel: (RpbChannel) -> Unit,
    onDate: (String) -> Unit,
    onQuery: (String) -> Unit,
    onRunQuick: (String) -> Unit,
    onOpenParams: (RpbApi) -> Unit,
    onRunAll: () -> Unit,
    onStopAll: () -> Unit,
    onModeChange: (OutputMode) -> Unit,
    onTogglePanel: () -> Unit,
    onProbeRowClick: (RpbProbeRow) -> Unit,
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
            RpbHeader(
                state = state,
                onSelfCheck = onSelfCheck,
                onChannel = onChannel,
                onDate = onDate,
                onQuery = onQuery,
                onRunQuick = onRunQuick,
                onRunAll = onRunAll,
                onStopAll = onStopAll,
            )
        },
        catalog = { catalogModifier ->
            val filtered = RpbCatalog.apis.filter { api ->
                state.query.isBlank() ||
                    api.title.contains(state.query, ignoreCase = true) ||
                    api.op.contains(state.query, ignoreCase = true)
            }
            LazyColumn(
                modifier = catalogModifier,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RpbCatalog.groups(filtered).forEach { (group, apis) ->
                    item(key = "group-$group") {
                        Text(
                            text = group,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                        )
                    }
                    items(apis, key = { it.op }) { api ->
                        ApiCatalogRow(
                            title = api.title,
                            code = api.op,
                            note = api.note,
                            running = state.runningOp == api.op,
                            onOpen = { onOpenParams(api) },
                            onRun = { onRunQuick(api.op) },
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
                resetKey = state.probeDone,
            ) {
                RpbStructuredContent(view = state.view, onProbeRowClick = onProbeRowClick)
            }
        },
    )

    state.paramsApi?.let { api ->
        RpbParamsSheet(
            api = api,
            text = state.paramsText,
            onTextChange = onParamsTextChange,
            onReset = onResetParams,
            onRun = onRunFromParams,
            onDismiss = onCloseParams,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RpbHeader(
    state: RpbUiState,
    onSelfCheck: () -> Unit,
    onChannel: (RpbChannel) -> Unit,
    onDate: (String) -> Unit,
    onQuery: (String) -> Unit,
    onRunQuick: (String) -> Unit,
    onRunAll: () -> Unit,
    onStopAll: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatusPill(
                    label = "LIB",
                    tone = state.tone(state.libReady),
                    onClick = onSelfCheck,
                )
                StatusPill(
                    label = "KEY",
                    tone = state.tone(state.keyReady),
                    onClick = onSelfCheck,
                )
                StatusPill(
                    label = "UDS",
                    tone = state.tone(state.udsReady),
                    onClick = onSelfCheck,
                )
            }
            IconButton(
                onClick = { if (state.probing) onStopAll() else onRunAll() },
                enabled = !state.busy || state.probing,
            ) {
                Icon(
                    imageVector = if (state.probing) Icons.Outlined.Stop else Icons.Outlined.Science,
                    contentDescription = if (state.probing) "停止全量探测" else "全量探测",
                    tint = if (state.probing) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
            IconButton(onClick = onSelfCheck, enabled = !state.busy && !state.booting) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "链路自检",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SegmentedControl(
                options = listOf(RpbChannel.Jp.label, RpbChannel.Cn.label),
                selectedIndex = state.channel.ordinal,
                onSelect = { onChannel(RpbChannel.entries[it]) },
                modifier = Modifier.weight(1f),
            )
            InfoChip(
                label = state.date,
                tone = PillTone.Accent,
                onClick = { pickDate(context, state.date, onDate) },
            )
        }

        ConsoleSearchField(
            value = state.query,
            onValueChange = onQuery,
            placeholder = "过滤接口名称或 op",
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                "home.load" to "热播榜",
                "video.categoryList" to "整榜",
                "calendar.videos" to "更新表",
                "type.listActive" to "频道",
            ).forEach { (op, label) ->
                AssistChip(
                    onClick = { onRunQuick(op) },
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
            text = state.bootError
                ?: if (state.probing) "全量探测 ${state.probeDone}/${state.probeTotal}" else state.footer,
            style = MaterialTheme.typography.bodySmall,
            color = if (state.bootError != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RpbParamsSheet(
    api: RpbApi,
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
                text = "${api.op} · ${api.note.ifBlank { "可编辑 JSON 参数后运行" }}",
                style = MonoTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MonoTextStyle,
                minLines = 5,
                maxLines = 10,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            )
            Text(
                text = "变量：{CHANNEL} {VIDEO} {CODE} {DATE} {EXCHANGE}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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

private fun RpbUiState.tone(ready: Boolean): PillTone = when {
    ready -> PillTone.Ok
    booting -> PillTone.Warning
    bootError != null -> PillTone.Error
    else -> PillTone.Idle
}

private fun pickDate(
    context: android.content.Context,
    current: String,
    onPick: (String) -> Unit,
) {
    val parts = current.split("-")
    DatePickerDialog(
        context,
        { _, year, month, day ->
            onPick(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day))
        },
        parts.getOrNull(0)?.toIntOrNull() ?: 2026,
        (parts.getOrNull(1)?.toIntOrNull() ?: 10) - 1,
        parts.getOrNull(2)?.toIntOrNull() ?: 30,
    ).show()
}
