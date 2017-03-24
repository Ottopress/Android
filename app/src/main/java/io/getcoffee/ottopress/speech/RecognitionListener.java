package io.getcoffee.ottopress.speech;

/**
 * Created by howard on 12/4/16.
 */

public interface RecognitionListener extends edu.cmu.pocketsphinx.RecognitionListener {
    void onPartialResult(float[] result);
}
