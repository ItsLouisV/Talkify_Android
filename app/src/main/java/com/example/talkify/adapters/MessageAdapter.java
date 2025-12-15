package com.example.talkify.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.talkify.R;
import com.example.talkify.models.Message;
import com.example.talkify.models.User;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class MessageAdapter extends ListAdapter<Message, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final Context context;
    private final String currentUserId;
    private boolean isGroupChat;

    public MessageAdapter(Context context, String currentUserId) {
        super(MESSAGE_DIFF_CALLBACK);
        this.context = context;
        this.currentUserId = currentUserId;
        this.isGroupChat = false;
    }

    // --- SỬA 1: Cập nhật UI khi đổi trạng thái nhóm ---
    public void setGroupChat(boolean isGroupChat) {
        this.isGroupChat = isGroupChat;
        notifyDataSetChanged(); // Vẽ lại để ẩn/hiện avatar đúng lúc
    }

    @Override
    public int getItemViewType(int position) {
        Message message = getItem(position);
        // Kiểm tra null an toàn
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = getItem(position);
        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).bind(message);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    // --- HÀM TIỆN ÍCH: Format thời gian ---
    private static String formatTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";

        try {
            // Supabase timestamptz: 2025-12-15T17:53:43.245561+00:00
            SimpleDateFormat inputFormat =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault());

            Date date = inputFormat.parse(isoTime);

            SimpleDateFormat outputFormat =
                    new SimpleDateFormat("HH:mm", Locale.getDefault());

            return outputFormat.format(date);

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // --- ViewHolder GỬI ---
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTimestamp;
        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
        void bind(Message message) {
            tvMessage.setText(message.getContent());

            // Ưu tiên server time, fallback local time
            if (message.getCreatedAt() != null) {
                tvTimestamp.setText(formatTime(message.getCreatedAt()));
            } else {
                tvTimestamp.setText(formatLocalTime(message.getLocalCreatedAt()));
            }
        }

    }

    // --- ViewHolder NHẬN ---
    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTimestamp, tvSenderName;
        ShapeableImageView ivAvatar;
        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
        }

        void bind(Message message) {
            tvMessage.setText(message.getContent());

            tvTimestamp.setText(formatTime(message.getCreatedAt()));

            // Lấy thông tin người gửi
            User sender = message.getSender();

            if (isGroupChat && sender != null) {
                tvSenderName.setText(sender.getFullName());
                tvSenderName.setVisibility(View.VISIBLE);
                ivAvatar.setVisibility(View.VISIBLE);

                Glide.with(context)
                        .load(sender.getAvatarUrl())
                        .placeholder(R.drawable.avatar_placeholder)
                        .into(ivAvatar);
            } else {
                tvSenderName.setVisibility(View.GONE);
                ivAvatar.setVisibility(View.GONE);
            }
        }
    }

    // --- Hàm thêm tin nhắn ---
    public void addMessage(Message message) {
        List<Message> currentList = getCurrentList();
        ArrayList<Message> newList = new ArrayList<>(currentList);
        newList.add(message);
        submitList(newList);
    }

    private static String formatLocalTime(long millis) {
        if (millis <= 0) return "";

        return new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date(millis));
    }

    public void replaceTempMessage(
            String tempId,
            String realMessageId,
            String createdAt
    ) {
        List<Message> currentList = getCurrentList();
        List<Message> newList = new ArrayList<>();

        boolean updated = false;

        for (Message old : currentList) {
            if (tempId != null && tempId.equals(old.getClientTempId())) {

                // TẠO OBJECT MỚI
                Message updatedMessage = new Message(old);

                updatedMessage.setMessageId(realMessageId);
                updatedMessage.setCreatedAt(createdAt);
                updatedMessage.setStatus(Message.SendStatus.SENT);
                updatedMessage.setClientTempId(null);

                newList.add(updatedMessage);
                updated = true;

            } else {
                newList.add(old);
            }
        }

        if (updated) {
            submitList(newList);
        }
    }

    // --- DiffUtil AN TOÀN ---
    private static final DiffUtil.ItemCallback<Message> MESSAGE_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Message>() {
                @Override
                public boolean areItemsTheSame(@NonNull Message old, @NonNull Message newMsg) {
                    // Ưu tiên messageId nếu có
                    if (old.getMessageId() != null && newMsg.getMessageId() != null) {
                        return old.getMessageId().equals(newMsg.getMessageId());
                    }

                    // Fallback cho optimistic
                    return old.getClientTempId() != null
                            && old.getClientTempId().equals(newMsg.getClientTempId());
                }


                @Override
                public boolean areContentsTheSame(@NonNull Message o, @NonNull Message n) {
                    return Objects.equals(o.getStatus(), n.getStatus())
                            && Objects.equals(o.getCreatedAt(), n.getCreatedAt())
                            && Objects.equals(o.getContent(), n.getContent());
                }
            };
}