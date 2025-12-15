package com.example.talkify.services;

import com.example.talkify.models.AppNotification;
import com.example.talkify.models.Conversation;
import com.example.talkify.models.FriendRequest;
import com.example.talkify.models.Message;
import com.example.talkify.models.RelationshipResponse;
import com.example.talkify.models.User;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface SupabaseApiService {

    // (HEADER) Thêm 2 header này vào MỌI request
    String AUTH_HEADER = "Authorization";
    String API_KEY_HEADER = "apikey";

    // ===========================
    // 1. QUẢN LÝ NGƯỜI DÙNG & BẠN BÈ
    // ===========================

    // Lấy chi tiết user
    @GET("users")
    Call<List<User>> getUserDetails(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("user_id") String userId,
            @Query("select") String select
    );

    // Kiểm tra trùng lặp Username
    // Tham số Query là tên cột (user_name)
    @GET("users")
    Call<List<User>> checkUsernameUnique(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("user_name") String usernameFilter,
            @Query("user_id") String notEqSelf // Thêm tham số loại trừ: user_id=neq.{my_id}
    );

    // Tìm kiếm người dùng theo tên hoặc email (cho SearchActivity)
    @GET("users")
    Call<List<User>> searchUsers(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("or") String query
    );

    // Tìm kiếm Global (cho CreateGroup)
    @GET("users")
    Call<List<User>> searchUsersGlobal(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("or") String queryFilter,
            @Query("user_id") String notEqSelf
    );

    // Lấy danh sách bạn bè (RPC)
    @POST("rpc/get_my_friends")
    Call<List<User>> getMyFriends(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Body Map<String, Object> body
    );

    // Lấy danh sách gợi ý bạn bè (RPC)
    @POST("rpc/get_user_suggestions_with_status")
    Call<List<User>> getUserSuggestionsWithStatus(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Body Map<String, String> body
    );

    // Lấy trạng thái quan hệ
    @POST("rpc/get_relationship_details")
    Call<RelationshipResponse> getRelationshipDetails(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Body Map<String, String> body
    );

    // ===========================
    // 2. LỜI MỜI KẾT BẠN
    // ===========================

    @GET("friend_requests")
    Call<List<FriendRequest>> getFriendRequests(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("receiver_id") String receiverId,
            @Query("status") String status,
            @Query("select") String select
    );

    @POST("friend_requests")
    Call<Void> sendFriendRequest(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Body Map<String, Object> body
    );

    @PATCH("friend_requests")
    Call<Void> acceptFriendRequest(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("request_id") String requestId,
            @Body Map<String, String> body
    );

    @DELETE("friend_requests")
    Call<Void> deleteFriendRequest(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("request_id") String requestId
    );

    @DELETE("friend_requests")
    Call<Void> cancelFriendRequest(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("sender_id") String senderId,
            @Query("receiver_id") String receiverId
    );

    // ===========================
    // 3. HỘI THOẠI & TIN NHẮN
    // ===========================

    // Lấy danh sách cuộc trò chuyện của User
    @GET("conversations")
    Call<List<Conversation>> getUserConversations(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("select") String select,
            @Query("conversation_participants.user_id") String userIdFilter,
            @Query("order") String order
    );

    // Lấy thông tin chi tiết 1 cuộc trò chuyện
    @GET("conversations")
    Call<List<Conversation>> getConversationDetails(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("conversation_id") String conversationId,
            @Query("select") String select,
            @Query("limit") int limit
    );

    // Lấy danh sách tin nhắn
    @GET("messages")
    Call<List<Message>> getMessagesForConversation(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("conversation_id") String conversationId,
            @Query("select") String select,
            @Query("order") String order
    );

    // Lấy chi tiết 1 tin nhắn theo ID (dùng cho Realtime để tải thông tin JOIN)
    @GET("messages")
    Call<List<Message>> getMessageById(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("message_id") String messageId, // Filter: message_id=eq.{UUID}
            @Query("select") String select
    );

    // Gửi tin nhắn
    @POST("messages")
    Call<Void> sendMessage(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Body Map<String, Object> body
    );

    // Lấy hoặc tạo cuộc trò chuyện 1-1 (RPC)
    @POST("rpc/get_or_create_conversation")
    Call<String> getOrCreateConversation(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Body Map<String, String> body
    );

    // Tạo nhóm chat (RPC)
    @POST("rpc/create_group_conversation")
    Call<String> createGroupConversation(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Body Map<String, Object> body
    );

    // XÓA CUỘC TRÒ CHUYỆN (Dùng để Xóa chat 1-1)
    @DELETE("conversations")
    Call<Void> deleteConversation(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("conversation_id") String conversationId // Filter: eq.UUID
    );

    /**
     * Rời nhóm (Xóa bản thân khỏi bảng conversation_participants)
     * DELETE /rest/v1/conversation_participants?conversation_id=eq.{id}&user_id=eq.{uid}
     */
    // Rời nhóm: Xóa dòng participant của mình
    @DELETE("conversation_participants")
    Call<Void> leaveGroup(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("conversation_id") String conversationId, // eq.ID
            @Query("user_id") String userId                  // eq.MY_ID
    );

    // Xóa lịch sử trò chuyện (Cập nhật mốc thời gian last_cleared_at)
    @PATCH("conversation_participants")
    Call<Void> clearChatHistory(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("conversation_id") String conversationId, // eq.ID
            @Query("user_id") String userId,                 // eq.MY_ID
            @Body Map<String, String> body                   // { "last_cleared_at": "..." }
    );

    // GIẢI TÁN NHÓM: Gọi RPC để vừa báo vừa xóa
    // POST /rest/v1/rpc/disband_group
    @POST("rpc/disband_group")
    Call<Void> disbandGroup(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Body Map<String, Object> body // { "group_id": "...", "admin_id": "..." }
    );

    // ===========================
    // 4. Ghim trò chuyện & Ghim tin nhắn
    // ===========================

    // Kiểm tra xem đã ghim chưa
    // GET /rest/v1/pinned_conversations?user_id=eq.{uid}&conversation_id=eq.{cid}
    @GET("pinned_conversations")
    Call<List<Object>> checkPinnedStatus(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("user_id") String userIdFilter,          // "eq." + currentUserId
            @Query("conversation_id") String convIdFilter   // "eq." + conversationId
    );

    // Ghim hội thoại (Insert)
    @POST("pinned_conversations")
    Call<Void> pinConversation(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Body Map<String, Object> body // { "user_id": "...", "conversation_id": "..." }
    );

    // Bỏ ghim (Delete)
    @DELETE("pinned_conversations")
    Call<Void> unpinConversation(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("user_id") String userIdFilter,
            @Query("conversation_id") String convIdFilter
    );

    // ===========================
    // 5. THÔNG BÁO
    // ===========================

    @POST("notifications")
    Call<Void> createNotification(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Body Map<String, Object> body
    );

    @GET("notifications")
    Call<List<AppNotification>> getNotifications(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("user_id") String receiverId,
            @Query("notification_id") String notificationIdFilter,
            @Query("select") String select,
            @Query("order") String order
    );

    @PATCH("notifications")
    Call<Void> markNotificationAsRead(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("notification_id") String notificationId,
            @Body Map<String, Boolean> body
    );

    @GET("notifications")
    Call<List<AppNotification>> getUnreadNotifications(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("user_id") String userId,
            @Query("is_read") String isRead,
            @Query("select") String select,
            @Query("order") String order
    );

    // ==========================
    // 6. Bạn thân
    // ==========================

    // 1. Kiểm tra trạng thái bạn thân
    @GET("close_friends")
    Call<List<Object>> checkCloseFriend(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("user_id") String myId,      // eq.MY_ID
            @Query("friend_id") String otherId  // eq.OTHER_ID
    );

    // 2. Thêm bạn thân
    @POST("close_friends")
    Call<Void> addCloseFriend(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Body Map<String, Object> body // { "user_id": "...", "friend_id": "..." }
    );

    // 3. Xóa bạn thân
    @DELETE("close_friends")
    Call<Void> removeCloseFriend(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("user_id") String myId,
            @Query("friend_id") String otherId
    );

    // ===================================
    // 7. Xóa tài khoản chính mình
    // ===================================

    /**
     * Xóa tài khoản vĩnh viễn (Gọi RPC)
     * POST /rest/v1/rpc/delete_user_account
     */
    @POST("rpc/delete_user_account")
    Call<Void> deleteMyAccount(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey
    );

    // ====================================
    // 8. Token
    // ====================================

    @POST
    Call<Object> refreshToken(
            @Url String fullUrl,
            @Header("apikey") String apiKey,
            @Body Map<String, String> body
    );

    // ===========================
    // 9. CẬP NHẬT HỒ SƠ
    // ===========================

    // Cập nhật thông tin hồ sơ
    @PATCH("users")
    Call<Void> updateUserProfile(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("user_id") String userIdFilter, // Filter: user_id=eq.{UUID}
            @Body Map<String, String> body          // Dữ liệu cần cập nhật
    );
}