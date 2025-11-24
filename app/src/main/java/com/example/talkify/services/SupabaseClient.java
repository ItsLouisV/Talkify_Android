package com.example.talkify.services;

public class SupabaseClient {
    // 🔗 Supabase Project URL
    public static final String URL = "https://pexrxkdexfygzxwjecgq.supabase.co";

    // 🔑 Public Anon Key (dùng cho REST + Realtime)
    public static final String ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBleHJ4a2RleGZ5Z3p4d2plY2dxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA4ODQ5ODQsImV4cCI6MjA3NjQ2MDk4NH0._LRVxl1JtOKpYtlHrOGriSwCEDmsVHDh_-JKUYo1t2w";

    // 📡 Realtime WebSocket URL
    public static final String REALTIME_URL = "wss://pexrxkdexfygzxwjecgq.supabase.co/realtime/v1/websocket?apikey=" + ANON_KEY + "&vsn=1.0.0";

}
