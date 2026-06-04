package com.nanackations2.papaya.rokid

import com.rokid.cxr.Caps

/**
 * 对 [RokidApi] 的业务级封装：把常用的 Compose/业务层操作拼装成一两行的调用。
 *
 * 这里**只**做便捷组合，不保存状态；状态请通过 [RokidApi.connectionStateFlow] 等 StateFlow 订阅。
 *
 * Caps 的使用注意事项（CXR SDK 1.0.8）：
 * - 写入：`caps.write(String)`、`caps.writeInt32(Int)`、`caps.writeDouble(Double)`…
 * - 读取：`caps.at(i)` 返回 `Caps.Value`，再调用 `getString()`/`getInt()`/`getBinary()` 等，
 *   **没有** `.string` / `.int` 属性；我们下方提供的 [capString] 做了兼容。
 */
object CxrSBridge {

    // ===================== 文本 / AI 结果 =====================

    /** 最简形式：直接把一条文本推送到眼镜的"文本更新通道"。 */
    fun sendTextToGlasses(api: RokidApi, text: String): Boolean =
        api.pushTextToGlasses(text)

    /** 推送 JSON（比如自定义卡片数据）到眼镜的通用自定义通道。 */
    fun sendJsonToGlasses(api: RokidApi, json: String): Boolean {
        val caps = Caps().apply {
            write("JSON_DATA")
            write(json)
        }
        return api.sendCustomCmd(RokidConstants.Channel.CUSTOM_DATA, caps)
    }

    /** 通过 AI 通道把一整段 LLM 回答推送给眼镜端的 AI 场景。 */
    fun sendAiResultToGlasses(api: RokidApi, result: String): Boolean {
        val caps = Caps().apply {
            write("AI_RESPONSE")
            write(result)
        }
        return api.sendCustomCmd(RokidConstants.Channel.AI_RESULT, caps)
    }

    /** 在眼镜上弹一个系统级 Toast（对应 SDK 的 sendGlobalToastContent）。 */
    fun glassToast(
        api: RokidApi,
        text: String,
        type: Int = RokidConstants.GlobalMsgType.INFO,
        overwrite: Boolean = false,
    ): Boolean = api.sendGlobalToast(type, text, overwrite)

    /** 在眼镜上弹一条系统消息（通知形式）。 */
    fun glassMessage(
        api: RokidApi,
        text: String,
        type: Int = RokidConstants.GlobalMsgType.INFO,
        overwrite: Boolean = false,
    ): Boolean = api.sendGlobalMessage(type, text, overwrite)

    /** 让眼镜用默认 TTS 播一段话。 */
    fun glassSpeak(api: RokidApi, text: String): Boolean = api.sendGlobalTts(text)

    // ===================== 提词器 (WordTips) =====================

    /**
     * 开启提词器场景，应用预设字体 + 推送内容。
     * Rokid 的提词器基于 CxrSceneType.WORD_TIPS；SDK 推送接口是 sendWordTipsAsrContent。
     */
    fun startTeleprompter(
        api: RokidApi,
        script: String,
        fontSize: Float = 28f,
        lineSpacing: Float = 1.4f,
        textColor: String = "#FFFFFF",
        maxLine: Int = 3,
        speed: Int = 50,
    ): Boolean {
        if (!api.controlScene(RokidConstants.SceneType.TELEPROMPTER, true)) return false
        api.configWordTipsText(
            fontSize = fontSize,
            lineSpacing = lineSpacing,
            color = textColor,
            bgAlpha = 0,
            textAlign = 0,
            maxLine = maxLine,
            speed = speed,
        )
        return api.sendWordTipsContent(script)
    }

    fun pushTeleprompterContent(api: RokidApi, text: String): Boolean =
        api.sendWordTipsContent(text)

    fun stopTeleprompter(api: RokidApi): Boolean =
        api.controlScene(RokidConstants.SceneType.TELEPROMPTER, false)

    // ===================== 翻译 =====================

    /** 开启翻译场景，并设置源/目标语言。 */
    fun startTranslation(
        api: RokidApi,
        sourceLang: String,
        targetLang: String,
        model: String = "default",
    ): Boolean {
        if (!api.controlScene(RokidConstants.SceneType.TRANSLATION, true)) return false
        return api.setTranslationLanguage(sourceLang, targetLang, model)
    }

    fun stopTranslation(api: RokidApi): Boolean =
        api.controlScene(RokidConstants.SceneType.TRANSLATION, false)

