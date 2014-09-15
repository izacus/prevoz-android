package org.prevoz.android.api.rest;

import com.google.gson.annotations.SerializedName;

public class RestAuthTokenResponse
{
    @SerializedName("access_token")
    public String accessToken;

    @SerializedName("refresh_token")
    public String refreshToken;

    @SerializedName("expires_in")
    public int expiresIn;
}
