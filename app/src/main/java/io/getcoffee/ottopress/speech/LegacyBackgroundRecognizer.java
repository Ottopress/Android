package io.getcoffee.ottopress.speech;

import android.media.AudioRecord;

/**
 * Created by howard on 12/4/16.
 */

class LegacyBackgroundRecognizer extends BackgroundRecognizer {

    private final VoiceActivityRecognizer activityRecognizer;

    LegacyBackgroundRecognizer(VoiceActivityRecognizer activityRecognizer) {
        super(activityRecognizer.listeners);
        this.activityRecognizer = activityRecognizer;
    }
}
