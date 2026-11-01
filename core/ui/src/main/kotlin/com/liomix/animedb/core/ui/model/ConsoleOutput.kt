package com.liomix.animedb.core.ui.model

/** 输出窗口的显示模式。 */
enum class OutputMode { Structured, Json }

/** 一次请求的整体状态，用于标题右侧的状态点与配色。 */
enum class OutputStatus { Idle, Running, Ok, Failed }

/** 输出窗口的统一数据：标题、meta 行、原始报文、错误。 */
data class ConsoleOutput(
    val title: String = "等待请求",
    val meta: String = "",
    val json: String = "",
    val error: String? = null,
    val status: OutputStatus = OutputStatus.Idle,
) {
    val isIdle: Boolean get() = status == OutputStatus.Idle
}
