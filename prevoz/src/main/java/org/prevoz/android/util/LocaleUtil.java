package org.prevoz.android.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
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

    private final PrevozDatabase database;
    private final Resources res;

    public LocaleUtil(Resources resources, PrevozDatabase database) {
        this.res = resources;
        this.database = database;
    }

    public static String getFormattedTime(ZonedDateTime date) {
        return timeFormatter.format(date);
    }

    public String getDayName(ZonedDateTime date)
    {
        String[] dayNames = res.getStringArray(R.array.day_names);
        return dayNames[date.getDayOfWeek().getValue() - 1];
    }

    public String getShortDayName(ZonedDateTime date)
    {
        String[] shortDayNames = res.getStringArray(R.array.short_day_names);
        return shortDayNames[date.getDayOfWeek().getValue() - 1];
    }

    public String getFormattedDate(ZonedDateTime date)
    {
        String[] monthNames = res.getStringArray(R.array.month_names);
        return date.getDayOfMonth() + ". "
                + monthNames[date.getMonthValue() - 1] + " "
                + date.getYear();
    }

    public static String getFormattedCurrency(double currency) {
        return String.format(getLocale(), "%1.1f €", currency);
    }

    public String getShortFormattedDate(ZonedDateTime date)
    {
        return getShortDayName(date) + ", " + date.getDayOfMonth() + ". " + date.getMonthValue() + ".";
    }

    /**
     * Builds a localized date string with day name
     */
    public String localizeDate(ZonedDateTime date)
    {
        ZonedDateTime today = LocalDate.now().atStartOfDay(LocaleUtil.getLocalTimezone());
        ZonedDateTime tomorrow = LocalDate.now().atStartOfDay(LocaleUtil.getLocalTimezone()).plus(1, ChronoUnit.DAYS);

        if (date.isAfter(today) && date.isBefore(tomorrow) || date.isEqual(today)) {
            return res.getString(R.string.today);
        } else if (date.isAfter(tomorrow) && date.isBefore(tomorrow.plus(1, ChronoUnit.DAYS)) || date.isEqual(tomorrow)) {
            return res.getString(R.string.tomorrow);
        }

        return getDayName(date) + ", " + getFormattedDate(date);
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

    public String getLocalizedCountryName(String countryCode)
    {
        if (!localizedCountryNamesCache.containsKey(countryCode))
        {
            localizedCountryNamesCache.put(countryCode, database.getLocalCountryName(getLocale().getLanguage(), countryCode).toBlocking().value());
        }

        return localizedCountryNamesCache.get(countryCode);
    }

    @NonNull public String getLocalizedCityName(String cityName, String countryCode)
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
            Configuration conf = ctx.getResources().getConfiguration();
            updateConfiguration(conf, appLocale);
            ctx.getResources().updateConfiguration(conf, ctx.getResources().getDisplayMetrics());

            Configuration systemConf = Resources.getSystem().getConfiguration();
            updateConfiguration(systemConf, appLocale);
            Resources.getSystem().updateConfiguration(conf, ctx.getResources().getDisplayMetrics());
            Locale.setDefault(appLocale);
        }
    }

    private static void updateConfiguration(Configuration conf, Locale locale) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            conf.setLocale(locale);
        }else {
            //noinspection deprecation
            conf.locale = locale;
        }
    }

    public static String getNotificationDayName(Resources res, LocalDate date)
    {
        String[] dayNames = res.getStringArray(R.array.notify_day_names);
        return dayNames[date.getDayOfWeek().getValue() - 1];
    }
}
