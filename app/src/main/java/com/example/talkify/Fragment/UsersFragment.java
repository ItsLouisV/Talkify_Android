package com.example.talkify.Fragment; // <-- Package của bạn

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Thay thế các import này bằng package đúng của bạn
import com.example.talkify.R;
import com.example.talkify.adapters.FriendRequestAdapter;
import com.example.talkify.adapters.UserSuggestionAdapter;
import com.example.talkify.models.FriendRequest;
import com.example.talkify.models.User;
import com.example.talkify.ui.ProfileActivity;
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

/**
 * Fragment hiển thị danh sách Lời mời kết bạn và Gợi ý bạn bè.
 * Implement listener từ cả 2 adapter.
 */
public class UsersFragment extends Fragment
        implements FriendRequestAdapter.OnRequestActionListener,
        UserSuggestionAdapter.OnSuggestionActionListener {

    // --- Khai báo View ---
    private View rootView;
    private RecyclerView recyclerViewFriendRequests;
    private RecyclerView recyclerViewSuggestions;
    private LinearLayout layoutFriendRequests; // Layout để ẨN/HIỆN

    private TextView tvSeeAllRequests;

    // --- Khai báo Services & Adapters ---
    private SupabaseApiService apiService;
    private FriendRequestAdapter friendRequestAdapter;
    private UserSuggestionAdapter userSuggestionAdapter;
    private SharedPrefManager sharedPrefManager;

    // --- Thông tin xác thực (Lấy từ SharedPref) ---
    private String currentUserId;
    private String authToken; // Token JWT của user
    private String apiKey;    // Anon Key công khai

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_users, container, false);

        // Ánh xạ View bằng findViewById
        recyclerViewFriendRequests = rootView.findViewById(R.id.recycler_view_friend_requests);
        recyclerViewSuggestions = rootView.findViewById(R.id.recycler_view_suggestions);
        layoutFriendRequests = rootView.findViewById(R.id.layout_friend_requests);

        tvSeeAllRequests = rootView.findViewById(R.id.tv_see_all_requests);

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Khởi tạo services và lấy thông tin user
        initServices();

        // 2. Setup RecyclerViews và Adapters
        setupRecyclerViews();

        // 3. Tải dữ liệu từ Supabase
        loadAllData();

        // Sự kiện click chuyển sang màn hình SeeAllRequestActivity.
        if (tvSeeAllRequests != null) {
            tvSeeAllRequests.setOnClickListener(v -> {
                // Chuyển sang màn hình SeeAllRequestActivity
                Intent intent = new Intent(getContext(), com.example.talkify.ui.SeeAllRequestActivity.class);
                startActivity(intent);
            });
        }
    }

    /**
     * Khởi tạo ApiService và lấy thông tin user từ SharedPrefManager.
     */
    private void initServices() {
        // Lấy context an toàn
        if (getContext() == null) return;

        apiService = RetrofitClient.getApiService();
        sharedPrefManager = SharedPrefManager.getInstance(getContext());

        // Lấy thông tin user thật, không còn hardcode
        currentUserId = sharedPrefManager.getUserId();
        String userToken = sharedPrefManager.getToken(); // Token JWT (Access Token)

        // Dùng token CÁ NHÂN cho Authorization
        authToken = "Bearer " + userToken;
        // Dùng ANON_KEY công khai cho apikey
        apiKey = SupabaseClient.ANON_KEY;
    }

    /**
     * Khởi tạo 2 adapter và gán chúng vào RecyclerViews.
     */
    private void setupRecyclerViews() {
        // Khởi tạo 2 adapter, truyền "this" (chính Fragment này) làm listener
        friendRequestAdapter = new FriendRequestAdapter(this);
        userSuggestionAdapter = new UserSuggestionAdapter(this);

        // Setup cho RecyclerView Lời mời
        recyclerViewFriendRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewFriendRequests.setAdapter(friendRequestAdapter);

        // Setup cho RecyclerView Gợi ý
        recyclerViewSuggestions.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewSuggestions.setAdapter(userSuggestionAdapter);
    }

    /**
     * Tải đồng thời cả 2 danh sách.
     */
    private void loadAllData() {
        // Kiểm tra an toàn
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Lỗi: Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        loadFriendRequests();
        loadUserSuggestions(); // <-- Sẽ gọi hàm đã được sửa đổi
    }

    /**
     * Tải danh sách lời mời kết bạn VÀ xử lý logic ẩn/hiện. (Giữ nguyên)
     */
    private void loadFriendRequests() {
        // Câu select này join sender_id với bảng users và đặt tên là "sender"
        String selectQuery = "*,sender:users!sender_id(user_id,full_name,user_name,avatar_url)";

        apiService.getFriendRequests(authToken, apiKey, "eq." + currentUserId, "eq.pending", selectQuery)
                .enqueue(new Callback<List<FriendRequest>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<FriendRequest>> call, @NonNull Response<List<FriendRequest>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<FriendRequest> requests = response.body();

                            // ---- LOGIC CỐT LÕI CỦA 2 BỨC ẢNH ----
                            if (requests.isEmpty()) {
                                layoutFriendRequests.setVisibility(View.GONE);
                            } else {
                                layoutFriendRequests.setVisibility(View.VISIBLE);
                            }

                            friendRequestAdapter.submitList(requests);
                        } else {
                            // Ẩn nếu có lỗi
                            layoutFriendRequests.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<FriendRequest>> call, @NonNull Throwable t) {
                        layoutFriendRequests.setVisibility(View.GONE);
                    }
                });
    }


    /**
     * Tải danh sách gợi ý bạn bè (dùng RPC MỚI)
     */
    private void loadUserSuggestions() {
        Map<String, String> rpcBody = new HashMap<>();
        rpcBody.put("current_user_id", currentUserId);

        // 1. GỌI HÀM API MỚI (từ Bước 2)
        apiService.getUserSuggestionsWithStatus(authToken, apiKey, rpcBody)
                .enqueue(new Callback<List<User>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<User> users = response.body();

                            // 2. "MỒI" TRẠNG THÁI CHO ADAPTER (từ Bước 3)
                            // Hàm này sẽ đọc (user.getRequestStatus())
                            // và "dạy" adapter biết ai là 'sent'
                            userSuggestionAdapter.primeSentStates(users);

                            // 3. CẬP NHẬT DANH SÁCH
                            // Adapter bây giờ sẽ hiển thị đúng ngay từ đầu
                            userSuggestionAdapter.submitList(users);

                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                        // Lỗi
                    }
                });
    }

    // ===================================================================
    // KẾT QUẢ CÁC LISTENER TỪ 2 ADAPTER (Giữ nguyên)
    // ===================================================================

    @Override
    public void onAcceptClick(FriendRequest request) {
        Map<String, String> body = new HashMap<>();
        body.put("status", "accepted");
        body.put("responded_at", "now()"); // Cập nhật thời gian phản hồi

        apiService.acceptFriendRequest(authToken, apiKey, "eq." + request.getRequestId(), body)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        Toast.makeText(getContext(), "Đã chấp nhận kết bạn", Toast.LENGTH_SHORT).show();

                        // Lấy ID người gửi từ request object
                        if (request.getSender() != null) {
                            pushNotificationToDatabase(request.getSender().getUserId(), "friend_accepted", "đã chấp nhận lời mời kết bạn.");
                        }
                        loadAllData(); // Tải lại toàn bộ dữ liệu (cả 2 list)
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        Toast.makeText(getContext(), "Lỗi, thử lại", Toast.LENGTH_SHORT).show();
                        loadAllData(); // Tải lại để reset nút
                    }
                });
    }

    @Override
    public void onDeclineClick(FriendRequest request) {
        apiService.deleteFriendRequest(authToken, apiKey, "eq." + request.getRequestId())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        Toast.makeText(getContext(), "Đã xóa lời mời", Toast.LENGTH_SHORT).show();
                        loadAllData(); // Tải lại
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        Toast.makeText(getContext(), "Lỗi, thử lại", Toast.LENGTH_SHORT).show();
                        loadAllData(); // Tải lại
                    }
                });
    }

    // --- Từ UserSuggestionAdapter (Thêm bạn bè / Hủy) ---

    @Override
    public void onAddFriendClick(User user) {
        // (Adapter đã tự đổi UI sang "Đã gửi")
        Map<String, Object> body = new HashMap<>();
        body.put("sender_id", currentUserId);
        body.put("receiver_id", user.getUserId());
        // status sẽ là 'pending' (mặc định trong CSDL)

        apiService.sendFriendRequest(authToken, apiKey, body)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (response.isSuccessful()) {
                            // === GỬI THÔNG BÁO ===
                            pushNotificationToDatabase(user.getUserId(), "friend_request", "đã gửi cho bạn lời mời kết bạn.");

                        } else {
                            Toast.makeText(getContext(), "Gửi lỗi", Toast.LENGTH_SHORT).show();
                            userSuggestionAdapter.removeUserFromSentState(user.getUserId());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        // Lỗi mạng
                        Toast.makeText(getContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show();
                        // QUAN TRỌNG: Revert UI
                        userSuggestionAdapter.removeUserFromSentState(user.getUserId());
                    }
                });
    }

    @Override
    public void onCancelClick(User user) {
        // (Adapter đã tự đổi UI về "Thêm bạn bè")

        // Dùng hàm API (DELETE ... WHERE sender_id=... AND receiver_id=...)
        apiService.cancelFriendRequest(authToken, apiKey, "eq." + currentUserId, "eq." + user.getUserId())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (!response.isSuccessful()) {
                            // Lỗi (ví dụ không tìm thấy request để hủy)
                            Toast.makeText(getContext(), "Hủy lỗi", Toast.LENGTH_SHORT).show();
                            // QUAN TRỌNG: Revert UI về "Đã gửi"
                            userSuggestionAdapter.addUserToSentState(user.getUserId());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        Toast.makeText(getContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show();
                        // QUAN TRỌNG: Revert UI
                        userSuggestionAdapter.addUserToSentState(user.getUserId());
                    }
                });
    }

    // --- Từ UserSuggestionAdapter ---
    @Override
    public void onAvatarClick(User user) {
        // Lấy user ID và gọi hàm điều hướng
        navigateToUserProfile(user.getUserId());
    }

    // --- Từ FriendRequestAdapter ---
    @Override
    public void onAvatarClick(FriendRequest request) {
        // Lấy user ID của người GỬI (sender) và gọi hàm điều hướng
        if (request.getSender() != null) {
            navigateToUserProfile(request.getSender().getUserId());
        }
    }

    /**
     * Hàm dùng chung để mở trang cá nhân của một người dùng
     * @param userId ID của người dùng cần xem
     */
    private void navigateToUserProfile(String userId) {
        if (userId == null || getContext() == null) {
            return; // Không có ID hoặc context thì không làm gì cả
        }

        // Giả sử Activity trang cá nhân của bạn tên là "ProfileActivity"
        Intent intent = new Intent(getContext(), ProfileActivity.class);

        // Gửi ID của user qua Intent, để ProfileActivity biết
        // cần tải thông tin của ai
        intent.putExtra("USER_ID", userId);

        startActivity(intent);
    }


    /**
     * Hàm gọi API để tạo thông báo (Fire-and-forget)
     */
    private void pushNotificationToDatabase(String receiverId, String type, String contentText) {
        if (receiverId == null || currentUserId == null) return;

        Map<String, Object> notif = new HashMap<>();
        notif.put("user_id", receiverId);      // Người nhận thông báo
        notif.put("actor_id", currentUserId);  // Người thực hiện (Mình)
        notif.put("type", type);               // "friend_request" hoặc "friend_accepted"
        notif.put("content", contentText);     // Nội dung

        // Gọi API (Không cần chờ kết quả hiển thị UI, chỉ cần gửi đi)
        apiService.createNotification(authToken, apiKey, notif)
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> c, Response<Void> r) {}
                    @Override public void onFailure(Call<Void> c, Throwable t) {}
                });
    }
}