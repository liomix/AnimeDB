package com.liomix.animedb.core.common.format

import java.net.URI
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/** 报文体积：B / KB / MB。 */
fun formatBytes(bytes: Int): String = when {
    bytes >= 1024 * 1024 -> String.format(Locale.US, "%.2f MB", bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}

/** 耗时：ms / s。 */
fun formatMs(ms: Long): String =
    if (ms >= 1000) String.format(Locale.US, "%.2fs", ms / 1000.0) else "${ms}ms"

/** 数量：万 / 亿。 */
fun formatCount(count: Long): String = when {
    count >= 100_000_000 -> String.format(Locale.US, "%.1f亿", count / 100_000_000.0)
    count >= 10_000 -> String.format(Locale.US, "%.1f万", count / 10_000.0)
    else -> count.toString()
}

/** 链接只显示 host，方便一眼看出走的是哪个源站。 */
fun hostLabel(url: String): String {
    if (url.isBlank()) return "—"
    return runCatching { URI(url).host }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: url.take(48)
}

/** 格式化 JSON；非法 JSON 原样返回。 */
fun prettyJson(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return "(empty)"
    return runCatching {
        if (trimmed.startsWith("[")) JSONArray(trimmed).toString(2) else JSONObject(trimmed).toString(2)
    }.getOrDefault(raw)
}

fun JSONArray.strings(): List<String> = buildList {
    for (i in 0 until length()) optString(i).takeIf { it.isNotBlank() }?.let(::add)
}

fun JSONObject.strings(key: String): List<String> = optJSONArray(key)?.strings().orEmpty()
