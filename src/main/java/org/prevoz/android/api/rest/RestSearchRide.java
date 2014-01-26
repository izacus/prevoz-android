package org.prevoz.android.api.rest;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class RestSearchRide
{
    @SerializedName("id")
    public Long id;
    @SerializedName("from")
    public String fromCity;
    @SerializedName("from_country")
    public String fromCountry;
    @SerializedName("to")
    public String toCity;
    @SerializedName("to_country")
    public String toCountry;
    @SerializedName("author")
    public String author;
    @SerializedName("price")
    public float price;
    @SerializedName("date_iso8601")
    public Date date;
}
