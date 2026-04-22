package com.faceAI.demo.base.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;

import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TTS 语音播报工具类，替代 VoicePlayer。
 * 根据当前系统语言自动选择最优 TTS 语音，无需为多语言准备多套音频文件。
 *
 * 优化点：
 *  1. 配置 AudioAttributes（USAGE_ASSISTANT + CONTENT_TYPE_SPEECH）提升音频通路质量
 *  2. 自动选择当前语言下质量最高、延迟最低的 Voice
 *  3. 优先使用网络语音（质量远超离线），离线时自动降级
 *  4. 微调 speechRate / pitch 使播报更自然
 *  5. 偏好高质量引擎（Google TTS > 其他）
 *
 * 用法：
 *   TTSPlayer.getInstance().init(context);
 *   TTSPlayer.getInstance().playTTS(R.string.success);
 *   TTSPlayer.getInstance().playTTS("自定义文本");
 *   TTSPlayer.getInstance().release();
 */
public class TTSPlayer {

    private static final String TAG = "TTSPlayer";

    /** 偏好的 TTS 引擎包名（Google TTS 质量最好，免费且不增加包体积） */
    private static final String PREFERRED_ENGINE = "com.google.android.tts";

    private volatile TextToSpeech mTTS;
    private volatile Context mContext;
    private final AtomicBoolean mReady = new AtomicBoolean(false);
    private final AtomicInteger mUtteranceId = new AtomicInteger(0);

    private final ConcurrentLinkedQueue<String> mPendingQueue = new ConcurrentLinkedQueue<>();

    /** 语速：0.95 略慢于默认，更清晰自然 */
    private float mSpeechRate = 0.95f;
    /** 音调：1.05 略高于默认，听感更生动 */
    private float mPitch = 1.05f;

    private TTSPlayer() {}

