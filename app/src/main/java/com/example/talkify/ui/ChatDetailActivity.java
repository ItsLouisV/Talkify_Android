package com.example.talkify.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.talkify.R;
import com.example.talkify.adapters.MessageAdapter;
import com.example.talkify.models.Conversation;
import com.example.talkify.models.Message;
import com.example.talkify.models.User;
import com.example.talkify.services.SharedPrefManager;
import com.example.talkify.services.SupabaseApiService;
import com.example.talkify.services.SupabaseClient;
import com.example.talkify.services.SupabaseRealtimeClient;
import com.example.talkify.utils.RetrofitClient;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatDetailActivity extends AppCompatActivity {

    // --- Views ---
    private ViewFlipper toolbarFlipper, inputViewFlipper;
    private ImageView ivBack, ivSearch, ivCall, ivMenu, ivCloseSearch, ivSearchUp, ivSearchDown;
    private ImageView ivShowAttachments, btnSend, ivHideAttachments, ivLink, ivCamera, ivGallery;
    private LinearLayout layoutInfo;
    private TextView tvName, tvInfo, tvSearchCount;
    private EditText etSearch, etMessageInput;
    private RecyclerView recyclerViewMessages;

    private MessageAdapter messageAdapter;

    // --- State ---
    private String conversationId;
    private String conversationName;
    private String currentUserId;
    private boolean isGroupChat = false;
    private Uri cameraImageUri;
    private boolean wantsToOpenCamera = false;

    // --- Services ---
    private SupabaseApiService apiService;
    private String authToken, apiKey;

    private SupabaseRealtimeClient.MessageListener messageInsertListener;

    // Lưu thông tin profile của mình (Dùng cho Optimistic Update)
    private User currentUserProfile;

    // --- Activity Launchers ---
    private ActivityResultLauncher<String[]> permissionLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        // 1. Nhận dữ liệu từ màn hình trước
        conversationId = getIntent().getStringExtra("CONVERSATION_ID");
        conversationName = getIntent().getStringExtra("CONVERSATION_NAME");
        isGroupChat = getIntent().getBooleanExtra("IS_GROUP", false);

        if (conversationId == null) {
            Toast.makeText(this, "Lỗi ID cuộc trò chuyện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Khởi tạo
        initViews();
        initServices();

        // BẮT ĐẦU KẾT NỐI REALTIME KHI VÀO CHAT
        SupabaseRealtimeClient.getInstance().connect();

        // 3. Set tên ngay lập tức
        if (conversationName != null) {
            tvName.setText(conversationName);
        } else {
            tvName.setText("Cuộc trò chuyện");
        }

        // 4. Cấu hình chức năng
        setupRecyclerView();
        registerActivityLaunchers();
        setupClickListeners();

        // 5. Tải dữ liệu và Realtime
        loadCurrentUserProfile();
        loadAllChatData();
        loadMessages();
        setupRealtimeListener();
    }

    private void initServices() {
        apiService = RetrofitClient.getApiService();
        SharedPrefManager spm = SharedPrefManager.getInstance(this);
        currentUserId = spm.getUserId();
        String userToken = spm.getToken();
        authToken = "Bearer " + userToken;
        apiKey = SupabaseClient.ANON_KEY;
    }

    // --- TẢI PROFILE CỦA CHÍNH MÌNH (Cho Optimistic Update) ---
    private void loadCurrentUserProfile() {
        SharedPrefManager spm = SharedPrefManager.getInstance(this);
        currentUserProfile = new User();
        currentUserProfile.setUserId(currentUserId);
        currentUserProfile.setFullName(spm.getUserFullName());
        // Có thể thêm dòng lấy avatar_url từ SharedPreferences nếu có
        currentUserProfile.setAvatarUrl(spm.getUserAvatarUrl());
    }

    private void initViews() {
        toolbarFlipper = findViewById(R.id.toolbarFlipper);
        inputViewFlipper = findViewById(R.id.inputViewFlipper);
        ivBack = findViewById(R.id.ivBack);
        layoutInfo = findViewById(R.id.layoutInfo);
        tvName = findViewById(R.id.tvName);
        tvInfo = findViewById(R.id.tvInfo);
        ivSearch = findViewById(R.id.ivSearch);
        ivCall = findViewById(R.id.ivCall);
        ivMenu = findViewById(R.id.ivMenu);
        ivCloseSearch = findViewById(R.id.ivCloseSearch);
        etSearch = findViewById(R.id.etSearch);
        tvSearchCount = findViewById(R.id.tvSearchCount);
        ivSearchUp = findViewById(R.id.ivSearchUp);
        ivSearchDown = findViewById(R.id.ivSearchDown);
        ivShowAttachments = findViewById(R.id.ivShowAttachments);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSend = findViewById(R.id.btnSend);
        ivHideAttachments = findViewById(R.id.ivHideAttachments);
        ivLink = findViewById(R.id.ivLink);
        ivCamera = findViewById(R.id.ivCamera);
        ivGallery = findViewById(R.id.ivGallery);
        recyclerViewMessages = findViewById(R.id.recycler_view_messages);
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(this, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(messageAdapter);
    }

    /**
     * Đăng ký các bộ lắng nghe kết quả (Camera, Gallery, Permission)
     */
    private void registerActivityLaunchers() {
        // 1. Kết quả từ Camera
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && cameraImageUri != null) {
                uploadImageAndSend(cameraImageUri);
            }
        });

        // 2. Kết quả từ Gallery
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                uploadImageAndSend(uri);
            }
        });

        // 3. Kết quả xin quyền
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
            boolean cameraGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.CAMERA));
            boolean storageGranted;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                storageGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.READ_MEDIA_IMAGES));
            } else {
                storageGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.READ_EXTERNAL_STORAGE));
            }

            if (wantsToOpenCamera) {
                if (cameraGranted) openCamera();
                else Toast.makeText(this, "Cần quyền Camera", Toast.LENGTH_SHORT).show();
            } else {
                if (storageGranted) openGallery();
                else Toast.makeText(this, "Cần quyền truy cập ảnh", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> onBackPressed());

        // Chuyển đổi thanh công cụ tìm kiếm
        ivSearch.setOnClickListener(v -> toolbarFlipper.setDisplayedChild(1));
        ivCloseSearch.setOnClickListener(v -> toolbarFlipper.setDisplayedChild(0));

        // Chuyển đổi thanh đính kèm
        ivShowAttachments.setOnClickListener(v -> inputViewFlipper.setDisplayedChild(1));
        ivHideAttachments.setOnClickListener(v -> inputViewFlipper.setDisplayedChild(0));

        // Gửi tin nhắn
        btnSend.setOnClickListener(v -> sendMessage());

        // Camera & Gallery
        ivCamera.setOnClickListener(v -> {
            wantsToOpenCamera = true;
            checkPermissions();
        });

        ivGallery.setOnClickListener(v -> {
            wantsToOpenCamera = false;
            checkPermissions();
        });

        // Tạo một sự kiện chung để mở màn hình Cài đặt
        View.OnClickListener openSettingsAction = v -> {
            Intent intent = new Intent(ChatDetailActivity.this, ConvSettingActivity.class);
            intent.putExtra("CONVERSATION_ID", conversationId);
            intent.putExtra("IS_GROUP", isGroupChat);
            startActivity(intent);
        };

        // Gán sự kiện cho cả layoutInfo và ivMenu
        layoutInfo.setOnClickListener(openSettingsAction);
        ivMenu.setOnClickListener(openSettingsAction);
        // ---------------------
    }

    // ================= LOGIC XỬ LÝ MEDIA (CAMERA/GALLERY) =================

    private void checkPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        permissionLauncher.launch(permissions);
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            // Tạo URI an toàn bằng FileProvider
            cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
            cameraLauncher.launch(cameraImageUri);
        } catch (IOException ex) {
            Toast.makeText(this, "Lỗi tạo file ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
    }

    private void uploadImageAndSend(Uri imageUri) {
        // Ẩn thanh đính kèm
        inputViewFlipper.setDisplayedChild(0);

        // Sau khi upload xong, lấy URL và gọi sendMessage(url, "image")
        Toast.makeText(this, "Đang phát triển", Toast.LENGTH_LONG).show();
    }

    // ================= LOGIC REALTIME VÀ TẢI TIN NHẮN =================

    /**
     * Đăng ký listener cho tin nhắn INSERT qua WebSocket.
     */
    private void setupRealtimeListener() {
        SupabaseRealtimeClient realtime = SupabaseRealtimeClient.getInstance();

        messageInsertListener = record -> {
            try {
                // 0️⃣ Lọc conversation
                if (!conversationId.equals(record.getString("conversation_id"))) return;

                // 1️⃣ Convert JSON → Message
                Message serverMessage = Message.fromRealtimeJson(record);

                Log.d("RealtimeDebug",
                        "INSERT message id=" + serverMessage.getMessageId()
                                + " temp=" + serverMessage.getClientTempId());

                // 2️⃣ Tin của chính mình → replace optimistic message
                if (currentUserId.equals(serverMessage.getSenderId())
                        && serverMessage.getClientTempId() != null) {

                    runOnUiThread(() -> {
                        messageAdapter.replaceTempMessage(
                                serverMessage.getClientTempId(),
                                serverMessage.getMessageId(),
                                serverMessage.getCreatedAt()
                        );
                    });
                    return;
                }

                // 3️⃣ Tin người khác → fetch JOIN đầy đủ
                fetchFullMessageAndDisplay(serverMessage.getMessageId());

            } catch (Exception e) {
                Log.e("ChatRealtime", "Realtime error", e);
            }
        };

        realtime.subscribe("messages", "INSERT", messageInsertListener);
    }

    /**
     * Tải lại tin nhắn mới theo ID, bao gồm thông tin JOIN (Sender) bị thiếu trong Realtime Payload.
     */
    private void fetchFullMessageAndDisplay(String messageId) {
        String selectQuery = "*,sender:users!messages_sender_id_fkey(user_id,full_name,avatar_url)";

        apiService.getMessageById(
                authToken,
                apiKey,
                "eq." + messageId,
                selectQuery
        ).enqueue(new Callback<List<Message>>() {

            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (!response.isSuccessful()
                        || response.body() == null
                        || response.body().isEmpty()) {
                    return;
                }

                Message fullMessage = response.body().get(0);

                runOnUiThread(() -> {

                    // 🔒 CHỐT CHẶN DUPLICATE
                    for (Message m : messageAdapter.getCurrentList()) {
                        if (m.getMessageId() != null
                                && m.getMessageId().equals(fullMessage.getMessageId())) {
                            return; // đã tồn tại
                        }
                    }

                    messageAdapter.addMessage(fullMessage);
                    recyclerViewMessages.scrollToPosition(
                            messageAdapter.getItemCount() - 1
                    );
                });
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Log.e("ChatDebug",
                        "Lỗi tải tin nhắn realtime: " + t.getMessage());
            }
        });
    }


    /**
     * Tải thông tin cuộc trò chuyện
     */
    private void loadAllChatData() {
        // Query chuẩn cho cấu trúc bảng mới
        String query = "conversation_id,type," +
                "group_details(name,avatar_url)," +
                "conversation_participants(user_id,users(full_name,avatar_url))";

        apiService.getConversationDetails(
                authToken,
                apiKey,
                "eq." + conversationId,
                query,
                1
        ).enqueue(new Callback<List<Conversation>>() {
            @Override
            public void onResponse(Call<List<Conversation>> call, Response<List<Conversation>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Log.d("ChatDebug", "Thành công! Đã lấy được dữ liệu.");
                    Conversation c = response.body().get(0);

                    // Cập nhật biến toàn cục
                    isGroupChat = c.isGroup();

                    // 1. Cập nhật Tên & Avatar
                    String smartName = c.getDisplayName(currentUserId);
                    tvName.setText(smartName);

                    // 2. Cập nhật UI nút gọi
                    if (isGroupChat) {
                        ivCall.setVisibility(View.GONE);
                    } else {
                        ivCall.setVisibility(View.VISIBLE);
                    }

                    // 3. Cập nhật trạng thái cho Adapter tin nhắn
                    if (messageAdapter != null) {
                        messageAdapter.setGroupChat(isGroupChat);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Conversation>> call, Throwable t) {
                Log.e("ChatDebug", "Lỗi Nghiêm Trọng (Failure): " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    /**
     * Tải tin nhắn từ Server (Chỉ dùng cho lần tải đầu tiên)
     */
    private void loadMessages() {
        String selectQuery = "*,sender:users!messages_sender_id_fkey(user_id,full_name,avatar_url)";

        apiService.getMessagesForConversation(authToken, apiKey, "eq." + conversationId, selectQuery, "created_at.asc")
                .enqueue(new Callback<List<Message>>() {
                    @Override
                    public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Message> newMessages = response.body();

                            // Thiết lập trạng thái cho tin nhắn cũ ---
                            for (Message msg : newMessages) {
                                // Tin nhắn cũ, đã có created_at, luôn được coi là đã SENT
                                msg.setStatus(Message.SendStatus.SENT);
                            }

                            // Sử dụng submitList với callback để đảm bảo cuộn sau khi list được cập nhật
                            messageAdapter.submitList(newMessages, () -> {
                                if (newMessages.size() > 0) {
                                    recyclerViewMessages.scrollToPosition(newMessages.size() - 1);
                                }
                            });
                        }
                    }
                    @Override
                    public void onFailure(Call<List<Message>> call, Throwable t) {
                        Log.e("ChatDebug", "Lỗi tải tin nhắn ban đầu: " + t.getMessage());
                    }
                });
    }


    /**
     * Gửi tin nhắn (Optimistic Update)
     */
    private void sendMessage() {
        String content = etMessageInput.getText().toString().trim();
        if (content.isEmpty()) return;

        // 1. Tạo temp id để map local ↔ server
        String tempId = java.util.UUID.randomUUID().toString();

        // 2. Tạo message local (Optimistic)
        Message localMsg = new Message();
        localMsg.setConversationId(conversationId);
        localMsg.setSenderId(currentUserId);
        localMsg.setContent(content);
        localMsg.setMessageType("text");
        localMsg.setClientTempId(tempId);
        localMsg.setLocalCreatedAt(System.currentTimeMillis());
        localMsg.setCreatedAt(null);
        localMsg.setStatus(Message.SendStatus.SENDING);
        localMsg.setSender(currentUserProfile);

        // 3. Hiển thị ngay
        messageAdapter.addMessage(localMsg);
        recyclerViewMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
        etMessageInput.setText("");

        // 4. Gửi lên server
        Map<String, Object> body = new HashMap<>();
        body.put("conversation_id", conversationId);
        body.put("sender_id", currentUserId);
        body.put("content", content);
        body.put("message_type", "text");
        body.put("client_temp_id", tempId);

        apiService.sendMessage(authToken, apiKey, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(ChatDetailActivity.this, "Gửi thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ChatDetailActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SupabaseRealtimeClient realtime = SupabaseRealtimeClient.getInstance();

        if (messageInsertListener != null) {
            realtime.unsubscribe("messages", "INSERT", messageInsertListener);
        }
    }

}