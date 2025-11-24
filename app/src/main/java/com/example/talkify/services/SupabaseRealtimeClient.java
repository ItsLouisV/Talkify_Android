package com.example.talkify.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * SupabaseRealtimeClient Generic
 * - Singleton, 1 WebSocket duy nhất
 * - Subscribe nhiều bảng, nhiều event
 * - Thêm/loại listener bất cứ lúc nào
 */
public class SupabaseRealtimeClient {

    private static SupabaseRealtimeClient instance;
    private WebSocket webSocket;
    private boolean isConnected = false;

    private int reconnectAttempts = 0;
    private final int MAX_RECONNECT = 5;
    private final int RECONNECT_DELAY_MS = 3000;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // <table:event, set of listeners>
    private final Map<String, Set<MessageListener>> listenersMap = new HashMap<>();

    public interface MessageListener {
        void onEvent(JSONObject record);
    }

    private SupabaseRealtimeClient() {}

    public static synchronized SupabaseRealtimeClient getInstance() {
        if (instance == null) {
            instance = new SupabaseRealtimeClient();
        }
        return instance;
    }

    // Connect WebSocket
    public void connect() {
        if (isConnected) return;

        OkHttpClient client = new OkHttpClient.Builder()
                .pingInterval(20, TimeUnit.SECONDS)
                .connectTimeout(15, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(SupabaseClient.REALTIME_URL)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(WebSocket ws, Response response) {
                isConnected = true;
                reconnectAttempts = 0;
                Log.d("SupabaseRealtime", "✅ Connected");

                // Join tất cả channel đang subscribe
                for (String key : listenersMap.keySet()) {
                    String[] parts = key.split(":"); // table:event
                    String table = parts[0];
                    JSONObject joinMsg = new JSONObject();
                    JSONObject payload = new JSONObject();
                    try {
                        joinMsg.put("topic", "realtime:public:" + table);
                        joinMsg.put("event", "phx_join");
                        joinMsg.put("payload", payload);
                        joinMsg.put("ref", "1");
                        ws.send(joinMsg.toString());

                        // Đăng ký lắng nghe event
                        JSONObject listenMsg = new JSONObject();
                        listenMsg.put("topic", "realtime:public:" + table);
                        listenMsg.put("event", "postgres_changes");
                        JSONObject p = new JSONObject();
                        p.put("event", parts[1]);
                        p.put("schema", "public");
                        p.put("table", table);
                        listenMsg.put("payload", p);
                        listenMsg.put("ref", "2");
                        ws.send(listenMsg.toString());

                        Log.d("SupabaseRealtime", "👂 Listening " + parts[1] + " on " + table);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                try {
                    JSONObject obj = new JSONObject(text);
                    if (obj.has("payload")) {
                        JSONObject payload = obj.getJSONObject("payload");
                        if (payload.has("record") && payload.has("event") && payload.has("table")) {
                            String table = payload.getString("table");
                            String event = payload.getString("event");
                            JSONObject record = payload.getJSONObject("record");

                            String key = table + ":" + event;
                            if (listenersMap.containsKey(key)) {
                                for (MessageListener listener : listenersMap.get(key)) {
                                    listener.onEvent(record);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("SupabaseRealtime", "❌ Parsing error: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                isConnected = false;
                Log.e("SupabaseRealtime", "💀 Failure: " + t.getMessage());
                attemptReconnect();
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                isConnected = false;
                Log.d("SupabaseRealtime", "🔌 Closed: " + reason);
                attemptReconnect();
            }
        });
    }

    private void attemptReconnect() {
        if (reconnectAttempts < MAX_RECONNECT) {
            reconnectAttempts++;
            Log.d("SupabaseRealtime", "🔄 Reconnect attempt " + reconnectAttempts);
            handler.postDelayed(this::connect, RECONNECT_DELAY_MS);
        } else {
            Log.e("SupabaseRealtime", "❌ Max reconnect reached");
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "User left");
            webSocket = null;
            isConnected = false;
        }
    }

    // Subscribe event cho bảng
    public void subscribe(String table, String event, MessageListener listener) {
        String key = table + ":" + event;
        if (!listenersMap.containsKey(key)) {
            listenersMap.put(key, new HashSet<>());
        }
        listenersMap.get(key).add(listener);

        // Nếu đang connect, gửi subscribe ngay
        if (isConnected) {
            try {
                JSONObject listenMsg = new JSONObject();
                listenMsg.put("topic", "realtime:public:" + table);
                listenMsg.put("event", "postgres_changes");
                JSONObject payload = new JSONObject();
                payload.put("event", event);
                payload.put("schema", "public");
                payload.put("table", table);
                listenMsg.put("payload", payload);
                listenMsg.put("ref", "sub_" + key);
                webSocket.send(listenMsg.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void unsubscribe(String table, String event, MessageListener listener) {
        String key = table + ":" + event;
        if (listenersMap.containsKey(key)) {
            listenersMap.get(key).remove(listener);
            if (listenersMap.get(key).isEmpty()) listenersMap.remove(key);
        }
    }
}
