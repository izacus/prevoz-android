package org.prevoz.android.api.rest;

import com.google.gson.annotations.SerializedName;

public class RestStatus
{
    @SerializedName("status")
    public String status;

    @SerializedName("error")
    public String error;
}
