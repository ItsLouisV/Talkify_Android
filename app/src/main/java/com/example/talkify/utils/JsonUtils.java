package com.example.talkify.utils;

import com.google.gson.JsonObject;

public class JsonUtils {
    public static String getSafeString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }
}
