package com.jeyar.mindease.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.jeyar.mindease.R;
import com.jeyar.mindease.databinding.ActivitySignupBinding;
import com.jeyar.mindease.repository.MindEaseRepository;
import com.jeyar.mindease.repository.RepoCallback;
import com.jeyar.mindease.util.SessionManager;
import com.google.android.material.snackbar.Snackbar;

public class SignUpActivity extends AppCompatActivity {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private ActivitySignupBinding binding;
    private MindEaseRepository repository;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        repository = MindEaseRepository.getInstance(this);

        binding.signupButton.setOnClickListener(v -> attemptSignUp());
        binding.loginLink.setOnClickListener(v -> finish());
    }

    private void attemptSignUp() {
        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);
        binding.confirmLayout.setError(null);

        String email = textOf(binding.emailInput.getText());
        String password = rawOf(binding.passwordInput.getText());
        String confirm = rawOf(binding.confirmInput.getText());

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError(getString(R.string.error_invalid_email));
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            binding.passwordLayout.setError(getString(R.string.error_password_too_short));
            return;
        }
        if (!password.equals(confirm)) {
            binding.confirmLayout.setError(getString(R.string.error_passwords_dont_match));
            return;
        }

        setLoading(true);
        final String finalEmail = email;
        repository.signUp(email, password, new RepoCallback<Long>() {
            @Override
            public void onSuccess(Long userId) {
                session.saveSession(userId, finalEmail);
                Intent intent = new Intent(SignUpActivity.this, ConversationListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.signupButton.setEnabled(!loading);
        binding.loginLink.setEnabled(!loading);
    }

    private String textOf(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private String rawOf(CharSequence cs) {
        return cs == null ? "" : cs.toString();
    }
}
