package com.example.talkify.Fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.talkify.R;
import com.example.talkify.adapters.ConversationAdapter;
import com.example.talkify.models.ChatItem;
import com.example.talkify.services.SharedPrefManager;
import com.example.talkify.services.SupabaseClient;
import com.example.talkify.services.SupabaseRealtimeClient;
import com.example.talkify.ui.ChatDetailActivity;
import com.example.talkify.ui.SearchActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatsFragment extends Fragment {

    private View headerDefault, headerSearch;
    private ImageView ivSearch, ivQrCode, ivAddChat, ivAvatar;
    private TextView btnCancelSearch;
    private EditText etSearch;
    private RecyclerView recyclerChats;

    private ConversationAdapter conversationAdapter;
    private final List<ChatItem> chatList = new ArrayList<>();

    // Biến xử lý tìm kiếm
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_chats, container, false);

        // 1. Ánh xạ View
        headerDefault = root.findViewById(R.id.headerDefault);
        headerSearch = root.findViewById(R.id.headerSearch);
        ivAvatar = root.findViewById(R.id.ivAvatar);
        ivSearch = root.findViewById(R.id.ivSearch);
        ivQrCode = root.findViewById(R.id.ivQrCode);
        ivAddChat = root.findViewById(R.id.ivAddChat);
        btnCancelSearch = root.findViewById(R.id.btnCancelSearch);
        etSearch = root.findViewById(R.id.etSearch);
        recyclerChats = root.findViewById(R.id.recyclerChats);

        // 2. Setup RecyclerView & Adapter
        recyclerChats.setLayoutManager(new LinearLayoutManager(getContext()));

        conversationAdapter = new ConversationAdapter(getContext(), chatList, item -> {
            // Chuyển sang màn hình Chat Detail
            Intent intent = new Intent(getContext(), ChatDetailActivity.class);
            intent.putExtra("CONVERSATION_ID", item.getConversationId());
            intent.putExtra("CONVERSATION_NAME", item.getName());
            intent.putExtra("CONVERSATION_AVATAR", item.getAvatarUrl());
            startActivity(intent);
        });
        recyclerChats.setAdapter(conversationAdapter);

        // --- Thêm Divider nhưng chỉ thêm 1 lần ---
        if (recyclerChats.getItemDecorationCount() == 0) {
            DividerItemDecoration divider = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
            Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.divider_line);
            if (drawable != null) divider.setDrawable(drawable);
            recyclerChats.addItemDecoration(divider);
        }

        // --- CÀI ĐẶT TÍNH NĂNG VUỐT ĐỂ XÓA ---
        setupSwipeToDelete();

        // 3. Gán sự kiện Click
        ivAddChat.setOnClickListener(v -> startActivity(new Intent(getContext(), SearchActivity.class)));

        ivSearch.setOnClickListener(v -> showSearchHeader(true));

        btnCancelSearch.setOnClickListener(v -> {
            showSearchHeader(false);
            loadConversations(null); // Load lại danh sách đầy đủ
        });

        // 4. Các hàm khởi tạo dữ liệu
        setupSearchListener();
        loadCurrentUserAvatar();
        loadConversations(null);
        setupRealtimeListener();

        return root;
    }

    /**
     * CẤU HÌNH VUỐT SANG TRÁI ĐỂ XÓA
     */
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                ChatItem itemToDelete = chatList.get(position);

                new AlertDialog.Builder(getContext())
                        .setTitle("Xóa cuộc trò chuyện?")
                        .setMessage("Bạn có chắc muốn xóa cuộc trò chuyện này không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            deleteConversationFromDB(itemToDelete.getConversationId(), position);
                        })
                        .setNegativeButton("Hủy", (dialog, which) -> {
                            conversationAdapter.notifyItemChanged(position);
                        })
                        .setCancelable(false)
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

                    View itemView = viewHolder.itemView;

                    float width = itemView.getWidth();
                    float swipeProgress = Math.min(Math.abs(dX) / width, 1f);

                    // --- NỀN ĐỎ (MỀM, BO CONG) ---
                    Paint paint = new Paint();
                    paint.setColor(Color.parseColor("#FF3B30")); // Đỏ iOS
                    float radius = 46f;

                    float left = itemView.getRight() + dX;
                    float top = itemView.getTop();
                    float right = itemView.getRight();
                    float bottom = itemView.getBottom();

                    canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint);

                    // --- ICON DELETE
                    Drawable deleteIcon = ContextCompat.getDrawable(getContext(), R.drawable.ic_delete);
                    if (deleteIcon != null) {
                        int iconSize = deleteIcon.getIntrinsicWidth();
                        int iconTop = itemView.getTop() + (itemView.getHeight() - iconSize) / 2;
                        int iconBottom = iconTop + iconSize;
                        int margin = 60;

                        int iconLeft = itemView.getRight() - margin - iconSize;
                        int iconRight = itemView.getRight() - margin;

                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);

                        // Fade in theo độ vuốt
                        deleteIcon.setAlpha((int) (255 * swipeProgress));

                        deleteIcon.draw(canvas);
                    }

                    // --- HIỆU ỨNG ĐÀN HỒI iOS ---
                    float bounceOffset = (float) (Math.sin(swipeProgress * Math.PI) * 18);
                    super.onChildDraw(canvas, recyclerView, viewHolder, dX - bounceOffset, dY,
                            actionState, isCurrentlyActive);
                    return;
                }

                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerChats);
    }


    /**
     * Gọi API xóa cuộc hội thoại (xóa mềm)
     */
    private void deleteConversationFromDB(String conversationId, int position) {
        String currentUserId = SharedPrefManager.getInstance(getContext()).getUserId();
        String userToken = SharedPrefManager.getInstance(getContext()).getToken();
        if (currentUserId == null || userToken == null) return;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                // URL đến conversation_participants của user hiện tại
                String urlString = SupabaseClient.URL + "/rest/v1/conversation_participants?" +
                        "conversation_id=eq." + conversationId +
                        "&user_id=eq." + currentUserId;

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PATCH"); // PATCH để update
                conn.setRequestProperty("apikey", SupabaseClient.ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + userToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Body JSON: cập nhật deleted_at
                String nowIso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                        .format(new Date());
                String body = "{\"deleted_at\":\"" + nowIso + "\"}";
                conn.getOutputStream().write(body.getBytes());

                int responseCode = conn.getResponseCode();

                handler.post(() -> {
                    if (responseCode == 200 || responseCode == 204) {
                        // Cập nhật UI: xóa mềm khỏi danh sách hiện tại
                        if (position >= 0 && position < chatList.size()) {
                            chatList.remove(position);
                            conversationAdapter.notifyItemRemoved(position);
                            Toast.makeText(getContext(), "Đã xóa hội thoại", Toast.LENGTH_SHORT).show();
                        } else {
                            loadConversations(null);
                        }
                    } else {
                        Toast.makeText(getContext(), "Lỗi xóa: " + responseCode, Toast.LENGTH_SHORT).show();
                        conversationAdapter.notifyItemChanged(position);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    Toast.makeText(getContext(), "Lỗi mạng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    conversationAdapter.notifyItemChanged(position);
                });
            }
        });
    }


    private void showSearchHeader(boolean show) {
        if (show) {
            headerDefault.setVisibility(View.GONE);
            headerSearch.setVisibility(View.VISIBLE);
            etSearch.requestFocus();
        } else {
            headerSearch.setVisibility(View.GONE);
            headerDefault.setVisibility(View.VISIBLE);
            etSearch.setText("");
        }
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
            }
            @Override public void afterTextChanged(Editable s) {
                searchRunnable = () -> loadConversations(s.toString().trim());
                searchHandler.postDelayed(searchRunnable, 500);
            }
        });
    }

    private void loadCurrentUserAvatar() {
        String currentUserId = SharedPrefManager.getInstance(getContext()).getUserId();
        if (currentUserId == null) return;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                String apiUrl = SupabaseClient.URL + "/rest/v1/users?select=full_name,avatar_url&user_id=eq." + currentUserId;
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("apikey", SupabaseClient.ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + SupabaseClient.ANON_KEY);
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONArray array = new JSONArray(sb.toString());
                if (array.length() > 0) {
                    JSONObject obj = array.getJSONObject(0);
                    String avatarUrl = obj.optString("avatar_url", "");
                    handler.post(() -> {
                        if (getContext() != null && !avatarUrl.isEmpty()) {
                            Glide.with(getContext()).load(avatarUrl).placeholder(R.drawable.ic_user).circleCrop().into(ivAvatar);
                        }
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    /**
     * Hàm Load danh sách (Đã thêm LOG DEBUG chi tiết)
     */
    private void loadConversations(@Nullable String searchQuery) {
        String currentUserId = SharedPrefManager.getInstance(getContext()).getUserId();
        String userToken = SharedPrefManager.getInstance(getContext()).getToken();

        if (currentUserId == null || userToken == null) {
            Log.e("DEBUG_CHAT", "Lỗi: User ID hoặc Token bị null. Chưa đăng nhập?");
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<ChatItem> result = new ArrayList<>();

            try {
                StringBuilder apiUrlBuilder = new StringBuilder(SupabaseClient.URL);
                apiUrlBuilder.append("/rest/v1/conversations");
                apiUrlBuilder.append("?select=");
                apiUrlBuilder.append("conversation_id,type,updated_at,");
                apiUrlBuilder.append("group_details(name,avatar_url),");
                apiUrlBuilder.append("messages:messages(content,created_at,sender_id),");
                apiUrlBuilder.append("conversation_participants(user_id,deleted_at,users(full_name,avatar_url))"); // Thêm deleted_at
                apiUrlBuilder.append("&order=updated_at.desc.nullslast");

                URL url = new URL(apiUrlBuilder.toString());
                Log.d("DEBUG_CHAT", "URL Gọi API: " + url);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("apikey", SupabaseClient.ANON_KEY);
                conn.setRequestProperty("Authorization", "Bearer " + userToken);
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                String line;
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONArray array = new JSONArray(sb.toString());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);

                        JSONArray participants = obj.optJSONArray("conversation_participants");
                        boolean userDeleted = false;
                        if (participants != null) {
                            for (int j = 0; j < participants.length(); j++) {
                                JSONObject p = participants.getJSONObject(j);
                                String uid = p.getString("user_id");
                                if (uid.equals(currentUserId)) {
                                    if (!p.isNull("deleted_at")) {
                                        userDeleted = true; // User đã xóa cuộc hội thoại này
                                        break;
                                    }
                                }
                            }
                        }

                        if (userDeleted) continue; // Bỏ qua conversation này

                        String id = obj.getString("conversation_id");
                        String type = obj.optString("type", "direct");
                        String displayName = "Không tên";
                        String displayAvatar = "";

                        if ("group".equals(type)) {
                            JSONObject group = obj.optJSONObject("group_details");
                            if (group != null) {
                                displayName = group.optString("name", "Nhóm");
                                displayAvatar = group.optString("avatar_url", "");
                            }
                        } else if (participants != null) {
                            for (int j = 0; j < participants.length(); j++) {
                                JSONObject p = participants.getJSONObject(j);
                                String uid = p.getString("user_id");
                                if (!uid.equals(currentUserId)) {
                                    JSONObject u = p.optJSONObject("users");
                                    if (u != null) {
                                        displayName = u.optString("full_name", "Người dùng");
                                        displayAvatar = u.optString("avatar_url", "");
                                    }
                                    break;
                                }
                            }
                        }

                        if (searchQuery != null && !searchQuery.isEmpty() &&
                                !displayName.toLowerCase().contains(searchQuery.toLowerCase())) {
                            continue;
                        }

                        // Lấy last message
                        String lastMsg = "Bắt đầu trò chuyện";
                        String time = "";
                        boolean isRead = true;
                        boolean lastMessageMine = false;
                        long timestamp = 0;

                        if (obj.has("messages")) {
                            JSONArray messages = obj.getJSONArray("messages");
                            if (messages.length() > 0) {
                                JSONObject last = messages.getJSONObject(messages.length() - 1);
                                lastMsg = last.optString("content", "Đã gửi file");
                                timestamp = isoToMillis(last.optString("created_at", ""));
                                time = formatTime(last.optString("created_at", ""));
                                lastMessageMine = last.optString("sender_id", "").equals(currentUserId);
                                if (!lastMessageMine) isRead = false;
                            }
                        }

                        ChatItem chatItem = new ChatItem(id, displayName, displayAvatar, lastMsg, time, lastMessageMine, isRead);
                        chatItem.setLastMessageTimestamp(timestamp);
                        result.add(chatItem);
                    }

                } else {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errorSb = new StringBuilder();
                    while ((line = errorReader.readLine()) != null) errorSb.append(line);
                    errorReader.close();
                    Log.e("DEBUG_CHAT", "LỖI API: " + errorSb.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("DEBUG_CHAT", "Exception Java: " + e.getMessage());
            }

            Collections.sort(result, (a, b) -> Long.compare(b.getLastMessageTimestamp(), a.getLastMessageTimestamp()));

            handler.post(() -> {
                chatList.clear();
                chatList.addAll(result);
                conversationAdapter.notifyDataSetChanged();
                Log.d("DEBUG_CHAT", "Đã cập nhật Adapter với " + chatList.size() + " item.");
            });
        });
    }

    private void setupRealtimeListener() {
        String currentUserId = SharedPrefManager.getInstance(getContext()).getUserId();
        if (currentUserId == null) return;

        // 1. Tin nhắn mới
        SupabaseRealtimeClient.getInstance().subscribe("messages", "INSERT", record -> {
            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                String conversationId = record.optString("conversation_id", "");
                String content = record.optString("content", "");
                String createdAt = record.optString("created_at", "");
                String senderId = record.optString("sender_id", "");

                for (ChatItem item : chatList) {
                    if (item.getConversationId().equals(conversationId)) {
                        item.setLastMessage(content);
                        item.setLastMessageTime(formatTime(createdAt));
                        boolean isMine = senderId.equals(currentUserId);
                        item.setLastMessageMine(isMine);
                        item.setRead(isMine);
                        break;
                    }
                }

                // Sắp xếp lại danh sách theo lastMessageTime giảm dần
                Collections.sort(chatList, (a, b) -> Long.compare(b.getLastMessageTimestamp(), a.getLastMessageTimestamp()));
                conversationAdapter.notifyDataSetChanged();
            });
        });

        // 2. Cập nhật deleted_at hoặc các thay đổi khác của conversation_participants
        SupabaseRealtimeClient.getInstance().subscribe("conversation_participants", "UPDATE", record -> {
            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                String conversationId = record.optString("conversation_id");
                String userId = record.optString("user_id");

                if (!currentUserId.equals(userId)) return;

                boolean isDeleted = !record.isNull("deleted_at");

                for (int i = 0; i < chatList.size(); i++) {
                    ChatItem item = chatList.get(i);
                    if (item.getConversationId().equals(conversationId)) {
                        if (isDeleted) {
                            chatList.remove(i);
                            conversationAdapter.notifyItemRemoved(i);
                        } else {
                            conversationAdapter.notifyItemChanged(i);
                        }
                        break;
                    }
                }
            });
        });

        SupabaseRealtimeClient.getInstance().connect();
    }

    private String formatTime(String isoTime) {
        long timeMillis = isoToMillis(isoTime);
        if (timeMillis == 0) return "";

        long now = System.currentTimeMillis();

        // Fix trường hợp thời gian server lớn hơn thời gian máy điện thoại một chút
        if (timeMillis > now) timeMillis = now;

        return DateUtils.getRelativeTimeSpanString(
                timeMillis,
                now,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString();
    }

    private long isoToMillis(String iso) {
        if (iso == null || iso.isEmpty()) return 0;
        try {
            // Chuẩn hóa chuỗi ngày tháng từ Supabase
            // Ví dụ: 2023-11-23T10:00:00.123456+00:00

            // 1. Xử lý Timezone: Chuyển +00:00 thành +0000 (để tương thích SimpleDateFormat 'Z')
            // Chỉ thay thế dấu : cuối cùng nếu nó nằm trong phần timezone
            if (iso.lastIndexOf(":") > iso.lastIndexOf("+") || iso.lastIndexOf(":") > iso.lastIndexOf("-")) {
                // Logic này để đảm bảo chỉ sửa : ở múi giờ (ví dụ +07:00 -> +0700), không sửa giờ (10:00)
                int lastColon = iso.lastIndexOf(":");
                if (lastColon > iso.indexOf("T")) { // Đảm bảo nằm sau phần ngày
                    String timezonePart = iso.substring(lastColon);
                    if (timezonePart.length() == 3) { // :00
                        iso = iso.substring(0, lastColon) + iso.substring(lastColon + 1);
                    }
                }
            }

            // 2. Xử lý mili-giây: Cắt bớt nếu quá 3 chữ số (Java chỉ hiểu 3 số mili giây)
            // Regex: Tìm dấu chấm theo sau là số, giữ lại tối đa 3 số.
            if (iso.contains(".")) {
                int dotIndex = iso.indexOf(".");
                int timeZoneIndex = iso.indexOf("+");
                if (timeZoneIndex == -1) timeZoneIndex = iso.indexOf("-", dotIndex); // Tìm dấu - của timezone nếu có
                if (timeZoneIndex == -1) timeZoneIndex = iso.indexOf("Z");

                if (timeZoneIndex > dotIndex) {
                    String millis = iso.substring(dotIndex + 1, timeZoneIndex);
                    if (millis.length() > 3) {
                        iso = iso.substring(0, dotIndex + 4) + iso.substring(timeZoneIndex);
                    }
                }
            }

            // 3. Định dạng mẫu
            String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
            if (!iso.contains(".")) {
                pattern = "yyyy-MM-dd'T'HH:mm:ssZ";
            }

            // Nếu kết thúc bằng Z (UTC), thay thế bằng +0000
            if (iso.endsWith("Z")) {
                iso = iso.replace("Z", "+0000");
            }

            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
            // Quan trọng: Supabase trả về giờ UTC, cần set timezone cho parser
            format.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date date = format.parse(iso);
            return date != null ? date.getTime() : 0;

        } catch (Exception e) {
            Log.e("TIME_ERROR", "Lỗi parse thời gian: " + iso + " | " + e.getMessage());
            return 0;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}