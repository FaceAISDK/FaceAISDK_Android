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
 * * 优化点：
 * 1. 配置 AudioAttributes 提升音频通路质量
 * 2. 自动选择当前语言下质量最高、延迟最低的 Voice
 * 3. 修复了筛选 Bug，真正实现优先使用网络高级语音模型（更具拟真感）
 * 4. 优化了 Pitch 和 SpeechRate 使得发音更加沉稳自然
 * 5. 修复了语言支持的平滑降级问题（例如 zh_SG 平滑降级到 zh_CN）
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

    // 【优化】放慢语速，降低音调。去掉电子尖锐感，增加沉稳感和呼吸空间
    private float mSpeechRate = 0.92f;
    private float mPitch = 0.98f;

    private TTSPlayer() {}

    public static TTSPlayer getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        static final TTSPlayer INSTANCE = new TTSPlayer();
    }

    public void init(Context context) {
        if (mTTS != null) return;
        mContext = context.getApplicationContext();

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

        AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            attrBuilder.setUsage(AudioAttributes.USAGE_ASSISTANT);
        } else {
            attrBuilder.setUsage(AudioAttributes.USAGE_MEDIA);
        }
        mTTS.setAudioAttributes(attrBuilder.build());

        mTTS.setSpeechRate(mSpeechRate);
        mTTS.setPitch(mPitch);

        applyLocaleAndVoice();

        mReady.set(true);
        drainPendingQueue();
        Log.i(TAG, "TTS engine initialized: " + mTTS.getDefaultEngine());
    }

    public void playTTS(@StringRes int stringResId) {
        if (mContext == null) {
            Log.e(TAG, "TTSPlayer not initialized. Call init() first.");
            return;
        }
        playTTS(mContext.getString(stringResId));
    }

    public void playTTS(String text) {
        if (text == null || text.isEmpty()) return;
        if (!mReady.get()) {
            mPendingQueue.offer(text);
            return;
        }
        speak(text);
    }

    public void stop() {
        mPendingQueue.clear();
        if (mTTS != null) mTTS.stop();
    }

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

    public void setSpeechRate(float rate) {
        mSpeechRate = Math.max(0.5f, Math.min(rate, 2.0f));
        if (mTTS != null) mTTS.setSpeechRate(mSpeechRate);
    }

    public void setPitch(float pitch) {
        mPitch = Math.max(0.5f, Math.min(pitch, 2.0f));
        if (mTTS != null) mTTS.setPitch(mPitch);
    }

    // ==================== internal ====================

    private void speak(String text) {
        if (mTTS == null) return;

        // 每次发声前重新应用最佳 Voice，防止引擎状态被系统重置
        applyLocaleAndVoice();

        String id = "tts_" + mUtteranceId.incrementAndGet();

        Bundle params = new Bundle();
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);

        boolean useNetwork = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
        // 强制允许网络合成
        params.putString(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS, String.valueOf(useNetwork));

        mTTS.speak(text, TextToSpeech.QUEUE_ADD, params, id);
    }

    private void applyLocaleAndVoice() {
        if (mTTS == null) return;
        Locale locale = Locale.getDefault();

        // 使用平滑降级策略协商最优 Locale
        locale = negotiateLocale(locale);

        selectBestVoice(locale);
    }

    private Locale negotiateLocale(Locale targetLocale) {
        int result = mTTS.setLanguage(targetLocale);
        if (isLanguageAvailable(result)) return targetLocale;

        if ("zh".equals(targetLocale.getLanguage())) {
            result = mTTS.setLanguage(Locale.SIMPLIFIED_CHINESE);
            if (isLanguageAvailable(result)) {
                return Locale.SIMPLIFIED_CHINESE;
            }
            result = mTTS.setLanguage(Locale.TRADITIONAL_CHINESE);
            if (isLanguageAvailable(result)) {
                return Locale.TRADITIONAL_CHINESE;
            }
        }

        Locale langOnlyLocale = new Locale(targetLocale.getLanguage());
        result = mTTS.setLanguage(langOnlyLocale);
        if (isLanguageAvailable(result)) {
            return langOnlyLocale;
        }

        mTTS.setLanguage(Locale.US);
        return Locale.US;
    }

    private boolean isLanguageAvailable(int ttsResultCode) {
        return ttsResultCode >= TextToSpeech.LANG_AVAILABLE;
    }

    /**
     * 【优化】修复了网络语音过滤 Bug，重构了最优语音选取逻辑
     */
    private void selectBestVoice(Locale targetLocale) {
        try {
            Set<Voice> voices = mTTS.getVoices();
            if (voices == null || voices.isEmpty()) return;

            String targetLang = targetLocale.getLanguage();
            List<Voice> candidates = new ArrayList<>();

            // 将所有匹配语言的 Voice 放入候选池，不再排除 Network 语音
            for (Voice v : voices) {
                if (v.getLocale().getLanguage().equals(targetLang)) {
                    candidates.add(v);
                }
            }

            if (candidates.isEmpty()) return;

            // 重新定义排序规则：更像真人的排在前面
            Collections.sort(candidates, (a, b) -> {
                // 1. 优先比对系统定义的质量分数 (Quality 越高越好)
                int qualDiff = b.getQuality() - a.getQuality();
                if (qualDiff != 0) return qualDiff;

                // 2. 质量相同的情况下，优先使用网络语音 (网络模型韵律更自然)
                boolean aNet = a.isNetworkConnectionRequired();
                boolean bNet = b.isNetworkConnectionRequired();
                if (aNet != bNet) return aNet ? -1 : 1;

                // 3. 最后比对延迟 (越低越好)
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