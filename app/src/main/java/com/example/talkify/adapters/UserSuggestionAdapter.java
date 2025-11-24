package com.example.talkify.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.talkify.R;
import com.example.talkify.models.User;

import java.util.HashSet;
import java.util.List; // <-- Import quan trọng

public class UserSuggestionAdapter extends ListAdapter<User, UserSuggestionAdapter.UserSuggestionViewHolder> {

    private final OnSuggestionActionListener listener;
    // HashSet này vẫn quan trọng để xử lý click UI ngay lập tức
    private final HashSet<String> sentUserIds = new HashSet<>();
    private Context context;

    /**
     * Interface để Fragment giao tiếp
     */
    public interface OnSuggestionActionListener {
        void onAddFriendClick(User user);
        void onCancelClick(User user);
        void onAvatarClick(User user);
    }

    public UserSuggestionAdapter(@NonNull OnSuggestionActionListener listener) {
        super(USER_DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserSuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_user_suggestion, parent, false);
        return new UserSuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserSuggestionViewHolder holder, int position) {
        User user = getItem(position);
        holder.bind(user);
    }

    /**
     * ViewHolder (Dùng findViewById)
     */
    public class UserSuggestionViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar;
        TextView tvFullName;
        TextView tvUserName;
        Button btnAddFriend;
        Button btnCancel;

        public UserSuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ View
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            btnAddFriend = itemView.findViewById(R.id.btnAddFriend);
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }

        public void bind(User user) {
            // Bind dữ liệu
            tvFullName.setText(user.getFullName());
            tvUserName.setText(user.getUserName());

            // Load ảnh
            Glide.with(context)
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.avatar_placeholder) // (Hãy thêm 1 ảnh placeholder)
                    .into(ivUserAvatar);

            // ---- LOGIC CỐT LÕI ----
            // Kiểm tra trạng thái "đã gửi" (đã được "mồi" bởi Fragment)
            if (sentUserIds.contains(user.getUserId())) {
                showSentState();
            } else {
                showDefaultState();
            }

            // Gán listener
            btnAddFriend.setOnClickListener(v -> {
                if (!sentUserIds.contains(user.getUserId())) {
                    // Cập nhật UI ngay lập tức (Optimistic Update)
                    addUserToSentState(user.getUserId());
                    // Báo cho Fragment gọi API
                    listener.onAddFriendClick(user);
                }
            });

            btnCancel.setOnClickListener(v -> {
                if (sentUserIds.contains(user.getUserId())) {
                    // Cập nhật UI ngay lập tức (Optimistic Update)
                    removeUserFromSentState(user.getUserId());
                    // Báo cho Fragment gọi API
                    listener.onCancelClick(user);
                }
            });

            ivUserAvatar.setOnClickListener(v -> {
                listener.onAvatarClick(user);
            });
        }

        /**
         * Chuyển UI sang trạng thái "Thêm bạn bè"
         */
        private void showDefaultState() {
            btnAddFriend.setText("Thêm bạn bè");
            btnAddFriend.setEnabled(true);
            btnCancel.setVisibility(View.GONE);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) btnAddFriend.getLayoutParams();
            params.weight = 2.0f;
            btnAddFriend.setLayoutParams(params);
        }

        /**
         * Chuyển UI sang trạng thái "Đã gửi" / "Hủy"
         */
        private void showSentState() {
            btnAddFriend.setText("Đã gửi");
            btnAddFriend.setEnabled(false); // Nút "Đã gửi" không nhấn được
            btnCancel.setVisibility(View.VISIBLE);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) btnAddFriend.getLayoutParams();
            params.weight = 1.0f;
            btnAddFriend.setLayoutParams(params);
        }
    }

    // ===========================================
    // --- HÀM MỚI TỪ BƯỚC 3 ---
    // ===========================================
    /**
     * Được gọi TỪ FRAGMENT, dùng để "mồi" (prime) các trạng thái "đã gửi"
     * dựa trên dữ liệu tải về từ API (user.getRequestStatus()).
     */
    public void primeSentStates(List<User> users) {
        sentUserIds.clear(); // Xóa trạng thái cũ
        if (users == null) return;

        for (User user : users) {
            // Nếu API nói user này ở trạng thái "sent"
            if ("sent".equals(user.getRequestStatus())) {
                sentUserIds.add(user.getUserId());
            }
        }
    }

    // ===========================================
    // --- CÁC HÀM HELPER (ĐÃ TỐI ƯU HÓA) ---
    // ===========================================

    /**
     * Cập nhật UI khi nhấn "Thêm bạn bè" (hoặc khi API Hủy bị lỗi)
     */
    public void addUserToSentState(String userId) {
        // Chỉ notify nếu trạng thái thực sự thay đổi
        if (sentUserIds.add(userId)) {
            notifyItemChanged(findPositionOfUser(userId));
        }
    }

    /**
     * Cập nhật UI khi nhấn "Hủy" (hoặc khi API Gửi bị lỗi)
     */
    public void removeUserFromSentState(String userId) {
        // Chỉ notify nếu trạng thái thực sự thay đổi
        if (sentUserIds.remove(userId)) {
            notifyItemChanged(findPositionOfUser(userId));
        }
    }

    /**
     * Tìm vị trí của user trong danh sách hiện tại
     */
    private int findPositionOfUser(String userId) {
        if (userId == null) return -1;
        for (int i = 0; i < getCurrentList().size(); i++) {
            if (userId.equals(getCurrentList().get(i).getUserId())) {
                return i;
            }
        }
        return -1;
    }

    // ===========================================
    // --- DIFFUTIL (GIỮ NGUYÊN) ---
    // ===========================================
    private static final DiffUtil.ItemCallback<User> USER_DIFF_CALLBACK =
            new DiffUtil.ItemCallback<User>() {
                @Override
                public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                    return oldItem.getUserId().equals(newItem.getUserId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                    // Chúng ta nên so sánh cả requestStatus để DiffUtil biết
                    // nội dung đã thay đổi
                    String oldStatus = oldItem.getRequestStatus() != null ? oldItem.getRequestStatus() : "new";
                    String newStatus = newItem.getRequestStatus() != null ? newItem.getRequestStatus() : "new";

                    return oldItem.getFullName().equals(newItem.getFullName()) &&
                            oldStatus.equals(newStatus);
                }
            };
}