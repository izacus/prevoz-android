package org.prevoz.android.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import org.prevoz.android.R;
import org.prevoz.android.model.PrevozDatabase;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.HashMap;
import java.util.Locale;

public class LocaleUtil
{
    private static final HashMap<String, String> localizedCountryNamesCache = new HashMap<>();
    private static final HashMap<String, String> localizedCityNamesCache = new HashMap<>();

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private static Locale localeCache = null;
    private static ZoneId timezoneCache = null;

    public static String getFormattedTime(ZonedDateTime date) {
        return timeFormatter.format(date);
    }

    public static String getDayName(Resources res, ZonedDateTime date)
    {
        String[] dayNames = res.getStringArray(R.array.day_names);
        return dayNames[date.getDayOfWeek().getValue() - 1];
    }

    public static String getShortDayName(Resources res, ZonedDateTime date)
    {
        String[] shortDayNames = res.getStringArray(R.array.short_day_names);
        return shortDayNames[date.getDayOfWeek().getValue() - 1];
    }

    public static String getFormattedDate(Resources res, ZonedDateTime date)
    {
        String[] monthNames = res.getStringArray(R.array.month_names);
        return date.getDayOfMonth() + ". "
                + monthNames[date.getMonthValue() - 1] + " "
                + date.getYear();
    }

    public static String getFormattedCurrency(double currency) {
        return String.format(getLocale(), "%1.1f €", currency);
    }

    public static String getShortFormattedDate(Resources res, ZonedDateTime date)
    {
        return getShortDayName(res, date) + ", " + date.getDayOfMonth() + ". " + date.getMonthValue() + ".";
    }

    /**
     * Builds a localized date string with day name
     */
    public static String localizeDate(Resources resources, ZonedDateTime date)
    {
        ZonedDateTime today = LocalDate.now().atStartOfDay(LocaleUtil.getLocalTimezone());
        ZonedDateTime tomorrow = LocalDate.now().atStartOfDay(LocaleUtil.getLocalTimezone()).plus(1, ChronoUnit.DAYS);

        if (date.isAfter(today) && date.isBefore(tomorrow) || date.isEqual(today)) {
            return resources.getString(R.string.today);
        } else if (date.isAfter(tomorrow) && date.isBefore(tomorrow.plus(1, ChronoUnit.DAYS)) || date.isEqual(tomorrow)) {
            return resources.getString(R.string.tomorrow);
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

    public static ZoneId getLocalTimezone()
    {
        if (timezoneCache == null) {
            timezoneCache = ZoneId.of("Europe/Ljubljana");
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

    public static String getLocalizedCountryName(PrevozDatabase database, String countryCode)
    {
        if (!localizedCountryNamesCache.containsKey(countryCode))
        {
            localizedCountryNamesCache.put(countryCode, database.getLocalCountryName(getLocale().getLanguage(), countryCode).toBlocking().value());
        }

        return localizedCountryNamesCache.get(countryCode);
    }

    @NonNull public static String getLocalizedCityName(PrevozDatabase database, String cityName, String countryCode)
    {
        if (!localizedCityNamesCache.containsKey(cityName))
        {
            localizedCityNamesCache.put(cityName, database.getLocalCityName(cityName).toBlocking().value() + (countryCode.equals(LocaleUtil.getCurrentCountryCode()) ? "" : " (" + countryCode + ")"));
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

    public static String getNotificationDayName(Resources res, LocalDate date)
    {
        String[] dayNames = res.getStringArray(R.array.notify_day_names);
        return dayNames[date.getDayOfWeek().getValue() - 1];
    }
}
