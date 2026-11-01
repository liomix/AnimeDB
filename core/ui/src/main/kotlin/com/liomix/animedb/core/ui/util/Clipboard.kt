package com.liomix.animedb.core.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Android 侧复制，避免依赖 Compose 剪贴板 API 的版本差异。 */
fun Context.copyToClipboard(label: String, text: String) {
    val manager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    manager.setPrimaryClip(ClipData.newPlainText(label, text))
}

/** 打开外部链接；失败返回 false，由调用方决定提示。 */
fun Context.openUrl(url: String): Boolean {
    if (url.isBlank()) return false
    return runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)
}
