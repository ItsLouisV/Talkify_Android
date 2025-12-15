package com.example.talkify.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.talkify.R;
import com.example.talkify.services.SupabaseClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private static final String DEFAULT_AVATAR_URL =
            "https://cdn-icons-png.flaticon.com/512/149/149071.png";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);

        findViewById(R.id.tvLogin).setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        btnRegister.setOnClickListener(v -> handleSignup());
    }

    // Định nghĩa Biểu thức chính quy cho mật khẩu mạnh
    private static final String PASSWORD_REGEX =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

    private void handleSignup() {
        String fullName = getText(etFullName);
        String email = getText(etEmail);
        String password = getText(etPassword);
        String confirm = getText(etConfirmPassword);

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showToast("Vui lòng điền đầy đủ các trường.");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Định dạng email không hợp lệ.");
            return;
        }
        if (!password.equals(confirm)) {
            showToast("Mật khẩu không khớp.");
            return;
        }

        // --- PHẦN KIỂM TRA MẬT KHẨU MỚI ---
        if (!password.matches(PASSWORD_REGEX)) {
            showToast("Vui lòng tuân theo quy định đặt mật khẩu của Talkify");
            return;
        }
        // ----------------------------------

        // Nếu tất cả kiểm tra đều qua
        RegisterRequest req = new RegisterRequest(email, password, fullName);
        signupSupabase(req);
    }

    private void signupSupabase(RegisterRequest request) {
        OkHttpClient client = new OkHttpClient();

        JsonObject json = new JsonObject();
        json.addProperty("email", request.getEmail());
        json.addProperty("password", request.getPassword());

        JsonObject meta = new JsonObject();
        meta.addProperty("full_name", request.getFullName());
        // Thêm avatar mặc định vào metadata để trigger có thể đọc
        meta.addProperty("avatar_url", DEFAULT_AVATAR_URL);
        json.add("data", meta);

        String url = SupabaseClient.URL + "/auth/v1/signup";
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(json.toString(), JSON);

        Request httpReq = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", SupabaseClient.ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(httpReq).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showToast("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Đọc body response một lần và lưu vào biến final
                // Sử dụng final để có thể truy cập trong inner class runOnUiThread
                final String resStr = response.body().string();

                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        showToast("Account created! Please Login.");
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        // --- PHẦN ĐÃ SỬA: PHÂN TÍCH VÀ DỊCH LỖI DỰA TRÊN THỰC TẾ ---
                        String errorMessage = "Đăng ký thất bại.";

                        try {
                            // 1. Parse body JSON
                            JsonObject jsonResponse = JsonParser.parseString(resStr).getAsJsonObject();

                            // 2. Lấy thông báo lỗi từ trường "msg" (Đã xác nhận từ Logcat)
                            if (jsonResponse.has("msg")) {
                                errorMessage = jsonResponse.get("msg").getAsString();
                            } else {
                                // Fallback nếu cấu trúc JSON lỗi khác (không khuyến nghị)
                                errorMessage = "Lỗi " + response.code() + ": " + resStr;
                            }

                            // 3. DỊCH LỖI: Kiểm tra nội dung tiếng Anh và thay thế
                            String lowerCaseError = errorMessage.toLowerCase(Locale.ROOT);

                            if (lowerCaseError.contains("user already registered")) {
                                errorMessage = "Email đã được sử dụng. Vui lòng thử email khác.";
                            }

                        } catch (Exception e) {
                            // Xử lý khi parse JSON thất bại
                            errorMessage = "Lỗi phản hồi không xác định: " + response.code();
                        }

                        showToast(errorMessage);
                        // ------------------------------------
                    }
                });
            }
        });
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
