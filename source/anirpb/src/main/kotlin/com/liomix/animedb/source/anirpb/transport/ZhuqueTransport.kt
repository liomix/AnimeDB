package com.liomix.animedb.source.anirpb.transport

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.ConnectivityManager
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.system.Os
import android.util.Base64
import android.util.Log
import com.yy.myuko.component.ktor.addressing.ZhuqueSessionNativeLib
import org.json.JSONObject
import java.io.DataInputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 复刻官方 APK 的“本地帧 + libzhuque”传输层。
 *
 * 结构（与 APK 内的 Kotlin 代码一一对应）：
 *   1. Os.setenv("ZQ_FILES_DIR", filesDir)          —— 告诉 so 把 UDS 建在哪
 *   2. System.loadLibrary("addressing_ffi"/"zhuque")—— 加载原生传输
 *   3. nativeDeviceContextInit(DeviceContext JSON)  —— 设备上下文
 *   4. nativeSessionKey() -> 32 字节 AES-GCM key     —— 本地帧密钥
 *   5. 连 <filesDir>/.zqsock-<pid>，发送 ZQF1 帧：
 *        "ZQF1" + version(1 byte) + IV(12) + len(4, BE) + AES-GCM(payload)
 *      payload = FrameSemanticRequest JSON
 *   6. 读回 ZQF1 帧，解密成 FrameSemanticResponse{token,status,bodyB64}
 *
 * 真正的 MMTLS/ESDK 握手、bootstrap、寻址、重协商都在 libzhuque.so 内部完成，
 * 这里不需要也不应该自己实现密码学。
 */
