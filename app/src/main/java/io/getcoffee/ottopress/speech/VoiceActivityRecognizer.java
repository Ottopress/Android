package io.getcoffee.ottopress.speech;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.util.Log;

/**
 * Created by howard on 11/23/16.
 */

public class VoiceActivityRecognizer {

    final static String tag = VoiceActivityRecognizer.class.getSimpleName();

    private AudioRecord audioRecord = null;
    private int audioBufferSize;

    private final int[] RECORDER_SAMPLERATES = new int[]{11025, 22050, 44100, 16000};

    private final Object lock = new Object();

    private BackgroundRecognizer backgroundRecognizer;

    public VoiceActivityRecognizer() {
        createAudioRecord();
        if (NoiseSuppressor.isAvailable()) {
            NoiseSuppressor noiseSuppressor = NoiseSuppressor.create(audioRecord.getAudioSessionId());
            if(noiseSuppressor != null && !noiseSuppressor.getEnabled()) {
                noiseSuppressor.setEnabled(true);
            }
        }

        backgroundRecognizer = BackgroundRecognizerFactory.create(audioRecord, audioBufferSize, lock);
    }

    public void start() {
        synchronized (lock) {
            backgroundRecognizer.start();
        }
    }

    public void stop() {
        synchronized (lock) {
            backgroundRecognizer.interrupt();
        }
    }

    private void createAudioRecord() {
        final int encoding;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            encoding = AudioFormat.ENCODING_PCM_FLOAT;
        } else {
            encoding = AudioFormat.ENCODING_PCM_16BIT;
        }

        for(int sampleRate : RECORDER_SAMPLERATES) {
            int internalBufferSize = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    encoding);
            if(internalBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                continue;
            }
            int bufferSize = (int) Math.pow(2,Math.round(Math.log(sampleRate/10)/Math.log(2)));
            Log.wtf(VoiceActivityRecognizer.tag, "Got internal buffer size: " + internalBufferSize);
            AudioRecord tempAudioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    encoding,
                    internalBufferSize);
            if(tempAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioBufferSize = bufferSize;
                audioRecord = tempAudioRecord;
                return;
            }
            tempAudioRecord.release();
        }
        return;
    }

}
