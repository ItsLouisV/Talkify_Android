package com.example.talkify.adapters;

import android.content.Context;
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
    // Chuyển từ chuỗi ISO 8601 (vd: 2023-11-20T10:30:00Z) sang giờ phút (10:30)
    private static String formatTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";
        try {
            // Định dạng đầu vào từ Supabase (thường là UTC)
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Giả sử server lưu UTC

            Date date = inputFormat.parse(isoTime);

            // Định dạng đầu ra (Giờ:Phút theo giờ máy)
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            // outputFormat tự động dùng TimeZone của điện thoại

            return outputFormat.format(date);
        } catch (Exception e) {
            // Nếu lỗi parse (do format khác), trả về nguyên gốc hoặc chuỗi rỗng
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
            // Xử lý hiển thị giờ nếu cần (dùng message.getCreatedAt())
            tvTimestamp.setText(formatTime(message.getCreatedAt()));
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

    // --- SỬA 2: Hàm thêm tin nhắn local ---
    public void addLocalMessage(Message message) {
        List<Message> currentList = getCurrentList();
        ArrayList<Message> newList = new ArrayList<>(currentList);
        newList.add(message);
        submitList(newList);
    }

    // --- SỬA 3: DiffUtil AN TOÀN (CHỐNG CRASH) ---
    private static final DiffUtil.ItemCallback<Message> MESSAGE_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Message>() {
                @Override
                public boolean areItemsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
                    // Kiểm tra null trước khi so sánh equals
                    String oldId = oldItem.getMessageId();
                    String newId = newItem.getMessageId();

                    // Nếu 1 trong 2 bị null (do tạo local chưa kịp có ID), coi như khác nhau để vẽ lại
                    if (oldId == null || newId == null) {
                        return false;
                    }
                    return oldId.equals(newId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
                    // Kiểm tra null cho nội dung
                    String oldContent = oldItem.getContent();
                    String newContent = newItem.getContent();
                    return Objects.equals(oldContent, newContent);
                }
            };
}