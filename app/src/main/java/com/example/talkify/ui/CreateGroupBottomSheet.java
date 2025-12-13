package com.example.talkify.ui;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.talkify.R;
import com.example.talkify.adapters.SelectMemberAdapter;
import com.example.talkify.models.User;
import com.example.talkify.services.SharedPrefManager;
import com.example.talkify.services.SupabaseClient;
import com.example.talkify.utils.RetrofitClient;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateGroupBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "DEBUG_GROUP"; // Tag để lọc Logcat

    // Views
    private EditText etGroupName, etSearch;
    private TextView tvCreateGroup;
    private ImageView ivClose;
    private RecyclerView recyclerMembers;
    private ProgressBar progressBar;

    // Adapter + Data
    private SelectMemberAdapter adapter;
    private final List<User> listFriends = new ArrayList<>(); // List gốc (Bạn bè)
    private final List<User> displayList = new ArrayList<>(); // List hiển thị

    private ArrayList<String> excludedIds = new ArrayList<>();
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private OnGroupCreatedListener listener;
    public interface OnGroupCreatedListener { void onGroupCreated(); }
    public void setOnGroupCreatedListener(OnGroupCreatedListener listener) { this.listener = listener; }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
        if (getArguments() != null) {
            ArrayList<String> args = getArguments().getStringArrayList("EXCLUDED_IDS");
            if (args != null) excludedIds = args;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_create_group, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Bắt đầu khởi tạo BottomSheet");

        initViews(view);
        setupRecyclerView();
        setupEvents();

        // Load danh sách bạn bè ngay khi mở
        loadDefaultFriends();
    }

    private void initViews(View view) {
        try {
            etGroupName = view.findViewById(R.id.etGroupName);
            tvCreateGroup = view.findViewById(R.id.tvCreateGroup);
            ivClose = view.findViewById(R.id.ivClose);
            recyclerMembers = view.findViewById(R.id.recyclerMembers);
            etSearch = view.findViewById(R.id.etSearchMember);
            progressBar = view.findViewById(R.id.progressBar);

            // Kiểm tra xem View có bị null không (tránh Crash)
            if (progressBar == null) Log.e(TAG, "LỖI: Không tìm thấy ID R.id.progressBar trong XML!");
            if (etSearch == null) Log.e(TAG, "LỖI: Không tìm thấy ID R.id.etSearchMember trong XML!");

        } catch (Exception e) {
            Log.e(TAG, "Lỗi initViews: " + e.getMessage());
        }
    }

    private void setupRecyclerView() {
        recyclerMembers.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SelectMemberAdapter(getContext(), displayList);
        recyclerMembers.setAdapter(adapter);
    }

    private void setupEvents() {
        ivClose.setOnClickListener(v -> dismiss());
        tvCreateGroup.setOnClickListener(v -> handleCreateGroup());

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                }
                @Override public void afterTextChanged(Editable s) {
                    searchRunnable = () -> performSearchOrReset(s.toString().trim());
                    searchHandler.postDelayed(searchRunnable, 500);
                }
            });
        }
    }

    // ====================================================================
    // 1. LOAD BẠN BÈ (GỌI RPC)
    // ====================================================================
    private void loadDefaultFriends() {
        String userId = SharedPrefManager.getInstance(getContext()).getUserId();
        String token = SharedPrefManager.getInstance(getContext()).getToken();

        if (userId == null) {
            Log.e(TAG, "Lỗi: User ID null, chưa đăng nhập?");
            return;
        }

        showLoading(true);
        Log.d(TAG, "Bắt đầu gọi RPC get_my_friends cho UserID: " + userId);

        Map<String, Object> body = new HashMap<>();
        body.put("current_user_id", userId);
        body.put("excluded_ids", excludedIds);

        RetrofitClient.getApiService().getMyFriends("Bearer " + token, SupabaseClient.ANON_KEY, body)
                .enqueue(new Callback<List<User>>() {
                    @Override
                    public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            Log.d(TAG, "Load bạn bè thành công! Số lượng: " + response.body().size());

                            listFriends.clear();
                            listFriends.addAll(response.body());
                            updateListUI(listFriends); // Hiển thị lên
                        } else {
                            try {
                                String error = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                                Log.e(TAG, "Lỗi API get_my_friends: " + response.code() + " - " + error);
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }

                    @Override
                    public void onFailure(Call<List<User>> call, Throwable t) {
                        showLoading(false);
                        Log.e(TAG, "Lỗi mạng loadFriends: " + t.getMessage());
                    }
                });
    }

    // ====================================================================
    // 2. TÌM KIẾM GLOBAL
    // ====================================================================
    private void performSearchOrReset(String query) {
        Log.d(TAG, "Tìm kiếm: '" + query + "'");
        if (query.isEmpty()) {
            Log.d(TAG, "Query rỗng -> Hiển thị lại danh sách bạn bè");
            updateListUI(listFriends);
        } else {
            searchGlobalUsers(query);
        }
    }

    private void searchGlobalUsers(String query) {
        String userId = SharedPrefManager.getInstance(getContext()).getUserId();
        String token = SharedPrefManager.getInstance(getContext()).getToken();

        showLoading(true);
        String filter = "(full_name.ilike.*" + query + "*,email.ilike.*" + query + "*)";

        RetrofitClient.getApiService().searchUsersGlobal(
                "Bearer " + token,
                SupabaseClient.ANON_KEY,
                filter,
                "neq." + userId
        ).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<User> results = response.body();
                    Log.d(TAG, "Tìm thấy " + results.size() + " kết quả Global");

                    // Lọc thủ công excludedIds
                    if (!excludedIds.isEmpty()) {
                        results.removeIf(user -> excludedIds.contains(user.getUserId()));
                    }
                    updateListUI(results);
                } else {
                    Log.e(TAG, "Lỗi tìm kiếm Global: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Lỗi mạng Search: " + t.getMessage());
            }
        });
    }

    private void updateListUI(List<User> newList) {
        displayList.clear();
        displayList.addAll(newList);
        adapter.notifyDataSetChanged();
    }

    // Helper để tránh crash khi progressBar null
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    // ============================
    // 3. TẠO NHÓM
    // ============================
    private void handleCreateGroup() {
        String groupName = etGroupName.getText().toString().trim();
        List<String> selectedIds = adapter.getSelectedUserIds();

        Log.d(TAG, "Bấm tạo nhóm: " + groupName + " | Thành viên: " + selectedIds.size());

        if (groupName.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập tên nhóm", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedIds.isEmpty()) {
            Toast.makeText(getContext(), "Chọn ít nhất 2 thành viên", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = SharedPrefManager.getInstance(getContext()).getUserId();
        String token = SharedPrefManager.getInstance(getContext()).getToken();

        Map<String, Object> body = new HashMap<>();
        body.put("creator_id", currentUserId);
        body.put("group_name", groupName);
        body.put("member_ids", selectedIds);

        tvCreateGroup.setEnabled(false);
        showLoading(true);

        RetrofitClient.getApiService().createGroupConversation(
                "Bearer " + token,
                SupabaseClient.ANON_KEY,
                body
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                tvCreateGroup.setEnabled(true);
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    String newConvId = response.body();
                    Log.d(TAG, "Tạo nhóm thành công! ID: " + newConvId);

                    Intent intent = new Intent(getContext(), ChatDetailActivity.class);
                    intent.putExtra("CONVERSATION_ID", newConvId);
                    intent.putExtra("CONVERSATION_NAME", groupName);
                    startActivity(intent);

                    if (listener != null) listener.onGroupCreated();
                    dismiss();
                } else {
                    try {
                        String err = response.errorBody().string();
                        Log.e(TAG, "Lỗi Server tạo nhóm: " + err);
                        Toast.makeText(getContext(), "Lỗi: " + err, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {}
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                tvCreateGroup.setEnabled(true);
                showLoading(false);
                Log.e(TAG, "Lỗi mạng tạo nhóm: " + t.getMessage());
                Toast.makeText(getContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                View view = getView();
                if (view != null) {
                    view.post(() -> {
                        View parent = (View) view.getParent();
                        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) parent.getLayoutParams();
                        CoordinatorLayout.Behavior behavior = params.getBehavior();
                        if (behavior instanceof BottomSheetBehavior) {
                            ((BottomSheetBehavior) behavior).setState(BottomSheetBehavior.STATE_EXPANDED);
                            ((BottomSheetBehavior) behavior).setSkipCollapsed(true);
                        }
                    });
                }
            }
        }
    }
}