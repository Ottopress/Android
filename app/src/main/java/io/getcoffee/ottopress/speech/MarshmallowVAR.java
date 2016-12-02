package io.getcoffee.ottopress.speech;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.jtransforms.fft.FloatFFT_1D;

/**
 * Created by howard on 11/23/16.
 */

@RequiresApi(api = Build.VERSION_CODES.M)
public class MarshmallowVAR implements VoiceActivityRecognizer{

    final static String tag = MarshmallowVAR.class.getSimpleName();

    private AudioRecord audioRecord = null;
    private float[] audioBuffer;

    private final int[] RECORDER_SAMPLERATES = new int[]{11025, 22050, 44100, 16000};

    private final int VOICE_TIMEOUT = (int) (7.5 * 1000);
    private final int SILENCE_TIMEOUT = (int) (0.5 * 1000);

    private final Object threadLock = new Object();

    public MarshmallowVAR() {
        createAudioRecord();
        if (NoiseSuppressor.isAvailable()) {
            NoiseSuppressor noiseSuppressor = NoiseSuppressor.create(audioRecord.getAudioSessionId());
            if(noiseSuppressor != null && !noiseSuppressor.getEnabled()) {
                noiseSuppressor.setEnabled(true);
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    private void createAudioRecord() {
        for(int sampleRate : RECORDER_SAMPLERATES) {
            int internalBufferSize = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_FLOAT);
            if(internalBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                continue;
            }
            int bufferSize = (int) Math.pow(2,Math.round(Math.log(sampleRate/10)/Math.log(2)));
            Log.wtf(MarshmallowVAR.tag, "Got internal buffer size: " + internalBufferSize);
            AudioRecord tempAudioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_FLOAT,
                    internalBufferSize);
            if(tempAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioBuffer = new float[bufferSize];
                audioRecord = tempAudioRecord;
                return;
            }
            tempAudioRecord.release();
        }
        return;
    }

    private class RecognizerThread extends Thread {

        private final int PRIME_THRESH_FE = 40;
        private final int PRIME_THRESH_MF = 185;
        private final int PRIME_THRESH_SFM = 5;

        private long timeStarted;
        private long timeNow;

        private int sum = 0;
        private int squaredSum = 0;
        private int itemsSeen = 0;



        @Override
        public void run() {
            timeStarted = System.currentTimeMillis();
            timeNow = timeStarted;

            FloatFFT_1D fft = new FloatFFT_1D(audioBuffer.length);
            float[] fftData = new float[audioBuffer.length * 2];
            float[] window = buildHammingWindow(audioBuffer.length);

            int frameCount = 0;
            boolean speechLastFrame = false;
            int speechCount = 0;
            int silenceCount = 0;

            int zeroMaxFreq = 0;
            double zeroSFM = 0;
            double zeroFrameEnergy = 0;

            double THRESH_FE = 0;

            while (true) {
                synchronized (threadLock) {
                    if(this.isInterrupted()) {
                        break;
                    }
                    audioRecord.read(audioBuffer, 0, audioBuffer.length, AudioRecord.READ_BLOCKING);
                    System.arraycopy(applyWindow(audioBuffer, window), 0, fftData, 0, audioBuffer.length);
                    fft.realForwardFull(fftData);

                    double maxMagnitude = Double.NEGATIVE_INFINITY;
                    int maxIndex = -1;

                    int psSize = 1798;
                    double psProduct = 1;
                    double psSum = 0;

                    double feSum = 0;

                    for(int i = 0; i < fftData.length / 2; i++) {
                        feSum+=Math.pow(audioBuffer[i],2);
                        double psValue = (fftData[2*i] * fftData[2*i]) + (fftData[2*i+1] * fftData[2*i+1]);
                        double magnitude = Math.sqrt(psValue);
                        if (magnitude > maxMagnitude) {
                            maxMagnitude = magnitude;
                            maxIndex = i;
                        }

                        if(i > 200 && i < 2000) {
                            psProduct*=psValue;
                            psSum+=psValue;
                        }
                    }

                    double sfm = 10*Math.log10(Math.pow(psProduct, 1 / psSize)/(psSum / psSize));

                    if(frameCount < 6) {
                        zeroMaxFreq = (maxIndex < zeroMaxFreq) ? maxIndex : zeroMaxFreq;
                        zeroFrameEnergy = (feSum < zeroFrameEnergy) ? feSum : zeroFrameEnergy;
                        zeroSFM = (sfm < zeroSFM) ? sfm : zeroSFM;
                        frameCount+=1;
                        continue;
                    } else if (frameCount == 6) {
                        THRESH_FE = PRIME_THRESH_FE * Math.log(zeroFrameEnergy);
                    }

                    boolean isSpeech = ((feSum - zeroFrameEnergy) >= THRESH_FE)
                            || ((maxIndex - zeroMaxFreq) >= PRIME_THRESH_MF)
                            || ((sfm - zeroSFM) >= PRIME_THRESH_SFM);

                    if(isSpeech) {
                        if(!speechLastFrame) {
                            speechCount = 0;
                            speechLastFrame = true;
                        }
                        speechCount+=1;
                    } else {
                        if(speechLastFrame) {
                            if(speechCount > 5) {
                                silenceCount = 0;
                            }
                            speechLastFrame = false;
                        }
                        silenceCount+=1;
                        zeroFrameEnergy=((silenceCount * zeroFrameEnergy) + feSum)/silenceCount + 1;
                        THRESH_FE = PRIME_THRESH_FE * Math.log(zeroFrameEnergy);
                    }
                }
            }
        }

        private float[] buildHammingWindow(int size) {
            float[] window = new float[size];
            for(int i = 0; i < size; i++) {
                window[i] = (float) (0.54 - 0.64 * Math.cos(2 * Math.PI * i / (size - 1)));
            }
            return window;
        }

        private float[] applyWindow(float[] data, float[] window) {
            float[] filtered = new float[data.length];
            for(int i = 0; i < data.length; i++) {
                filtered[i] = data[i] * window[i];
            }
            return filtered;
        }

    }

}
