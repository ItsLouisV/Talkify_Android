package com.example.talkify.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.talkify.R;
import com.example.talkify.models.User;
import com.example.talkify.services.SharedPrefManager;
import com.example.talkify.services.SupabaseApiService;
import com.example.talkify.services.SupabaseClient; // Import để lấy ANON_KEY
import com.example.talkify.utils.RetrofitClient;
import com.bumptech.glide.Glide; // Giả định dùng Glide để tải Avatar

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileInfoActivity extends AppCompatActivity {

    private ImageView btnBack, imgAvatar;
    private TextView tvBio;
    private LinearLayout btnEdit;

    // Các View cha của mỗi dòng thông tin
    private View rowFullName, rowUsername, rowEmail, rowDob, rowGender, rowCreatedAt;

    private SharedPrefManager sharedPrefManager;
    private SupabaseApiService apiService;

    private static final String DEFAULT_NOT_SET = "Chưa cập nhật";
    private static final String DEFAULT_BIO = "Bio hiện ở đây, hiện tại chưa cập nhật";
    private static final String DATE_FORMAT = "dd/MM/yyyy";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_info);

        sharedPrefManager = SharedPrefManager.getInstance(this);
        // RetrofitClient đã được cấu hình để tạo service
        apiService = RetrofitClient.getApiService();

        initViews();
        setupRowDetails();
        setupListeners();
        loadAccountInfo();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        imgAvatar = findViewById(R.id.imgAvatar);
        tvBio = findViewById(R.id.tvBio);
        btnEdit = findViewById(R.id.btnEdit);

        // Ánh xạ các View cha (include layouts)
        rowFullName = findViewById(R.id.rowFullName);
        rowUsername = findViewById(R.id.rowUsername);
        rowEmail = findViewById(R.id.rowEmail);
        rowDob = findViewById(R.id.rowDob);
        rowGender = findViewById(R.id.rowGender);
        rowCreatedAt = findViewById(R.id.rowCreatedAt);
    }

    /**
     * Thiết lập Tên và Icon cho các dòng thông tin (sử dụng id trong item_row_info.xml: tvLabel và ivIcon)
     */
    private void setupRowDetails() {
        // FULLNAME
        // Giả định ic_fullname, ic_username, v.v. đã tồn tại trong drawable
        setRowDetails(rowFullName, "Tên Talkify", R.drawable.ic_fullname);

        // USERNAME
        setRowDetails(rowUsername, "Username", R.drawable.ic_username);

        // EMAIL
        setRowDetails(rowEmail, "Email", R.drawable.ic_mail);

        // NGÀY SINH
        setRowDetails(rowDob, "Ngày sinh", R.drawable.ic_dob);

        // GIỚI TÍNH
        setRowDetails(rowGender, "Giới tính", R.drawable.ic_gender);

        // NGÀY TẠO TÀI KHOẢN
        setRowDetails(rowCreatedAt, "Ngày tạo tài khoản", R.drawable.ic_calender);
    }

    // Hàm tiện ích để gán giá trị Label và Icon cho một dòng
    private void setRowDetails(View row, String label, int iconResId) {
        TextView tvLabel = row.findViewById(R.id.tvLabel);
        ImageView ivIcon = row.findViewById(R.id.ivIcon);

        if (tvLabel != null) {
            tvLabel.setText(label);
        }
        if (ivIcon != null) {
            ivIcon.setImageResource(iconResId);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Đến màn hình Chỉnh sửa thông tin
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileInfoActivity.this, EditProfileInfoActivity.class);
            startActivity(intent);
            Toast.makeText(this, "Chỉnh sửa thông tin", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadAccountInfo() {
        String userId = sharedPrefManager.getUserId(); // Lấy User ID từ SharedPreferences
        if (userId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy User ID", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = "Bearer " + sharedPrefManager.getToken(); // Lấy Token từ SharedPreferences

        String apiKey = SupabaseClient.ANON_KEY;

        // Gọi API getUserDetails
        apiService.getUserDetails(
                token,
                apiKey,
                "eq." + userId,
                // Yêu cầu tất cả các trường cần thiết
                "*,email,full_name,user_name,bio,avatar_url,created_at,dob,gender"
        ).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User user = response.body().get(0);
                    displayUserInfo(user);
                } else {
                    Toast.makeText(ProfileInfoActivity.this, "Không thể tải thông tin cá nhân. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(ProfileInfoActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayUserInfo(User user) {

        // 1. Bio
        tvBio.setText(user.getBio() != null && !user.getBio().isEmpty() ? user.getBio() : DEFAULT_BIO);

        // 2. Avatar (Dùng Glide để tải ảnh)
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(this).load(user.getAvatarUrl()).placeholder(R.drawable.avatar_user_default).into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.drawable.avatar_user_default);
        }

        // 3. Gán giá trị vào các dòng thông tin

        // FULLNAME
        setRowValue(rowFullName, user.getFullName());

        // USERNAME
        // Thêm ký tự "@" trước khi gọi hàm setRowValue
        String formattedUsername = user.getUserName() != null
                ? "@" + user.getUserName()
                : user.getUserName(); // Giữ nguyên null/rỗng nếu username không có
        setRowValue(rowUsername, formattedUsername);

        // EMAIL
        setRowValue(rowEmail, user.getEmail());

        // NGÀY SINH (DOB)
        setRowValue(rowDob, formatDob(user.getDob()));

        // GIỚI TÍNH
        setRowValue(rowGender, formatGender(user.getGender()));

        // NGÀY TẠO TÀI KHOẢN (created_at)
        setRowValue(rowCreatedAt, formatTimestamp(user.getCreatedAt()));
    }

    // Hàm tiện ích để gán giá trị cho TextView Value trong một dòng
    private void setRowValue(View row, String value) {
        TextView tvValue = row.findViewById(R.id.tvValue);
        if (tvValue != null) {
            tvValue.setText(value != null && !value.isEmpty() ? value : DEFAULT_NOT_SET);
        }
    }

    // Hàm chuyển đổi Timestamp từ Supabase sang định dạng dd/MM/yyyy
    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return DEFAULT_NOT_SET;

        // Supabase trả về ISO 8601, ví dụ: "2023-12-13T05:55:38.123+00:00"
        try {
            // Định dạng đầu vào (giả định)
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            // Định dạng đầu ra mong muốn
            SimpleDateFormat outputFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());

            return outputFormat.format(inputFormat.parse(timestamp));
        } catch (ParseException e) {
            // Nếu parse lỗi, chỉ trả về ngày thô hoặc mặc định
            return timestamp.length() >= 10 ? timestamp.substring(0, 10) : timestamp;
        }
    }

    // Hàm chuyển đổi DOB
    private String formatDob(String dob) {
        // Nếu DOB được lưu dưới dạng yyyy-MM-dd, có thể format lại
        if (dob == null || dob.isEmpty()) return DEFAULT_NOT_SET;

        // Nếu DOB là YYYY-MM-DD
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
            return outputFormat.format(inputFormat.parse(dob));
        } catch (ParseException e) {
            return dob;
        }
    }

    // Hàm dịch giới tính sang tiếng Việt
    private String formatGender(String gender) {
        if (gender == null) return DEFAULT_NOT_SET;
        switch (gender.toLowerCase(Locale.ROOT)) {
            case "male":
                return "Nam";
            case "female":
                return "Nữ";
            case "other":
                return "Khác";
            case "prefer_not_to_say":
                return "Không tiết lộ";
            default:
                return DEFAULT_NOT_SET;
        }
    }
}