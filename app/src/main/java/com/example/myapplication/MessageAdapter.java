package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;

    private List<Message> messageList;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        return message.getType();  // ← Fixed: getType()
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_bot, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).textViewMessage.setText(message.getMessage());  // ← Fixed: getMessage()
        } else if (holder instanceof BotViewHolder) {
            ((BotViewHolder) holder).textViewMessage.setText(message.getMessage());   // ← Fixed: getMessage()
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public void addMessage(Message message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    // ViewHolders
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;

        UserViewHolder(View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.text_message);
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMessage;

        BotViewHolder(View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.text_message);
        }
    }
}