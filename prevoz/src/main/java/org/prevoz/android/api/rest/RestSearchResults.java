package org.prevoz.android.api.rest;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class RestSearchResults implements Serializable
{
    @SerializedName("carshare_list")
    public List<RestRide> results;
}
