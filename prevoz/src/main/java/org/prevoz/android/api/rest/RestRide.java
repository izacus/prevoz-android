package org.prevoz.android.api.rest;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.prevoz.android.model.Bookmark;
import org.prevoz.android.util.LocaleUtil;

import java.io.Serializable;
import java.util.Calendar;

public class RestRide implements Comparable, Parcelable, Serializable
{
    @SerializedName("type")
    public int type = 0;        // 0 - share, 1 - seek

    @Nullable
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

    @Nullable
    @SerializedName("price")
    public Float price;

    @Nullable
    @SerializedName("num_people")
    public Integer numPeople;
    @SerializedName("full")
    public boolean isFull;

    @SerializedName("date_iso8601")
    public Calendar date;

    @Nullable
    @SerializedName("added")
    public Calendar published;

    @Nullable
    @SerializedName("contact")
    public String phoneNumber;

    @SerializedName("confirmed_contact")
    public boolean phoneNumberConfirmed;
    @SerializedName("insured")
    public boolean insured;

    @Nullable
    @SerializedName("author")
    public String author;

    @Nullable
    @SerializedName("bookmark")
    public Bookmark bookmark;

    @Nullable
    @SerializedName("comment")
    public String comment;

    @SerializedName("is_author")
    public boolean isAuthor;

    @Nullable
    private String localizedFromCity;
    @Nullable
    private String localizedToCity;

    public RestRide(String fromCity, String fromCountry, String toCity, String toCountry, @Nullable Float price, @Nullable Integer numPeople, Calendar date, @Nullable String phoneNumber, boolean insured, @Nullable String comment)
    {
        this.id = null;
        this.fromCity = fromCity;
        this.fromCountry = fromCountry;
        this.toCity = toCity;
        this.toCountry = toCountry;
        this.price = price;
        this.numPeople = numPeople;
        this.date = date;
        this.phoneNumber = phoneNumber;
        this.phoneNumberConfirmed = true;
        this.isAuthor = true;
        this.insured = insured;
        this.comment = comment;
    }

    public String getLocalizedFrom(Context ctx) {
        if (localizedFromCity == null)
            localizedFromCity = LocaleUtil.getLocalizedCityName(ctx, fromCity, fromCountry);
        return localizedFromCity;
    }

    public String getLocalizedTo(Context ctx) {
        if (localizedToCity == null)
            localizedToCity = LocaleUtil.getLocalizedCityName(ctx, toCity, toCountry);
        return localizedToCity;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof RestRide)) return false;

        RestRide restRide = (RestRide) o;
        return id.equals(restRide.id);
    }

    @Override
    public int hashCode()
    {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public int compareTo(@Nullable Object another)
    {
        if (!(another instanceof RestRide))
            return 0;

        final RestRide other = (RestRide) another;
        final int cityNameCompare = (fromCity + toCity).compareTo(other.fromCity + other.toCity);
        if (cityNameCompare == 0) {
            return date.compareTo(other.date);
        }

        return cityNameCompare;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeLong(this.id == null ? -1 : this.id);
        dest.writeString(this.fromCity);
        dest.writeString(this.fromCountry);
        dest.writeString(this.toCity);
        dest.writeString(this.toCountry);
        dest.writeValue(this.price);
        dest.writeValue(this.numPeople);
        dest.writeByte(isFull ? (byte) 1 : (byte) 0);
        dest.writeLong(date != null ? date.getTimeInMillis() : -1);
        dest.writeString(this.phoneNumber);
        dest.writeByte(phoneNumberConfirmed ? (byte) 1 : (byte) 0);
        dest.writeByte(insured ? (byte) 1 : (byte) 0);
        dest.writeString(this.author);
        dest.writeString(this.comment);
        dest.writeInt(this.bookmark == null ? -1 : this.bookmark.ordinal());
    }

    private RestRide(Parcel in)
    {
        this.id = in.readLong();
        if (id == -1) id = null;

        this.fromCity = in.readString();
        this.fromCountry = in.readString();
        this.toCity = in.readString();
        this.toCountry = in.readString();
        this.price = (Float) in.readValue(Float.class.getClassLoader());
        this.numPeople = (Integer) in.readValue(Integer.class.getClassLoader());
        this.isFull = in.readByte() != 0;
        long tmpDate = in.readLong();

        if (tmpDate > 0)
        {
            Calendar calendar = Calendar.getInstance(LocaleUtil.getLocalTimezone());
            calendar.setTimeInMillis(tmpDate);
            this.date = calendar;
        }

        this.phoneNumber = in.readString();
        this.phoneNumberConfirmed = in.readByte() != 0;
        this.insured = in.readByte() != 0;
        this.author = in.readString();
        this.comment = in.readString();

        int bookmarkInt = in.readInt();
        this.bookmark = bookmarkInt == -1 ? null : Bookmark.values()[bookmarkInt];
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
