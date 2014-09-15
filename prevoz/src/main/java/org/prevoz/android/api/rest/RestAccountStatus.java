package org.prevoz.android.api.rest;

import com.google.gson.annotations.SerializedName;

public class RestAccountStatus
{
    @SerializedName("is_authenticated")
    public boolean isAuthenticated;

    @SerializedName("username")
    public String username;
}
