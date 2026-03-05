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
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    @Override
    public View onCreateInputView() {
        keyboardView = getLayoutInflater().inflate(R.layout.keyboard_view, null);
        voiceButton = keyboardView.findViewById(R.id.btn_voice);
        
        voiceButton.setOnClickListener(v -> {
            voiceButton.setText("Starting mic...");
            speechRecognizer.startListening(speechIntent);
        });
        
        applyCustomSettings();
        return keyboardView;
    }

    private void applyCustomSettings() {
        SharedPreferences prefs = getSharedPreferences("KeyboardPrefs", MODE_PRIVATE);
        
        // Set the height dynamically
        int height = prefs.getInt("height", 250);
        float density = getResources().getDisplayMetrics().density;
        int pxHeight = (int) (height * density); // Convert to exact screen pixels
        
        LinearLayout mainLayout = (LinearLayout) keyboardView;
        ViewGroup.LayoutParams params = mainLayout.getLayoutParams();
        if (params == null) params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, pxHeight);
        params.height = pxHeight;
        mainLayout.setLayoutParams(params);

        // Set the theme color
        String theme = prefs.getString("theme", "system");
        if (theme.equals("custom")) {
            try {
                String hex = prefs.getString("hexColor", "#000000");
                mainLayout.setBackgroundColor(Color.parseColor(hex));
            } catch (Exception e) {
                mainLayout.setBackgroundColor(Color.parseColor("#121212")); // Fallback if hex is invalid
            }
        } else if (theme.equals("dark")) {
            mainLayout.setBackgroundColor(Color.parseColor("#121212"));
        } else {
            // System Default (Auto): Sets a transparent background so it uses the layout's default system colors
            mainLayout.setBackgroundColor(Color.TRANSPARENT); 
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
