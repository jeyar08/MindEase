package com.jeyar.mindease.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jeyar.mindease.R;
import com.jeyar.mindease.data.Message;
import com.jeyar.mindease.databinding.ActivityChatBinding;
import com.jeyar.mindease.util.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";

    private ActivityChatBinding binding;
    private ChatViewModel viewModel;
    private MessageAdapter adapter;
    private SessionManager session;

    private boolean queueBannerVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);

        if (!session.isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.new_chat);
        }

        long userId = session.getUserId();
        long conversationId = getIntent().getLongExtra(EXTRA_CONVERSATION_ID, -1L);

        adapter = new MessageAdapter();
        adapter.setOnMessageLongClickListener(this::onMessageLongClick);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.messagesRecycler.setLayoutManager(layoutManager);
        binding.messagesRecycler.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        viewModel.init(userId, conversationId);

        viewModel.getMessages().observe(this, messages -> {
            adapter.submit(messages);
            scrollToBottom();
        });

        viewModel.getLoading().observe(this, loading -> {
            binding.typingIndicator.setVisibility(
                    Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            if (Boolean.TRUE.equals(loading)) scrollToBottom();
        });

        viewModel.getTitle().observe(this, t -> {
            if (t != null && getSupportActionBar() != null)
                getSupportActionBar().setTitle(t);
        });

        viewModel.getError().observe(this, message -> {
            if (message != null) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                        .setAction(R.string.retry, v -> viewModel.retry())
                        .show();
                viewModel.clearError();
            }
        });

        viewModel.getQueueCount().observe(this, this::updateQueueBanner);

        binding.sendButton.setOnClickListener(v -> send());

        binding.queueClearButton.setOnClickListener(v -> viewModel.clearQueue());
    }

    private void send() {
        String text = binding.messageInput.getText() == null
                ? "" : binding.messageInput.getText().toString();
        if (text.trim().isEmpty()) return;
        binding.messageInput.setText("");
        viewModel.sendMessage(text);
    }

    private void updateQueueBanner(int count) {
        if (count > 0) {
            String label = count == 1
                    ? getString(R.string.queue_banner_one)
                    : getString(R.string.queue_banner_many, count);
            binding.queueBannerText.setText(label);

            if (!queueBannerVisible) {
                queueBannerVisible = true;
                binding.queueBanner.setVisibility(View.VISIBLE);
                binding.queueBanner.startAnimation(
                        AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
            }
        } else {
            if (queueBannerVisible) {
                queueBannerVisible = false;
                binding.queueBanner.startAnimation(
                        AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                binding.queueBanner.setVisibility(View.GONE);
            }
        }
    }

    private void onMessageLongClick(Message message) {
        if ("user".equals(message.role)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.message_options_title)
                    .setItems(new CharSequence[]{
                            getString(R.string.action_edit_message),
                            getString(R.string.action_retry_message)
                    }, (dialog, which) -> {
                        if (which == 0) showEditDialog(message);
                        else           viewModel.retryFromMessage(message);
                    })
                    .show();
        } else {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.message_options_title)
                    .setItems(new CharSequence[]{getString(R.string.action_retry_message)},
                            (dialog, which) -> viewModel.retryFromMessage(message))
                    .show();
        }
    }

    private void showEditDialog(Message message) {
        View editView = getLayoutInflater().inflate(R.layout.dialog_edit_message, null);
        EditText editText = editView.findViewById(R.id.edit_message_input);
        editText.setText(message.text);
        editText.setSelection(editText.getText().length());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.action_edit_message)
                .setView(editView)
                .setPositiveButton(R.string.action_send_edit, (dialog, which) ->
                        viewModel.editMessage(message, editText.getText().toString()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void scrollToBottom() {
        int count = adapter.getItemCount();
        if (count > 0) {
            binding.messagesRecycler.post(() ->
                    binding.messagesRecycler.scrollToPosition(count - 1));
        }
    }

    private void showSupportDialog() {
        String message = getString(R.string.disclaimer_message)
                + "\n\n"
                + getString(R.string.crisis_resources);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.support_title)
                .setMessage(message)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_support) {
            showSupportDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
