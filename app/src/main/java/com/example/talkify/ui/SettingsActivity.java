package com.example.talkify.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.talkify.R;
import com.example.talkify.auth.LoginActivity;

public class SettingsActivity extends AppCompatActivity {

    private Button btnLogout;
    private SharedPreferences prefs;
    private final String PREFS_NAME = "MyAppPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> {
            // Xóa token và trạng thái login
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            // Chuyển về màn hình Login và xóa stack
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
