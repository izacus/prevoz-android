package org.prevoz.android.api.rest;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class RestRide implements Comparable, Parcelable
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
    @SerializedName("confirmed_contact")
    public boolean phoneNumberConfirmed;
    @SerializedName("insured")
    public boolean insured;
    @SerializedName("author")
    public String author;
    @SerializedName("comment")
    public String comment;



    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof RestRide)) return false;

        RestRide restRide = (RestRide) o;
        return id == restRide.id;
    }

    @Override
    public int hashCode()
    {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public int compareTo(Object another)
    {
        if (!(another instanceof RestRide))
            return 0;

        RestRide other = (RestRide) another;
        return (fromCity + toCity).compareTo(other.fromCity + other.toCity);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeLong(this.id);
        dest.writeString(this.fromCity);
        dest.writeString(this.fromCountry);
        dest.writeString(this.toCity);
        dest.writeString(this.toCountry);
        dest.writeValue(this.price);
        dest.writeValue(this.numPeople);
        dest.writeByte(isFull ? (byte) 1 : (byte) 0);
        dest.writeLong(date != null ? date.getTime() : -1);
        dest.writeString(this.phoneNumber);
        dest.writeByte(phoneNumberConfirmed ? (byte) 1 : (byte) 0);
        dest.writeByte(insured ? (byte) 1 : (byte) 0);
        dest.writeString(this.author);
        dest.writeString(this.comment);
    }

    private RestRide(Parcel in)
    {
        this.id = in.readLong();
        this.fromCity = in.readString();
        this.fromCountry = in.readString();
        this.toCity = in.readString();
        this.toCountry = in.readString();
        this.price = (Float) in.readValue(Float.class.getClassLoader());
        this.numPeople = (Integer) in.readValue(Integer.class.getClassLoader());
        this.isFull = in.readByte() != 0;
        long tmpDate = in.readLong();
        this.date = tmpDate == -1 ? null : new Date(tmpDate);
        this.phoneNumber = in.readString();
        this.phoneNumberConfirmed = in.readByte() != 0;
        this.insured = in.readByte() != 0;
        this.author = in.readString();
        this.comment = in.readString();
    }

    public static Parcelable.Creator<RestRide> CREATOR = new Parcelable.Creator<RestRide>()
    {
        public RestRide createFromParcel(Parcel source)
        {
            return new RestRide(source);
        }

        public RestRide[] newArray(int size)
        {
            return new RestRide[size];
        }
    };
}
