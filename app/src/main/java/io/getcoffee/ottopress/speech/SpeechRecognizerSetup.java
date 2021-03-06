/* ====================================================================
 * Copyright (c) 2014 Alpha Cephei Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY ALPHA CEPHEI INC. ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 */

package io.getcoffee.ottopress.speech;

import android.media.AudioRecord;

import static edu.cmu.pocketsphinx.Decoder.defaultConfig;
import static edu.cmu.pocketsphinx.Decoder.fileConfig;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Config;

/**
 * Wrapper for the decoder configuration to implement builder pattern.
 * Configures most important properties of the decoder
 */
public class SpeechRecognizerSetup {

    static {
        System.loadLibrary("pocketsphinx_jni");
    }

    private final Config config;
    private AudioRecord audioRecord;

    /**
     * Creates new speech recognizer builder with default configuration.
     */
    public static SpeechRecognizerSetup defaultSetup(AudioRecord audioRecord) {
        return new SpeechRecognizerSetup(defaultConfig(), audioRecord)
                .setFloat("-samprate", audioRecord.getSampleRate())
                .setFloat("-upperf", audioRecord.getSampleRate() / 2)
                .setInteger("-nfft" ,(int) Math.pow(2,Math.round(Math.log(audioRecord.getSampleRate()/10)/Math.log(2))));
    }

    private SpeechRecognizerSetup(Config config, AudioRecord audioRecord) {
        this.config = config;
        this.audioRecord = audioRecord;
    }

    public SpeechRecognizer getRecognizer() throws IOException {
        return new SpeechRecognizer(this.config, this.audioRecord);
    }

    public SpeechRecognizerSetup setAcousticModel(File model) {
        return setString("-hmm", model.getPath());
    }

    public SpeechRecognizerSetup setDictionary(File dictionary) {
        return setString("-dict", dictionary.getPath());
    }

    public SpeechRecognizerSetup setSampleRate(int rate) {
        return setFloat("-samprate", rate);
    }

    public SpeechRecognizerSetup setRawLogDir(File dir) {
        return setString("-rawlogdir", dir.getPath());
    }

    public SpeechRecognizerSetup setKeywordThreshold(float threshold) {
        return setFloat("-kws_threshold", threshold);
    }

    public SpeechRecognizerSetup setBoolean(String key, boolean value) {
        config.setBoolean(key, value);
        return this;
    }

    public SpeechRecognizerSetup setInteger(String key, int value) {
        config.setInt(key, value);
        return this;
    }

    public SpeechRecognizerSetup setFloat(String key, double value) {
        config.setFloat(key, value);
        return this;
    }

    public SpeechRecognizerSetup setString(String key, String value) {
        config.setString(key, value);
        return this;
    }
}