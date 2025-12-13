package com.example.talkify.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.talkify.MainActivity;
import com.example.talkify.R;
import com.example.talkify.services.SharedPrefManager;
import com.example.talkify.services.SupabaseClient;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;

import okhttp3.*;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin, btnGoogle, btnFacebook;
    private TextView tvRegister;

    private SharedPrefManager prefManager;
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Google Sign-In
    private GoogleSignInClient mGoogleSignInClient;
    private static final String GOOGLE_ANDROID_CLIENT_ID =
            "677059573412-fi4ftvk1abt49r179epmbijpj2276k2v.apps.googleusercontent.com";

    // (Lưu ý: Bạn phải thêm hàm saveUserSession vào SharedPrefManager)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefManager = SharedPrefManager.getInstance(this);

        if (prefManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        initViews();
        setupListeners();
        setupGoogleSignIn();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);
        tvRegister = findViewById(R.id.tvRegister);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email và mật khẩu.", Toast.LENGTH_SHORT).show();
                return;
            }

            loginWithEmail(email, password);
        });

        btnGoogle.setOnClickListener(v -> signInWithGoogle());
        btnFacebook.setOnClickListener(v -> Toast.makeText(this, "Tính năng Facebook đang được phát triển.", Toast.LENGTH_SHORT).show());
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(GOOGLE_ANDROID_CLIENT_ID)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleSignInResult(task);
                } else {
                    Toast.makeText(LoginActivity.this, "Đăng nhập Google thất bại hoặc bị hủy.", Toast.LENGTH_SHORT).show();
                }
            });

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String idToken = account.getIdToken();

            if (idToken != null) {
                signInWithSupabaseGoogle(idToken);
            } else {
                Toast.makeText(this, "Không thể lấy Google ID Token.", Toast.LENGTH_SHORT).show();
            }
        } catch (ApiException e) {
            Log.e(TAG, "Google Sign-In Error: " + e.getMessage());
            Toast.makeText(this, "Đăng nhập Google thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * BƯỚC 1 (GOOGLE): Lấy Token
     */
    private void signInWithSupabaseGoogle(String idToken) {
        String url = SupabaseClient.URL + "/auth/v1/token?grant_type=id_token";
        JsonObject json = new JsonObject();
        json.addProperty("provider", "google");
        json.addProperty("id_token", idToken);

        RequestBody body = RequestBody.create(gson.toJson(json), JSON);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", SupabaseClient.ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Lỗi mạng: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String resStr = response.body().string();
                if (response.isSuccessful()) {
                    JsonObject resJson = gson.fromJson(resStr, JsonObject.class);
                    String accessToken = resJson.get("access_token").getAsString();
                    String refreshToken = resJson.get("refresh_token").getAsString();
                    JsonObject userObj = resJson.getAsJsonObject("user");
                    String userId = userObj.get("id").getAsString();
                    String email = userObj.get("email").getAsString();

                    // === SỬA ĐỔI ===
                    // (Không lưu và chuyển màn hình vội)
                    // (Gọi Bước 2: Lấy hồ sơ)
                    fetchUserProfileAndProceed(userId, email, accessToken, refreshToken);

                } else {
                    Log.e(TAG, "Supabase Google login failed: " + resStr);
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Đăng nhập Google thất bại.", Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    /**
     * BƯỚC 1 (EMAIL): Lấy Token
     */
    private void loginWithEmail(String email, String password) {
        String url = SupabaseClient.URL + "/auth/v1/token?grant_type=password";
        JsonObject json = new JsonObject();
        json.addProperty("email", email);
        json.addProperty("password", password);

        RequestBody body = RequestBody.create(gson.toJson(json), JSON);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", SupabaseClient.ANON_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String resStr = response.body().string();
                if (response.isSuccessful()) {
                    JsonObject resJson = gson.fromJson(resStr, JsonObject.class);
                    String accessToken = resJson.get("access_token").getAsString();
                    String refreshToken = resJson.get("refresh_token").getAsString();
                    JsonObject userObj = resJson.getAsJsonObject("user");
                    String userId = userObj.get("id").getAsString();

                    // === SỬA ĐỔI ===
                    // (Không lưu và chuyển màn hình vội)
                    // (Gọi Bước 2: Lấy hồ sơ)
                    fetchUserProfileAndProceed(userId, email, accessToken, refreshToken);

                } else {
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this, "Sai email hoặc mật khẩu.", Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    /**
     * ==========================================================
     * HÀM MỚI (BƯỚC 2 & 3): Lấy Hồ sơ và Lưu Session
     * ==========================================================
     */
    private void fetchUserProfileAndProceed(String userId, String email, String accessToken, String refeshToken) {
        // (API call: GET /rest/v1/users?user_id=eq.{userId}&select=*)
        String url = SupabaseClient.URL + "/rest/v1/users?user_id=eq." + userId + "&select=*";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", SupabaseClient.ANON_KEY)
                .addHeader("Authorization", "Bearer " + accessToken) // Dùng token MỚI
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Lỗi tải hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String resStr = response.body().string();
                if (response.isSuccessful()) {
                    // Kết quả là một mảng JSON: "[{...}]"
                    JsonArray jsonArray = gson.fromJson(resStr, JsonArray.class);

                    if (jsonArray.size() > 0) {
                        // Lấy hồ sơ (profile) đầu tiên
                        JsonObject userProfile = jsonArray.get(0).getAsJsonObject();

                        String fullName = userProfile.get("full_name").getAsString();
                        String avatarUrl = userProfile.get("avatar_url").getAsString();

                        // BƯỚC 3: LƯU TẤT CẢ VÀO SESSION
                        prefManager.saveUserSession(
                                userId,
                                email,
                                accessToken,
                                refeshToken,
                                fullName,
                                avatarUrl
                        );

                        // Bây giờ mới chuyển màn hình
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        });
                    } else {
                        // (Lỗi này xảy ra nếu Trigger (bước 1) bị lỗi:
                        //  đã tạo user (auth) nhưng chưa tạo profile (database))
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Lỗi: Không tìm thấy hồ sơ người dùng.", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    // Lỗi (ví dụ: 401 do RLS, 404, ...)
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Lỗi tải hồ sơ: " + resStr, Toast.LENGTH_LONG).show());
                }
            }
        });
    }
}