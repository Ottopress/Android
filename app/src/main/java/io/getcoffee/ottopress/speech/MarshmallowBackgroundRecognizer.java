package io.getcoffee.ottopress.speech;

import android.media.AudioRecord;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.jtransforms.fft.FloatFFT_1D;

/**
 * Created by howard on 12/4/16.
 */

@RequiresApi(api = Build.VERSION_CODES.M)
class MarshmallowBackgroundRecognizer extends BackgroundRecognizer {

    private VoiceActivityRecognizer activityRecognizer;
    private short[] audioBuffer;

    private int zeroMaxFreq;
    private double zeroSFM;
    private double zeroFrameEnergy;

    private double THRESH_FE = 0;


    MarshmallowBackgroundRecognizer(VoiceActivityRecognizer activityRecognizer) {
        super(activityRecognizer.listeners);
        this.activityRecognizer = activityRecognizer;
        this.audioBuffer = new short[activityRecognizer.audioBufferSize];
    }

    @Override
    public void run() {
        Log.wtf("Marshmallow", "I'm in here");
        long timeStarted = System.currentTimeMillis();
        long timeNow = timeStarted;

        float[] audioFloatBuffer = new float[audioBuffer.length];
        FloatFFT_1D fft = new FloatFFT_1D(audioBuffer.length);
        float[] fftData = new float[audioBuffer.length * 2];
        float[] window = buildHammingWindow(audioBuffer.length);

        int frameCount = 0;
        boolean speechLastFrame = false;
        int speechCount = 0;
        int silenceCount = 0;
        long silenceTime = Long.MAX_VALUE;

        activityRecognizer.audioRecord.startRecording();
        activityRecognizer.audioRecord.read(audioBuffer, 0, audioBuffer.length, AudioRecord.READ_BLOCKING);

        while (true) {
            synchronized (activityRecognizer.lock) {
                if (this.isInterrupted()) {
                    break;
                }
                activityRecognizer.audioRecord.read(audioBuffer, 0, audioBuffer.length, AudioRecord.READ_BLOCKING);
                for (int i = 0; i < audioBuffer.length; i++) {
                    audioFloatBuffer[i] = ((float) audioBuffer[i] / Short.MAX_VALUE);
                }
                System.arraycopy(applyWindow(audioFloatBuffer, window), 0, fftData, 0, audioBuffer.length);
                fft.realForwardFull(fftData);

                double maxMagnitude = Double.NEGATIVE_INFINITY;
                int maxIndex = 0;

                double psProduct = 1;
                double psSum = 0;

                double feSum = 0;

                for (int i = 0; i < fftData.length / 2; i++) {
                    double psValue = (fftData[2 * i] * fftData[2 * i]) + (fftData[2 * i + 1] * fftData[2 * i + 1]);
                    feSum += audioFloatBuffer[i] * audioFloatBuffer[i];
                    double magnitude = Math.sqrt(psValue);
                    if (magnitude > maxMagnitude) {
                        maxMagnitude = magnitude;
                        maxIndex = i;
                    }

                    if (i > SFM_MIN && i < SFM_MAX) {
                        psProduct *= psValue;
                        psSum += psValue;
                    }
                }

                feSum = 10 * Math.log(feSum);

                double sfm = 10 * Math.log10(Math.pow(psProduct, 1 / SFM_RANGE) / (psSum / SFM_RANGE));

                frameCount += 1;
                if(frameCount < 30) {
                    zeroFrameEnergy = (feSum > zeroFrameEnergy || zeroFrameEnergy == 0) ? feSum : zeroFrameEnergy;
                    zeroMaxFreq = (maxIndex > zeroMaxFreq || zeroMaxFreq == 0) ? maxIndex : zeroMaxFreq;
                    zeroSFM = ((sfm < zeroSFM || zeroSFM == 0) && sfm > 0) ? sfm : zeroSFM;
                    Log.wtf("Marshmallow", "feSum " + feSum + " psProd " + psProduct + " psSum " + psSum + " SFM" + sfm);
                    continue;
                }

                boolean isSpeech = ((zeroFrameEnergy + feSum) >= PRIME_THRESH_FE)
                        || ((maxIndex - zeroMaxFreq) >= PRIME_THRESH_MF)
                        || ((sfm - zeroSFM) <= PRIME_THRESH_SFM);

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
                        mainHandler.post(new InSpeechChangeEvent(false));
                        break;
                    }
                    silenceCount+=1;
                }
//                if((timeNow - timeStarted) >= VOICE_TIMEOUT) {
//                    mainHandler.post(new TimeoutEvent());
//                    break;
//                }
                mainHandler.post(new UpdateEvent(audioFloatBuffer));
                timeNow = System.currentTimeMillis();

                Log.wtf("Marshmallow", "Cycle:\n"
                        + "Frame Count:       " + frameCount + "\n"
                        + "Speech Last Frame: " + speechLastFrame + "\n"
                        + "Speech Now:        " + isSpeech + "\n"
                        + "Speech Count:      " + speechCount + "\n"
                        + "Silence Time:      " + silenceTime + "\n"
                        + "Silence Count:     " + silenceCount + "\n"
                        + "Frame Energy:      " + feSum + "\n"
                        + "Frequency Max:     " + maxIndex + "\n"
                        + "Frequency Mag:     " + maxMagnitude + "\n"
                        + "SFM:               " + sfm + "\n"
                        + "FE Threshold:      " + THRESH_FE + "\n"
                        + "-----" + "\n"
                        + "Zero FE:        " + zeroFrameEnergy + "\n"
                        + "Zero FQ:        " + zeroMaxFreq + "\n"
                        + "Zero SFM:       " + zeroSFM + "\n"
                        + "-----" + "\n"
                        + "Hit FE:        " + ((feSum - zeroFrameEnergy) >= PRIME_THRESH_FE) + "\n"
                        + "Hit FQ:        " + ((maxIndex - zeroMaxFreq) >= PRIME_THRESH_MF) + "\n"
                        + "Hit SFM:       " + ((sfm - zeroSFM) <= PRIME_THRESH_SFM) + "\n"
                        + "-----" + frameCount + "\n"
                        + "Time Now:     " + timeNow + "\n"
                        + "Time Started: " + timeStarted + "\n");

            }
        }

        activityRecognizer.audioRecord.stop();
        mainHandler.removeCallbacksAndMessages(null);
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
