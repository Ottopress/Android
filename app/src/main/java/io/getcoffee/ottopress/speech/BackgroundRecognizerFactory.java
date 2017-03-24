package io.getcoffee.ottopress.speech;

import android.media.AudioRecord;
import android.os.Build;

import java.util.Collection;

import edu.cmu.pocketsphinx.RecognitionListener;

/**
 * Created by howard on 12/4/16.
 */

class BackgroundRecognizerFactory {
    private BackgroundRecognizerFactory(){}

    static BackgroundRecognizer create(VoiceActivityRecognizer activityRecognizer) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new MarshmallowBackgroundRecognizer(activityRecognizer);
        } else {
            return new LegacyBackgroundRecognizer(activityRecognizer);
        }
    }

}
