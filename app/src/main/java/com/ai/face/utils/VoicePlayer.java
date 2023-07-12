package com.ai.face.utils;

import android.content.Context;
import android.media.MediaPlayer;

import androidx.annotation.RawRes;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class VoicePlayer {
    private MediaPlayer mMediaPlayer;
    private Context mContext;
    private List<Integer> mAudioList = new CopyOnWriteArrayList<>();

    private VoicePlayer() {

    }

    public static VoicePlayer getInstance() {
        return Factory.INSTANCE;
    }

    private static class Factory {
        public static VoicePlayer INSTANCE = new VoicePlayer();
    }

    /**
     * 新加一个开关是否可以打开声音播放
     *
     * @param context
     */
    public void init(Context context) {
        mContext = context.getApplicationContext();
    }


    public void play(int id, MediaPlayer.OnCompletionListener onCompletionListener) {
        if (mContext == null) {
            return;
        }


        stop();
        mMediaPlayer = MediaPlayer.create(mContext, id);
        if (mMediaPlayer != null) {
            if (onCompletionListener != null) {
                mMediaPlayer.setOnCompletionListener(onCompletionListener);
            }
            mMediaPlayer.start();
        }
    }


    /**
     * @param rawId
     */
    public synchronized void addPayList(@RawRes int rawId) {
        if (mContext == null) {
            return;
        }
        if (mAudioList.size() == 0) {
            stop();
            mAudioList.add(rawId);
            mMediaPlayer = MediaPlayer.create(mContext, mAudioList.get(0));
            if (mAudioList.size() >= 1) {
                mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        if (mAudioList.size() == 0) {
                            return;
                        }
                        mAudioList.remove(0);
                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.stop();
                        }
                        mMediaPlayer.setOnCompletionListener(null);
                        mMediaPlayer.release();
                        if (mAudioList.size() > 0) {
                            mMediaPlayer = MediaPlayer.create(mContext, mAudioList.get(0));
                            mMediaPlayer.setOnCompletionListener(this);
                            mMediaPlayer.start();
                        }
                    }
                });
            }
            mMediaPlayer.start();
        } else {
            mAudioList.add(rawId);
        }

    }

    public void stop() {
        try {
            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                mAudioList.clear();
                mMediaPlayer.release();
            }
        } catch (
                IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void play(int id) {
        play(id, null);
    }


}
