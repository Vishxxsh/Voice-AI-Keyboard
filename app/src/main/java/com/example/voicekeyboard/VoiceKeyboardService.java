package com.example.voicekeyboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("en", "IN"));
        });

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");

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
        
        btnDictate.setOnClickListener(v -> {
            btnDictate.setText("Starting mic...");
            speechRecognizer.startListening(speechIntent);
        });

        btnReadBox.setOnClickListener(v -> {
            String fullText = getEntireTextFromBox();
            if (!fullText.isEmpty()) {
                tts.speak(fullText, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        btnAiCorrect.setOnClickListener(v -> {
            String rawText = getEntireTextFromBox();
            if (rawText.isEmpty()) return;
            SharedPreferences prefs = getSharedPreferences("KeyboardPrefs", MODE_PRIVATE);
            String prompt = prefs.getString("grammarPrompt", "Correct the grammar and spelling. Reply ONLY with the corrected text.");
            callGeminiAPI(prompt, rawText, btnAiCorrect);
        });

        btnAiDraft.setOnClickListener(v -> {
            String rawText = getEntireTextFromBox();
            if (rawText.isEmpty()) return;
            SharedPreferences prefs = getSharedPreferences("KeyboardPrefs", MODE_PRIVATE);
            String prompt = prefs.getString("draftPrompt", "Draft a professional message based on this prompt.");
            callGeminiAPI(prompt, rawText, btnAiDraft);
        });

        btnBackspace.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
        });

        btnSwitchKb.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        });
        
        applyCustomSettings();
        return keyboardView;
    }

    private void callGeminiAPI(String systemRule, String userText, Button pressedButton) {
        SharedPreferences prefs = getSharedPreferences("KeyboardPrefs", MODE_PRIVATE);
        String apiKey = prefs.getString("apiKey", "");

        if (apiKey.isEmpty()) {
            getCurrentInputConnection().commitText("\n[Error: Please enter your Gemini API Key in Settings]\n", 1);
            return;
        }

        String originalText = pressedButton.getText().toString();
        pressedButton.setText("Thinking...");
        pressedButton.setEnabled(false);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            String finalResult = "";
            boolean callSuccessful = false;
            try {
                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Build the precise JSON payload for Gemini API
                JSONObject payload = new JSONObject();
                
                // System Instruction block
                JSONObject sysInstruction = new JSONObject();
                JSONArray sysPartsArray = new JSONArray();
                JSONObject sysPart = new JSONObject();
                sysPart.put("text", systemRule);
                sysPartsArray.put(sysPart);
                sysInstruction.put("parts", sysPartsArray);
                payload.put("systemInstruction", sysInstruction);

                // User Content block
                JSONObject contentObj = new JSONObject();
                JSONArray userPartsArray = new JSONArray();
                JSONObject userPart = new JSONObject();
                userPart.put("text", userText);
                userPartsArray.put(userPart);
                contentObj.put("parts", userPartsArray);
                JSONArray contentsArray = new JSONArray();
                contentsArray.put(contentObj);
                payload.put("contents", contentsArray);

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.close();

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                
                JSONObject jsonResponse = new JSONObject(response.toString());
                finalResult = jsonResponse.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
                        
                callSuccessful = true;

            } catch (Exception e) {
                finalResult = "\n[API Error: Please check your API key and connection.]\n";
            }

            // --- NEW: Increment the Daily Tracker on Success ---
            if (callSuccessful) {
                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                String savedDate = prefs.getString("lastApiDate", "");
                int apiCount = prefs.getInt("apiCount", 0);
                
                if (!currentDate.equals(savedDate)) {
                    apiCount = 0; // Reset if it's a new day
                }
                apiCount++; // Add 1 to the tracker
                
                prefs.edit()
                     .putInt("apiCount", apiCount)
                     .putString("lastApiDate", currentDate)
                     .apply();
            }
            // ---------------------------------------------------

            final String aiOutput = finalResult;
            handler.post(() -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null && callSuccessful) {
                    CharSequence currentText = ic.getExtractedText(new ExtractedTextRequest(), 0).text;
                    if (currentText != null) {
                        CharSequence beforeCursor = ic.getTextBeforeCursor(currentText.length(), 0);
                        CharSequence afterCursor = ic.getTextAfterCursor(currentText.length(), 0);
                        ic.deleteSurroundingText(beforeCursor.length(), afterCursor.length());
                    }
                    ic.commitText(aiOutput, 1);
                } else if (ic != null) {
                    ic.commitText(aiOutput, 1); // Print the error message if it failed
                }
                
                pressedButton.setText(originalText);
                pressedButton.setEnabled(true);
            });
        });
    }

    private String getEntireTextFromBox() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ExtractedText et = ic.getExtractedText(new ExtractedTextRequest(), 0);
            if (et != null && et.text != null) return et.text.toString().trim();
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
            try { activeColor = Color.parseColor(prefs.getString("hexColor", "#000000"));
            } catch (Exception e) { activeColor = Color.parseColor("#121212"); }
        } else if (theme.equals("dark")) { activeColor = Color.parseColor("#121212"); }

        mainLayout.setBackgroundColor(activeColor);

        android.app.Dialog dialog = getWindow();
        if (dialog != null && dialog.getWindow() != null) dialog.getWindow().setNavigationBarColor(activeColor);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
    }
}
