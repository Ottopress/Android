package io.getcoffee.ottopress.speech;

import android.media.AudioRecord;
import android.os.Build;

/**
 * Created by howard on 12/4/16.
 */

public class BackgroundRecognizerFactory {
    private BackgroundRecognizerFactory(){}

    public static BackgroundRecognizer create(AudioRecord audioRecord, int audioBufferSize, Object lock) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new MarshmallowBackgroundRecognizer(audioRecord, audioBufferSize, lock);
        } else {
            return new LegacyBackgroundRecognizer();
        }
    }

}
