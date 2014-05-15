package org.prevoz.android.api.rest;

import com.google.gson.annotations.SerializedName;

public class RestPushStatus
{
    @SerializedName("status")
    public String status;

    public boolean isSuccessful()
    {
        return "success".equals(status);
    }
}
