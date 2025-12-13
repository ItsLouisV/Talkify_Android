package com.example.talkify.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.talkify.R;
import com.example.talkify.models.User;
import com.example.talkify.services.SharedPrefManager;
import com.example.talkify.services.SupabaseApiService;
import com.example.talkify.services.SupabaseClient;
import com.example.talkify.utils.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileInfoActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etUsername, etBio;
    private TextView tvSave, tvDob, tvUsernameStatus;
    private ImageView ivBack;
    private LinearLayout llDobContainer;
    private RadioGroup rgGender;

    private SharedPrefManager sharedPrefManager;
    private SupabaseApiService apiService;

    // Dữ liệu tạm thời
    private String selectedDob = null;
    private String currentUsername = "";
    private final Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile_info);

        sharedPrefManager = SharedPrefManager.getInstance(this);
        apiService = RetrofitClient.getApiService(); // Sử dụng getApiService() đã tối ưu

        initViews();
        loadCurrentUserDetails();
        setupListeners();
        setupUsernameValidation();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        tvSave = findViewById(R.id.tvSave);

        etFullName = findViewById(R.id.etFullName);
        etUsername = findViewById(R.id.etUsername);
        etBio = findViewById(R.id.etBio);
        tvUsernameStatus = findViewById(R.id.tvUsernameStatus);

        llDobContainer = findViewById(R.id.llDobContainer);
        tvDob = findViewById(R.id.tvDob);
        rgGender = findViewById(R.id.rgGender);
    }

    // Tải dữ liệu hiện tại để hiển thị vào form
    private void loadCurrentUserDetails() {
        String userId = sharedPrefManager.getUserId();
        String token = "Bearer " + sharedPrefManager.getToken();
        String apiKey = SupabaseClient.ANON_KEY;

        apiService.getUserDetails(
                token,
                apiKey,
                "eq." + userId,
                "*,email,full_name,user_name,bio,avatar_url,dob,gender"
        ).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User user = response.body().get(0);
                    displayData(user);
                } else {
                    Toast.makeText(EditProfileInfoActivity.this, "Không thể tải dữ liệu cũ.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(EditProfileInfoActivity.this, "Lỗi kết nối.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Hiển thị dữ liệu từ API lên các trường
    private void displayData(User user) {
        etFullName.setText(user.getFullName());
        etUsername.setText(user.getUserName());
        currentUsername = user.getUserName(); // Lưu username hiện tại
        etBio.setText(user.getBio());

        // Ngày sinh
        if (user.getDob() != null && !user.getDob().isEmpty()) {
            selectedDob = user.getDob();
            tvDob.setText(formatDateForDisplay(user.getDob()));
        }

        // Giới tính
        if (user.getGender() != null) {
            switch (user.getGender().toLowerCase(Locale.ROOT)) {
                case "male":
                    rgGender.check(R.id.rbMale);
                    break;
                case "female":
                    rgGender.check(R.id.rbFemale);
                    break;
                case "other":
                    rgGender.check(R.id.rbOther);
                    break;
            }
        }
    }

    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());
        llDobContainer.setOnClickListener(v -> showDatePicker());
        tvSave.setOnClickListener(v -> saveProfile());
    }

    // ===========================================
    // ==== XỬ LÝ DATE PICKER (NGÀY SINH) ====
    // ===========================================

    private void showDatePicker() {
        // 1. Nếu đã có ngày sinh được chọn (từ API hoặc lần trước), thiết lập Calendar về ngày đó
        if (selectedDob != null && !selectedDob.isEmpty()) {
            try {
                // Định dạng lưu trong DB: YYYY-MM-DD
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                calendar.setTime(dbFormat.parse(selectedDob));
            } catch (Exception e) {
                // Nếu lỗi parse, dùng ngày hiện tại (đã được khởi tạo)
                e.printStackTrace();
            }
        }

        // 2. Định nghĩa listener và hiển thị dialog
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, monthOfYear);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDobLabel();
        };

        new DatePickerDialog(this,
                R.style.DatePickerTheme,
                dateSetListener,
                // Khởi tạo Dialog bằng ngày hiện tại của biến 'calendar'
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void updateDobLabel() {
        // Định dạng lưu trong DB: YYYY-MM-DD
        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDob = dbFormat.format(calendar.getTime());

        // Định dạng hiển thị trên màn hình: dd/MM/yyyy
        tvDob.setText(formatDateForDisplay(selectedDob));
    }

    private String formatDateForDisplay(String dob) {
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return displayFormat.format(dbFormat.parse(dob));
        } catch (Exception e) {
            return dob;
        }
    }

    // ===========================================
    // ==== XỬ LÝ USERNAME VÀ TRÙNG LẶP ====
    // ===========================================

    private void setupUsernameValidation() {
        etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String newUsername = s.toString().trim();
                // Chỉ kiểm tra nếu username thay đổi và không rỗng
                if (!newUsername.isEmpty() && !newUsername.equals(currentUsername)) {
                    // Tránh kiểm tra quá nhiều lần (debounce)
                    tvUsernameStatus.setText("Đang kiểm tra...");
                    checkUsernameAvailability(newUsername);
                } else if (newUsername.equals(currentUsername)) {
                    tvUsernameStatus.setText("Tên người dùng hiện tại.");
                    tvUsernameStatus.setTextColor(getResources().getColor(android.R.color.white));
                }
            }
        });
    }

    private void checkUsernameAvailability(String username) {
        // 1. Kiểm tra định dạng hợp lệ (chỉ chữ, số, gạch dưới)
        if (!username.matches("^[a-zA-Z0-9_]{3,20}$")) {
            tvUsernameStatus.setText("Username không hợp lệ. Chỉ 3-20 ký tự (a-z, 0-9, _).");
            tvUsernameStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            return;
        }

        // 2. Gọi API để kiểm tra trùng lặp (loại trừ chính mình)
        String apiKey = SupabaseClient.ANON_KEY;
        String token = "Bearer " + sharedPrefManager.getToken();

        String myUserId = sharedPrefManager.getUserId(); // Lấy ID của mình

        // Điều kiện loại trừ: user_id=neq.{myUserId}
        String notEqSelfFilter = "neq." + myUserId;

        // Supabase PostgREST: Lọc user_name có giá trị bằng username mới
        String filterCondition = "eq." + username;

        // Gọi hàm checkUsernameUnique
        apiService.checkUsernameUnique(
                token, apiKey, filterCondition, notEqSelfFilter
        ).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isEmpty()) {
                        tvUsernameStatus.setText("Username available.");
                        tvUsernameStatus.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                    } else {
                        tvUsernameStatus.setText("Username has already been used.");
                        tvUsernameStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                    }
                } else {
                    tvUsernameStatus.setText("Lỗi kiểm tra trùng lặp.");
                    tvUsernameStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                tvUsernameStatus.setText("Lỗi kết nối khi kiểm tra.");
                tvUsernameStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            }
        });
    }

    // ===========================================
    // ==== LƯU HỒ SƠ (SAVE PROFILE) ====
    // ===========================================

    private void saveProfile() {
        String newFullName = etFullName.getText().toString().trim();
        String newUsername = etUsername.getText().toString().trim();
        String newBio = etBio.getText().toString().trim();
        String newGender = getSelectedGender();

        if (newFullName.isEmpty()) {
            Toast.makeText(this, "Họ và tên không được để trống.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (tvUsernameStatus.getText().toString().contains("sử dụng")) {
            Toast.makeText(this, "Vui lòng chọn Username khác.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Chuẩn bị dữ liệu cập nhật
        Map<String, String> updateFields = new HashMap<>();
        updateFields.put("full_name", newFullName);
        updateFields.put("user_name", newUsername);
        updateFields.put("bio", newBio);
        updateFields.put("dob", selectedDob); // Định dạng YYYY-MM-DD
        updateFields.put("gender", newGender);

        // 2. Gọi API PATCH để cập nhật
        String userId = sharedPrefManager.getUserId();
        String token = "Bearer " + sharedPrefManager.getToken();
        String apiKey = SupabaseClient.ANON_KEY;

        // API PATCH: Cập nhật dòng có user_id hiện tại
        apiService.updateUserProfile(
                token,
                apiKey,
                "eq." + userId, // Tham số Query: user_id=eq.{userId}
                updateFields
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditProfileInfoActivity.this, "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                    // Cập nhật SharedPreferences (nếu cần)
                    sharedPrefManager.saveUserFullName(newFullName);

                    // Đóng màn hình
                    finish();
                } else {
                    Toast.makeText(EditProfileInfoActivity.this, "Cập nhật thất bại. Mã lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(EditProfileInfoActivity.this, "Lỗi kết nối khi lưu: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Lấy giá trị Giới tính được chọn
    private String getSelectedGender() {
        int checkedId = rgGender.getCheckedRadioButtonId();
        if (checkedId == R.id.rbMale) {
            return "male";
        } else if (checkedId == R.id.rbFemale) {
            return "female";
        } else if (checkedId == R.id.rbOther) {
            return "other";
        }
        return null;
    }
}