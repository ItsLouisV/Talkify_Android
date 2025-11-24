package com.example.talkify.ui; // (Package của bạn)

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.talkify.R;
import com.example.talkify.models.Conversation; // (Bạn cần file Model này)
import com.example.talkify.services.SharedPrefManager;
import com.example.talkify.services.SupabaseApiService; // (Cần hàm getConversationDetails)
import com.example.talkify.services.SupabaseClient;
import com.example.talkify.utils.RetrofitClient;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// (Giả sử tên file layout là activity_conv_setting.xml)
public class ConvSettingActivity extends AppCompatActivity {

    // Views
    private ImageView ivBack;
    private ShapeableImageView ivConversationAvatar;
    private TextView tvConversationName, tvMemberCount;
    private SwitchMaterial switchMute;
    private LinearLayout layoutViewMedia, groupOptionsLayout, layoutViewMembers, layoutAddMember;
    private Button btnLeaveGroup, btnBlockUser;

    // Services
    private SupabaseApiService apiService;
    private String authToken, apiKey, currentUserId;

    // State
    private String conversationId;
    private Conversation currentConversation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_conv_setting);

        // 1. Lấy ID
        conversationId = getIntent().getStringExtra("CONVERSATION_ID");
        if (conversationId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Khởi tạo
        initViews();
        initServices();

        // 3. Gán sự kiện
        setupClickListeners();

        // 4. Tải dữ liệu
        loadConversationData();
    }

    private void initViews() {
        // Ánh xạ các ID từ file XML của bạn
        ivBack = findViewById(R.id.ivBack);
        ivConversationAvatar = findViewById(R.id.ivConversationAvatar);
        tvConversationName = findViewById(R.id.tvConversationName);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        switchMute = findViewById(R.id.switchMute);
        layoutViewMedia = findViewById(R.id.layoutViewMedia);
        groupOptionsLayout = findViewById(R.id.groupOptionsLayout);
        layoutViewMembers = findViewById(R.id.layoutViewMembers);
        layoutAddMember = findViewById(R.id.layoutAddMember);
        btnLeaveGroup = findViewById(R.id.btnLeaveGroup);
        btnBlockUser = findViewById(R.id.btnBlockUser);
    }

    private void initServices() {
        apiService = RetrofitClient.getApiService();
        currentUserId = SharedPrefManager.getInstance(this).getUserId();
        String userToken = SharedPrefManager.getInstance(this).getToken();
        authToken = "Bearer " + userToken;
        apiKey = SupabaseClient.ANON_KEY;
    }

    /**
     * Tải dữ liệu từ bảng 'conversations'
     */
    private void loadConversationData() {
        // (Giả sử bạn đã thêm hàm 'getConversationDetails' vào SupabaseApiService)
        apiService.getConversationDetails(authToken, apiKey, "eq." + conversationId, "*", 1)
                .enqueue(new Callback<List<Conversation>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Conversation>> call, @NonNull Response<List<Conversation>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            currentConversation = response.body().get(0);

                            // Cập nhật UI
                            updateUiBasedOnConversationType();
                        } else {
                            Toast.makeText(ConvSettingActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<List<Conversation>> call, @NonNull Throwable t) {
                        Toast.makeText(ConvSettingActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Hàm quan trọng: Ẩn/hiện tùy chọn dựa trên loại chat
     */
    private void updateUiBasedOnConversationType() {
        if (currentConversation == null) return;

        String name = currentConversation.getDisplayName(currentUserId);
        String avatar = currentConversation.getDisplayAvatar(currentUserId);

        // Cập nhật thông tin chung
        tvConversationName.setText(name);
        Glide.with(this)
                .load(avatar)
                .placeholder(R.drawable.avatar_placeholder)
                .into(ivConversationAvatar);

        // Logic ẨN/HIỆN
        if (currentConversation.isGroup()) {
            // Là CHAT NHÓM
            tvMemberCount.setText("... Thành viên"); // (Cần 1 RPC để đếm)
            tvMemberCount.setVisibility(View.VISIBLE);

            groupOptionsLayout.setVisibility(View.VISIBLE);
            btnLeaveGroup.setVisibility(View.VISIBLE);
            btnBlockUser.setVisibility(View.GONE);
        } else {
            // Là CHAT 1-1
            tvMemberCount.setVisibility(View.GONE);

            groupOptionsLayout.setVisibility(View.GONE);
            btnLeaveGroup.setVisibility(View.GONE);
            btnBlockUser.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Gán sự kiện click cho các nút
     */
    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> onBackPressed());

        // (Chỉ là Toast, bạn có thể tự code chức năng sau)
        layoutViewMedia.setOnClickListener(v -> Toast.makeText(this, "Mở trang media...", Toast.LENGTH_SHORT).show());
        layoutViewMembers.setOnClickListener(v -> Toast.makeText(this, "Mở danh sách thành viên...", Toast.LENGTH_SHORT).show());
        layoutAddMember.setOnClickListener(v -> Toast.makeText(this, "Mở trang thêm thành viên...", Toast.LENGTH_SHORT).show());
        switchMute.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(this, "Đã tắt thông báo", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Đã bật thông báo", Toast.LENGTH_SHORT).show();
            }
            // TODO: Gọi API cập nhật trạng thái Mute
        });

        // Nút Rời nhóm
        btnLeaveGroup.setOnClickListener(v -> showConfirmationDialog(
                "Rời nhóm",
                "Bạn có chắc chắn muốn rời khỏi nhóm này không?",
                this::performLeaveGroup)); // (Dùng Java 8 method reference)

        // Nút Chặn
        btnBlockUser.setOnClickListener(v -> showConfirmationDialog(
                "Chặn người dùng",
                "Bạn có chắc chắn muốn chặn người dùng này? Bạn sẽ không thể gửi hoặc nhận tin nhắn.",
                this::performBlockUser));
    }

    /**
     * Hàm chung để hiển thị popup xác nhận
     */
    private void showConfirmationDialog(String title, String message, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    // Chạy hành động (ví dụ: performLeaveGroup)
                    onConfirm.run();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * TODO: Code logic rời nhóm
     */
    private void performLeaveGroup() {
        // TODO:
        // 1. Gọi API (Supabase) để xóa user khỏi bảng 'conversation_members'
        //    (DELETE /rest/v1/conversation_members?conversation_id=eq...&user_id=eq...)
        // 2. Gửi một tin nhắn hệ thống "User... đã rời nhóm" (type: 'system')
        // 3. Đóng Activity, quay về danh sách chat
        Toast.makeText(this, "Đang rời nhóm... (chưa code)", Toast.LENGTH_SHORT).show();
        // (Sau khi API thành công -> finish())
    }

    /**
     * TODO: Code logic chặn
     */
    private void performBlockUser() {
        // TODO:
        // 1. Gọi API (Supabase) để thêm vào bảng 'blocked_users' (nếu bạn có)
        // 2. Đóng Activity
        Toast.makeText(this, "Đang chặn... (chưa code)", Toast.LENGTH_SHORT).show();
        // (Sau khi API thành công -> finish())
    }
}