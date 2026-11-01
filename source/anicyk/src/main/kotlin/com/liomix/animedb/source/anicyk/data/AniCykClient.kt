package com.liomix.animedb.source.anicyk.data

import android.content.Context
import android.provider.Settings
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

data class CykResult(
    val node: String,
    val httpCode: Int,
    val body: String,
    val ms: Long,
    val encrypted: Boolean,
)

/**
 * 次元库主 API 客户端。
 *
 * 匿名只读接口只需要 `app-version` + `appId` 两个头；响应以 `CYK2026` 开头时
 * 按 AES-256-CBC 解密，`Dddm` 开头则是 Base64 明文。4 个节点串行降级。
 */
@Singleton
class AniCykClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    @Volatile
    var lastNode: String = ""
        private set

    /** 阻塞请求；调用方负责放到 IO 线程。 */
    fun request(method: String, path: String, params: JSONObject): CykResult {
        var lastFailure = "all nodes failed"
        for (node in NODES) {
            try {
                val result = requestNode(node, method, path, params)
                if (result.httpCode in 200..499) {
                    lastNode = node
                    return result
                }
                lastFailure = "HTTP ${result.httpCode} @ $node"
            } catch (throwable: Throwable) {
                lastFailure = "${throwable.javaClass.simpleName}: ${throwable.message} @ $node"
            }
        }
        throw IOException(lastFailure)
    }

    private fun requestNode(node: String, method: String, path: String, params: JSONObject): CykResult {
        val started = System.nanoTime()
        val body = encode(params)
        val isGet = method.equals("GET", ignoreCase = true)
        val url = node.trimEnd('/') + path + if (isGet && body.isNotBlank()) "?$body" else ""
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method.uppercase(Locale.US)
            connectTimeout = 8_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            useCaches = false
            setRequestProperty("app-version", APP_VERSION)
            setRequestProperty("appId", APP_ID)
            setRequestProperty("deviceId", deviceId())
            setRequestProperty("User-Agent", "okhttp/3.12.13")
            setRequestProperty("Accept", "application/json")
            val now = System.currentTimeMillis().toString()
            setRequestProperty(
                "imei",
                Base64.encodeToString(now.toByteArray(Charsets.UTF_8), Base64.NO_WRAP),
            )
            setRequestProperty(
                "cvb",
                Base64.encodeToString(
                    (System.currentTimeMillis() - APP_VERSION.toLong()).toString()
                        .toByteArray(Charsets.UTF_8),
                    Base64.NO_WRAP,
                ),
            )
            if (!isGet) {
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            }
        }
        if (!isGet) {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        val code = connection.responseCode
        val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        connection.disconnect()
        val (decoded, encrypted) = decode(raw)
        val elapsed = (System.nanoTime() - started) / 1_000_000
        return CykResult(node, code, decoded, elapsed, encrypted)
    }

    private fun encode(params: JSONObject): String {
        val keys = params.keys()
        val pairs = ArrayList<String>()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = params.opt(key)
            if (value == null || value == JSONObject.NULL) continue
            pairs += URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value.toString(), "UTF-8")
        }
        return pairs.joinToString("&")
    }

    private fun decode(rawInput: String): Pair<String, Boolean> {
        var raw = rawInput.trim()
        if (raw.startsWith("\uFEFF")) raw = raw.removePrefix("\uFEFF")
        if (raw.startsWith("Dddm")) {
            val text = runCatching {
                String(Base64.decode(raw.removePrefix("Dddm"), Base64.DEFAULT), Charsets.UTF_8)
            }.getOrNull()
            if (text != null) return text to true
        }
        if (!raw.startsWith("CYK2026")) return raw to false
        return runCatching {
            val payload = raw.removePrefix("CYK2026")
            val separator = payload.indexOf(':')
            require(separator > 0) { "missing iv separator" }
            val iv = Base64.decode(payload.substring(0, separator), Base64.DEFAULT)
            val cipherText = Base64.decode(payload.substring(separator + 1), Base64.DEFAULT)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(KEY, "AES"), IvParameterSpec(iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8) to true
        }.getOrDefault(raw to false)
    }

    private fun deviceId(): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "animedb"

    companion object {
        const val APP_VERSION = "278"
        const val APP_ID = "info.ciyuan.pro"

        val NODES = listOf(
            "http://47.103.33.207:8081",
            "http://47.93.60.215:8081",
            "http://39.106.100.156:8081",
            "http://8.134.159.218:8081",
        )

        private val KEY: ByteArray by lazy {
            MessageDigest.getInstance("SHA-256")
                .digest("%ZZJ_ciyuan_2025!@".toByteArray(Charsets.UTF_8))
        }
    }
}
