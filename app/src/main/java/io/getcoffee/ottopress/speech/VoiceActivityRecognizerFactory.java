package io.getcoffee.ottopress.speech;

import android.os.Build;

/**
 * Created by howard on 11/30/16.
 */

public class VoiceActivityRecognizerFactory {

    private VoiceActivityRecognizerFactory() {}

    public static VoiceActivityRecognizer build() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new MarshmallowVAR();
        } else {
            return new KitKatVAR();
        }
    }

}