    /** 把外部 ASR + 翻译的结果推给眼镜显示。 */
    fun pushTranslation(
        api: RokidApi,
        srcLangIndex: Int,
        dstLangIndex: Int,
        isAsrSide: Boolean,
        isFinal: Boolean,
        text: String,
    ): Boolean = api.sendTranslationContent(srcLangIndex, dstLangIndex, isAsrSide, isFinal, text)

    // ===================== AI Assistant Pipeline =====================

    /**
     * 把一次完整的 AI 对话下发到眼镜：
     *   1. notifyAiStart
     *   2. sendAsrContent（用户说的话）
     *   3. sendTtsContent（模型回答）
     *   4. notifyTtsAudioFinished
     */
    fun pushAiTurn(
        api: RokidApi,
        userAsr: String,
        assistantTts: String,
    ): Boolean {
        api.notifyAiStart()
        api.sendAsrContent(userAsr)
        api.notifyAsrEnd()
        val ok = api.sendTtsContent(assistantTts)
        api.notifyTtsAudioFinished(true)
        return ok
    }

    // ===================== 拍照快捷方法 =====================

    /** 简洁版：拍一张，完成时回调 [onResult]。传 `null` 可让 SDK 忽略二进制结果。 */
    fun takePhoto(
        api: RokidApi,
        width: Int = 1920,
        height: Int = 1080,
        quality: Int = 80,
        onResult: ((ok: Boolean, data: ByteArray) -> Unit)? = null,
    ): Boolean = api.takeGlassPhoto(width, height, quality, onResult?.let { cb ->
        object : RokidPhotoResultCallback {
            override fun onPhotoResult(statusOk: Boolean, data: ByteArray) = cb(statusOk, data)
        }
    })

    // ===================== 自定义通道消息监听 =====================

    /**
     * 常用写法：注册一个函数式的监听器，把 Caps 解包成 (channel, subCmd, data) 三元组。
     * 适合协议形式：`caps.write("SUB_CMD"); caps.write("payload-string")`。
     */
    fun registerGlassesMessageListener(
        api: RokidApi,
        onMessage: (channel: String, subCmd: String?, data: String?) -> Unit,
    ) {
        api.setCustomCmdListener(object : RokidCustomCmdListener {
            override fun onCustomCmd(channel: String, args: Caps?) {
                val sub = capString(args, 0)
                val data = capString(args, 1)
                onMessage(channel, sub, data)
            }
        })
    }

    /** 直接暴露原始 Caps 的监听器（如果业务需要二进制/整型 payload）。 */
    fun registerRawGlassesMessageListener(
        api: RokidApi,
        onMessage: (channel: String, args: Caps?) -> Unit,
    ) {
        api.setCustomCmdListener(object : RokidCustomCmdListener {
            override fun onCustomCmd(channel: String, args: Caps?) {
                onMessage(channel, args)
            }
        })
    }

    /**
     * 安全地读取 [Caps] 中第 [index] 个字段，返回 `String?`。
     * 兼容 Caps.Value 没有 kotlin 属性 `string` 的情况（SDK 实际只暴露 `getString()`）。
     */
    fun capString(args: Caps?, index: Int): String? {
        if (args == null || index < 0 || index >= args.size()) return null
        return try {
            args.at(index)?.getString()
        } catch (_: Throwable) {
            null
        }
    }

    fun capInt(args: Caps?, index: Int): Int? {
        if (args == null || index < 0 || index >= args.size()) return null
        return try {
            args.at(index)?.getInt()
        } catch (_: Throwable) {
            null
        }
    }

    fun capLong(args: Caps?, index: Int): Long? {
        if (args == null || index < 0 || index >= args.size()) return null
        return try {
            args.at(index)?.getLong()
        } catch (_: Throwable) {
            null
        }
    }

    fun capBinary(args: Caps?, index: Int): ByteArray? {
        if (args == null || index < 0 || index >= args.size()) return null
        return try {
            args.at(index)?.getBinary()?.let { binary ->
                // Caps.Binary 里有 `a` / 长度，SDK 源码没有公开 API 直接拿 byte[]，
                // 退化成通过 serialize 路径尝试取字节；大多数场景建议使用 getString() + Base64。
                val method = binary.javaClass.declaredFields.firstOrNull { it.type == ByteArray::class.java }
                method?.apply { isAccessible = true }?.get(binary) as? ByteArray
            }
        } catch (_: Throwable) {
            null
        }
    }
}
