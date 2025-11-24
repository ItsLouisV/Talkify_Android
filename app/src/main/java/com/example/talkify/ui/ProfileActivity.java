package com.example.talkify.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.talkify.R;
import com.example.talkify.models.RelationshipResponse;
import com.example.talkify.models.User;
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

public class ProfileActivity extends AppCompatActivity {

    // --- Views ---
    private ImageView ivBack, ivAvatar;
    private TextView tvFullName, tvFriendCount, tvUsername;
    private LinearLayout layoutStateNew, layoutStateSent, layoutStateReceived, layoutStateFriends, layoutStateSelf;

    // --- Nút bấm ---
    private Button btnAddNewFriend, btnCancelSent, btnAccept, btnDecline, btnFriends, btnMessage, btnEditProfile;

    // --- Services ---
    private SupabaseApiService apiService;
    private SharedPrefManager sharedPrefManager;
    private String authToken, apiKey;

    // --- State (Trạng thái) ---
    private String viewedUserId; // ID của người đang xem
    private String currentUserId; // ID của chính mình
    private String pendingRequestId; // ID lời mời (để hủy/chấp nhận)
    private User viewedUser; // Thông tin người đang xem

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 1. Lấy ID user từ Intent
        viewedUserId = getIntent().getStringExtra("USER_ID");
        if (viewedUserId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Khởi tạo
        initViews();
        initServices();

        // 3. Gán sự kiện
        addClickListeners();

        // 4. Tải dữ liệu
        loadUserProfile();
        loadRelationshipStatus();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        ivAvatar = findViewById(R.id.ivAvatar);
        tvFullName = findViewById(R.id.tvFullName);
        tvFriendCount = findViewById(R.id.tvFriendCount);
        tvUsername = findViewById(R.id.tvUsername);

        layoutStateNew = findViewById(R.id.layoutStateNew);
        layoutStateSent = findViewById(R.id.layoutStateSent);
        layoutStateReceived = findViewById(R.id.layoutStateReceived);
        layoutStateFriends = findViewById(R.id.layoutStateFriends);
        layoutStateSelf = findViewById(R.id.layoutStateSelf);

        btnAddNewFriend = findViewById(R.id.btnAddNewFriend);
        btnCancelSent = findViewById(R.id.btnCancelSent);
        btnAccept = findViewById(R.id.btnAccept);
        btnDecline = findViewById(R.id.btnDecline);
        btnFriends = findViewById(R.id.btnFriends);
        btnMessage = findViewById(R.id.btnMessage);
        btnEditProfile = findViewById(R.id.btnEditProfile);
    }

    private void initServices() {
        apiService = RetrofitClient.getApiService();
        sharedPrefManager = SharedPrefManager.getInstance(this);
        currentUserId = sharedPrefManager.getUserId();
        authToken = "Bearer " + sharedPrefManager.getToken();
        apiKey = SupabaseClient.ANON_KEY;
    }

