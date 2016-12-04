package io.getcoffee.ottopress.speech;

import android.media.AudioRecord;
import android.os.Build;
import android.support.annotation.RequiresApi;

import org.jtransforms.fft.FloatFFT_1D;

/**
 * Created by howard on 12/4/16.
 */

@RequiresApi(api = Build.VERSION_CODES.M)
public class MarshmallowBackgroundRecognizer extends BackgroundRecognizer {

    private AudioRecord audioRecord;
    private float[] audioBuffer;
    private Object threadLock;

    private long timeStarted;
    private long timeNow;

    private int zeroMaxFreq = 0;
    private double zeroSFM = 0;
    private double zeroFrameEnergy = 0;

    private double THRESH_FE = 0;


    MarshmallowBackgroundRecognizer(AudioRecord audioRecord, int audioBufferSize, Object threadLock) {
        this.audioBuffer = new float[audioBufferSize];
        this.audioRecord = audioRecord;
        this.threadLock = threadLock;
    }

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
        long silenceTime = timeNow;

        while (true) {
            synchronized (threadLock) {
                if(this.isInterrupted()) {
                    break;
                }
                audioRecord.read(audioBuffer, 0, audioBuffer.length, AudioRecord.READ_BLOCKING);
                System.arraycopy(applyWindow(audioBuffer, window), 0, fftData, 0, audioBuffer.length);
                fft.realForwardFull(fftData);

                double maxMagnitude = Double.NEGATIVE_INFINITY;
                int maxIndex = 0;

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

                    if(i > SFM_MIN && i < SFM_MAX) {
                        psProduct*=psValue;
                        psSum+=psValue;
                    }
                }

                double sfm = 10*Math.log10(Math.pow(psProduct, 1 / SFM_RANGE)/(psSum / SFM_RANGE));

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
                            silenceTime = System.currentTimeMillis();
                        }
                        speechLastFrame = false;
                    }
                    if((System.currentTimeMillis() - silenceTime) >= SILENCE_TIMEOUT) {
                        break;
                    }
                    silenceCount+=1;
                    zeroFrameEnergy=((silenceCount * zeroFrameEnergy) + feSum)/silenceCount + 1;
                    THRESH_FE = PRIME_THRESH_FE * Math.log(zeroFrameEnergy);
                }

                if((timeStarted - timeNow) >= VOICE_TIMEOUT) {
                    break;
                }
                timeNow = System.currentTimeMillis();
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
