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
import java.util.ArrayList;
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
        
        // UPGRADED: Set to Indian English for significantly better accent recognition
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
                        
                        SharedPreferences prefs = getSharedPreferences("
