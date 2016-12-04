package io.getcoffee.ottopress.speech;

/**
 * Created by howard on 12/4/16.
 */

public abstract class BackgroundRecognizer extends Thread {

    protected final int PRIME_THRESH_FE = 40;
    protected final int PRIME_THRESH_MF = 185;
    protected final int PRIME_THRESH_SFM = 5;

    protected final int VOICE_TIMEOUT = (int) (7.5 * 1000);
    protected final int SILENCE_TIMEOUT = (int) (0.5 * 1000);

    protected final int SFM_MIN = 200;
    protected final int SFM_MAX = 2000;
    protected final int SFM_RANGE = SFM_MAX - SFM_MIN;

}
