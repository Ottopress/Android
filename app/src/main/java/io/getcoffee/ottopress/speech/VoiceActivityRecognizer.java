package io.getcoffee.ottopress.speech;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.util.Log;

import java.util.Collection;
import java.util.HashSet;

/**
 * Created by howard on 11/23/16.
 */

public class VoiceActivityRecognizer {

    final static String tag = VoiceActivityRecognizer.class.getSimpleName();

    public final Collection<RecognitionListener> listeners = new HashSet<RecognitionListener>();

    private BackgroundRecognizer backgroundRecognizer;

    final Object lock = new Object();

    final AudioRecord audioRecord;
    int audioBufferSize;

    public VoiceActivityRecognizer(AudioRecord audioRecord) {
        this.audioRecord = audioRecord;
        this.audioBufferSize = (int) Math.pow(2,Math.round(Math.log(audioRecord.getSampleRate()/50)/Math.log(2)));
        if (NoiseSuppressor.isAvailable()) {
            NoiseSuppressor noiseSuppressor = NoiseSuppressor.create(audioRecord.getAudioSessionId());
            if(noiseSuppressor != null && !noiseSuppressor.getEnabled()) {
                noiseSuppressor.setEnabled(true);
            }
        }
    }

    public void start() {
        Log.wtf(tag, "Starting VAD");
        if(backgroundRecognizer != null) {
            Log.wtf(tag, "VAD NULL!");
            return;
        }
        synchronized (lock) {
            backgroundRecognizer = BackgroundRecognizerFactory.create(this);
            backgroundRecognizer.start();
            Log.wtf(tag, "Stuff started");
        }
    }

    public void stop() {
        if(backgroundRecognizer == null) {
            return;
        }
        synchronized (lock) {
            try {
                backgroundRecognizer.interrupt();
                backgroundRecognizer.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }

        backgroundRecognizer = null;
    }

    public void addListener(RecognitionListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(RecognitionListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

}
