package com.jeyar.mindease.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jeyar.mindease.R;
import com.jeyar.mindease.data.Conversation;
import com.jeyar.mindease.databinding.ActivityConversationListBinding;
import com.jeyar.mindease.repository.RepoCallback;
import com.jeyar.mindease.util.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class ConversationListActivity extends AppCompatActivity {

    private ActivityConversationListBinding binding;
    private ConversationListViewModel viewModel;
    private ConversationAdapter adapter;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);

        if (!session.isLoggedIn()) {
            openLogin();
            finish();
            return;
        }

        binding = ActivityConversationListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        long userId = session.getUserId();

        adapter = new ConversationAdapter(
                conversation -> openChat(conversation.id),
                this::onConversationLongClick
        );
        binding.conversationsRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.conversationsRecycler.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ConversationListViewModel.class);
        viewModel.init(userId);
        viewModel.getConversations().observe(this, conversations -> {
            adapter.submit(conversations);
            boolean empty = conversations == null || conversations.isEmpty();
            binding.emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        binding.newChatFab.setOnClickListener(v -> openChat(-1L));
    }

    private void onConversationLongClick(Conversation conversation) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_conversation_title)
                .setMessage(R.string.delete_conversation_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    viewModel.deleteConversation(conversation.id, new RepoCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Snackbar.make(binding.getRoot(),
                                    R.string.conversation_deleted,
                                    Snackbar.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openChat(long conversationId) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId);
        startActivity(intent);
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_conversation_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            session.clear();
            openLogin();
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_settings) {
            openSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
