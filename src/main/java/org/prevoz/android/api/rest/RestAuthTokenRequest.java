package org.prevoz.android.api.rest;

import com.google.gson.annotations.SerializedName;

public class RestAuthTokenRequest
{
    @SerializedName("grant_type")
    public String grantType;

    @SerializedName("client_id")
    public String clientId;

    @SerializedName("client_secret")
    public String clientSecret;

    public RestAuthTokenRequest(String code, String clientId, String clientSecret)
    {
        this.grantType = "authorization_code";
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }
}
