package com.faceAI.demo.base

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import java.util.UUID
import android.speech.tts.UtteranceProgressListener


/**
 * 自动匹配系统语言的 TTS 封装
 */
class SystemLanguageTTS(
    context: Context,
    private val callback: InitCallback
) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var isReady = false
    private val TAG = "SystemLanguageTTS"

    // 初始化回调接口
    interface InitCallback {
        fun onSuccess()
        fun onFailure(errorMsg: String)
    }

    init {
        // 使用 ApplicationContext 防止内存泄漏
        textToSpeech = TextToSpeech(context.applicationContext, this)
    }

    /**
     * 系统回调：TTS 引擎初始化结果
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // 1. 获取当前系统语言
            val systemLocale = Locale.getDefault()

            // 2. 尝试设置语言
            val result = textToSpeech?.setLanguage(systemLocale)

            // 3. 检查语言支持情况
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                isReady = false
                val errorMsg = "当前系统语言 (${systemLocale.displayName}) 不支持或数据丢失"
                Log.e(TAG, errorMsg)
                callback.onFailure(errorMsg)
            } else {
                isReady = true
                Log.i(TAG, "TTS 初始化成功，当前语言: ${systemLocale.displayName}")

                // 可选：设置语速、音调等
                // textToSpeech?.setPitch(1.0f)
                // textToSpeech?.setSpeechRate(1.0f)

                callback.onSuccess()
            }
        } else {
            isReady = false
            val errorMsg = "TTS 引擎初始化失败，错误码: $status"
            Log.e(TAG, errorMsg)
            callback.onFailure(errorMsg)
        }
    }

    /**
     * 播放语音
     * @param text 要朗读的文本
     * @param flush 是否打断当前正在播放的声音 (true: 打断, false: 排队)
     */
    fun speak(text: String, flush: Boolean = true) {
        if (!isReady || textToSpeech == null) {
            Log.e(TAG, "TTS 未就绪，无法播放")
            return
        }

        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD

        // 兼容性参数配置
        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId")

        textToSpeech?.speak(text, queueMode, params, "UniqueUtteranceId")
    }

    /**
     * 停止播放
     */
    fun stop() {
        textToSpeech?.stop()
    }

    /**
     * 销毁资源 (必须在 Activity/Fragment onDestroy 中调用)
     */
    fun destroy() {
        if (textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isReady = false
        }
    }

    /**
     * 判断当前是否正在发声
     */
    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking ?: false
    }
}