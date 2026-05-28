package com.jeyar.mindease.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.jeyar.mindease.R;
import com.jeyar.mindease.databinding.ActivitySettingsBinding;
import com.jeyar.mindease.util.SessionManager;
import com.google.android.material.snackbar.Snackbar;

public class SettingsActivity extends AppCompatActivity {

    private static final String[] GEMINI_MODELS = {
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-1.5-pro"
    };

    private ActivitySettingsBinding binding;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.action_settings);
        }

        ArrayAdapter<String> modelAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, GEMINI_MODELS);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.modelSpinner.setAdapter(modelAdapter);

        String savedModel = session.getGeminiModel();
        for (int i = 0; i < GEMINI_MODELS.length; i++) {
            if (GEMINI_MODELS[i].equals(savedModel)) {
                binding.modelSpinner.setSelection(i);
                break;
            }
        }

        String savedKey = session.getCustomApiKey();
        if (savedKey != null && !savedKey.isEmpty()) {
            binding.apiKeyInput.setText(savedKey);
        }

        binding.saveButton.setOnClickListener(v -> save());
    }

    private void save() {
        int selectedPos = binding.modelSpinner.getSelectedItemPosition();
        if (selectedPos >= 0 && selectedPos < GEMINI_MODELS.length) {
            session.setGeminiModel(GEMINI_MODELS[selectedPos]);
        }

        String apiKey = binding.apiKeyInput.getText() == null
                ? "" : binding.apiKeyInput.getText().toString().trim();
        session.setCustomApiKey(apiKey);

        Snackbar.make(binding.getRoot(), R.string.settings_saved, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
