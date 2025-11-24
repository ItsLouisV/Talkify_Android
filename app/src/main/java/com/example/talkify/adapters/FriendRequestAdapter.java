package com.example.talkify.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.talkify.R;
import com.example.talkify.models.FriendRequest;
import com.example.talkify.models.User;

public class FriendRequestAdapter extends ListAdapter<FriendRequest, FriendRequestAdapter.FriendRequestViewHolder> {

    private final OnRequestActionListener listener;
    private Context context;

    public interface OnRequestActionListener {
        void onAcceptClick(FriendRequest request);
        void onDeclineClick(FriendRequest request);
        void onAvatarClick(FriendRequest request);
    }

    public FriendRequestAdapter(@NonNull OnRequestActionListener listener) {
        super(REQUEST_DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_friend_request, parent, false);
        return new FriendRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendRequestViewHolder holder, int position) {
        FriendRequest request = getItem(position);
        holder.bind(request);
    }

    /**
     * ViewHolder (Dùng findViewById)
     */
    public class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar;
        TextView tvFullName;
        TextView tvUsername; // (TextView này bạn đang ẩn trong XML)
        Button btnAccept;
        Button btnDecline;

        public FriendRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }

        public void bind(FriendRequest request) {
            // Lấy thông tin người GỬI (sender)
            User sender = request.getSender();
            if (sender == null) return; // An toàn

            tvFullName.setText(sender.getFullName());

            // Hiển thị tvUsername nếu nó không rỗng
            if (sender.getUserName() != null && !sender.getUserName().isEmpty()) {
                tvUsername.setText(sender.getUserName());
                tvUsername.setVisibility(View.VISIBLE);
            } else {
                tvUsername.setVisibility(View.GONE);
            }

            // (Glide để load ảnh...)
            Glide.with(context).load(sender.getAvatarUrl()).into(ivUserAvatar);

            // Gán listener
            btnAccept.setOnClickListener(v -> {
                // Tắt nút đi để tránh click nhiều lần
                btnAccept.setEnabled(false);
                btnDecline.setEnabled(false);
                listener.onAcceptClick(request);
            });

            btnDecline.setOnClickListener(v -> {
                btnAccept.setEnabled(false);
                btnDecline.setEnabled(false);
                listener.onDeclineClick(request);
            });

            ivUserAvatar.setOnClickListener(v -> {
                listener.onAvatarClick(request);
            });
        }
    }

    // ---- DiffUtil cho FriendRequest ----
    private static final DiffUtil.ItemCallback<FriendRequest> REQUEST_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<FriendRequest>() {
                @Override
                public boolean areItemsTheSame(@NonNull FriendRequest oldItem, @NonNull FriendRequest newItem) {
                    // Dùng request_id để so sánh
                    return oldItem.getRequestId().equals(newItem.getRequestId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull FriendRequest oldItem, @NonNull FriendRequest newItem) {
                    // So sánh status để biết item có thay đổi nội dung không
                    return oldItem.getStatus().equals(newItem.getStatus());
                }
            };
}