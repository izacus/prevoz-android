package org.prevoz.android.api.rest;

import com.google.gson.annotations.SerializedName;

public class RestSearchRequest
{
    @SerializedName("f")
    public final String fromCity;
    @SerializedName("fc")
    public final String fromCountry;
    @SerializedName("t")
    public final String toCity;
    @SerializedName("tc")
    public final String toCountry;
    @SerializedName("d")
    public final String date;   // Has to be formatted in YYYY-MM-DD

    public RestSearchRequest(String fromCity, String fromCountry, String toCity, String toCountry, String date)
    {
        this.fromCity = fromCity;
        this.fromCountry = fromCountry;
        this.toCity = toCity;
        this.toCountry = toCountry;
        this.date = date;
    }
}
