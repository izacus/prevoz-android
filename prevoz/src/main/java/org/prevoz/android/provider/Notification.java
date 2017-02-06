package org.prevoz.android.provider;

import android.support.annotation.NonNull;

import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteColumn;
import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteType;

import org.prevoz.android.model.City;
import org.prevoz.android.model.Route;
import org.prevoz.android.util.LocaleUtil;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

@StorIOSQLiteType(table=Notification.TABLE)
public class Notification
{
    public static final String TABLE = "notification";
    public static final String FROM_CITY = "l_from";
    public static final String FROM_COUNTRY = "c_from";
    public static final String TO_CITY = "l_to";
    public static final String TO_COUNTRY = "c_to";
    public static final String DATE = "date";

    @StorIOSQLiteColumn(name = "_id", key = true)
    long id;

    @StorIOSQLiteColumn(name = FROM_CITY)
    String fromCity;

    @StorIOSQLiteColumn(name = FROM_COUNTRY)
    String fromCountry;

    @StorIOSQLiteColumn(name = TO_CITY)
    String toCity;

    @StorIOSQLiteColumn(name = TO_COUNTRY)
    String toCountry;

    @StorIOSQLiteColumn(name = DATE)
    long date;

    @StorIOSQLiteColumn(name="reg_date")
    long registrationDate;

    Notification() {}

    public Notification(String fromCity, String fromCountry, String toCity, String toCountry, long date) {
        this.fromCity = fromCity;
        this.fromCountry = fromCountry;
        this.toCity = toCity;
        this.toCountry = toCountry;
        this.date = date;
        this.registrationDate = LocalDate.now()
                                         .atStartOfDay(LocaleUtil.getLocalTimezone())
                                         .toEpochSecond() * 1000;
    }


    public long getId() {
        return id;
    }

    public String getFromCity() {
        return fromCity;
    }

    public String getFromCountry() {
        return fromCountry;
    }

    public String getToCity() {
        return toCity;
    }

    public String getToCountry() {
        return toCountry;
    }

    @NonNull
    public LocalDate getDate() {
        return LocalDate.from(Instant.ofEpochMilli(date).atZone(LocaleUtil.getLocalTimezone()));
    }

    @NonNull
    public Route getRoute() {
        return new Route(new City(fromCity, fromCountry), new City(toCity, toCountry));
    }

    public LocalDateTime getRegistrationDate() {
        return LocalDateTime.from(Instant.ofEpochMilli(date));
    }
}
