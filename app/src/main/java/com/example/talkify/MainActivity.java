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
import com.example.talkify.services.SupabaseRealtimeClient;
import com.example.talkify.ui.ProfileActivity;
import com.example.talkify.ui.SeeAllRequestActivity;
import com.example.talkify.utils.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;

    // --- REALTIME: Listener để nhận thông báo tức thì ---
    private SupabaseRealtimeClient.MessageListener notificationRealtimeListener;

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

        // 3. Kiểm tra và làm mới Token (Nếu thành công sẽ gọi setupUI)
        checkUserSession();
    }

    /**
     * Hàm này được gọi khi Token đã được xác thực (hoặc refresh) thành công.
     * Lúc này mới bắt đầu load giao diện và Realtime.
     */
    private void setupUI() {
        // Bắt đầu kết nối Realtime khi ứng dụng đã xác thực
        SupabaseRealtimeClient.getInstance().connect();

        // Cài đặt hệ thống Thông báo (chuyển sang Realtime)
        setupNotificationSystem();

        if (getSupportFragmentManager().findFragmentById(R.id.fragmentContainer) == null) {
            loadFragment(new ChatsFragment());
        }
    }


    /**
     * Kiểm tra và làm mới Token khi mở App
     */
    private void checkUserSession() {
        String refreshToken = prefManager.getRefreshToken();

        if (refreshToken == null) {
            showSessionExpiredDialog();
            return;
        }

        String fullUrl = SupabaseClient.URL + "/auth/v1/token?grant_type=refresh_token";
        Map<String, String> body = new HashMap<>();
        body.put("refresh_token", refreshToken);

        apiService.refreshToken(fullUrl, SupabaseClient.ANON_KEY, body).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                if (response.isSuccessful()) {
                    try {
                        String json = new com.google.gson.Gson().toJson(response.body());
                        JsonObject obj = new com.google.gson.Gson().fromJson(json, JsonObject.class);

                        String newAccessToken = obj.get("access_token").getAsString();
                        String newRefreshToken = obj.get("refresh_token").getAsString();

                        prefManager.saveToken(newAccessToken);
                        prefManager.saveRefreshToken(newRefreshToken);

                        setupUI(); // Token đã sống lại -> LOAD GIAO DIỆN VÀ REALTIME

                    } catch (Exception e) {
                        e.printStackTrace();
                        showSessionExpiredDialog();
                    }
                } else {
                    showSessionExpiredDialog();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Không có kết nối mạng", Toast.LENGTH_SHORT).show();
                setupUI(); // Vẫn load UI để xem cache (chế độ Offline)
            }
        });
    }

    /**
     * Hiển thị Dialog bắt buộc đăng nhập lại
     */
    private void showSessionExpiredDialog() {
        if (isFinishing()) return;

        new AlertDialog.Builder(this)
                .setTitle("Thông báo")
                .setMessage("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    performLogout();
                })
                .show();
    }

    /**
     * Hàm thực hiện xóa dữ liệu và chuyển về màn hình Login
     */
    private void performLogout() {
        prefManager.clearUserSession();

        Intent intent = new Intent(this, com.example.talkify.auth.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    // ==================================================================
    // =================== KHU VỰC XỬ LÝ THÔNG BÁO REALTIME =============
    // ==================================================================

    private void setupNotificationSystem() {
        createNotificationChannel();
        checkAndRequestPermissions();

        // CHỈ GỌI MỘT LẦN để lấy các thông báo cũ bị sót
        checkUnreadNotificationsOnce();

        // BẮT ĐẦU NGHE REALTIME
        setupRealtimeNotificationListener();
    }

    /**
     * ⭐ Bắt đầu lắng nghe sự kiện INSERT vào bảng notifications qua Realtime
     */
    private void setupRealtimeNotificationListener() {
        String userId = prefManager.getUserId();
        if (userId == null) return;

        notificationRealtimeListener = record -> {
            if (!record.optString("user_id").equals(userId)) return;

            // Xử lý thông báo trên Main Thread
            runOnUiThread(() -> {
                try {
                    String notificationId = record.getString("notification_id");
                    // Realtime không có JOIN data -> Gọi API REST để lấy đầy đủ chi tiết
                    fetchNotificationDetailsAndShow(notificationId);
                } catch (Exception e) {
                    // Xử lý lỗi nếu không lấy được ID
                    showSystemNotification(AppNotification.fromRealtimeRecord(record));
                }
            });
        };

        SupabaseRealtimeClient.getInstance().subscribe("notifications", "INSERT", notificationRealtimeListener);
    }

    /**
     * ⭐ Gọi API để lấy thông tin chi tiết thông báo (kèm JOIN Actor Name)
     */
    private void fetchNotificationDetailsAndShow(String notificationId) {
        String token = "Bearer " + prefManager.getToken();
        // Join bảng users để lấy tên người gửi (actor)
        String select = "*,actor:users!actor_id(full_name)";

        // Tái sử dụng getNotifications và filter theo notification_id
        apiService.getNotifications(
                token,
                SupabaseClient.ANON_KEY,
                null,
                "eq." + notificationId, // Filter theo ID thông báo
                select,
                null
        ).enqueue(new Callback<List<AppNotification>>() {
            @Override
            public void onResponse(@NonNull Call<List<AppNotification>> call, @NonNull Response<List<AppNotification>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    AppNotification notif = response.body().get(0);
                    showSystemNotification(notif);
                    markAsRead(notif.getNotificationId());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AppNotification>> call, @NonNull Throwable t) {
                // Lỗi mạng khi fetch chi tiết
            }
        });
    }

    /**
     * Gọi API lấy các thông báo CHƯA ĐỌC LẦN ĐẦU (is_read = false)
     */
    private void checkUnreadNotificationsOnce() {
        String userId = prefManager.getUserId();
        if (userId == null) return;

        String token = "Bearer " + prefManager.getToken();
        String select = "*,actor:users!actor_id(full_name)";

        apiService.getUnreadNotifications(
                        token,
                        SupabaseClient.ANON_KEY,
                        "eq." + userId,
                        "eq.false",
                        select,
                        "created_at.desc"
                )
                .enqueue(new Callback<List<AppNotification>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<AppNotification>> call, @NonNull Response<List<AppNotification>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            for (AppNotification notif : response.body()) {
                                showSystemNotification(notif);
                                markAsRead(notif.getNotificationId());
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<AppNotification>> call, @NonNull Throwable t) {
                        // Lỗi mạng, bỏ qua
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
     * Tạo và hiển thị Notification Android
     */
    private void showSystemNotification(AppNotification notif) {
        String actorName = (notif.getActor() != null) ? notif.getActor().getFullName() : "Ai đó";
        String title = "Talkify";
        String content = "";
        Intent intent = null;

        // --- LOGIC CHUYỂN MÀN HÌNH ---
        if ("friend_request".equals(notif.getType())) {
            content = actorName + " đã gửi cho bạn lời mời kết bạn.";
            intent = new Intent(this, SeeAllRequestActivity.class);

        } else if ("friend_accepted".equals(notif.getType())) {
            content = actorName + " đã chấp nhận lời mời kết bạn.";
            intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("USER_ID", notif.getActorId());

        } else {
            // Trường hợp khác (ví dụ tin nhắn) -> Mở MainActivity
            content = actorName + ": " + notif.getContent();
            intent = new Intent(this, MainActivity.class);
        }

        if (intent == null) return;

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            // Sử dụng ID thông báo làm Notification ID để tránh trùng lặp
            manager.notify(notif.getNotificationId().hashCode(), builder.build());
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền thông báo", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Không có quyền, có thể không nhận được thông báo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Không cần logic polling
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Không cần logic polling
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Ngắt đăng ký Realtime khi Activity bị hủy
        if (notificationRealtimeListener != null) {
            SupabaseRealtimeClient.getInstance().unsubscribe("notifications", "INSERT", notificationRealtimeListener);
        }
        // Có thể thêm SupabaseRealtimeClient.getInstance().disconnect(); nếu đây là Activity duy nhất sử dụng Realtime
    }
}