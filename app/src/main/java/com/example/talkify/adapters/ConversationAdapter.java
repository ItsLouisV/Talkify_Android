package com.example.talkify.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.talkify.R;
import com.example.talkify.models.ChatItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ChatViewHolder> {

    private final Context context;
    private final List<ChatItem> chatList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onChatClick(ChatItem item);
    }

    public ConversationAdapter(Context context, List<ChatItem> chatList, OnItemClickListener listener) {
        this.context = context;
        this.chatList = chatList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_conv, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatItem item = chatList.get(position);

        // Tên hội thoại
        holder.tvName.setText(item.getName());

        // Avatar
        Glide.with(context)
                .load(item.getAvatarUrl())
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(holder.imgAvatar);

        // Tin nhắn cuối (thêm tiền tố “Bạn:” nếu là của mình)
        String prefix = item.isLastMessageMine() ? "Bạn: " : "";
        holder.tvLastMessage.setText(prefix + (item.getLastMessage() != null ? item.getLastMessage() : ""));

        // Thời gian hiển thị (định dạng lại cho dễ đọc)
        holder.tvTime.setText(formatTime(item.getLastMessageTime()));

//        // Trạng thái đọc
//        if (item.isRead()) {
//            holder.imgStatus.setImageResource(R.drawable.ic_double_check);
//            holder.imgStatus.setVisibility(View.VISIBLE);
//        } else if (item.isLastMessageMine()) {
//            holder.imgStatus.setImageResource(R.drawable.ic_single_check);
//            holder.imgStatus.setVisibility(View.VISIBLE);
//        } else {
//            holder.imgStatus.setVisibility(View.GONE);
//        }

        // Click mở chi tiết hội thoại
        holder.itemView.setOnClickListener(v -> listener.onChatClick(item));
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar, imgStatus;
        TextView tvName, tvLastMessage, tvTime;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            imgStatus = itemView.findViewById(R.id.imgStatus);
        }
    }

    /**
     * Chuyển chuỗi ISO time thành dạng giờ:phút hoặc ngày/tháng
     */
    private String formatTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";

        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = isoFormat.parse(isoTime);
            SimpleDateFormat outFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return outFormat.format(date);
        } catch (ParseException e) {
            return "";
        }
    }
}
