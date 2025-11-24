package com.example.talkify.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.talkify.R;
import com.example.talkify.adapters.FriendRequestAdapter;
import com.example.talkify.models.FriendRequest;
import com.example.talkify.services.SharedPrefManager;
import com.example.talkify.services.SupabaseApiService;
import com.example.talkify.services.SupabaseClient;
import com.example.talkify.utils.RetrofitClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SeeAllRequestActivity extends AppCompatActivity implements FriendRequestAdapter.OnRequestActionListener {

    private ImageView ivBack;
    private RecyclerView recyclerView;
    private TextView tvEmpty;

    private SupabaseApiService apiService;
    private FriendRequestAdapter adapter;
    private String currentUserId, authToken, apiKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_all_request);

        initServices();
        initViews();
        loadFriendRequests();
    }

    private void initServices() {
        apiService = RetrofitClient.getApiService();
        SharedPrefManager prefs = SharedPrefManager.getInstance(this);
        currentUserId = prefs.getUserId();
        authToken = "Bearer " + prefs.getToken();
        apiKey = SupabaseClient.ANON_KEY;
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        recyclerView = findViewById(R.id.recycler_view_requests);
        tvEmpty = findViewById(R.id.tvEmpty);

        ivBack.setOnClickListener(v -> finish());

        // Tái sử dụng FriendRequestAdapter
        adapter = new FriendRequestAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadFriendRequests() {
        // Lấy danh sách lời mời (status = pending)
        String selectQuery = "*,sender:users!sender_id(user_id,full_name,user_name,avatar_url)";

        apiService.getFriendRequests(authToken, apiKey, "eq." + currentUserId, "eq.pending", selectQuery)
                .enqueue(new Callback<List<FriendRequest>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<FriendRequest>> call, @NonNull Response<List<FriendRequest>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<FriendRequest> list = response.body();
                            adapter.submitList(list);

                            if (list.isEmpty()) {
                                tvEmpty.setVisibility(View.VISIBLE);
                            } else {
                                tvEmpty.setVisibility(View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<FriendRequest>> call, @NonNull Throwable t) {
                        Toast.makeText(SeeAllRequestActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ==================================================
    // XỬ LÝ HÀNH ĐỘNG TỪ ADAPTER
    // ==================================================

    @Override
    public void onAcceptClick(FriendRequest request) {
        Map<String, String> body = new HashMap<>();
        body.put("status", "accepted");
        body.put("responded_at", "now()");

        apiService.acceptFriendRequest(authToken, apiKey, "eq." + request.getRequestId(), body)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(SeeAllRequestActivity.this, "Đã chấp nhận", Toast.LENGTH_SHORT).show();

                            // --- GỬI THÔNG BÁO CHO NGƯỜI GỬI ---
                            if (request.getSender() != null) {
                                pushNotificationToDatabase(
                                        request.getSender().getUserId(),
                                        "friend_accepted",
                                        "đã chấp nhận lời mời kết bạn."
                                );
                            }
                            // ------------------------------------

                            loadFriendRequests(); // Tải lại danh sách để xóa item
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        Toast.makeText(SeeAllRequestActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDeclineClick(FriendRequest request) {
        apiService.deleteFriendRequest(authToken, apiKey, "eq." + request.getRequestId())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        Toast.makeText(SeeAllRequestActivity.this, "Đã xóa lời mời", Toast.LENGTH_SHORT).show();
                        loadFriendRequests(); // Tải lại
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        Toast.makeText(SeeAllRequestActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onAvatarClick(FriendRequest request) {
        if (request.getSender() != null) {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("USER_ID", request.getSender().getUserId());
            startActivity(intent);
        }
    }

    /**
     * Hàm tạo thông báo (Giống hệt bên UsersFragment)
     */
    private void pushNotificationToDatabase(String receiverId, String type, String contentText) {
        Map<String, Object> notif = new HashMap<>();
        notif.put("user_id", receiverId);      // Người nhận (A)
        notif.put("actor_id", currentUserId);  // Người thực hiện (Mình - B)
        notif.put("type", type);               // friend_accepted
        notif.put("content", contentText);

        apiService.createNotification(authToken, apiKey, notif)
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> c, Response<Void> r) {}
                    @Override public void onFailure(Call<Void> c, Throwable t) {}
                });
    }
}