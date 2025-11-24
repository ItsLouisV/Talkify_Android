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

public interface SupabaseApiService {

    // (HEADER) Thêm 2 header này vào MỌI request
    String AUTH_HEADER = "Authorization";
    String API_KEY_HEADER = "apikey";

    /**
     * Lấy danh sách lời mời kết bạn
     */
    @GET("friend_requests") // <-- ĐÃ SỬA
    Call<List<FriendRequest>> getFriendRequests(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("receiver_id") String receiverId,
            @Query("status") String status,
            @Query("select") String select
    );

    /**
     * Lấy danh sách gợi ý bạn bè (dùng RPC MỚI)
     * POST /rpc/get_user_suggestions_with_status
     */
    @POST("rpc/get_user_suggestions_with_status") // <-- TÊN RPC MỚI
    Call<List<User>> getUserSuggestionsWithStatus( // <-- ĐỔI TÊN HÀM
         @Header(AUTH_HEADER) String authorization,
         @Header(API_KEY_HEADER) String apiKey,
         @Body Map<String, String> body
    );

    /**
     * Gửi lời mời kết bạn (Thêm bạn bè)
     */
    @POST("friend_requests") // <-- ĐÃ SỬA
    Call<Void> sendFriendRequest(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Body Map<String, Object> body
    );

    /**
     * Chấp nhận lời mời (Cập nhật status)
     */
    @PATCH("friend_requests") // <-- ĐÃ SỬA
    Call<Void> acceptFriendRequest(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("request_id") String requestId,
            @Body Map<String, String> body
    );

    /**
     * Từ chối / Hủy lời mời
     */
    @DELETE("friend_requests") // <-- ĐÃ SỬA
    Call<Void> deleteFriendRequest(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("request_id") String requestId
    );

    /**
     * Hủy lời mời (dùng cho Nút Hủy ở Gợi ý)
     */
    @DELETE("friend_requests") // <-- ĐÃ SỬA
    Call<Void> cancelFriendRequest(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("sender_id") String senderId,
            @Query("receiver_id") String receiverId
    );

    // Hàm MỚI để lấy chi tiết user (cho ProfileActivity)
    @GET("users")
    Call<List<User>> getUserDetails(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("user_id") String userId,
            @Query("select") String select
    );

    // Hàm MỚI để lấy trạng thái quan hệ
    @POST("rpc/get_relationship_details")
    Call<RelationshipResponse> getRelationshipDetails(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Body Map<String, String> body
    );

    /**
     * Lấy thông tin chi tiết 1 cuộc trò chuyện (để setup Toolbar)
     * GET /rest/v1/conversations?conversation_id=eq.{id}&select=*
     */
    @GET("conversations")
    Call<List<Conversation>> getConversationDetails(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("conversation_id") String conversationId,
            @Query("select") String select,
            @Query("limit") int limit
    );

    @GET("conversations")
    Call<List<Conversation>> getUserConversations(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("select") String select,
            @Query("conversation_participants.user_id") String userIdFilter, // Filter: eq.USER_ID
            @Query("order") String order
    );

    /**
     * Lấy danh sách tin nhắn của 1 cuộc trò chuyện
     * GET /rest/v1/messages?conversation_id=eq.{id}&select=*,sender:users(user_id,full_name,avatar_url)&order=created_at.asc
     */
    @GET("messages")
    Call<List<Message>> getMessagesForConversation(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("conversation_id") String conversationId,
            @Query("select") String select,
            @Query("order") String order
    );

    /**
     * Gửi một tin nhắn mới
     * POST /rest/v1/messages
     */
    @POST("messages")
    Call<Void> sendMessage(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Body Map<String, Object> body // Body: {conversation_id, sender_id, content, ...}
    );

    // 1. Tìm kiếm người dùng theo tên hoặc email
    @GET("users")
    Call<List<User>> searchUsers(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("or") String query // Dùng cú pháp 'or' của Supabase để tìm cả tên và email
    );

    // 2. Gọi hàm RPC vừa tạo để lấy conversation_id
    @POST("rpc/get_or_create_conversation")
    Call<String> getOrCreateConversation(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Body Map<String, String> body // Body: {"target_user_id": "..."}
    );

    /** Notifications */

    // 1. TẠO THÔNG BÁO MỚI
    @POST("notifications")
    Call<Void> createNotification(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Body Map<String, Object> body
    );

    // 2. LẤY THÔNG BÁO
    // Lấy danh sách thông báo (Không lọc is_read, để hiển thị lịch sử)
    @GET("notifications")
    Call<List<AppNotification>> getNotifications(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("user_id") String receiverId,
            @Query("select") String select,
            @Query("order") String order
    );

    // 3. ĐÁNH DẤU ĐÃ ĐỌC
    @PATCH("notifications")
    Call<Void> markNotificationAsRead(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("notification_id") String notificationId,
            @Body Map<String, Boolean> body // {"is_read": true}
    );

    // 4. Polling tin chưa đọc
    @GET("notifications")
    Call<List<AppNotification>> getUnreadNotifications(
            @Header(AUTH_HEADER) String authorization,
            @Header(API_KEY_HEADER) String apiKey,
            @Query("user_id") String userId,    // Truyền vào: "eq." + id
            @Query("is_read") String isRead,    // Truyền vào: "eq.false"
            @Query("select") String select,
            @Query("order") String order
    );
}