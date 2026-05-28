package com.jeyar.mindease.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.jeyar.mindease.R;
import com.jeyar.mindease.data.User;
import com.jeyar.mindease.databinding.ActivityLoginBinding;
import com.jeyar.mindease.repository.MindEaseRepository;
import com.jeyar.mindease.repository.RepoCallback;
import com.jeyar.mindease.util.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private MindEaseRepository repository;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);

        if (session.isLoggedIn()) {
            openConversationList();
            finish();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = MindEaseRepository.getInstance(this);

        if (!session.isDisclaimerShown()) {
            showDisclaimer();
        }

        binding.loginButton.setOnClickListener(v -> attemptLogin());
        binding.signupLink.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));
    }

    private void attemptLogin() {
        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);

        String email = textOf(binding.emailInput.getText());
        String password = rawOf(binding.passwordInput.getText());

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError(getString(R.string.error_invalid_email));
            return;
        }
        if (password.isEmpty()) {
            binding.passwordLayout.setError(getString(R.string.error_password_required));
            return;
        }

        setLoading(true);
        repository.login(email, password, new RepoCallback<User>() {
            @Override
            public void onSuccess(User result) {
                session.saveSession(result.id, result.email);
                openConversationList();
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
        binding.loginButton.setEnabled(!loading);
        binding.signupLink.setEnabled(!loading);
    }

    private void showDisclaimer() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.disclaimer_title)
                .setMessage(R.string.disclaimer_message)
                .setCancelable(false)
                .setPositiveButton(R.string.disclaimer_acknowledge,
                        (dialog, which) -> session.setDisclaimerShown())
                .show();
    }

    private void openConversationList() {
        Intent intent = new Intent(this, ConversationListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private String textOf(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private String rawOf(CharSequence cs) {
        return cs == null ? "" : cs.toString();
    }
}
