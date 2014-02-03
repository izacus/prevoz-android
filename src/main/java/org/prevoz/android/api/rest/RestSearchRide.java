package org.prevoz.android.api.rest;

import com.google.gson.annotations.SerializedName;

import org.prevoz.android.model.City;

import java.io.Serializable;
import java.util.Date;

public class RestSearchRide implements Serializable, Comparable
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

    public City getFrom()
    {
        return new City(fromCity, fromCountry);
    }

    public City getTo()
    {
        return new City(toCity, toCountry);
    }


    @Override
    public int compareTo(Object another)
    {
        if (!(another instanceof RestSearchRide))
            return 0;

        RestSearchRide other = (RestSearchRide) another;
        return (fromCity + toCity).compareTo(other.fromCity + other.toCity);
    }
}
