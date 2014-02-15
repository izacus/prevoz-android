package org.prevoz.android.api.rest;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Created by jernej on 15/02/14.
 */
public class RestRide
{
    @SerializedName("id")
    public long id;

    @SerializedName("from")
    public String fromCity;
    @SerializedName("from_country")
    public String fromCountry;

    @SerializedName("to")
    public String toCity;
    @SerializedName("to_country")
    public String toCountry;

    @SerializedName("price")
    public Float price;

    @SerializedName("num_people")
    public Integer numPeople;
    @SerializedName("full")
    public boolean isFull;

    @SerializedName("date_iso8601")
    public Date date;
    /*@SerializedName("added")
    public Date published; */

    @SerializedName("contact")
    public String phoneNumber;
    @SerializedName("author")
    public String author;
    @SerializedName("comment")
    public String comment;


}
