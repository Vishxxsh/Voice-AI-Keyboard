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
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceKeyboardService extends InputMethodService {
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private TextToSpeech tts;
    private View keyboardView;
    private Button btnDictate, btnReadBox, btnAiCorrect, btnAiDraft, btnBackspace, btnSwitchKb;

    @Override
    public void onCreate() {
        super.onCreate();
        
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.US);
        });

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { btnDictate.setText("Listening..."); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { btnDictate.setText("Processing..."); }
            @Override public void onError(int error) { btnDictate.setText("🎤 Dictate (Raw Text)"); }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0);
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        ic.commitText(spokenText + " ", 1); 
                        
                        SharedPreferences prefs = getSharedPreferences("KeyboardPrefs", MODE_PRIVATE);
                        if (prefs.getBoolean("autoTts", true)) {
                            tts.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    }
                }
                btnDictate.setText("🎤 Dictate (Raw Text)");
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    @Override
    public View onCreateInputView() {
        keyboardView = getLayoutInflater().inflate(R.layout.keyboard_view, null);
        
        btnDictate = keyboardView.findViewById(R.id.btn_dictate);
        btnReadBox = keyboardView.findViewById(R.id.btn_read_box);
        btnAiCorrect = keyboardView.findViewById(R.id.btn_ai_correct);
        btnAiDraft = keyboardView.findViewById(R.id.btn_ai_draft);
        btnBackspace = keyboardView.findViewById(R.id.btn_backspace);
        btnSwitchKb = keyboardView.findViewById(R.id.btn_switch_kb);
        
        // 1. Dictate Raw Text
        btnDictate.setOnClickListener(v -> {
            btnDictate.setText("Starting mic...");
            speechRecognizer.startListening(speechIntent);
        });

        // 2. Read the entire text box aloud
        btnReadBox.setOnClickListener(v -> {
            String fullText = getEntireTextFromBox();
            if (!fullText.isEmpty()) {
                tts.speak(fullText, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                tts.speak("The text box is empty.", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        // 3. AI Correct Placeholder
        btnAiCorrect.setOnClickListener(v -> {
            String fullText = getEntireTextFromBox();
            if (!fullText.isEmpty()) {
                getCurrentInputConnection().commitText("\n\n[AI PLUG-IN REQUIRED: Correcting grammar for -> " + fullText + "]\n", 1);
            }
        });

        // 4. AI Draft Placeholder
        btnAiDraft.setOnClickListener(v -> {
            String fullText = getEntireTextFromBox();
            if (!fullText.isEmpty()) {
                getCurrentInputConnection().commitText("\n\n[AI PLUG-IN REQUIRED: Drafting message for Jacarti context based on -> " + fullText + "]\n", 1);
            }
        });

        // 5. Backspace Button
        btnBackspace.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
        });

        // 6. Switch Keyboard Button (Pulls up system keyboard selector)
        btnSwitchKb.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        });
        
        applyCustomSettings();
        return keyboardView;
    }

    // Helper method to grab everything currently written in the app's text box
    private String getEntireTextFromBox() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ExtractedText et = ic.getExtractedText(new ExtractedTextRequest(), 0);
            if (et != null && et.text != null) {
                return et.text.toString().trim();
            }
        }
        return "";
    }

    private void applyCustomSettings() {
        SharedPreferences prefs = getSharedPreferences("KeyboardPrefs", MODE_PRIVATE);
        
        int height = prefs.getInt("height", 250);
        float density = getResources().getDisplayMetrics().density;
        int pxHeight = (int) (height * density); 
        
        LinearLayout mainLayout = (LinearLayout) keyboardView;
        ViewGroup.LayoutParams params = mainLayout.getLayoutParams();
        if (params == null) params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, pxHeight);
        params.height = pxHeight;
        mainLayout.setLayoutParams(params);

        int activeColor = Color.TRANSPARENT; 
        String theme = prefs.getString("theme", "system");
        
        if (theme.equals("custom")) {
            try {
                activeColor = Color.parseColor(prefs.getString("hexColor", "#000000"));
            } catch (Exception e) { activeColor = Color.parseColor("#121212"); }
        } else if (theme.equals("dark")) { activeColor = Color.parseColor("#121212"); }

        mainLayout.setBackgroundColor(activeColor);

        android.app.Dialog dialog = getWindow();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setNavigationBarColor(activeColor);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
    }
}
