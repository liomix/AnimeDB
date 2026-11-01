package com.yy.myuko.component.ktor.addressing

/**
 * JNI 桥接类。
 *
 * 注意：包名、类名、方法名必须与原 APK 完全一致，
 * 因为 libzhuque.so 导出的是：
 *   Java_com_yy_myuko_component_ktor_addressing_ZhuqueSessionNativeLib_nativeDeviceContextInit
 *   Java_com_yy_myuko_component_ktor_addressing_ZhuqueSessionNativeLib_nativeNetworkChanged
 *   Java_com_yy_myuko_component_ktor_addressing_ZhuqueSessionNativeLib_nativeSessionKey
 *
 * 不要重命名这个类或移动包。
 */
object ZhuqueSessionNativeLib {

    /** 传入 DeviceContext JSON（UTF-8 字节），返回原生层是否接受。 */
    external fun nativeDeviceContextInit(deviceContextJson: ByteArray): Boolean

    /** 网络状态变化通知（可不用）。 */
    external fun nativeNetworkChanged(online: Boolean, timestampMs: Long)

    /** 返回 32 字节的本地帧加密密钥；未就绪时可能返回 null / 非 32 字节。 */
    external fun nativeSessionKey(): ByteArray?
}
