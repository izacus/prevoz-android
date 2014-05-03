package org.prevoz.android.api.rest;

import com.google.gson.annotations.SerializedName;

public class RestApiKey
{
    @SerializedName("apikey")
    public String apiKey;

    public RestApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }
}
