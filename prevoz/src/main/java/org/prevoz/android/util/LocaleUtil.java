package org.prevoz.android.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import org.prevoz.android.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class LocaleUtil
{
    private static final HashMap<String, String> localizedCountryNamesCache = new HashMap<>();
    private static final HashMap<String, String> localizedCityNamesCache = new HashMap<>();
    private static final HashMap<String, SimpleDateFormat> dateFormatCache = new HashMap<>();

    private static final SimpleDateFormat timeFormatter = LocaleUtil.getSimpleDateFormat("HH:mm");
    private static Locale localeCache = null;
    private static TimeZone timezoneCache = null;

    public static String getFormattedTime(Calendar date)
    {
        return getFormattedTime(date.getTime());
    }

    public static String getFormattedTime(Date date)
    {
        return timeFormatter.format(date);
    }

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

    public static String getFormattedCurrency(double currency) {
        return String.format(getLocale(), "%1.1f €", currency);
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

        return LocaleUtil.getDayName(resources, date) + ", " + LocaleUtil.getFormattedDate(resources, date);
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
        if (timezoneCache == null) {
            timezoneCache = TimeZone.getTimeZone("Europe/Ljubljana");

            if (timezoneCache.getID().equals(TimeZone.getTimeZone("GMT").getID()))
            {
                return TimeZone.getDefault();
            }
        }

        return timezoneCache;
    }

    public static Locale getLocale()
    {
        if (localeCache == null)
        {
            localeCache = new Locale("sl");
        }
        return localeCache;
    }

    public static String getLocalizedCountryName(Context context, String countryCode)
    {
        if (!localizedCountryNamesCache.containsKey(countryCode))
        {
            localizedCountryNamesCache.put(countryCode, ContentUtils.getLocalCountryName(context, getLocale().getLanguage(), countryCode));
        }

        return localizedCountryNamesCache.get(countryCode);
    }

    public static String getLocalizedCityName(Context context, String cityName, String countryCode)
    {
        if (!localizedCityNamesCache.containsKey(cityName))
        {
            localizedCityNamesCache.put(cityName, ContentUtils.getLocalCityName(context, cityName) + (countryCode.equals(LocaleUtil.getCurrentCountryCode()) ? "" : " (" + countryCode + ")"));
        }

        return localizedCityNamesCache.get(cityName);
    }

    public static String getCurrentCountryCode()
    {
        return "SI";
    }

    public static void checkSetLocale(Context ctx, Configuration config)
    {
        Locale appLocale = LocaleUtil.getLocale();
        if (config.locale != appLocale)
        {
            Locale.setDefault(appLocale);
            config.locale = appLocale;
            ctx.getResources().updateConfiguration(config, ctx.getResources().getDisplayMetrics());
        }
    }

    public static String getNotificationDayName(Resources res, Calendar date)
    {
        String[] dayNames = res.getStringArray(R.array.notify_day_names);
        return dayNames[date.get(Calendar.DAY_OF_WEEK) - 1];
    }

    public static Calendar getMidnightCalendar(Calendar date)
    {
        Calendar newCalendar = Calendar.getInstance(LocaleUtil.getLocalTimezone());
        newCalendar.setTime(date.getTime());
        newCalendar.set(Calendar.HOUR_OF_DAY, 0);
        newCalendar.set(Calendar.MINUTE, 0);
        newCalendar.set(Calendar.SECOND, 0);
        newCalendar.set(Calendar.MILLISECOND, 0);

        return newCalendar;
    }
}
