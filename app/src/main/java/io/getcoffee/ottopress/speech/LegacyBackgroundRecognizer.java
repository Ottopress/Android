package io.getcoffee.ottopress.speech;

import android.media.AudioRecord;

/**
 * Created by howard on 12/4/16.
 */

public class LegacyBackgroundRecognizer extends BackgroundRecognizer {

    private final AudioRecord audioRecord;

    LegacyBackgroundRecognizer(AudioRecord audioRecord, int audioBufferSize, Object lock) {
        this.audioRecord = audioRecord;
    }
}
