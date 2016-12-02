package io.getcoffee.ottopress.service;

import android.app.IntentService;
import android.content.Intent;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class SpeechHandlerService extends IntentService implements RecognitionListener {

    /* Used to set the recognizer search mode */
    private static final String KEYPHRASE_SEARCH = "listen";
    /* Keyword used to activate the recording */
    private static final String KEYPHRASE = "Hey Auto";

    private SpeechRecognizer keyphraseRecognizer;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public SpeechHandlerService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        setupRecognizer();
    }

    private void setupRecognizer() {
        try {
            Assets assets = new Assets(SpeechHandlerService.this);
            File assetDir = assets.syncAssets();
            keyphraseRecognizer = createRecognizer(assetDir);
        } catch (IOException e) {
            Toast.makeText(this.getApplicationContext(), "Failed to initialize recognizer", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
        keyphraseRecognizer.addListener(this);
        keyphraseRecognizer.addKeyphraseSearch(KEYPHRASE_SEARCH, KEYPHRASE);
    }

    public static SpeechRecognizer createRecognizer(File assetDir) throws IOException {
        return SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetDir, "en-us-ptm"))
                .setDictionary(new File(assetDir, "cmudict-en-us.dict"))
                .setRawLogDir(assetDir)
                .getRecognizer();
    }

    @Override
    protected void onHandleIntent(Intent intent) {}

    @Override
    public void onBeginningOfSpeech() {}

    @Override
    public void onEndOfSpeech() {}

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null) {
            return;
        }

        String hypothesisText = hypothesis.getHypstr();
        if (hypothesisText.equals(KEYPHRASE)) {
            keyphraseRecognizer.stop();
        }
    }

    @Override
    public void onResult(Hypothesis hypothesis) {}

    @Override
    public void onError(Exception e) {}

    @Override
    public void onTimeout() {}
}
