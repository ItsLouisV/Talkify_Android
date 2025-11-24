package com.example.talkify.models;

import com.google.gson.annotations.SerializedName;

public class RelationshipResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("request_id")
    private String requestId;

    public String getStatus() {
        return status;
    }

    public String getRequestId() {
        return requestId;
    }
}