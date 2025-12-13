package com.example.talkify.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.talkify.R;
import com.example.talkify.services.SharedPrefManager;
import com.example.talkify.services.SupabaseApiService;
import com.example.talkify.services.SupabaseClient;
import com.example.talkify.utils.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountPrivacyActivity extends AppCompatActivity {

    private ImageView ivBack;

    // Các View cha (được include từ XML)
    private View itemPrivacyProfile, itemLastSeen, itemBlockedUsers;
    private View itemAccountInfo, itemPasswordChange, itemTwoFactorAuth;
    private View itemDownloadData, itemDeleteAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_privacy);

        initViews();
        setupListeners();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);

        // 1. Ánh xạ các khối layout cha
        itemPrivacyProfile = findViewById(R.id.itemPrivacyProfile);
        itemLastSeen = findViewById(R.id.itemLastSeen);
        itemBlockedUsers = findViewById(R.id.itemBlockedUsers);

        itemAccountInfo = findViewById(R.id.itemAccountInfo);
        itemPasswordChange = findViewById(R.id.itemPasswordChange);
        itemTwoFactorAuth = findViewById(R.id.itemTwoFactorAuth);

        itemDownloadData = findViewById(R.id.itemDownloadData);
        itemDeleteAccount = findViewById(R.id.itemDeleteAccount);

        // 2. Điền dữ liệu vào từng dòng (Title, Icon)
        // Lưu ý: Bạn có thể thay đổi R.drawable.ic_xxx thành icon thực tế của bạn

        // --- Nhóm Quyền riêng tư ---
        setupItem(itemPrivacyProfile, "Quyền riêng tư hồ sơ", R.drawable.ic_user);
        setupItem(itemLastSeen, "Trạng thái hoạt động", R.drawable.ic_clock);
        setupItem(itemBlockedUsers, "Tài khoản bị chặn", R.drawable.ic_block);

        // --- Nhóm Bảo mật ---
        setupItem(itemAccountInfo, "Thông tin", R.drawable.ic_user);
        setupItem(itemPasswordChange, "Đổi mật khẩu", R.drawable.ic_key);
        setupItem(itemTwoFactorAuth, "Xác thực 2 bước", R.drawable.ic_shield);

        // --- Nhóm Dữ liệu ---
        setupItem(itemDownloadData, "Tải xuống dữ liệu", R.drawable.ic_download);

        // Mục Xóa tài khoản (Icon thùng rác)
        setupRedItem(itemDeleteAccount, "Xóa tài khoản", R.drawable.ic_trash);
    }

    /**
     * Hàm quan trọng: Tìm và gán dữ liệu vào các view con trong layout include
     */
    private void setupItem(View view, String title, int iconResId) {
        if (view == null) return;

        // Tìm các view con dựa trên ID trong file XML bạn cung cấp
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        ImageView ivIcon = view.findViewById(R.id.ivIcon);

        // Gán Tiêu đề
        if (tvTitle != null) {
            tvTitle.setText(title);
        }

        // Gán Icon
        if (ivIcon != null) {
            ivIcon.setImageResource(iconResId);
        }
    }

    private void setupRedItem(View view, String title, int iconResId) {
        // 1. Gọi lại hàm cũ để gán Text và Icon như bình thường
        setupItem(view, title, iconResId);

        // 2. Định nghĩa màu đỏ (Màu đỏ chuẩn iOS/Android cảnh báo)
        int redColor = android.graphics.Color.parseColor("#FF3B30");
        // Hoặc dùng: android.graphics.Color.RED

        // 3. Tìm View và đổi màu
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        ImageView ivIcon = view.findViewById(R.id.ivIcon);

        if (tvTitle != null) {
            tvTitle.setTextColor(redColor); // Đổi màu chữ tiêu đề
        }

        if (ivIcon != null) {
            ivIcon.setColorFilter(redColor); // Đổi màu (tint) của icon
        }
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());

        // Các sự kiện click thông thường (Demo Toast)
        itemPrivacyProfile.setOnClickListener(v -> Toast.makeText(this, "Quyền riêng tư", Toast.LENGTH_SHORT).show());
        itemLastSeen.setOnClickListener(v -> showLastSeenDialog());
        itemBlockedUsers.setOnClickListener(v -> Toast.makeText(this, "Danh sách chặn", Toast.LENGTH_SHORT).show());

        itemAccountInfo.setOnClickListener(v -> {
            // Khởi tạo Intent để chuyển sang ProfileInfoActivity
            Intent intent = new Intent(this, ProfileInfoActivity.class);
            startActivity(intent);
        });

        itemPasswordChange.setOnClickListener(v -> Toast.makeText(this, "Đổi mật khẩu", Toast.LENGTH_SHORT).show());
        itemTwoFactorAuth.setOnClickListener(v -> Toast.makeText(this, "2FA", Toast.LENGTH_SHORT).show());

        itemDownloadData.setOnClickListener(v -> Toast.makeText(this, "Tải dữ liệu", Toast.LENGTH_SHORT).show());

        // --- SỰ KIỆN XÓA TÀI KHOẢN (GIỮ NGUYÊN) ---
        itemDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    // =========================================================
    // KHU VỰC XỬ LÝ XÓA TÀI KHOẢN
    // =========================================================

    private void showLastSeenDialog() {
        String[] options = {"Mọi người", "Danh sách bạn bè", "Không ai cả"};
        new AlertDialog.Builder(this)
                .setTitle("Ai có thể thấy bạn Online?")
                .setSingleChoiceItems(options, 0, (dialog, which) -> {
                    Toast.makeText(this, "Đã chọn: " + options[which], Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Hiển thị Dialog cảnh báo xóa tài khoản
     */
    private void showDeleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xóa tài khoản vĩnh viễn?");
        builder.setMessage("Hành động này sẽ xóa toàn bộ tin nhắn, danh bạ và dữ liệu của bạn khỏi hệ thống.\n\nBạn KHÔNG THỂ khôi phục lại tài khoản sau khi xóa. Bạn có chắc chắn muốn tiếp tục?");
        builder.setIcon(R.drawable.ic_warning);
        builder.setCancelable(false);

        builder.setPositiveButton("Xóa ngay", (dialog, which) -> {
            performDeleteAccount();
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Đổi màu nút Xóa thành Đỏ
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.RED);
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.GRAY);
        }
    }

    /**
     * Gọi API xóa tài khoản
     */
    private void performDeleteAccount() {
        SharedPrefManager prefs = SharedPrefManager.getInstance(this);
        String token = prefs.getToken();
        String apiKey = SupabaseClient.ANON_KEY;

        if (token == null) {
            Toast.makeText(this, "Lỗi xác thực, vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Đang xử lý xóa tài khoản...", Toast.LENGTH_SHORT).show();

        SupabaseApiService apiService = RetrofitClient.getApiService();

        // Gọi API với Bearer Token
        apiService.deleteMyAccount("Bearer " + token, apiKey).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AccountPrivacyActivity.this, "Tài khoản đã được xóa vĩnh viễn.", Toast.LENGTH_LONG).show();

                    // Xóa dữ liệu cục bộ
                    SharedPrefManager.getInstance(AccountPrivacyActivity.this).clearUserSession();

                    // Về màn hình Login
                    Intent intent = new Intent(AccountPrivacyActivity.this, com.example.talkify.auth.LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown";
                        android.util.Log.e("DeleteAccount", "Lỗi: " + response.code() + " - " + errorBody);
                        Toast.makeText(AccountPrivacyActivity.this, "Lỗi xóa tài khoản: " + response.code(), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(AccountPrivacyActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}