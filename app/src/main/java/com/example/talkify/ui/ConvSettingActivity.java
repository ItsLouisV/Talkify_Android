package com.example.talkify.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.talkify.R;
import com.example.talkify.models.Conversation;
import com.example.talkify.services.SharedPrefManager;
import com.example.talkify.services.SupabaseApiService;
import com.example.talkify.services.SupabaseClient;
import com.example.talkify.utils.RetrofitClient;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConvSettingActivity extends AppCompatActivity {

    // --- Views ---
    private ImageView ivBack;
    private ShapeableImageView ivConversationAvatar;
    private TextView tvConversationName, tvSubInfo;

    // Header Actions
    private ImageView btnCall, btnVideo, btnProfile_or_Addmember;

    // Layout Containers (Để ẩn hiện toàn bộ khối)
    private LinearLayout layoutOneOnOneActions, layoutGroupActions;
    private LinearLayout layoutDestructive1on1, layoutDestructiveGroup;

    // Individual Actions (Các dòng bấm được)
    private LinearLayout rowMedia;
    private LinearLayout btnViewMembers, btnLeaveGroup, btnDisbandGroup;
    private LinearLayout btnBlock, btnDeleteHistory, btnDeleteChat;

    // Switches
    private SwitchMaterial switchMute, switchPin, switchCloseFriend;

    // --- Services & Data ---
    private SupabaseApiService apiService;
    private String authToken, apiKey, currentUserId;
    private String conversationId;
    private boolean isGroupFromIntent;
    private Conversation currentConversation;

    private String targetUserId = null; // Lưu ID người kia để dùng cho tính năng bạn thân

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conv_setting); // Đảm bảo tên file XML đúng

        // 1. Nhận dữ liệu
        if (getIntent() != null) {
            conversationId = getIntent().getStringExtra("CONVERSATION_ID");
            isGroupFromIntent = getIntent().getBooleanExtra("IS_GROUP", false);
        }

        if (conversationId == null) {
            Toast.makeText(this, "Lỗi ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Khởi tạo
        initServices();
        initViews();

        // 3. Setup giao diện ban đầu (Icon & Layout)
        setupInitialUI();

        checkPinStatus(); // Kiểm tra trạng thái ghim ngay khi mở màn hình

        // 4. Gán sự kiện Click
        setupClickListeners();

        // 5. Tải dữ liệu
        loadConversationData();
    }

    private void initServices() {
        apiService = RetrofitClient.getApiService();
        SharedPrefManager prefs = SharedPrefManager.getInstance(this);
        currentUserId = prefs.getUserId();
        authToken = "Bearer " + prefs.getToken();
        apiKey = SupabaseClient.ANON_KEY;
    }

    private void initViews() {
        // Toolbar & Header
        ivBack = findViewById(R.id.ivBack);
        ivConversationAvatar = findViewById(R.id.ivConversationAvatar);
        tvConversationName = findViewById(R.id.tvConversationName);
        tvSubInfo = findViewById(R.id.tvSubInfo);

        // Header Buttons
        btnCall = findViewById(R.id.btnCall);
        btnVideo = findViewById(R.id.btnVideo);
        btnProfile_or_Addmember = findViewById(R.id.btnProfile_or_Addmember);

        // Containers
        layoutOneOnOneActions = findViewById(R.id.layoutOneOnOneActions);
        layoutGroupActions = findViewById(R.id.layoutGroupActions);
        layoutDestructive1on1 = findViewById(R.id.layoutDestructive1on1);
        layoutDestructiveGroup = findViewById(R.id.layoutDestructiveGroup);

        // Switches
        switchMute = findViewById(R.id.switchMute);
        switchPin = findViewById(R.id.switchPin);
        switchCloseFriend = findViewById(R.id.switchCloseFriend);

        // Action Rows
        rowMedia = findViewById(R.id.rowMedia);

        // Group Buttons
        btnViewMembers = findViewById(R.id.btnViewMembers); // Nút xem thành viên
        btnLeaveGroup = findViewById(R.id.btnLeaveGroup);
        btnDisbandGroup = findViewById(R.id.btnDisbandGroup);

        // 1-1 Buttons
        btnBlock = findViewById(R.id.btnBlock);
        btnDeleteHistory = findViewById(R.id.btnDeleteHistory);
        btnDeleteChat = findViewById(R.id.btnDeleteChat);
    }

    /**
     * Kiểm tra xem user hiện tại đã ghim cuộc trò chuyện này chưa
     */
    private void checkPinStatus() {
        // Tạm khóa switch để người dùng không bấm loạn xạ khi đang load
        switchPin.setEnabled(false);

        apiService.checkPinnedStatus(
                authToken,
                apiKey,
                "eq." + currentUserId,
                "eq." + conversationId
        ).enqueue(new Callback<List<Object>>() {
            @Override
            public void onResponse(@NonNull Call<List<Object>> call, @NonNull Response<List<Object>> response) {
                switchPin.setEnabled(true); // Mở lại switch

                if (response.isSuccessful() && response.body() != null) {
                    // Nếu list trả về > 0, nghĩa là đã ghim
                    boolean isPinned = !response.body().isEmpty();

                    // Set trạng thái mà KHÔNG kích hoạt listener
                    switchPin.setOnCheckedChangeListener(null);
                    switchPin.setChecked(isPinned);
                    setupPinSwitchListener(); // Gán lại listener sau khi set xong
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Object>> call, @NonNull Throwable t) {
                switchPin.setEnabled(true);
            }
        });
    }

    /**
     * Gán sự kiện lắng nghe khi bấm nút Switch
     */
    private void setupPinSwitchListener() {
        switchPin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                performPin();
            } else {
                performUnpin();
            }
        });
    }

    private void performPin() {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("user_id", currentUserId);
        body.put("conversation_id", conversationId);

        apiService.pinConversation(authToken, apiKey, body)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(ConvSettingActivity.this, "Đã ghim trò chuyện", Toast.LENGTH_SHORT).show();
                        } else {
                            // Nếu lỗi thì bật lại switch về cũ
                            switchPin.setChecked(false);
                            Toast.makeText(ConvSettingActivity.this, "Lỗi ghim", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        switchPin.setChecked(false);
                        Toast.makeText(ConvSettingActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void performUnpin() {
        apiService.unpinConversation(
                authToken,
                apiKey,
                "eq." + currentUserId,
                "eq." + conversationId
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ConvSettingActivity.this, "Đã bỏ ghim", Toast.LENGTH_SHORT).show();
                } else {
                    switchPin.setChecked(true); // Lỗi thì bật lại
                    Toast.makeText(ConvSettingActivity.this, "Lỗi bỏ ghim", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                switchPin.setChecked(true);
                Toast.makeText(ConvSettingActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Thiết lập giao diện dựa trên loại hội thoại (1-1 hoặc Group)
     */
    private void setupInitialUI() {
        if (isGroupFromIntent) {
            // --- GIAO DIỆN NHÓM ---
            // Hiện các khối của nhóm
            layoutGroupActions.setVisibility(View.VISIBLE);
            layoutDestructiveGroup.setVisibility(View.VISIBLE);

            // Ẩn các khối của 1-1
            layoutOneOnOneActions.setVisibility(View.GONE);
            layoutDestructive1on1.setVisibility(View.GONE);

            tvSubInfo.setText("Đang tải thành viên...");

            // LOGIC ICON NÚT THỨ 3: Đổi thành "Thêm thành viên"
            btnProfile_or_Addmember.setImageResource(R.drawable.ic_group_add);

            // Mặc định ẩn nút giải tán nhóm (Sẽ hiện lại nếu check thấy là Admin)
            btnDisbandGroup.setVisibility(View.GONE);

        } else {
            // --- GIAO DIỆN 1-1 ---
            // Ẩn các khối của nhóm
            layoutGroupActions.setVisibility(View.GONE);
            layoutDestructiveGroup.setVisibility(View.GONE);

            // Hiện các khối của 1-1
            layoutOneOnOneActions.setVisibility(View.VISIBLE);
            layoutDestructive1on1.setVisibility(View.VISIBLE);

            tvSubInfo.setText("Thông tin cá nhân");

            // LOGIC ICON NÚT THỨ 3: Đổi thành "Xem Profile"
            btnProfile_or_Addmember.setImageResource(R.drawable.ic_user);
        }
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        // Xử lý nút thay đổi chức năng (Profile / Add Member)
        btnProfile_or_Addmember.setOnClickListener(v -> {
            if (isGroupFromIntent) {
                // Logic thêm thành viên
                Toast.makeText(this, "Mở màn hình Thêm thành viên", Toast.LENGTH_SHORT).show();
            } else {
                // Logic xem profile
                Toast.makeText(this, "Xem trang cá nhân", Toast.LENGTH_SHORT).show();
            }
        });

        // Rời nhóm
        btnLeaveGroup.setOnClickListener(v -> showConfirmationDialog(
                "Rời nhóm",
                "Bạn có chắc chắn muốn rời khỏi nhóm này? Bạn sẽ không nhận được tin nhắn nữa.",
                this::performLeaveGroup
        ));

        // Chặn (1-1)
        btnBlock.setOnClickListener(v -> showConfirmationDialog(
                "Chặn người dùng",
                "Bạn có chắc chắn muốn chặn người này?",
                () -> Toast.makeText(this, "Đã chặn", Toast.LENGTH_SHORT).show()
        ));

        // Xóa tin nhắn phía mình
        btnDeleteHistory.setOnClickListener(v -> showConfirmationDialog(
                "Xóa lịch sử",
                "Bạn có chắc muốn xóa toàn bộ tin nhắn phía bạn? Hành động này không thể hoàn tác.",
                this::performClearHistory
        ));

        // Các nút khác
        btnViewMembers.setOnClickListener(v -> Toast.makeText(this, "Xem danh sách thành viên", Toast.LENGTH_SHORT).show());
    }

    private void loadConversationData() {
        // Query quan trọng: Phải lấy role trong participants để check Admin
        String query = "conversation_id,type," +
                "group_details(name,avatar_url)," +
                "conversation_participants(user_id,role,users(full_name,avatar_url,user_name))";

        apiService.getConversationDetails(authToken, apiKey, "eq." + conversationId, query, 1)
                .enqueue(new Callback<List<Conversation>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Conversation>> call, @NonNull Response<List<Conversation>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            currentConversation = response.body().get(0);
                            updateFullUI();
                        } else {
                            try {
                                // In chi tiết lỗi ra Logcat để debug
                                Log.e("ConvSetting", "Lỗi API " + response.code() + ": " + response.errorBody().string());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Conversation>> call, @NonNull Throwable t) {
                        Log.e("ConvSetting", "Lỗi mạng: " + t.getMessage());
                    }
                });
    }

    private void updateFullUI() {
        if (currentConversation == null) return;

        // Set hiển thị tên & avatar
        String name = currentConversation.getDisplayName(currentUserId);
        String avatar = currentConversation.getDisplayAvatar(currentUserId);

        tvConversationName.setText(name);
        Glide.with(this)
                .load(avatar)
                .placeholder(currentConversation.isGroup() ? R.drawable.avt_group : R.drawable.ic_user)
                .circleCrop()
                .into(ivConversationAvatar);

        // Logic riêng cho nhóm
        if (currentConversation.isGroup()) {
            // Trường hộp là nhóm: Đếm số thành viên
            int count = 0;
            if (currentConversation.getParticipants() != null) {
                count = currentConversation.getParticipants().size();
            }
            tvSubInfo.setText(count + " thành viên");

            // Gọi hàm check Admin
            checkIfAdmin();
        } else {
            // Trường hợp chat 1-1: Hiển thị Username
            String usernameDisplay = ""; // Mặc định để trống

            if (currentConversation.getParticipants() != null) {
                for (Conversation.ParticipantWrapper p : currentConversation.getParticipants()) {
                    // Tìm người kia (không phải mình)
                    if (!p.getUserId().equals(currentUserId)) {
                        Conversation.User otherUser = p.getUser();
                        if (otherUser != null && otherUser.getUsername() != null && !otherUser.getUsername().isEmpty()) {
                            // Nếu có username thì thêm @ đằng trước
                            usernameDisplay = "@" + otherUser.getUsername();
                        }
                        break;
                    }
                }
            }
            tvSubInfo.setText(usernameDisplay);
        }

        // Nếu là Chat 1-1, cần tìm ID của người kia để xử lý "Bạn thân"
        if (!currentConversation.isGroup()) {
            findTargetUserId(); // Tìm ID người kia

            if (targetUserId != null) {
                checkCloseFriendStatus(); // Kiểm tra xem đã là bạn thân chưa
            }
        }
    }

    /**
     * Hàm tìm ID người đối diện trong chat 1-1
     */
    private void findTargetUserId() {
        if (currentConversation.getParticipants() != null) {
            for (Conversation.ParticipantWrapper p : currentConversation.getParticipants()) {
                if (!p.getUserId().equals(currentUserId)) {
                    targetUserId = p.getUserId();
                    break;
                }
            }
        }
    }

    /**
     * Kiểm tra trạng thái từ Server để update UI Switch
     */
    private void checkCloseFriendStatus() {
        switchCloseFriend.setEnabled(false); // Khóa tạm

        apiService.checkCloseFriend(
                authToken, apiKey,
                "eq." + currentUserId,
                "eq." + targetUserId
        ).enqueue(new Callback<List<Object>>() {
            @Override
            public void onResponse(Call<List<Object>> call, Response<List<Object>> response) {
                switchCloseFriend.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    boolean isClose = !response.body().isEmpty();

                    // Set trạng thái (bỏ listener cũ để tránh loop)
                    switchCloseFriend.setOnCheckedChangeListener(null);
                    switchCloseFriend.setChecked(isClose);

                    // Gán sự kiện click
                    setupCloseFriendListener();
                }
            }
            @Override
            public void onFailure(Call<List<Object>> call, Throwable t) {
                switchCloseFriend.setEnabled(true);
            }
        });
    }

    /**
     * Xử lý sự kiện bật/tắt Switch
     */
    private void setupCloseFriendListener() {
        switchCloseFriend.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (targetUserId == null) return;

            if (isChecked) {
                // Thêm bạn thân
                Map<String, Object> body = new java.util.HashMap<>();
                body.put("user_id", currentUserId);
                body.put("friend_id", targetUserId);

                apiService.addCloseFriend(authToken, apiKey, body).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> c, Response<Void> r) {
                        if (!r.isSuccessful()) {
                            switchCloseFriend.setChecked(false); // Revert nếu lỗi
                            Toast.makeText(ConvSettingActivity.this, "Lỗi thêm bạn thân", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ConvSettingActivity.this, "Đã thêm vào danh sách bạn thân", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<Void> c, Throwable t) {
                        switchCloseFriend.setChecked(false);
                    }
                });
            } else {
                // Xóa bạn thân
                apiService.removeCloseFriend(authToken, apiKey, "eq." + currentUserId, "eq." + targetUserId)
                        .enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> c, Response<Void> r) {
                                if (!r.isSuccessful()) switchCloseFriend.setChecked(true);
                                else Toast.makeText(ConvSettingActivity.this, "Đã bỏ đánh dấu bạn thân", Toast.LENGTH_SHORT).show();
                            }
                            @Override
                            public void onFailure(Call<Void> c, Throwable t) {
                                switchCloseFriend.setChecked(true);
                            }
                        });
            }
        });
    }

    /**
     * Kiểm tra xem User hiện tại có phải Admin không
     */
    private void checkIfAdmin() {
        boolean isAdmin = false;
        List<Conversation.ParticipantWrapper> participants = currentConversation.getParticipants();

        if (participants != null) {
            for (Conversation.ParticipantWrapper p : participants) {
                if (p.getUserId().equals(currentUserId)) {
                    // Kiểm tra role (Cần đảm bảo Model Conversation có trường 'role')
                    String role = p.getRole();
                    if ("admin".equals(role) || "owner".equals(role)) {
                        isAdmin = true;
                    }
                    break;
                }
            }
        }

        // Chỉ hiện nút giải tán nếu là Admin
        if (isAdmin) {
            btnDisbandGroup.setVisibility(View.VISIBLE);
            btnDisbandGroup.setOnClickListener(v -> showConfirmationDialog(
                    "Giải tán nhóm",
                    "Hành động này sẽ xóa nhóm vĩnh viễn và không thể hoàn tác.",
                    this::performDisbandGroup
            ));
        } else {
            btnDisbandGroup.setVisibility(View.GONE);
        }
    }

    // --- Dialog & Actions ---

    private void showConfirmationDialog(String title, String message, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Đồng ý", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performLeaveGroup() {
        // Khóa nút để tránh bấm nhiều lần
        btnLeaveGroup.setEnabled(false);
        Toast.makeText(this, "Đang rời nhóm...", Toast.LENGTH_SHORT).show();

        // Gọi API DELETE participant
        apiService.leaveGroup(
                authToken,
                apiKey,
                "eq." + conversationId,
                "eq." + currentUserId
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ConvSettingActivity.this, "Đã rời nhóm thành công", Toast.LENGTH_SHORT).show();
                    navigateHome(); // Về trang chủ ngay
                } else {
                    try {
                        Log.e("LeaveGroup", "Lỗi: " + response.errorBody().string());
                    } catch (Exception e) {}

                    Toast.makeText(ConvSettingActivity.this, "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
                    btnLeaveGroup.setEnabled(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                btnLeaveGroup.setEnabled(true);
                Toast.makeText(ConvSettingActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performDisbandGroup() {
        // Khóa nút
        if (btnDisbandGroup != null) btnDisbandGroup.setEnabled(false);
        Toast.makeText(this, "Đang giải tán nhóm...", Toast.LENGTH_SHORT).show();

        // Tạo Body cho RPC
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("group_id", conversationId);
        body.put("admin_id", currentUserId);

        // Gọi RPC disband_group
        apiService.disbandGroup(authToken, apiKey, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ConvSettingActivity.this, "Nhóm đã được giải tán!", Toast.LENGTH_SHORT).show();
                    navigateHome(); // Về trang chủ ngay
                } else {
                    if (btnDisbandGroup != null) btnDisbandGroup.setEnabled(true);
                    Toast.makeText(ConvSettingActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (btnDisbandGroup != null) btnDisbandGroup.setEnabled(true);
                Toast.makeText(ConvSettingActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- HÀM XỬ LÝ ---
    private void performClearHistory() {
        Toast.makeText(this, "Đang xóa lịch sử...", Toast.LENGTH_SHORT).show();

        // Lấy thời gian hiện tại chuẩn ISO 8601
        String nowIso = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                .format(new java.util.Date());

        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("last_cleared_at", nowIso);

        apiService.clearChatHistory(
                authToken,
                apiKey,
                "eq." + conversationId,
                "eq." + currentUserId,
                body
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ConvSettingActivity.this, "Đã xóa lịch sử tin nhắn", Toast.LENGTH_SHORT).show();
                    // Có thể gửi EventBus hoặc Result về để màn hình Chat xóa list tin nhắn
                } else {
                    Toast.makeText(ConvSettingActivity.this, "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ConvSettingActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateHome() {
        // Chuyển về MainActivity và XÓA HẾT các màn hình cũ (ChatDetail, Setting)
        Intent intent = new Intent(this, com.example.talkify.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}