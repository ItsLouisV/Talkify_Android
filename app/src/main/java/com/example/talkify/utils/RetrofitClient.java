package com.example.talkify.utils;
import com.example.talkify.services.SupabaseApiService;
import com.example.talkify.services.SupabaseClient;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofit = null;
    private static SupabaseApiService apiService = null;

    public static SupabaseApiService getApiService() {
        if (apiService == null) {
            apiService = getRetrofitInstance().create(SupabaseApiService.class);
        }
        return apiService;
    }

    private static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            // Dùng để log request ra Logcat (hữu ích khi debug)
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    // Dùng URL từ file SupabaseClient của bạn
                    .baseUrl(SupabaseClient.URL + "/rest/v1/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }
}