package com.example.voicekeyboard;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button btnEnable = findViewById(R.id.btn_enable);
        Button btnSelect = findViewById(R.id.btn_select);
        Switch switchAutoTts = findViewById(R.id.switch_auto_tts);
        
        TextView textApiUsage = findViewById(R.id.text_api_usage);
        EditText inputApiKey = findViewById(R.id.input_api_key);
        EditText inputGrammarPrompt = findViewById(R.id.input_grammar_prompt);
        EditText inputDraftPrompt = findViewById(R.id.input_draft_prompt);

        SeekBar seekHeight = findViewById(R.id.seek_height);
        RadioGroup radioTheme = findViewById(R.id.radio_theme);
        EditText inputHex = findViewById(R.id.input_hex);
        Button btnSave = findViewById(R.id.btn_save);

        btnEnable.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        btnSelect.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        });

        SharedPreferences prefs = getSharedPreferences("KeyboardPrefs", MODE_PRIVATE);
        
        // --- NEW: Daily API Counter Logic ---
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String savedDate = prefs.getString("lastApiDate", "");
        int apiCount = prefs.getInt("apiCount", 0);
        
        // If it is a new day, reset the counter to 0 visually
        if (!currentDate.equals(savedDate)) {
            apiCount = 0;
        }
        textApiUsage.setText("API Calls Used Today: " + apiCount + " / 1000");
        // ------------------------------------

        switchAutoTts.setChecked(prefs.getBoolean("autoTts", true));
        
        inputApiKey.setText(prefs.getString("apiKey", ""));
        inputGrammarPrompt.setText(prefs.getString("grammarPrompt", "You are an expert editor. Fix all spelling, grammar, and punctuation mistakes in the following text. Do not add any conversational filler. Respond ONLY with the corrected text."));
        inputDraftPrompt.setText(prefs.getString("draftPrompt", "You are a highly professional executive assistant. Draft a polite, clear, and well-formatted message based on the following instructions."));

        seekHeight.setProgress(prefs.getInt("height", 250));
        inputHex.setText(prefs.getString("hexColor", "#000000"));
        
        String savedTheme = prefs.getString("theme", "system");
        if (savedTheme.equals("custom")) radioTheme.check(R.id.theme_custom);
        else if (savedTheme.equals("dark")) radioTheme.check(R.id.theme_dark);
        else radioTheme.check(R.id.theme_system);

        btnSave.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("autoTts", switchAutoTts.isChecked());
            editor.putString("apiKey", inputApiKey.getText().toString().trim());
            editor.putString("grammarPrompt", inputGrammarPrompt.getText().toString().trim());
            editor.putString("draftPrompt", inputDraftPrompt.getText().toString().trim());
            editor.putInt("height", seekHeight.getProgress());
            editor.putString("hexColor", inputHex.getText().toString().trim());
            
            int selectedId = radioTheme.getCheckedRadioButtonId();
            if (selectedId == R.id.theme_custom) editor.putString("theme", "custom");
            else if (selectedId == R.id.theme_dark) editor.putString("theme", "dark");
            else editor.putString("theme", "system");
            
            editor.apply();
            Toast.makeText(this, "Settings Saved! Close and reopen keyboard.", Toast.LENGTH_SHORT).show();
        });
    }
}
