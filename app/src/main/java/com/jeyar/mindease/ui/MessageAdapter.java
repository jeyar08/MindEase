package com.jeyar.mindease.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jeyar.mindease.data.Message;
import com.jeyar.mindease.databinding.ItemMessageAssistantBinding;
import com.jeyar.mindease.databinding.ItemMessageUserBinding;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_MODEL = 2;

    public interface OnMessageLongClick {
        void onLongClick(Message message);
    }

    private final List<Message> items = new ArrayList<>();
    private OnMessageLongClick longClickListener;

    public void setOnMessageLongClickListener(OnMessageLongClick listener) {
        this.longClickListener = listener;
    }

    public void submit(List<Message> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return "user".equals(items.get(position).role) ? TYPE_USER : TYPE_MODEL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            return new UserViewHolder(ItemMessageUserBinding.inflate(inflater, parent, false));
        }
        return new ModelViewHolder(ItemMessageAssistantBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = items.get(position);
        if (holder instanceof UserViewHolder) {
            UserViewHolder userHolder = (UserViewHolder) holder;
            userHolder.binding.messageText.setText(message.text);
            userHolder.binding.getRoot().setOnLongClickListener(v -> {
                if (longClickListener != null) longClickListener.onLongClick(message);
                return true;
            });
        } else {
            ModelViewHolder modelHolder = (ModelViewHolder) holder;
            modelHolder.binding.messageText.setText(message.text);
            modelHolder.binding.getRoot().setOnLongClickListener(v -> {
                if (longClickListener != null) longClickListener.onLongClick(message);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final ItemMessageUserBinding binding;

        UserViewHolder(ItemMessageUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class ModelViewHolder extends RecyclerView.ViewHolder {
        final ItemMessageAssistantBinding binding;

        ModelViewHolder(ItemMessageAssistantBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
