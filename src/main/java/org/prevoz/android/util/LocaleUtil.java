package org.prevoz.android.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import org.prevoz.android.R;

import android.content.Context;
import android.content.res.Resources;

public class LocaleUtil
{
    private static HashMap<String, String> localizedCountryNamesCache = new HashMap<String, String>();
    private static HashMap<String, String> localizedCityNamesCache = new HashMap<String, String>();
    private static Locale localeCache = null;
    private static HashMap<String, SimpleDateFormat> dateFormatCache = new HashMap<String, SimpleDateFormat>();

    public static String getDayName(Resources res, Calendar date)
    {
        String[] dayNames = res.getStringArray(R.array.day_names);
        return dayNames[date.get(Calendar.DAY_OF_WEEK) - 1];
    }

    public static String getShortDayName(Resources res, Calendar date)
    {
        String[] shortDayNames = res.getStringArray(R.array.short_day_names);
        return shortDayNames[date.get(Calendar.DAY_OF_WEEK) - 1];
    }

    public static String getFormattedDate(Resources res, Calendar date)
    {
        String[] monthNames = res.getStringArray(R.array.month_names);
        return date.get(Calendar.DATE) + ". "
                + monthNames[date.get(Calendar.MONTH)] + " "
                + date.get(Calendar.YEAR);
    }

    public static String getShortFormattedDate(Resources res, Calendar date)
    {
        return getShortDayName(res, date) + ", " + date.get(Calendar.DATE) + ". " + (date.get(Calendar.MONTH) + 1) + ".";
    }

    /**
     * Builds a localized date string with day name
     */
    public static String localizeDate(Resources resources, Calendar date)
    {
        Calendar now = Calendar.getInstance(LocaleUtil.getLocalTimezone());
        // Check for today and tomorrow
        if (date.get(Calendar.ERA) == now.get(Calendar.ERA) &&
                date.get(Calendar.YEAR) == now.get(Calendar.YEAR))
        {
            // Today
            if (date.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR))
            {
                return resources.getString(R.string.today);
            }

            // Add one day to now to get tomorrows date
            now.roll(Calendar.DAY_OF_YEAR, 1);

            // Tomorrow, because we added one day to now
            if (date.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR))
            {
                return resources.getString(R.string.tomorrow);
            }
        }

        StringBuilder dateString = new StringBuilder();

        dateString.append(LocaleUtil.getDayName(resources, date) + ", ");
        dateString.append(LocaleUtil.getFormattedDate(resources, date));

        return dateString.toString();
    }


    public static String getStringNumberForm(Resources res, int resourceArray,
                                             int number)
    {
        String[] wordArray = res.getStringArray(resourceArray);

        int mod = number % 100;

        switch (mod)
        {
            case 1:
                return wordArray[0];
            case 2:
                return wordArray[1];
            case 3:
            case 4:
                return wordArray[2];
            default:
                return wordArray[3];
        }
    }

    public static SimpleDateFormat getSimpleDateFormat(String format)
    {
        // Cache SimpleDateFormat since retrieving timezone data takes alot of time
        if (!dateFormatCache.containsKey(format))
        {
            SimpleDateFormat sdf = new SimpleDateFormat(format, getLocale());
            sdf.setTimeZone(getLocalTimezone());
            dateFormatCache.put(format, sdf);
        }

        return dateFormatCache.get(format);
    }

    public static TimeZone getLocalTimezone()
    {
        TimeZone tz = TimeZone.getTimeZone("Europe/Ljubljana");

        if (tz.getID().equals(TimeZone.getTimeZone("GMT").getID()))
        {
            return TimeZone.getDefault();
        }

        return tz;
    }

    public static Locale getLocale()
    {
        if (localeCache == null)
        {
            localeCache = new Locale("sl-SI");
        }
        return localeCache;
    }

    public static String getLocalizedCountryName(Context context, String countryCode)
    {
        if (!localizedCountryNamesCache.containsKey(countryCode))
        {
            localizedCountryNamesCache.put(countryCode, Database.getLocalCountryName(context, getLocale().getLanguage(), countryCode));
        }

        return localizedCountryNamesCache.get(countryCode);
    }

    public static String getLocalizedCityName(Context context, String cityName, String countryCode)
    {
        if (!localizedCityNamesCache.containsKey(cityName))
        {
            localizedCityNamesCache.put(cityName, Database.getLocalCityName(context, cityName));
        }

        return localizedCityNamesCache.get(cityName);
    }
}
