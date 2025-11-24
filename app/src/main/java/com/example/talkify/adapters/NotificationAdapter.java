package com.example.talkify.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.talkify.R;
import com.example.talkify.models.AppNotification;
import com.example.talkify.models.User;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private Context context;
    private List<AppNotification> notificationList;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification notification);
    }

    public NotificationAdapter(Context context, OnNotificationClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.notificationList = new ArrayList<>();
    }

    public void setList(List<AppNotification> list) {
        this.notificationList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppNotification notif = notificationList.get(position);
        User actor = notif.getActor();

        // 1. Hiển thị Avatar
        if (actor != null) {
            Glide.with(context)
                    .load(actor.getAvatarUrl())
                    .placeholder(R.drawable.avatar_placeholder)
                    .into(holder.ivAvatar);

            // 2. Hiển thị nội dung (In đậm tên người gửi)
            String actorName = actor.getFullName();
            String contentText = " " + notif.getContent(); // Ví dụ: " đã gửi lời mời..."

            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(actorName);
            builder.setSpan(new StyleSpan(Typeface.BOLD), 0, actorName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append(contentText);

            holder.tvContent.setText(builder);
        } else {
            holder.tvContent.setText("Thông báo từ hệ thống");
        }

        // 3. Xử lý trạng thái Đã đọc / Chưa đọc
        if (notif.isRead()) {
            holder.ivUnreadDot.setVisibility(View.GONE);
            holder.itemView.setBackgroundColor(Color.BLACK); // Màu nền bình thường
        } else {
            holder.ivUnreadDot.setVisibility(View.VISIBLE);
            // Có thể đổi màu nền nhẹ để nổi bật tin chưa đọc
            holder.itemView.setBackgroundColor(Color.parseColor("#1A1A1D"));
        }

        // 4. Thời gian (Format đơn giản)
        holder.tvTime.setText(formatTime(notif.getCreatedAt())); // Cần hàm format ở dưới

        // 5. Sự kiện Click
        holder.itemView.setOnClickListener(v -> listener.onNotificationClick(notif));
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    // Hàm format thời gian (VD: 2023-10-10T... -> 10:30 10/10)
    private String formatTime(String timestamp) {
        if (timestamp == null) return "";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(timestamp);

            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
            return outputFormat.format(date);
        } catch (ParseException e) {
            return "";
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar, ivUnreadDot;
        TextView tvContent, tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivActorAvatar);
            ivUnreadDot = itemView.findViewById(R.id.ivUnreadDot);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}