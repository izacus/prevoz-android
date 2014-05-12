package org.prevoz.android.api.rest;

import com.google.gson.annotations.SerializedName;

/**
 * Created by jernej on 10/05/14.
 */
public class RestPushStatus
{
    @SerializedName("status")
    public String status;

    public boolean isSuccessful()
    {
        return "success".equals(status);
    }
}
