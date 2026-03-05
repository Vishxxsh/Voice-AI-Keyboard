package com.example.voicekeyboard;

import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;

public class VoiceKeyboardService extends InputMethodService {
    @Override
    public View onCreateInputView() {
        // This loads the visual layout we made in the last step
        View keyboardView = getLayoutInflater().inflate(R.layout.keyboard_view, null);
        
        // This finds our big blue button and tells it what to do when tapped
        Button voiceButton = keyboardView.findViewById(R.id.btn_voice);
        voiceButton.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                // For now, it just types a test message. We will add Voice/AI here later.
                ic.commitText("Voice and AI features coming soon! ", 1);
            }
        });
        
        return keyboardView;
    }
}
