package com.jeyar.mindease.ui;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jeyar.mindease.data.Conversation;
import com.jeyar.mindease.databinding.ItemConversationBinding;

import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    public interface OnConversationClick {
        void onClick(Conversation conversation);
    }

    public interface OnConversationLongClick {
        void onLongClick(Conversation conversation);
    }

    private final List<Conversation> items = new ArrayList<>();
    private final OnConversationClick listener;
    private final OnConversationLongClick longClickListener;

    public ConversationAdapter(OnConversationClick listener, OnConversationLongClick longClickListener) {
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    public void submit(List<Conversation> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemConversationBinding binding = ItemConversationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ConversationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = items.get(position);
        holder.binding.title.setText(conversation.title);
        CharSequence relative = DateUtils.getRelativeTimeSpanString(
                conversation.updatedAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS);
        holder.binding.subtitle.setText(relative);
        holder.binding.getRoot().setOnClickListener(v -> listener.onClick(conversation));
        holder.binding.getRoot().setOnLongClickListener(v -> {
            longClickListener.onLongClick(conversation);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        final ItemConversationBinding binding;

        ConversationViewHolder(ItemConversationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