    private void loadUserProfile() {
        apiService.getUserDetails(authToken, apiKey, "eq." + viewedUserId, "*")
                .enqueue(new Callback<List<User>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            viewedUser = response.body().get(0);
                            tvFullName.setText(viewedUser.getFullName());
                            tvUsername.setText("@" + viewedUser.getUserName());
                            Glide.with(ProfileActivity.this)
                                    .load(viewedUser.getAvatarUrl())
                                    .placeholder(R.drawable.avatar_placeholder)
                                    .into(ivAvatar);
                            tvFriendCount.setText("Talkify User");
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                        Toast.makeText(ProfileActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadRelationshipStatus() {
        if (viewedUserId.equals(currentUserId)) {
            showButtonState("self");
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("profile_user_id", viewedUserId);

        apiService.getRelationshipDetails(authToken, apiKey, body).enqueue(new Callback<RelationshipResponse>() {
            @Override
            public void onResponse(@NonNull Call<RelationshipResponse> call, @NonNull Response<RelationshipResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RelationshipResponse rel = response.body();
                    pendingRequestId = rel.getRequestId();
                    showButtonState(rel.getStatus());
                } else {
                    showButtonState("new");
                }
            }
            @Override
            public void onFailure(@NonNull Call<RelationshipResponse> call, @NonNull Throwable t) {
                showButtonState("new");
            }
        });
    }

    private void showButtonState(String status) {
        layoutStateNew.setVisibility(View.GONE);
        layoutStateSent.setVisibility(View.GONE);
        layoutStateReceived.setVisibility(View.GONE);
        layoutStateFriends.setVisibility(View.GONE);
        layoutStateSelf.setVisibility(View.GONE);

        switch (status) {
            case "sent": layoutStateSent.setVisibility(View.VISIBLE); break;
            case "received": layoutStateReceived.setVisibility(View.VISIBLE); break;
            case "friends": layoutStateFriends.setVisibility(View.VISIBLE); break;
            case "self": layoutStateSelf.setVisibility(View.VISIBLE); break;
            default: layoutStateNew.setVisibility(View.VISIBLE);
        }
    }

    private void addClickListeners() {
        ivBack.setOnClickListener(v -> onBackPressed());

        // 1. Kết bạn
        btnAddNewFriend.setOnClickListener(v -> handleSendRequest());

        // 2. Hủy lời mời đã gửi
        btnCancelSent.setOnClickListener(v -> handleCancelRequest());

        // 3. Chấp nhận / Từ chối
        btnAccept.setOnClickListener(v -> handleAcceptRequest());
        btnDecline.setOnClickListener(v -> handleDeclineRequest());

        // 4. Khi bấm nút Nhắn tin
        btnMessage.setOnClickListener(v -> {
            // viewedUser là biến chứa thông tin người đang xem profile
            if (viewedUser != null) {
                openChatWithUser(viewedUser);
            }
        });

        btnFriends.setOnClickListener(v -> Toast.makeText(this, "Đã là bạn bè", Toast.LENGTH_SHORT).show());
        btnEditProfile.setOnClickListener(v -> Toast.makeText(this, "Chức năng Edit đang phát triển", Toast.LENGTH_SHORT).show());
    }

    // ================= LOGIC API =================

    private void handleSendRequest() {
        btnAddNewFriend.setEnabled(false);
        Map<String, Object> body = new HashMap<>();
        body.put("sender_id", currentUserId);
        body.put("receiver_id", viewedUserId);

        apiService.sendFriendRequest(authToken, apiKey, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    loadRelationshipStatus();
                    // Gửi thông báo
                    pushNotificationToDatabase(viewedUserId, "friend_request", "đã gửi cho bạn lời mời kết bạn.");
                } else {
                    btnAddNewFriend.setEnabled(true);
                }
            }
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                btnAddNewFriend.setEnabled(true);
            }
        });
    }

    private void handleCancelRequest() {
        btnCancelSent.setEnabled(false);
        apiService.cancelFriendRequest(authToken, apiKey, "eq." + currentUserId, "eq." + viewedUserId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        loadRelationshipStatus();
                    }
                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        btnCancelSent.setEnabled(true);
                    }
                });
    }

    private void handleAcceptRequest() {
        if (pendingRequestId == null) return;
        btnAccept.setEnabled(false);

        Map<String, String> body = new HashMap<>();
        body.put("status", "accepted");
        body.put("responded_at", "now()");

        apiService.acceptFriendRequest(authToken, apiKey, "eq." + pendingRequestId, body)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (response.isSuccessful()) {
                            loadRelationshipStatus();
                            // Gửi thông báo
                            pushNotificationToDatabase(viewedUserId, "friend_accepted", "đã chấp nhận lời mời kết bạn.");
                        } else {
                            btnAccept.setEnabled(true);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        btnAccept.setEnabled(true);
                    }
                });
    }

    private void handleDeclineRequest() {
        if (pendingRequestId == null) return;
        apiService.deleteFriendRequest(authToken, apiKey, "eq." + pendingRequestId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        loadRelationshipStatus();
                    }
                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {}
                });
    }

    /**
     * SỬA LỖI CRASH: Gọi RPC lấy ID cuộc trò chuyện trước khi chuyển màn hình
     */
    private void handleMessageClick() {
        if (viewedUser == null) return;

        btnMessage.setEnabled(false);
        Toast.makeText(this, "Đang mở...", Toast.LENGTH_SHORT).show();

        Map<String, String> body = new HashMap<>();
        body.put("target_user_id", viewedUserId);

        // Gọi hàm RPC get_or_create_conversation (đã tạo ở bước Search)
        apiService.getOrCreateConversation(authToken, apiKey, body).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                btnMessage.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    String conversationId = response.body();

                    Intent intent = new Intent(ProfileActivity.this, ChatDetailActivity.class);
                    intent.putExtra("CONVERSATION_ID", conversationId); // <-- ID quan trọng
                    intent.putExtra("CONVERSATION_NAME", viewedUser.getFullName());
                    startActivity(intent);
                } else {
                    Toast.makeText(ProfileActivity.this, "Lỗi tạo hội thoại", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                btnMessage.setEnabled(true);
                Toast.makeText(ProfileActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pushNotificationToDatabase(String receiverId, String type, String contentText) {
        Map<String, Object> notif = new HashMap<>();
        notif.put("user_id", receiverId);
        notif.put("actor_id", currentUserId);
        notif.put("type", type);
        notif.put("content", contentText);

        apiService.createNotification(authToken, apiKey, notif).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> c, Response<Void> r) {}
            @Override public void onFailure(Call<Void> c, Throwable t) {}
        });
    }

    /**
     * Hàm xử lý logic: Tìm chat cũ hoặc Tạo chat mới
     * @param targetUser Người mà mình muốn chat
     */
    private void openChatWithUser(User targetUser) {
        // 1. Hiện loading để người dùng biết đang xử lý
        Toast.makeText(this, "Đang kết nối...", Toast.LENGTH_SHORT).show();
        // Nếu có nút bấm, hãy disable nó: btnMessage.setEnabled(false);

        // 2. Chuẩn bị dữ liệu gọi API
        String currentUserId = SharedPrefManager.getInstance(this).getUserId();
        String token = "Bearer " + SharedPrefManager.getInstance(this).getToken();

        Map<String, String> body = new HashMap<>();
        body.put("target_user_id", targetUser.getUserId());

        // 3. Gọi hàm RPC thần thánh chúng ta vừa tạo
        RetrofitClient.getApiService().getOrCreateConversation(token, SupabaseClient.ANON_KEY, body)
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        // btnMessage.setEnabled(true); // Mở lại nút bấm

                        if (response.isSuccessful() && response.body() != null) {
                            String conversationId = response.body();

                            // 4. Đã có ID (dù cũ hay mới), chuyển màn hình ngay
                            Intent intent = new Intent(getApplicationContext(), ChatDetailActivity.class);

                            // Gửi ID cuộc trò chuyện (QUAN TRỌNG)
                            intent.putExtra("CONVERSATION_ID", conversationId);

                            // Gửi Tên & Avatar người kia để ChatDetail hiển thị ngay (không bị tên rỗng)
                            intent.putExtra("CONVERSATION_NAME", targetUser.getFullName());
                            // intent.putExtra("CONVERSATION_AVATAR", targetUser.getAvatarUrl());

                            startActivity(intent);
                        } else {
                            Toast.makeText(getApplicationContext(), "Lỗi tạo cuộc trò chuyện", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        // btnMessage.setEnabled(true);
                        Toast.makeText(getApplicationContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}