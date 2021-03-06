package org.prevoz.android.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.prevoz.android.model.City;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil
{
    private static final String REGEX_CITY_NAME_MATCHER = "([\\p{L}\\. ]+)(\\(([A-Za-z][A-Za-z])\\))?";    // Used to match <CITY> (<COUNTRY CODE>) strings
    private static final Pattern CITY_NAME_PATTERN = Pattern.compile(REGEX_CITY_NAME_MATCHER);

    public static City splitStringToCity(String str)
    {
        Matcher m = CITY_NAME_PATTERN.matcher(str);
        if (m.find())
        {
            String cityName = m.group(1).trim();
            String country = LocaleUtil.getCurrentCountryCode();

            if (m.groupCount() > 2 && m.group(3) != null)
                country = m.group(3).trim();

            return new City(cityName, country);
        }

        return null;
    }

    @NonNull
    public static String toNonNull(@Nullable String string) {
        return string == null ? "" : string;
    }
}
