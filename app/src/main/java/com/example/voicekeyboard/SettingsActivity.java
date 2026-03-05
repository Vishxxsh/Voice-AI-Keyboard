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
import android.widget.Toast;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button btnEnable = findViewById(R.id.btn_enable);
        Button btnSelect = findViewById(R.id.btn_select);
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
        seekHeight.setProgress(prefs.getInt("height", 250));
        inputHex.setText(prefs.getString("hexColor", "#000000"));
        
        String savedTheme = prefs.getString("theme", "system");
        if (savedTheme.equals("custom")) radioTheme.check(R.id.theme_custom);
        else if (savedTheme.equals("dark")) radioTheme.check(R.id.theme_dark);
        else radioTheme.check(R.id.theme_system);

        btnSave.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("height", seekHeight.getProgress());
            editor.putString("hexColor", inputHex.getText().toString());
            
            int selectedId = radioTheme.getCheckedRadioButtonId();
            if (selectedId == R.id.theme_custom) editor.putString("theme", "custom");
            else if (selectedId == R.id.theme_dark) editor.putString("theme", "dark");
            else editor.putString("theme", "system");
            
            editor.apply();
            Toast.makeText(this, "Settings Saved! Close and reopen keyboard.", Toast.LENGTH_SHORT).show();
        });
    }
}
