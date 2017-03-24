package io.getcoffee.ottopress.service;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import io.getcoffee.ottopress.speech.RecognitionListener;
import io.getcoffee.ottopress.speech.SpeechRecognizer;
import io.getcoffee.ottopress.speech.SpeechRecognizerSetup;
import io.getcoffee.ottopress.speech.VoiceActivityRecognizer;

public class SpeechHandlerService extends Service implements RecognitionListener {

    final static String tag = SpeechHandlerService.class.getSimpleName();

    private static final String KEYPHRASE_SEARCH = "listen";
    private static final String ACTIVITY_SEARCH = "activity";
    private String SEARCH_MODE;
    private static final String KEYPHRASE = "hey auto";

    private AudioRecord audioRecord;

    private SpeechRecognizer keyphraseRecognizer;
    private VoiceActivityRecognizer activityRecognizer;

    private final int[] RECORDER_SAMPLERATES = new int[]{44100, 22050, 11025, 16000};

    @Override
    public void onCreate() {
        super.onCreate();
        createAudioRecord();
        setupActivityRecognizer();
        setupKeywordRecognizer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(this.tag, "STARTED");
        Log.wtf(this.tag, keyphraseRecognizer.startListening(KEYPHRASE_SEARCH) + "");
        SEARCH_MODE = KEYPHRASE_SEARCH;
        return Service.START_STICKY;
    }

    private void setupActivityRecognizer() {
        activityRecognizer = new VoiceActivityRecognizer(audioRecord);
        activityRecognizer.addListener(this);
    }

    private void setupKeywordRecognizer() {
        try {
            Assets assets = new Assets(SpeechHandlerService.this);
            File assetDir = assets.syncAssets();
            keyphraseRecognizer = createRecognizer(assetDir, audioRecord);
        } catch (IOException e) {
            Toast.makeText(this.getApplicationContext(), "Failed to initialize recognizer", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        keyphraseRecognizer.addListener(this);
        keyphraseRecognizer.addKeyphraseSearch(KEYPHRASE_SEARCH, KEYPHRASE);
    }

    public SpeechRecognizer createRecognizer(File assetDir, AudioRecord audioRecord) throws IOException {
        return SpeechRecognizerSetup.defaultSetup(audioRecord)
                .setAcousticModel(new File(assetDir, "en-us-ptm"))
                .setDictionary(new File(assetDir, "cmudict-en-us.dict"))
                .setRawLogDir(assetDir)
                .setKeywordThreshold((float) 1e-20)
                .getRecognizer();
    }

    private void switchSearch(String searchMode) {
        keyphraseRecognizer.stop();
        activityRecognizer.stop();
        Log.wtf(tag, searchMode);
        switch (searchMode) {
            case KEYPHRASE_SEARCH:
                keyphraseRecognizer.startListening(KEYPHRASE_SEARCH);
                SEARCH_MODE = searchMode;
                break;
            case ACTIVITY_SEARCH:
                activityRecognizer.start();
                SEARCH_MODE = searchMode;
                break;
        }
    }

    private void createAudioRecord() {
        for(int sampleRate : RECORDER_SAMPLERATES) {
            int internalBufferSize = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if(internalBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                continue;
            }
            Log.wtf(tag, "Got internal buffer size: " + internalBufferSize);
            AudioRecord tempAudioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    internalBufferSize * 2);
            if(tempAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord = tempAudioRecord;
                return;
            }
            tempAudioRecord.release();
        }
        return;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        keyphraseRecognizer.stop();
        activityRecognizer.stop();
        audioRecord.release();
    }

    @Override
    public void onBeginningOfSpeech() {}

    @Override
    public void onEndOfSpeech() {
        if(SEARCH_MODE == ACTIVITY_SEARCH) {
            Log.wtf(this.tag, "ACTIVITY ENDED");
            switchSearch(KEYPHRASE_SEARCH);
        }
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null) {
            return;
        }

        String hypothesisText = hypothesis.getHypstr();
        Log.wtf(tag, hypothesisText);
        if (hypothesisText.equals(KEYPHRASE)) {
            Log.wtf(this.tag, "GOT THE THING");
            switchSearch(ACTIVITY_SEARCH);
        }
    }

    @Override
    public void onPartialResult(float[] result) {
    }

    @Override
    public void onResult(Hypothesis hypothesis) {}

    @Override
    public void onError(Exception e) {}

    @Override
    public void onTimeout() {
        activityRecognizer.stop();
        keyphraseRecognizer.startListening(KEYPHRASE_SEARCH);
    }
}
