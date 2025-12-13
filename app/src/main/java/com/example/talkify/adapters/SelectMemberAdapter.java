package com.example.talkify.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.talkify.R;
import com.example.talkify.models.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectMemberAdapter extends RecyclerView.Adapter<SelectMemberAdapter.ViewHolder> {

    private Context context;
    private List<User> userList;
    // Dùng Set để tránh trùng lặp ID
    private Set<String> selectedUserIds = new HashSet<>();

    public SelectMemberAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    // Hàm trả về danh sách ID đã chọn để gửi lên Server
    public List<String> getSelectedUserIds() {
        return new ArrayList<>(selectedUserIds);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout item_select_member
        View view = LayoutInflater.from(context).inflate(R.layout.item_select_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);

        // Gán tên
        if (holder.tvName != null) {
            holder.tvName.setText(user.getFullName());
        }

        // Gán Avatar
        if (holder.ivAvatar != null) {
            Glide.with(context)
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.ic_user)
                    .circleCrop() // Bo tròn ảnh cho đẹp
                    .into(holder.ivAvatar);
        }

        // --- XỬ LÝ CHECKBOX ---
        // 1. Xóa listener cũ để tránh lỗi khi scroll
        holder.cbSelect.setOnCheckedChangeListener(null);

        // 2. Set trạng thái checked dựa vào dữ liệu đã lưu
        holder.cbSelect.setChecked(selectedUserIds.contains(user.getUserId()));

        // 3. Gán listener mới để lưu trạng thái khi user tích vào ô vuông
        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedUserIds.add(user.getUserId());
            } else {
                selectedUserIds.remove(user.getUserId());
            }
        });

        // 4. Cho phép bấm vào cả dòng item cũng chọn được (Trải nghiệm tốt hơn)
        holder.itemView.setOnClickListener(v -> {
            // Đảo ngược trạng thái check của CheckBox
            holder.cbSelect.toggle();
            // Logic lưu ID sẽ được xử lý bởi OnCheckedChangeListener ở trên
        });
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        ImageView ivAvatar;
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // === SỬA ID TẠI ĐÂY CHO KHỚP VỚI XML ===

            // XML: <CheckBox android:id="@+id/cbSelect" ... />
            cbSelect = itemView.findViewById(R.id.cbSelect);

            // XML: <ImageView android:id="@+id/imgAvatar" ... />
            ivAvatar = itemView.findViewById(R.id.imgAvatar); // Đã sửa từ ivGroupAvatar thành imgAvatar

            // XML: <TextView android:id="@+id/tvName" ... />
            tvName = itemView.findViewById(R.id.tvName); // Đã sửa từ tvCreateGroup thành tvName
        }
    }
}