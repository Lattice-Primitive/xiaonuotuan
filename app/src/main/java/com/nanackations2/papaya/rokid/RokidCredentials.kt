package com.nanackations2.papaya.rokid

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Rokid CXR-M SDK 连接眼镜需要的凭据管理。
 *
 * SDK [com.rokid.cxr.client.extend.CxrApi.connectBluetooth] 接受：
 * - socketUuid: 蓝牙 RFCOMM/BLE 通道 UUID（在 initBluetooth/onConnectionInfo 回调里拿到）
 * - macAddress: 眼镜蓝牙 MAC
 * - snEncrypt: 厂商分配的 SN 加密串（开发者后台获取）
 * - clientSecret: 厂商分配的 Client Secret（开发者后台获取）
 *
 * 凭据来源优先级：
 * 1. 开发者页面填写的 Rokid Client Secret / Access Key
 * 2. 专用 SharedPreferences（兼容旧版本）
 * 3. BuildConfig（local.properties + build.gradle 里 buildConfigField 注入；若未配置则跳过）
 * 4. 默认空（首次连接会失败，提示用户填入）
 */
object RokidCredentials {
    private const val TAG = "RokidCredentials"
    private const val PREFS = "rokid_credentials"
    private const val DEV_PREFS = "dev_options"
    private const val KEY_CLIENT_ID = "client_id"
    private const val KEY_CLIENT_SECRET = "client_secret"
    private const val KEY_SN_ENCRYPT_HEX = "sn_encrypt_hex"
    private const val KEY_LAST_SOCKET_UUID = "last_socket_uuid"
    private const val KEY_LAST_MAC = "last_mac"
    private const val DEV_KEY_CLIENT_ID = "rokid_client_id"
    private const val DEV_KEY_CLIENT_SECRET = "rokid_client_secret"
    private const val DEV_KEY_ACCESS_KEY = "rokid_access_key"

    /** 完整凭据 + 上次连接信息，便于断线重连。 */
    data class Snapshot(
        val clientId: String,
        val clientSecret: String,
        val snEncryptHex: String,
        val lastSocketUuid: String?,
        val lastMacAddress: String?,
    ) {
        val isUsable: Boolean
            get() = clientSecret.isNotBlank() && snEncrypt.isNotEmpty()

        val snEncrypt: ByteArray
            get() = accessKeyToBytes(snEncryptHex)
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun devPrefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(DEV_PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): Snapshot {
        val sp = prefs(context)
        val devSp = devPrefs(context)
        // BuildConfig 兜底（如果项目里有 buildConfigField，可在此读取；当前项目未配置时返回空字符串）。
        val defaultClientId = buildConfigString("ROKID_CLIENT_ID")
        val defaultClientSecret = buildConfigString("ROKID_CLIENT_SECRET")
        val defaultSnEncryptHex = firstNonBlank(
            buildConfigString("ROKID_SN_ENCRYPT_HEX"),
            buildConfigString("ROKID_ACCESS_KEY"),
        )

        return Snapshot(
            clientId = firstNonBlank(
                devSp.getString(DEV_KEY_CLIENT_ID, null),
                sp.getString(KEY_CLIENT_ID, null),
                defaultClientId,
            ),
            clientSecret = firstNonBlank(
                devSp.getString(DEV_KEY_CLIENT_SECRET, null),
                sp.getString(KEY_CLIENT_SECRET, null),
                defaultClientSecret,
            ),
            snEncryptHex = firstNonBlank(
                devSp.getString(DEV_KEY_ACCESS_KEY, null),
                sp.getString(KEY_SN_ENCRYPT_HEX, null),
                defaultSnEncryptHex,
            ),
            lastSocketUuid = sp.getString(KEY_LAST_SOCKET_UUID, null),
            lastMacAddress = sp.getString(KEY_LAST_MAC, null),
        )
    }

    fun save(
        context: Context,
        clientId: String? = null,
        clientSecret: String? = null,
        snEncryptHex: String? = null,
    ) {
        val sp = prefs(context)
        sp.edit().apply {
            clientId?.let { putString(KEY_CLIENT_ID, it) }
            clientSecret?.let { putString(KEY_CLIENT_SECRET, it) }
            snEncryptHex?.let { putString(KEY_SN_ENCRYPT_HEX, it) }
        }.apply()
    }

    fun rememberConnection(context: Context, socketUuid: String?, macAddress: String?) {
        prefs(context).edit().apply {
            putString(KEY_LAST_SOCKET_UUID, socketUuid)
            putString(KEY_LAST_MAC, macAddress)
        }.apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun buildConfigString(field: String): String {
        return try {
            val cls = Class.forName("com.nanackations2.papaya.BuildConfig")
            val f = cls.getDeclaredField(field)
            (f.get(null) as? String).orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }

    private fun firstNonBlank(vararg values: String?): String =
        values.firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()

    private fun accessKeyToBytes(value: String): ByteArray {
        val raw = value.trim()
        if (raw.isEmpty()) return ByteArray(0)

        val hex = raw
            .removePrefix("0x")
            .replace(" ", "")
            .replace("-", "")
            .replace(":", "")

        if (hex.length % 2 == 0 && hex.all { it.digitToIntOrNull(16) != null }) {
            return hexToBytes(hex)
        }

        return raw.toByteArray(Charsets.UTF_8)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim().removePrefix("0x").replace(" ", "")
        if (clean.isEmpty() || clean.length % 2 != 0) return ByteArray(0)
        return try {
            ByteArray(clean.length / 2) { i ->
                val byte = clean.substring(i * 2, i * 2 + 2).toInt(16)
                byte.toByte()
            }
        } catch (e: Throwable) {
            Log.w(TAG, "hexToBytes failed", e)
            ByteArray(0)
        }
    }

    fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }
}
