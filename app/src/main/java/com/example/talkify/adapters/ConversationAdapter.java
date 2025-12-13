package com.example.talkify.adapters;

import android.content.Context;
import android.graphics.Color;
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

        // --- XỬ LÝ TIỀN TỐ TIN NHẮN CUỐI ---
        String content = item.getLastMessage() != null ? item.getLastMessage() : "";
        String prefix = "";

        if (item.isLastMessageMine()) {
            // 1. Nếu là tin nhắn của mình
            prefix = "Bạn: ";
        } else if (item.isGroup()) {
            // 2. Nếu là nhóm VÀ tin nhắn của người khác -> Hiển thị tên người đó
            String senderName = item.getLastSenderName();
            if (senderName != null && !senderName.isEmpty()) {
                prefix = senderName + ": ";
            }
        }

        // 3. Nếu là chat 1-1 và người khác nhắn -> Không hiện prefix (prefix = "")

        holder.tvLastMessage.setText(prefix + content);

        // Thời gian hiển thị
        holder.tvTime.setText(item.getLastMessageTime());

        // ==== TRẠNG THÁI ĐỌC ====
        if (item.isLastMessageMine() && !item.isRead()) {
            holder.imgStatus.setVisibility(View.VISIBLE);
            holder.imgStatus.setImageResource(R.drawable.ic_dot);
        } else {
            holder.imgStatus.setVisibility(View.GONE);
        }


        // Click mở chi tiết hội thoại
        holder.itemView.setOnClickListener(v -> listener.onChatClick(item));

        // Xử lý hiển thị icon Ghim
        if (item.isPinned()) {
            holder.ivPin.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(Color.parseColor("#1A1A1D")); // Đổi màu nền nhẹ để nổi bật
        } else {
            holder.ivPin.setVisibility(View.GONE);
            holder.itemView.setBackgroundColor(Color.BLACK); // Màu mặc định
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar, imgStatus, ivPin;
        TextView tvName, tvLastMessage, tvTime;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            imgStatus = itemView.findViewById(R.id.imgStatus);
            ivPin = itemView.findViewById(R.id.ivPin);
        }
    }

}
