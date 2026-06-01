package com.hiface.demo.base.utils;

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
 * TTS 语音播报工具类
 */
public class TTSPlayer {

    private static final String TAG = "TTSPlayer";
    private static final String PREFERRED_ENGINE = "com.google.android.tts";

    /**
     * 播放模式
     */
    public enum PlayMode {
        /** 排队播放（默认）：加入队列等待上一条播完再播 */
        QUEUE,
        /** 忙时丢弃：如果当前正在播报则直接丢弃本次请求 */
        DROP_IF_BUSY
    }

    private volatile TextToSpeech mTTS;
    private volatile Context mContext;
    private final AtomicBoolean mReady = new AtomicBoolean(false);
    private final AtomicBoolean mSpeaking = new AtomicBoolean(false); // 追踪播放状态
    private final AtomicInteger mUtteranceId = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<String> mPendingQueue = new ConcurrentLinkedQueue<>();

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
            @Override
            public void onStart(String utteranceId) {
                mSpeaking.set(true);
            }

            @Override
            public void onDone(String utteranceId) {
                mSpeaking.set(false);
            }

            @Override
            public void onError(String utteranceId) {
                mSpeaking.set(false);
                Log.e(TAG, "TTS utterance error: " + utteranceId);
            }
        });
    }

    // ==================== playTTS 接口 ====================

    /** 默认模式（QUEUE）播放 */
    public void playTTS(@StringRes int stringResId) {
        playTTS(stringResId, PlayMode.QUEUE);
    }

    /** 指定模式播放 */
    public void playTTS(@StringRes int stringResId, PlayMode mode) {
        if (mContext == null) {
            Log.e(TAG, "TTSPlayer not initialized. Call init() first.");
            return;
        }
        playTTS(mContext.getString(stringResId), mode);
    }

    /** 默认模式（QUEUE）播放 */
    public void playTTS(String text) {
        playTTS(text, PlayMode.QUEUE);
    }

    /** 指定模式播放 */
    public void playTTS(String text, PlayMode mode) {
        if (text == null || text.isEmpty()) return;

        if (mode == PlayMode.DROP_IF_BUSY && isSpeaking()) {
            Log.d(TAG, "DROP_IF_BUSY: discarded \"" + text + "\"");
            return;
        }

        if (!mReady.get()) {
            if (mode == PlayMode.DROP_IF_BUSY) return; // 未就绪也丢弃
            mPendingQueue.offer(text);
            return;
        }
        speak(text);
    }

    /** 当前是否正在播报 */
    public boolean isSpeaking() {
        if (mTTS != null) {
            try {
                return mTTS.isSpeaking() || mSpeaking.get();
            } catch (Exception e) {
                return mSpeaking.get();
            }
        }
        return mSpeaking.get();
    }

    public void stop() {
        mPendingQueue.clear();
        mSpeaking.set(false);
        if (mTTS != null) mTTS.stop();
    }

    public void release() {
        mReady.set(false);
        mSpeaking.set(false);
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

    private void speak(String text) {
        if (mTTS == null) return;
        applyLocaleAndVoice();
        String id = "tts_" + mUtteranceId.incrementAndGet();
        Bundle params = new Bundle();
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
        params.putString(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS,
                String.valueOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O));
        mTTS.speak(text, TextToSpeech.QUEUE_ADD, params, id);
    }

    private void applyLocaleAndVoice() {
        if (mTTS == null) return;
        Locale locale = negotiateLocale(Locale.getDefault());
        selectBestVoice(locale);
    }

    private Locale negotiateLocale(Locale targetLocale) {
        int result = mTTS.setLanguage(targetLocale);
        if (isLanguageAvailable(result)) return targetLocale;

        if ("zh".equals(targetLocale.getLanguage())) {
            result = mTTS.setLanguage(Locale.SIMPLIFIED_CHINESE);
            if (isLanguageAvailable(result)) return Locale.SIMPLIFIED_CHINESE;
            result = mTTS.setLanguage(Locale.TRADITIONAL_CHINESE);
            if (isLanguageAvailable(result)) return Locale.TRADITIONAL_CHINESE;
        }

        Locale langOnly = new Locale(targetLocale.getLanguage());
        result = mTTS.setLanguage(langOnly);
        if (isLanguageAvailable(result)) return langOnly;

        mTTS.setLanguage(Locale.US);
        return Locale.US;
    }

    private boolean isLanguageAvailable(int ttsResultCode) {
        return ttsResultCode >= TextToSpeech.LANG_AVAILABLE;
    }

    private void selectBestVoice(Locale targetLocale) {
        try {
            Set<Voice> voices = mTTS.getVoices();
            if (voices == null || voices.isEmpty()) return;

            String targetLang = targetLocale.getLanguage();
            List<Voice> candidates = new ArrayList<>();
            for (Voice v : voices) {
                if (v.getLocale().getLanguage().equals(targetLang)) {
                    candidates.add(v);
                }
            }
            if (candidates.isEmpty()) return;

            Collections.sort(candidates, (a, b) -> {
                int qualDiff = b.getQuality() - a.getQuality();
                if (qualDiff != 0) return qualDiff;
                boolean aNet = a.isNetworkConnectionRequired();
                boolean bNet = b.isNetworkConnectionRequired();
                if (aNet != bNet) return aNet ? -1 : 1;
                return a.getLatency() - b.getLatency();
            });

            Voice best = candidates.get(0);
            mTTS.setVoice(best);
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