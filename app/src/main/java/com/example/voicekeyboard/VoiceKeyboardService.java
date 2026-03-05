package com.example.voicekeyboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceKeyboardService extends InputMethodService {
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private Button voiceButton;
    private TextToSpeech tts;
    private View keyboardView;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // The Mouth: Initialize Text-to-Speech
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            }
        });

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { voiceButton.setText("Listening... Speak now"); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { voiceButton.setText("Processing..."); }
            @Override public void onError(int error) { voiceButton.setText("🎤 Tap to Speak (AI Auto-Correct)"); }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0);
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        // Type it out
                        ic.commitText(spokenText + " ", 1); 
                        // Speak it back automatically
                        tts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                }
                voiceButton.setText("🎤 Tap to Speak (AI Auto-Correct)");
            }
