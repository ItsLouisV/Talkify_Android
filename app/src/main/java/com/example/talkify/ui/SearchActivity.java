package com.example.talkify.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.talkify.R;
import com.example.talkify.adapters.SearchUserAdapter;
import com.example.talkify.models.User;
import com.example.talkify.services.SharedPrefManager;
import com.example.talkify.services.SupabaseApiService;
import com.example.talkify.services.SupabaseClient;
import com.example.talkify.utils.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private ImageView ivBack;
    private RecyclerView recyclerView;
    private TextView tvEmptyState;

    private SearchUserAdapter adapter;
    private SupabaseApiService apiService;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initServices();
        initViews();
        setupSearch();
    }

    private void initServices() {
        apiService = RetrofitClient.getApiService();
        currentUserId = SharedPrefManager.getInstance(this).getUserId();
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        ivBack = findViewById(R.id.ivBack);
        recyclerView = findViewById(R.id.recyclerViewSearch);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        ivBack.setOnClickListener(v -> finish());

        // Gán listener là hàm onUserClicked bên dưới
        adapter = new SearchUserAdapter(this, this::onUserClicked);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchRunnable = () -> performSearch(s.toString());
                handler.postDelayed(searchRunnable, 500); // Debounce 500ms
            }
        });
    }

    private void performSearch(String query) {
        if (query.trim().isEmpty()) {
            adapter.setList(new ArrayList<>());
            return;
        }

        String authToken = "Bearer " + SharedPrefManager.getInstance(this).getToken();
        String searchQuery = "(full_name.ilike.*" + query + "*,email.ilike.*" + query + "*)";

        apiService.searchUsers(authToken, SupabaseClient.ANON_KEY, searchQuery)
                .enqueue(new Callback<List<User>>() {
                    @Override
                    public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<User> results = response.body();
                            // Loại bỏ bản thân khỏi kết quả tìm kiếm
                            results.removeIf(u -> u.getUserId().equals(currentUserId));

                            adapter.setList(results);

                            if (results.isEmpty()) {
                                tvEmptyState.setVisibility(View.VISIBLE);
                                tvEmptyState.setText("Không tìm thấy người dùng");
                            } else {
                                tvEmptyState.setVisibility(View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<List<User>> call, Throwable t) {
                        Toast.makeText(SearchActivity.this, "Lỗi tìm kiếm", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * HÀM DUY NHẤT XỬ LÝ CLICK
     * (Đã gộp logic của navigateToChatDetail và openChatWithUser vào đây cho gọn)
     */
    private void onUserClicked(User targetUser) {
        // 1. Hiệu ứng UX
        Toast.makeText(this, "Đang kết nối...", Toast.LENGTH_SHORT).show();

        // 2. Chuẩn bị gọi API
        String authToken = "Bearer " + SharedPrefManager.getInstance(this).getToken();
        Map<String, String> body = new HashMap<>();
        body.put("target_user_id", targetUser.getUserId());

        // 3. Gọi RPC lấy/tạo conversation
        apiService.getOrCreateConversation(authToken, SupabaseClient.ANON_KEY, body)
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String conversationId = response.body();

                            // 4. Chuyển sang màn hình Chat
                            Intent intent = new Intent(SearchActivity.this, ChatDetailActivity.class);

                            // Put ID và Tên (quan trọng để ChatDetail hiển thị đúng)
                            intent.putExtra("CONVERSATION_ID", conversationId);
                            intent.putExtra("CONVERSATION_NAME", targetUser.getFullName());
                            // intent.putExtra("CONVERSATION_AVATAR", targetUser.getAvatarUrl());

                            startActivity(intent);
                            finish(); // Đóng màn hình tìm kiếm để quay lại không bị rối
                        } else {
                            Toast.makeText(SearchActivity.this, "Không thể tạo cuộc trò chuyện", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                        Toast.makeText(SearchActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}