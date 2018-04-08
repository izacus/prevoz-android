package org.prevoz.android.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.prevoz.android.util.LocaleUtil;

public class City implements Comparable<City>, Parcelable
{
    private final String displayName;
    private final String countryCode;

    public City(@NonNull String displayName, @NonNull String countryCode)
    {
        this.displayName = displayName;
        this.countryCode = countryCode;
    }

    @NonNull public String getDisplayName()
    {
        return displayName;
    }

    @NonNull public String getCountryCode()
    {
        return countryCode;
    }

    @NonNull public String getLocalizedName(LocaleUtil localeUtil)
    {
        return localeUtil.getLocalizedCityName(getDisplayName(), getCountryCode());
    }

    @Override
    public String toString()
    {
        return getDisplayName() + (countryCode.equals("SI") ? "" : " (" + getCountryCode() + ")");
    }

    @Override
    public int compareTo(@NonNull City another)
    {
        return getDisplayName().compareTo(another.getDisplayName());
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof City)) return false;
        City c = (City)o;
        return c.getDisplayName().equalsIgnoreCase(displayName) && c.getCountryCode().equalsIgnoreCase(countryCode);
    }

    @Override
    public int hashCode() {
        int result = displayName != null ? displayName.hashCode() : 0;
        result = 31 * result + (countryCode != null ? countryCode.hashCode() : 0);
        return result;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(this.displayName);
        dest.writeString(this.countryCode);
    }

    private City(Parcel in)
    {
        this.displayName = in.readString();
        this.countryCode = in.readString();
    }

    public static Creator<City> CREATOR = new Creator<City>()
    {
        public City createFromParcel(Parcel source)
        {
            return new City(source);
        }

        public City[] newArray(int size)
        {
            return new City[size];
        }
    };
}
