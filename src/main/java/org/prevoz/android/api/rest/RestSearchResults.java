package org.prevoz.android.api.rest;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RestSearchResults
{
    @SerializedName("carshare_list")
    List<RestSearchRide> results;
}
