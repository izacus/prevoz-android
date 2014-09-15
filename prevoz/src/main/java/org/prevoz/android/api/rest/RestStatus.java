package org.prevoz.android.api.rest;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class RestStatus
{
    @SerializedName("status")
    public String status;

    @SerializedName("error")
    public Map<String, List<String>> error;
}
