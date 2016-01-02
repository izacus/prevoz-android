package org.prevoz.android.provider;

import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteColumn;
import com.pushtorefresh.storio.sqlite.annotations.StorIOSQLiteType;


@StorIOSQLiteType(table = Country.TABLE)
public class Country
{
    public static final String TABLE = "country";
    public static final String COUNTRY_CODE = "country_code";
    public static final String LANGUAGE = "lang";
    public static final String NAME = "name";

    @StorIOSQLiteColumn(name = "_id", key = true)
    Long id;

    @StorIOSQLiteColumn(name=COUNTRY_CODE)
    String countryCode;

    @StorIOSQLiteColumn(name=LANGUAGE)
    String language;

    @StorIOSQLiteColumn(name=NAME)
    String name;

    Country() {}

    public Country(String countryCode, String language, String name) {
        this.countryCode = countryCode;
        this.language = language;
        this.name = name;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getLanguage() {
        return language;
    }

    public String getName() {
        return name;
    }
}
