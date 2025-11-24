package com.example.talkify.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.talkify.R;
import com.example.talkify.adapters.NotificationAdapter;
import com.example.talkify.models.AppNotification;
import com.example.talkify.ui.ProfileActivity;
import com.example.talkify.ui.SeeAllRequestActivity;
import com.example.talkify.utils.RetrofitClient;
import com.example.talkify.services.SharedPrefManager;
import com.example.talkify.services.SupabaseApiService;
import com.example.talkify.services.SupabaseClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmpty;

    private NotificationAdapter adapter;
    private SupabaseApiService apiService;
    private String currentUserId, authToken, apiKey;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);

        // Ánh xạ View
        recyclerView = root.findViewById(R.id.recycler_notifications);
        tvEmpty = root.findViewById(R.id.tvEmpty);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initServices();
        setupRecyclerView();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tải lại dữ liệu mỗi khi quay lại màn hình này
        // (Ví dụ: Sau khi bấm vào xem, quay lại thì chấm xanh phải mất đi)
        loadNotifications();
    }

    private void initServices() {
        if (getContext() == null) return;

        apiService = RetrofitClient.getApiService();
        SharedPrefManager prefs = SharedPrefManager.getInstance(getContext());

        currentUserId = prefs.getUserId();
        authToken = "Bearer " + prefs.getToken();
        apiKey = SupabaseClient.ANON_KEY;
    }

    private void setupRecyclerView() {
        // Truyền "this::onNotificationClick" làm listener xử lý sự kiện bấm
        adapter = new NotificationAdapter(getContext(), this::onNotificationClick);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Gọi API lấy danh sách thông báo
     */
    private void loadNotifications() {
        if (currentUserId == null) return;

        // Join bảng users để lấy thông tin người gửi (actor)
        String select = "*,actor:users!actor_id(full_name,avatar_url)";

        // Gọi API getNotifications (hàm này KHÔNG lọc is_read, lấy tất cả để xem lịch sử)
        apiService.getNotifications(
                authToken,
                apiKey,
                "eq." + currentUserId, // <--- QUAN TRỌNG: Phải có "eq."
                select,
                "created_at.desc"      // Sắp xếp mới nhất lên đầu
        ).enqueue(new Callback<List<AppNotification>>() {
            @Override
            public void onResponse(Call<List<AppNotification>> call, Response<List<AppNotification>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AppNotification> list = response.body();

                    // Cập nhật Adapter
                    adapter.setList(list);

                    // Ẩn/Hiện Empty State
                    if (list.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                } else {
                    Log.e("NotificationsFragment", "Lỗi API: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<List<AppNotification>> call, Throwable t) {
                Log.e("NotificationsFragment", "Lỗi mạng: " + t.getMessage());
            }
        });
    }

    /**
     * Xử lý khi người dùng bấm vào một dòng thông báo
     */
    private void onNotificationClick(AppNotification notif) {
        // 1. Đánh dấu là đã đọc (nếu nó đang chưa đọc)
        if (!notif.isRead()) {
            markAsRead(notif.getNotificationId());
        }

        // 2. Điều hướng dựa trên loại thông báo
        Intent intent = null;

        if ("friend_request".equals(notif.getType())) {
            // Nếu là lời mời -> Mở màn hình Xem tất cả lời mời
            intent = new Intent(getContext(), SeeAllRequestActivity.class);

        } else if ("friend_accepted".equals(notif.getType())) {
            // Nếu được chấp nhận -> Mở trang cá nhân người đó
            intent = new Intent(getContext(), ProfileActivity.class);
            intent.putExtra("USER_ID", notif.getActorId());
        }

        if (intent != null) {
            startActivity(intent);
        }
    }

    /**
     * Gọi API cập nhật is_read = true
     */
    private void markAsRead(String notifId) {
        Map<String, Boolean> body = new HashMap<>();
        body.put("is_read", true);

        apiService.markNotificationAsRead(authToken, apiKey, "eq." + notifId, body)
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> c, Response<Void> r) {
                        // Khi đánh dấu thành công, tải lại danh sách để mất chấm xanh
                        loadNotifications();
                    }
                    @Override public void onFailure(Call<Void> c, Throwable t) {}
                });
    }
}