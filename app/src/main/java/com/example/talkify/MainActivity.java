package com.example.talkify;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.talkify.Fragment.ChatsFragment;
import com.example.talkify.Fragment.NotificationsFragment;
import com.example.talkify.Fragment.SettingsFragment;
import com.example.talkify.Fragment.UsersFragment;
import com.example.talkify.models.AppNotification;
import com.example.talkify.services.SharedPrefManager;
import com.example.talkify.services.SupabaseApiService;
import com.example.talkify.services.SupabaseClient;
import com.example.talkify.ui.ProfileActivity;
import com.example.talkify.ui.SeeAllRequestActivity;
import com.example.talkify.utils.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    // --- CÁC BIẾN CHO THÔNG BÁO ---
    private Handler notificationHandler;
    private Runnable notificationRunnable;
    private static final String CHANNEL_ID = "TALKIFY_CHANNEL_ID";
    private static final int PERMISSION_REQUEST_CODE = 101;

    private SupabaseApiService apiService;
    private SharedPrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Khởi tạo các dịch vụ
        apiService = RetrofitClient.getApiService();
        prefManager = SharedPrefManager.getInstance(this);

        // 2. Setup Bottom Navigation
        bottomNavigation = findViewById(R.id.bottomNavigation);


        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            int id = item.getItemId();
            if (id == R.id.nav_chats) {
                selected = new ChatsFragment();
            } else if (id == R.id.nav_users) {
                selected = new UsersFragment();
            } else if (id == R.id.nav_notifications) {
                selected = new NotificationsFragment();
            } else if (id == R.id.nav_settings) {
                selected = new SettingsFragment();
            }

            if (selected != null) {
                loadFragment(selected);
            }
            return true;
        });

        // Nếu token chết, app sẽ tự refresh tại đây
        checkUserSession();

        // 3. Cài đặt hệ thống Thông báo
        setupNotificationSystem();
    }

    /**
     * Kiểm tra và làm mới Token khi mở App
     * Tránh lỗi màn hình trắng khi để qua đêm
     */
    private void checkUserSession() {
        String refreshToken = prefManager.getRefreshToken();

        if (refreshToken == null) {
            // Không có refresh token -> Bắt buộc đăng nhập lại
            showSessionExpiredDialog();
            return;
        }

        // 1. Tạo đường dẫn tuyệt đối (Full URL) để tránh bị nối vào /rest/v1
        String fullUrl = SupabaseClient.URL + "/auth/v1/token?grant_type=refresh_token";

        // Gọi API Refresh Token
        Map<String, String> body = new HashMap<>();
        body.put("refresh_token", refreshToken);

        apiService.refreshToken(fullUrl, SupabaseClient.ANON_KEY, body).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                if (response.isSuccessful()) {
                    try {
                        // 1. Parse JSON thủ công
                        String json = new com.google.gson.Gson().toJson(response.body());
                        com.google.gson.JsonObject obj = new com.google.gson.Gson().fromJson(json, com.google.gson.JsonObject.class);

                        String newAccessToken = obj.get("access_token").getAsString();
                        String newRefreshToken = obj.get("refresh_token").getAsString();

                        // 2. Cập nhật lại vào SharedPref
                        prefManager.saveToken(newAccessToken);
                        prefManager.saveRefreshToken(newRefreshToken);

                        // 3. [QUAN TRỌNG] Token đã sống lại -> LOAD GIAO DIỆN NGAY
                        setupUI();

                    } catch (Exception e) {
                        e.printStackTrace();
                        // Parse lỗi -> Coi như phiên hết hạn -> Hiện Dialog
                        showSessionExpiredDialog();
                    }
                } else {
                    // Token hết hạn quá lâu (> 60 ngày) hoặc bị thu hồi -> Hiện Dialog bắt đăng nhập lại
                    showSessionExpiredDialog();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                // Mất mạng -> Vẫn cho vào app để xem cache (chế độ Offline)
                Toast.makeText(MainActivity.this, "Không có kết nối mạng", Toast.LENGTH_SHORT).show();
                setupUI();
            }
        });
    }

    /**
     * Hiển thị Dialog bắt buộc đăng nhập lại
     * Không cho phép hủy (setCancelable = false)
     */
    private void showSessionExpiredDialog() {
        if (isFinishing()) return; // Kiểm tra để tránh lỗi crash

        new AlertDialog.Builder(this)
                .setTitle("Thông báo")
                .setMessage("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
                .setCancelable(false) // QUAN TRỌNG: Không cho bấm ra ngoài hoặc nút Back
                .setPositiveButton("OK", (dialog, which) -> {
                    // Khi bấm OK thì mới thực hiện Logout
                    performLogout();
                })
                .show();
    }

    /**
     * Hàm thực hiện xóa dữ liệu và chuyển về màn hình Login
     */
    private void performLogout() {
        // Xóa sạch dữ liệu trong máy
        prefManager.clearUserSession();

        // Chuyển về màn hình Login
        Intent intent = new Intent(this, com.example.talkify.auth.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Xóa Stack để không Back lại được
        startActivity(intent);
        finish();
    }

    /**
     * Hàm này được gọi khi Token đã được xác thực (hoặc refresh) thành công.
     * Lúc này mới bắt đầu load giao diện để tránh lỗi 401.
     */
    private void setupUI() {
        // Kiểm tra nếu Fragment chưa được add thì mới add (để tránh reload khi xoay màn hình)
        if (getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) == null) {
            loadFragment(new ChatsFragment()); // Mặc định vào màn hình chat
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    // ==================================================================
    // =================== KHU VỰC XỬ LÝ THÔNG BÁO ======================
    // ==================================================================

    private void setupNotificationSystem() {
        createNotificationChannel(); // Tạo kênh (Android 8+)
        checkAndRequestPermissions(); // Xin quyền (Android 13+)
        startCheckingNotifications(); // Bắt đầu chạy ngầm kiểm tra
    }

    /**
     * Bắt đầu vòng lặp kiểm tra thông báo mỗi 5 giây
     */
    private void startCheckingNotifications() {
        notificationHandler = new Handler(Looper.getMainLooper());
        notificationRunnable = new Runnable() {
            @Override
            public void run() {
                // Gọi API kiểm tra
                checkUnreadNotifications();

                // Lặp lại sau 5 giây
                notificationHandler.postDelayed(this, 5000);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Khi app mở lên, chạy polling
        if (notificationHandler != null) notificationHandler.post(notificationRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Khi app ẩn xuống/tắt màn hình, dừng polling để tiết kiệm pin
        if (notificationHandler != null) notificationHandler.removeCallbacks(notificationRunnable);
    }

    /**
     * Gọi API lấy các thông báo CHƯA ĐỌC (is_read = false)
     */
    private void checkUnreadNotifications() {
        String userId = prefManager.getUserId();
        if (userId == null) return;

        String token = "Bearer " + prefManager.getToken();
        // Join bảng users để lấy tên người gửi (actor)
        String select = "*,actor:users!actor_id(full_name)";

        apiService.getUnreadNotifications(
                        token,
                        SupabaseClient.ANON_KEY,
                        "eq." + userId,  // <--- THÊM "eq."
                        "eq.false",      // <--- THÊM "eq." VÀO TRƯỚC FALSE
                        select,
                        "created_at.desc"
                )
                .enqueue(new Callback<List<AppNotification>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<AppNotification>> call, @NonNull Response<List<AppNotification>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            for (AppNotification notif : response.body()) {
                                // 1. Hiển thị thông báo lên thanh trạng thái
                                showSystemNotification(notif);

                                // 2. Đánh dấu là đã đọc ngay lập tức (để không hiện lại lần sau)
                                markAsRead(notif.getNotificationId());
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<AppNotification>> call, @NonNull Throwable t) {
                        // Lỗi mạng, bỏ qua, lát check lại sau
                    }
                });
    }

    /**
     * Gọi API đánh dấu thông báo đã đọc
     */
    private void markAsRead(String notifId) {
        String token = "Bearer " + prefManager.getToken();
        Map<String, Boolean> body = new HashMap<>();
        body.put("is_read", true);

        apiService.markNotificationAsRead(token, SupabaseClient.ANON_KEY, "eq." + notifId, body)
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> c, Response<Void> r) {}
                    @Override public void onFailure(Call<Void> c, Throwable t) {}
                });
    }

    /**
     * Tạo và hiển thị Notification Android (Đã bao gồm logic chuyển màn hình)
     */
    private void showSystemNotification(AppNotification notif) {
        String actorName = (notif.getActor() != null) ? notif.getActor().getFullName() : "Ai đó";
        String title = "Talkify";
        String content = "";
        Intent intent = null;

        // --- LOGIC CHUYỂN MÀN HÌNH ---
        if ("friend_request".equals(notif.getType())) {
            // A gửi lời mời -> B nhận được -> Bấm vào mở SeeAllRequestActivity
            content = actorName + " đã gửi cho bạn lời mời kết bạn.";
            intent = new Intent(this, SeeAllRequestActivity.class);

        } else if ("friend_accepted".equals(notif.getType())) {
            // B chấp nhận -> A nhận được -> Bấm vào mở Profile của B
            content = actorName + " đã chấp nhận lời mời kết bạn.";
            intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("USER_ID", notif.getActorId());

        } else {
            // Trường hợp khác (ví dụ tin nhắn) -> Mở MainActivity
            content = actorName + ": " + notif.getContent();
            intent = new Intent(this, MainActivity.class);
        }

        if (intent == null) return;

        // Tạo PendingIntent để mở Activity khi bấm vào thông báo
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(), // RequestCode ngẫu nhiên để không trùng
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        // Build thông báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell) // Đảm bảo bạn có icon này hoặc dùng ic_launcher
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Hiện ngay lập tức
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); // Bấm vào thì tự biến mất

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    /**
     * Tạo kênh thông báo (Bắt buộc cho Android 8.0+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Talkify Notifications";
            String description = "Thông báo kết bạn và tin nhắn";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Xin quyền hiển thị thông báo (Bắt buộc cho Android 13+)
     */
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
            }
        }
    }
}