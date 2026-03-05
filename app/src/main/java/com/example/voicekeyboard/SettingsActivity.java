package com.example.voicekeyboard;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
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
        Button btnSave = findViewById(R.id.btn_save);

        btnEnable.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        btnSelect.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        });

        // Load saved preferences so the menu remembers your choices
        SharedPreferences prefs = getSharedPreferences("KeyboardPrefs", MODE_PRIVATE);
        seekHeight.setProgress(prefs.getInt("height", 250));
        
        String savedTheme = prefs.getString("theme", "dark");
        if (savedTheme.equals("light")) radioTheme.check(R.id.theme_light);
        else if (savedTheme.equals("blue")) radioTheme.check(R.id.theme_blue);
        else radioTheme.check(R.id.theme_dark);

        // Save preferences when you click the button
        btnSave.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("height", seekHeight.getProgress());
            
            int selectedId = radioTheme.getCheckedRadioButtonId();
            if (selectedId == R.id.theme_light) editor.putString("theme", "light");
            else if (selectedId == R.id.theme_blue) editor.putString("theme", "blue");
            else editor.putString("theme", "dark");
            
            editor.apply();
            Toast.makeText(this, "Settings Saved! Close and reopen keyboard to see changes.", Toast.LENGTH_SHORT).show();
        });
    }
}
