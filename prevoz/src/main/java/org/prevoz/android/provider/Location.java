package org.prevoz.android.provider;

import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteColumn;
import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteType;

@StorIOSQLiteType(table = Location.TABLE)
public class Location
{
    public static final String TABLE = "location";
    public static final String NAME = "name";
    public static final String NAME_ASCII = "name_ascii";
    public static final String COUNTRY = "country";
    public static final String SORT_NUMBER = "sort";

    @StorIOSQLiteColumn(name = "_id", key = true)
    Long id;

    @StorIOSQLiteColumn(name = NAME)
    String name;

    @StorIOSQLiteColumn(name = NAME_ASCII)
    String nameAscii;

    @StorIOSQLiteColumn(name = COUNTRY)
    String country;

    @StorIOSQLiteColumn(name="lat")
    float latitude;

    @StorIOSQLiteColumn(name="lng")
    float longtitude;

    @StorIOSQLiteColumn(name = SORT_NUMBER)
    int sort;

    Location() {}

    public Location(String name, String nameAscii, String country, float latitude, float longtitude, int sort) {
        this.name = name;
        this.nameAscii = nameAscii;
        this.country = country;
        this.latitude = latitude;
        this.longtitude = longtitude;
        this.sort = sort;
    }

    public String getName() {
        return name;
    }

    public String getNameAscii() {
        return nameAscii;
    }

    public String getCountry() {
        return country;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongtitude() {
        return longtitude;
    }

    public int getSort() {
        return sort;
    }
}
