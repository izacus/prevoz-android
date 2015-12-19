package org.prevoz.android.provider;

import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteColumn;
import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteType;

import org.prevoz.android.model.City;

@StorIOSQLiteType(table = SearchHistoryItem.TABLE)
public class SearchHistoryItem
{
    public static final String TABLE = "searchhistoryitem";
    public static final String FROM_CITY = "l_from";
    public static final String FROM_COUNTRY = "c_from";
    public static final String TO_CITY = "l_to";
    public static final String TO_COUNTRY = "c_to";
    public static final String SEARCH_DATE = "date";
    public static final String ID = "_id";

    @StorIOSQLiteColumn(name = ID, key = true)
    Long id;

    @StorIOSQLiteColumn(name =FROM_CITY)
    String fromCity;

    @StorIOSQLiteColumn(name=FROM_COUNTRY)
    String fromCountry;

    @StorIOSQLiteColumn(name=TO_CITY)
    String toCity;

    @StorIOSQLiteColumn(name= TO_COUNTRY)
    String toCountry;

    @StorIOSQLiteColumn(name=SEARCH_DATE)
    long date;

    SearchHistoryItem() {}

    public SearchHistoryItem(String fromCity, String fromCountry, String toCity, String toCountry, long date) {
        this.fromCity = fromCity;
        this.fromCountry = fromCountry;
        this.toCity = toCity;
        this.toCountry = toCountry;
        this.date = date;
    }

    public City getFrom() {
        return new City(fromCity, fromCountry);
    }

    public City getTo() {
        return new City(toCity, toCountry);
    }

    public void setDate(long date) {
        this.date = date;
    }
}