    public static TTSPlayer getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        static final TTSPlayer INSTANCE = new TTSPlayer();
    }

    /**
     * 初始化 TTS 引擎。
     * 优先尝试 Google TTS 引擎，不可用时回退到系统默认引擎。
     */
    public void init(Context context) {
        if (mTTS != null) return;
        mContext = context.getApplicationContext();

        // 优先使用 Google TTS 引擎（音质最佳）
        TextToSpeech.OnInitListener listener = this::onTTSInit;

        if (isEngineInstalled(PREFERRED_ENGINE)) {
            mTTS = new TextToSpeech(mContext, listener, PREFERRED_ENGINE);
        } else {
            mTTS = new TextToSpeech(mContext, listener);
        }

        mTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {}
            @Override public void onDone(String utteranceId) {}
            @Override public void onError(String utteranceId) {
                Log.e(TAG, "TTS utterance error: " + utteranceId);
            }
        });
    }

    private void onTTSInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            Log.e(TAG, "TTS engine init failed, status=" + status);
            return;
        }

        // 1. 配置 AudioAttributes —— 告诉系统这是「语音助手」音频
        //    系统会为此通路启用更好的音频后处理（降噪、均衡等）
        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            attrBuilder.setUsage(AudioAttributes.USAGE_ASSISTANT);
        } else {
            attrBuilder.setUsage(AudioAttributes.USAGE_MEDIA);
        }
        mTTS.setAudioAttributes(attrBuilder.build());

        // 2. 设置语速和音调
        mTTS.setSpeechRate(mSpeechRate);
        mTTS.setPitch(mPitch);

        // 3. 应用最优语言和声音
        applyLocaleAndVoice();

        mReady.set(true);
        drainPendingQueue();
        Log.i(TAG, "TTS engine initialized: " + mTTS.getDefaultEngine());
    }

    /**
     * 根据字符串资源 ID 播报。
     */
    public void playTTS(@StringRes int stringResId) {
        if (mContext == null) {
            Log.e(TAG, "TTSPlayer not initialized. Call init() first.");
            return;
        }
        playTTS(mContext.getString(stringResId));
    }

    /**
     * 直接播报文本。
     */
    public void playTTS(String text) {
        if (text == null || text.isEmpty()) return;
        if (!mReady.get()) {
            mPendingQueue.offer(text);
            return;
        }
        speak(text);
    }

    /** 停止播报并清空队列 */
    public void stop() {
        mPendingQueue.clear();
        if (mTTS != null) mTTS.stop();
    }

    /** 释放资源 */
    public void release() {
        mReady.set(false);
        mPendingQueue.clear();
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
            mTTS = null;
        }
        mContext = null;
    }

    /** 允许外部微调语速（0.5~2.0，默认 0.95） */
    public void setSpeechRate(float rate) {
        mSpeechRate = Math.max(0.5f, Math.min(rate, 2.0f));
        if (mTTS != null) mTTS.setSpeechRate(mSpeechRate);
    }

    /** 允许外部微调音调（0.5~2.0，默认 1.05） */
    public void setPitch(float pitch) {
        mPitch = Math.max(0.5f, Math.min(pitch, 2.0f));
        if (mTTS != null) mTTS.setPitch(mPitch);
    }

    // ==================== internal ====================

    private void speak(String text) {
        if (mTTS == null) return;
        applyLocaleAndVoice();

        String id = "tts_" + mUtteranceId.incrementAndGet();

        // 使用 Bundle 传递参数，可以设置音频流类型等
        Bundle params = new Bundle();
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);

        // 兼容性优化：
        // 1. Android 8.0 (API 26) 及以上版本，网络合成（High Quality）相对稳定。
        // 2. Android 7.0 及以下版本建议禁用网络合成（networkTts=false），
        //    防止在弱网环境下因为引擎尝试在线取词而导致长时间静音或失败。
        boolean useNetwork = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
        params.putString(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS, String.valueOf(useNetwork));

        mTTS.speak(text, TextToSpeech.QUEUE_ADD, params, id);
    }

    /**
     * 设置语言，并从可用 Voice 中挑选质量最高的。
     * 优先选择：网络语音 > 高质量离线 > 普通离线
     */
    private void applyLocaleAndVoice() {
        if (mTTS == null) return;
        Locale locale = Locale.getDefault();

        int result = mTTS.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language not supported: " + locale + ", falling back to English");
            locale = Locale.US;
            mTTS.setLanguage(locale);
        }

        // Voice API available since API 21
        selectBestVoice(locale);
    }

    /**
     * 从引擎可用 Voice 列表中，为指定 Locale 挑选最佳 Voice。
     * 排序规则：质量高 > 不需网络（在线声音质量好但依赖网络） > 延迟低
     */
    private void selectBestVoice(Locale targetLocale) {
        try {
            Set<Voice> voices = mTTS.getVoices();
            if (voices == null || voices.isEmpty()) return;

            String targetLang = targetLocale.getLanguage();
            List<Voice> candidates = new ArrayList<>();
            for (Voice v : voices) {
                if (v.getLocale().getLanguage().equals(targetLang) && !v.isNetworkConnectionRequired()) {
                    // 第一轮：收集离线可用的
                    candidates.add(v);
                }
            }
            // 如果离线没有，退而收集包含网络的
            if (candidates.isEmpty()) {
                for (Voice v : voices) {
                    if (v.getLocale().getLanguage().equals(targetLang)) {
                        candidates.add(v);
                    }
                }
            }
            if (candidates.isEmpty()) return;

            // 按质量降序 → 延迟升序 排序
            Collections.sort(candidates, (a, b) -> {
                int qualDiff = b.getQuality() - a.getQuality();
                if (qualDiff != 0) return qualDiff;
                return a.getLatency() - b.getLatency();
            });

            Voice best = candidates.get(0);
            mTTS.setVoice(best);
            Log.i(TAG, "Selected voice: " + best.getName()
                    + " quality=" + best.getQuality()
                    + " latency=" + best.getLatency()
                    + " network=" + best.isNetworkConnectionRequired());
        } catch (Exception e) {
            Log.w(TAG, "Voice selection failed, using default", e);
        }
    }

    private boolean isEngineInstalled(String enginePackage) {
        try {
            if (mContext == null) return false;
            mContext.getPackageManager().getPackageInfo(enginePackage, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void drainPendingQueue() {
        String text;
        while ((text = mPendingQueue.poll()) != null) {
            speak(text);
        }
    }
}
