package com.example.voicekeyboard;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.Toast;
import java.util.ArrayList;

public class VoiceKeyboardService extends InputMethodService {
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private Button voiceButton;

    @Override
    public void onCreate() {
        super.onCreate();
        // Set up Android's built-in speech recognizer
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
            @Override public void onError(int error) {
                voiceButton.setText("🎤 Tap to Speak (AI Auto-Correct)");
                Toast.makeText(VoiceKeyboardService.this, "Mic Error! Please enable Microphone permission in your phone's App Settings.", Toast.LENGTH_LONG).show();
            }
            @Override public void onResults(Bundle results) {
                // Get the spoken words
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0);
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        ic.commitText(spokenText + " ", 1); // Type it into the app
                    }
                }
                voiceButton.setText("🎤 Tap to Speak (AI Auto-Correct)");
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    @Override
    public View onCreateInputView() {
        View keyboardView = getLayoutInflater().inflate(R.layout.keyboard_view, null);
        voiceButton = keyboardView.findViewById(R.id.btn_voice);
        
        voiceButton.setOnClickListener(v -> {
            voiceButton.setText("Starting mic...");
            speechRecognizer.startListening(speechIntent);
        });
        
        return keyboardView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}