@Singleton
class ZhuqueTransport @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        private const val TAG = "ZhuqueHarness"
        private const val FRAME_MAGIC_0 = 0x5A.toByte() // Z
        private const val FRAME_MAGIC_1 = 0x51.toByte() // Q
        private const val FRAME_MAGIC_2 = 0x46.toByte() // F
        private const val FRAME_MAGIC_3 = 0x31.toByte() // 1
        private const val FRAME_VERSION_SEMANTIC = 3.toByte()
        private const val MAX_FRAME = 8 * 1024 * 1024

        /**
         * 上报的包名/签名。
         * 默认上报本 Harness 自己的包名与签名；如果服务端校验官方身份，
         * 可以改成 false 之外的值并填入官方信息（见 README）。
         */
        @Volatile
        var useOriginalIdentity: Boolean = false

        /**
         * 与官方 RdicReplaySafeOps 一致的“可安全重放”集合。
         * 注意 home.load 不在集合里，replaySafe 必须为 false。
         */
        private val REPLAY_SAFE_OPS = setOf(
            "ad.getSlots", "profile.userInfo", "profile.query", "profile.defaultAvatars",
            "auth.checkEmailWhitelist", "auth.listEmailWhitelist", "auth.passwordPolicy", "auth.status",
            "device.list", "filter.dimensions", "filter.options", "video.categoryList", "type.listActive",
            "search.config", "search.suggest", "resolve.episodes", "danmaku.list", "danmaku.config",
            "danmaku.coinCheck", "danmaku.curatedList", "comment.config", "comment.list", "comment.replies",
            "comment.my", "review.summary", "review.list", "interaction.likedStatus", "episode.likeCount",
            "episode.batchLikeCount", "episode.likedStatus", "episode.batchLikedStatus", "engagement.counts",
            "favorite.status", "favorite.list", "subscription.list", "resume.list", "resume.detail",
            "cast.entitlement", "vip.info", "vip.plans", "vip.subscribe.status", "vip.signInCalendar",
            "vip.signInRewardPreview", "vip.signInDoubleStatus", "vip.taskList", "vip.taskConfig",
            "vip.taskProgress", "vip.taskRewardStatus", "vip.watchProgressStatus", "vip.walletInfo",
            "vip.serverTime", "welfare.hub", "coin.records", "membership.catalog", "bubbleSkin.catalog",
            "notice.list", "notice.popupList", "notification.sessions", "notification.unreadCount",
            "notification.subscriptionFeed", "feedback.types", "feedback.list", "feedback.replies",
            "download.episodes", "download.quota", "download.cacheList", "banner.list", "calendar.videos",
            "guide.listActive", "splash.active", "config.resolve", "base.agreement", "ui.config",
            "ui.version", "dynscript.head", "dynscript.blob", "dynscript.pubkeys",
            "zhuque.transportConfig", "auth.refreshToken"
        )

        // 官方 APK 的签名证书（从 AniRpb.apk 的 META-INF/YZS.RSA 提取，公开信息）
        const val ORIGINAL_PKG = "com.sdmnfowa.soad.p1"
        const val ORIGINAL_ASIG = "de17140c22b492a9e38d188c675a185d9d67e1607342f03ed0a181d9f3696e74"
        const val ORIGINAL_ACERT =
            "MIIDRzCCAi+gAwIBAgIEOZfzfDANBgkqhkiG9w0BAQsFADBTMQswCQYDVQQGEwJjbjELMAkGA1UECBMCYnYxCzAJBgNVBAcTAnBrMQ0wCwYDVQQKEwR0amF4MQ0wCwYDVQQLEwR0amF4MQwwCgYDVQQDEwN5enMwIBcNMjQwOTExMDEyODQ3WhgPMjA2MzAxMTAwMTI4NDdaMFMxCzAJBgNVBAYTAmNuMQswCQYDVQQIEwJidjELMAkGA1UEBxMCcGsxDTALBgNVBAoTBHRqYXgxDTALBgNVBAsTBHRqYXgxDDAKBgNVBAMTA3l6czCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKbcaD5vhksvYyvUvtLQvvacGv129oQE0dgTwxdfOHQAN1V3lcayud6BM3nY7Nr8YOUAWxS90fp9XhR3izmIXhYvH2ZvhsLaboICdmI1SAR35/rvOlxCeuzo5GEFF1e3ZcjU/BDUc/rgDU1jcoX6X5Kadhy/oo1mxr2fwRmVnU2ttvk5H8tMQjn/g6ReLnhfgyd866kdkyGC3BxMcER6P3dyEKNdqxBQseS8lxkWuPbi4V9hVcX6xAg4OMsxCXhp3BhmM7K2IJo73O1qXbKL3VltUHAMO8Sj6BLU4e+ZAUAChppQlJ2X3Pbdy+q5wFSWukOX7SZMjZonBW64FtKcVKsCAwEAAaMhMB8wHQYDVR0OBBYEFH+FmOVzIiG1d8ynjKIWUhaUo01dMA0GCSqGSIb3DQEBCwUAA4IBAQAg6UheCCOipgyP7gBHxQfI1gpClvXyFZg0MQz62hLnDzW4XQZNm3dw5t/FMb8f3FHT+JQ92GAw1kcu8a+ZyhDITMsalEToc/VUnwt1D7OTGIQsq6YABpxCvGOewZb1U/jxInB0U725CIurNab8Y1aspbZUiohKQ/qjwXmmZph1UMmXX5I4bXtsh0ueZlI3pdKCyH+nX1HhZiz3UtV2U9hljmm0PGvcyTv2S/1Zw2HLJkXS6NS/Or5JsiiIdwJeor3Fns57Ui9ENrLZyert1h8UP24W+AZfOIMTrl1x+OUd5cy5WC7J2VypY6VI+hgy1NsPb8EqTXahznIVqRZ6irhO"
    }

    private val random = SecureRandom()
    private val loaded = java.util.concurrent.atomic.AtomicBoolean(false)
    private val linkLock = Any()
    private var linkSocket: LocalSocket? = null
    private var linkInput: DataInputStream? = null

    /** 第一次调用：设置环境变量、加载 so、提交 DeviceContext。 */
    fun initialize(): String {
        if (loaded.get()) return "already initialized"

        Os.setenv("ZQ_FILES_DIR", context.filesDir.absolutePath, true)

        // 先加载 addressing_ffi（zhuque 内部也会 dlopen，显式加载一次更稳）
        runCatching { System.loadLibrary("addressing_ffi") }
            .onFailure { Log.w(TAG, "load addressing_ffi failed: $it") }
        System.loadLibrary("zhuque")
        loaded.set(true)

        val deviceContext = buildDeviceContext()
        Log.i(TAG, "deviceContext=$deviceContext")

        val ok = ZhuqueSessionNativeLib.nativeDeviceContextInit(
            deviceContext.toByteArray(Charsets.UTF_8)
        )
        Log.i(TAG, "nativeDeviceContextInit=$ok socket=${socketPath()}")
        return "deviceContextInit=$ok\nsocket=${socketPath()}\npid=${Process.myPid()}"
    }

    /** 幂等初始化；session key / 业务调用前会自动调用。 */
    @Synchronized
    fun ensureInitialized() {
        if (!loaded.get()) initialize()
    }

    /** 轮询等待本地帧密钥（32 字节）。 */
    fun awaitSessionKey(timeoutMs: Long = 20_000): ByteArray? {
        ensureInitialized()
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            val key = runCatching { ZhuqueSessionNativeLib.nativeSessionKey() }.getOrNull()
            if (key != null && key.size == 32) return key
            Thread.sleep(200)
        }
        return null
    }

    // ------------------------------------------------------------------
    // 业务：三个接口
    // ------------------------------------------------------------------

    /** 日漫/国漫热播榜（home.load） */
    fun hotRanking(categoryId: String, pageIndex: Int = 1, pageSize: Int = 20): JSONObject {
        val params = JSONObject()
            .put("categoryId", categoryId)
            .put("pageIndex", pageIndex)
            .put("pageSize", pageSize)
            .toString()
        return semantic("home.load", params)
    }

    /** 整榜分页（video.categoryList，sort=hottest） */
    fun hotList(categoryId: String, pageIndex: Int = 1, pageSize: Int = 20): JSONObject {
        val params = JSONObject()
            .put("pageIndex", pageIndex)
            .put("pageSize", pageSize)
            .put("categoryId", categoryId)
            .put("filters", org.json.JSONArray())
            .put("sort", "hottest")
            .put("billing", "")
            .put("seriesMerge", 0)
            .toString()
        return semantic("video.categoryList", params)
    }

    /** 更新表（calendar.videos） */
    fun calendar(playDate: String, pageIndex: Int = 1, pageSize: Int = 20): JSONObject {
        val params = JSONObject()
            .put("playDate", playDate)
            .put("pageIndex", pageIndex)
            .put("pageSize", pageSize)
            .toString()
        return semantic("calendar.videos", params)
    }

    /** 频道列表（type.listActive） */
    fun channels(): JSONObject {
        val params = JSONObject()
            .put("pageIndex", 1)
            .put("pageSize", 100)
            .put("parentId", JSONObject.NULL)
            .put("parentCode", JSONObject.NULL)
            .put("withChannel", JSONObject.NULL)
            .put("dataId", JSONObject.NULL)
            .toString()
        return semantic("type.listActive", params)
    }

    // ------------------------------------------------------------------
    // RDIC 语义帧
    // ------------------------------------------------------------------

    fun semantic(op: String, paramsJson: String, attempts: Int = 3): JSONObject {
        var last: Exception? = null
        for (i in 0 until attempts) {
            try {
                return semanticOnce(op, paramsJson)
            } catch (t: Throwable) {
                last = t as? Exception ?: RuntimeException(t)
                Log.w(TAG, "semantic($op) attempt ${i + 1} failed: ${t.message}")
                Thread.sleep(800L * (i + 1))
            }
        }
        throw last ?: IllegalStateException("semantic($op) failed")
    }

    private fun semanticOnce(op: String, paramsJson: String): JSONObject {
        val key = awaitSessionKey(20_000)
            ?: throw IllegalStateException("session key 未就绪：libzhuque 还没起来（检查 so/ABI/ZQ_FILES_DIR）")

        val token = hex(randomBytes(16))
        val request = JSONObject()
            .put("token", token)
            .put("op", op)
            .put("params", paramsJson)
            .put("ad_signature", JSONObject.NULL)
            .put("authorization", JSONObject.NULL)
            .put("replay_safe", op in REPLAY_SAFE_OPS)
            .toString()

        val frame = sealFrame(key, FRAME_VERSION_SEMANTIC, request.toByteArray(Charsets.UTF_8))

        // 与官方 LocalSocketRdicLink 一样复用一条长连接；出错则关闭重建。
        synchronized(linkLock) {
            try {
                val (socket, input) = ensureLink()
                socket.outputStream.write(frame)
                socket.outputStream.flush()

                val raw = readFrame(key, input)
                    ?: throw IllegalStateException("bad/empty response frame")

                val response = JSONObject(String(raw, Charsets.UTF_8))
                val status = response.optInt("status", -1)
                val tokenBack = response.optString("token", "")
                val bodyB64 = response.optString("body_b64", "")
                val body = if (bodyB64.isEmpty()) "" else
                    String(Base64.decode(bodyB64, Base64.NO_WRAP), Charsets.UTF_8)

                Log.i(TAG, "op=$op status=$status tokenMatch=${tokenBack == token} bodyLen=${body.length}")
                Log.i(TAG, "body[$op]=${body.take(400)}")
                runCatching {
                    java.io.File(context.filesDir, "last_" + op.replace('.', '_') + ".json")
                        .writeText(body, Charsets.UTF_8)
                }
                return JSONObject()
                    .put("op", op)
                    .put("frameStatus", status)
                    .put("tokenMatch", tokenBack == token)
                    .put("body", body)
            } catch (t: Throwable) {
                closeLink()
                throw t
            }
        }
    }

    private fun ensureLink(): Pair<LocalSocket, DataInputStream> {
        val existing = linkSocket
        val existingInput = linkInput
        if (existing != null && existingInput != null) return existing to existingInput

        val socket = LocalSocket(2) // 与官方 ZhuqueLocalSocket 完全一致的构造参数
        socket.connect(
            LocalSocketAddress(socketPath(), LocalSocketAddress.Namespace.FILESYSTEM)
        )
        socket.soTimeout = 60_000
        val input = DataInputStream(socket.inputStream)
        linkSocket = socket
        linkInput = input
        Log.i(TAG, "UDS connected: ${socketPath()}")
        return socket to input
    }

    private fun closeLink() {
        runCatching { linkSocket?.close() }
        linkSocket = null
        linkInput = null
    }

    // ------------------------------------------------------------------
    // ZQF1 帧
    // ------------------------------------------------------------------

    private fun sealFrame(key: ByteArray, version: Byte, payload: ByteArray): ByteArray {
        val iv = randomBytes(12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val cipherText = cipher.doFinal(payload)

        val out = ByteArray(21 + cipherText.size)
        out[0] = FRAME_MAGIC_0
        out[1] = FRAME_MAGIC_1
        out[2] = FRAME_MAGIC_2
        out[3] = FRAME_MAGIC_3
        out[4] = version
        System.arraycopy(iv, 0, out, 5, 12)
        val len = cipherText.size
        out[17] = (len ushr 24).toByte()
        out[18] = (len ushr 16).toByte()
        out[19] = (len ushr 8).toByte()
        out[20] = len.toByte()
        System.arraycopy(cipherText, 0, out, 21, cipherText.size)
        return out
    }

    private fun readFrame(key: ByteArray, input: DataInputStream): ByteArray? {
        val header = ByteArray(21)
        input.readFully(header)
        if (header[0] != FRAME_MAGIC_0 || header[1] != FRAME_MAGIC_1 ||
            header[2] != FRAME_MAGIC_2 || header[3] != FRAME_MAGIC_3
        ) {
            return null
        }
        val iv = header.copyOfRange(5, 17)
        val len = ((header[17].toInt() and 0xFF) shl 24) or
                ((header[18].toInt() and 0xFF) shl 16) or
                ((header[19].toInt() and 0xFF) shl 8) or
                (header[20].toInt() and 0xFF)
        if (len < 16 || len > MAX_FRAME) return null

        val cipherText = ByteArray(len)
        input.readFully(cipherText)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(cipherText)
    }

    // ------------------------------------------------------------------
    // DeviceContext（20 字段，字段名与官方 @Serializable 模型一致）
    // ------------------------------------------------------------------

    @Suppress("DEPRECATION")
    private fun buildDeviceContext(): String {
        val pm = context.packageManager
        val packageInfo = pm.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName ?: "Unknown"
        val versionCode = if (Build.VERSION.SDK_INT >= 28) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }

        val sigs = signatures()
        val certDer = if (sigs.isNotEmpty()) sigs[0].toByteArray() else ByteArray(0)
        var asig = if (certDer.isNotEmpty()) hex(sha256(certDer)) else ""
        var acert = if (certDer.isNotEmpty()) Base64.encodeToString(certDer, Base64.NO_WRAP) else ""
        var pkg = context.packageName

        if (useOriginalIdentity) {
            pkg = ORIGINAL_PKG
            asig = ORIGINAL_ASIG
            acert = ORIGINAL_ACERT
        }

        val metrics = context.resources.displayMetrics
        val deviceId = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ANDROID_ID
        )?.takeIf { it.isNotBlank() } ?: "Unknown"

        val json = JSONObject()
        json.put("fp", deviceId)
        json.put("dn", "${Build.BRAND} ${Build.MODEL}".trim().ifBlank { "Unknown" })
        json.put("dm", "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Unknown" })
        json.put("os", "Android")
        json.put("osv", Build.VERSION.RELEASE ?: "Unknown")
        json.put("av", versionName)
        json.put("avc", versionCode)
        json.put("pkg", pkg)
        json.put("asig", asig)
        json.put("hv", "0")
        json.put("pt", "android")
        json.put("dt", deviceType())
        json.put("sw", metrics.widthPixels)
        json.put("sh", metrics.heightPixels)
        json.put("lang", Locale.getDefault().toLanguageTag())
        json.put("tz", TimeZone.getDefault().id)
        json.put("net", networkType())
        json.put("ts", 0L) // 官方 RdicExtender 调用 buildAwaitingDeviceId(0L)，ts 恒为 0
        json.put("bi", biEncode(versionName, versionCode))
        json.put("acert", acert)
        return json.toString()
    }

    private fun deviceType(): Int {
        val smallest = context.resources.configuration.smallestScreenWidthDp
        return if (smallest >= 600) 2 else 1
    }

    private fun networkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "other"
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "other"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "mobile"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "other"
        }
    }

    /** 与官方 DeviceContextBiEncoder 等价的零宽字符编码。 */
    private fun biEncode(appVersion: String, versionCode: Long): String {
        val mask = 16 // specialEdition = false
        val sb = StringBuilder()
        for (i in 7 downTo 4) {
            sb.append(if ((1 shl i) and mask != 0) '\u2060' else '\u200B')
        }
        sb.append(appVersion).append('-').append(versionCode)
        for (i in 3 downTo 0) {
            sb.append(if ((1 shl i) and mask != 0) '\u2060' else '\u200B')
        }
        return sb.toString()
    }

    @Suppress("DEPRECATION")
    private fun signatures(): Array<Signature> {
        val pm = context.packageManager
        return if (Build.VERSION.SDK_INT >= 28) {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners ?: emptyArray()
        } else {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
                ?: emptyArray()
        }
    }

    private fun socketPath(): String =
        context.filesDir.absolutePath + "/.zqsock-" + Process.myPid()

    private fun randomBytes(size: Int): ByteArray =
        ByteArray(size).also { random.nextBytes(it) }

    private fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    private fun hex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append("0123456789abcdef"[v ushr 4])
            sb.append("0123456789abcdef"[v and 0x0F])
        }
        return sb.toString()
    }
}
