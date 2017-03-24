package io.getcoffee.ottopress.speech;

import android.os.Handler;
import android.os.Looper;

import java.util.Collection;
import java.util.HashSet;

/**
 * Created by howard on 12/4/16.
 */

abstract class BackgroundRecognizer extends Thread {

    protected final Handler mainHandler = new Handler(Looper.getMainLooper());

    protected final int PRIME_THRESH_FE = -40;
    protected final int PRIME_THRESH_MF = 185;
    protected final int PRIME_THRESH_SFM = 5;

    protected final int VOICE_TIMEOUT = (int) (7.5 * 1000);
    protected final int SILENCE_TIMEOUT = (int) (0.5 * 1000);

    protected final int SFM_MIN = 200;
    protected final int SFM_MAX = 2000;
    protected final int SFM_RANGE = SFM_MAX - SFM_MIN;

    protected final Collection<RecognitionListener> listeners;

    protected BackgroundRecognizer(Collection<RecognitionListener> listeners) {
        this.listeners = listeners;
    }

    private abstract class ActivityEvent implements Runnable {
        public void run() {
            RecognitionListener[] emptyArray = new RecognitionListener[0];
            for (RecognitionListener listener : listeners.toArray(emptyArray)) {
                execute(listener);
            }
        }
        protected abstract void execute(RecognitionListener listener);
    }

    protected class UpdateEvent extends ActivityEvent {

        private final float[] update;

        public UpdateEvent(float[] update) {
            this.update = update;
        }

        @Override
        protected void execute(RecognitionListener listener) {
            listener.onPartialResult(update);
        }
    }

    protected class InSpeechChangeEvent extends ActivityEvent {
        private final boolean state;

        public InSpeechChangeEvent(boolean state) {
            this.state = state;
        }

        @Override
        protected void execute(RecognitionListener listener) {
            if(state) {
                listener.onBeginningOfSpeech();
            } else {
                listener.onEndOfSpeech();
            }
        }
    }

    protected class TimeoutEvent extends ActivityEvent {
        public TimeoutEvent() {}

        @Override
        protected void execute(RecognitionListener listener) {
            listener.onTimeout();
        }
    }

}
