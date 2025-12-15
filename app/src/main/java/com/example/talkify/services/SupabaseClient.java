package com.example.talkify.services;

import com.example.talkify.BuildConfig;

public class SupabaseClient {
    // 🔗 Supabase Project URL
    public static final String URL = BuildConfig.SUPABASE_URL;

    // 🔑 Public Anon Key (dùng cho REST + Realtime)
    public static final String ANON_KEY = BuildConfig.SUPABASE_KEY;

    // 📡 Realtime WebSocket URL
    public static final String REALTIME_URL = URL.replace("https://", "wss://") + "/realtime/v1/websocket?apikey=" + ANON_KEY + "&vsn=1.0.0";

}
