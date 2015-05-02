package org.prevoz.android.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;

import org.prevoz.android.R;
import org.prevoz.android.model.City;
import org.prevoz.android.model.NotificationSubscription;
import org.prevoz.android.model.Route;
import org.prevoz.android.provider.Country;
import org.prevoz.android.provider.Location;
import org.prevoz.android.provider.Notification;
import org.prevoz.android.provider.SearchHistoryItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ContentUtils
{
    private static final int CONTENT_DB_VERSION = 3;
    private static final String PREF_CONTENT_DB_VERSION = "content_db_ver";
    private static final String LOG_TAG = "Prevoz.Content";

    private static final String[] CITY_CURSOR_COLUMNS = { Location._ID, Location.NAME, Location.COUNTRY };

    public static void importDatabase(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getInt(PREF_CONTENT_DB_VERSION, -1) == CONTENT_DB_VERSION)
            return;

        ContentResolver resolver = context.getContentResolver();
        Log.d(LOG_TAG, "Inserting data into content provider.....");
        resolver.delete(Location.CONTENT_URI, null, null);
        resolver.delete(Country.CONTENT_URI, null, null);

        // Cities
        {
            String[] lines = FileUtil.readLines(context, R.raw.locations);
            ContentValues[] values = new ContentValues[lines.length];
            int i = 0;
            for (String line : lines)
            {
                String[] tokens = line.split(",");
                ContentValues value = new ContentValues();
                value.put(Location.SORT_NUMBER, Integer.parseInt(tokens[0]));
                value.put(Location.NAME, tokens[1]);
                value.put(Location.NAME_ASCII, tokens[2]);
                value.put(Location.COUNTRY, tokens[3]);
                value.put(Location.LATITUDE, Float.parseFloat(tokens[4]));
                value.put(Location.LONGTITUDE, Float.parseFloat(tokens[5]));
                values[i++] = value;
            }

            resolver.bulkInsert(Location.CONTENT_URI, values);
        }

        // Countries
        {
            String[] lines = FileUtil.readLines(context, R.raw.countries);
            ContentValues[] values = new ContentValues[lines.length];
            int i = 0;
            for (String line : lines)
            {
                String[] tokens = line.split(",");
                ContentValues value = new ContentValues();
                value.put(Country.COUNTRY_CODE, tokens[0]);
                value.put(Country.NAME, tokens[1]);
                value.put(Country.LANGUAGE, tokens[2]);
                values[i++] = value;
            }

            resolver.bulkInsert(Country.CONTENT_URI, values);
        }

        resolver.notifyChange(Location.CONTENT_URI, null);
        resolver.notifyChange(Country.CONTENT_URI, null);
        prefs.edit().putInt(PREF_CONTENT_DB_VERSION, CONTENT_DB_VERSION).apply();

        Log.d(LOG_TAG, "Insert done!");
    }

    public static String getLocalCityName(Context context, String city)
    {
        ContentResolver resolver = context.getContentResolver();
        Cursor result = resolver.query(Location.CONTENT_URI,
                new String[] { Location.NAME },
                Location.NAME_ASCII + " = ?",
                new String[] { city },
                null);

        String name = city;
        if (result.moveToFirst())
        {
            int ci = result.getColumnIndex(Location.NAME);
            name = result.getString(ci);
        }

        result.close();
        return name;
    }

    public static String getLocalCountryName(Context context, String locale, String countryCode)
    {
        // TODO: Update database with proper parameters
        if (locale.equalsIgnoreCase("sl"))
            locale = "sl-si";

        String name = countryCode;
        ContentResolver resolver = context.getContentResolver();
        Cursor result = resolver.query(Country.CONTENT_URI,
                                       new String[] { Country.NAME },
                                       Country.LANGUAGE + " = ? AND " + Country.COUNTRY_CODE + " = ?",
                                       new String[] { locale, countryCode },
                                       null);

        int nameIdx = result.getColumnIndex("name");
        if (result.moveToFirst())
        {
            name = result.getString(nameIdx);
        }
        else
        {
            Log.e("GetLocalCountryName", "No entry found for " + countryCode + " for language " + locale);
        }

        result.close();
        return name;
    }

    public static boolean doesCityExist(Context context, String city)
    {
        ContentResolver resolver = context.getContentResolver();
        Cursor c = resolver.query(Location.CONTENT_URI,
                                  new String[] {},
                                  Location.NAME + " = ?",
                                  new String[] { city },
                                  null);

        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    public static Cursor getCityCursor(Context ctx, String constraint, String country)
    {
        ContentResolver contentResolver = ctx.getContentResolver();

        if (constraint == null || constraint.trim().length() == 0)
        {
            return contentResolver.query(Location.CONTENT_URI,
                    CITY_CURSOR_COLUMNS,
                    null,
                    null,
                    Location.SORT_NUMBER + " DESC");
        }
        else
        {
            // Unforunately arguments do not work with LIKE filter
            String query = Location.NAME + " LIKE '" + constraint + "%' OR " + Location.NAME_ASCII + " LIKE '" + constraint + "%'";
            String[] params = null;

            if (country != null)
            {
                query = Location.COUNTRY + " = ? AND " + query;
                params = new String[] { country };
            }

            return contentResolver.query(Location.CONTENT_URI,
                    CITY_CURSOR_COLUMNS,
                    query,
                    params,
                    Location.SORT_NUMBER + " DESC");
        }
    }

    public static void addSearchToHistory(Context context, City from, City to, Date date)
    {
        ContentResolver resolver = context.getContentResolver();

        String fcity = from == null ? "" : from.getDisplayName();
        String fcountry = from == null ? "" : from.getCountryCode();
        String tcity = to == null ? "" : to.getDisplayName();
        String tcountry = to == null ? "" : to.getCountryCode();
        long time = date.getTime();

        // Check if entry exists
        Cursor existing = resolver.query(SearchHistoryItem.CONTENT_URI,
                                         new String[] { SearchHistoryItem._ID },
                                         SearchHistoryItem.FROM_CITY + " = ? AND " + SearchHistoryItem.FROM_COUNTRY + " = ? AND " +
                                         SearchHistoryItem.TO_CITY + " = ? AND " + SearchHistoryItem.TO_COUNTRY + " = ? ",
                                         new String[] { fcity, fcountry, tcity, tcountry }, null);

        ContentValues values = new ContentValues();
        values.put(SearchHistoryItem.FROM_CITY, fcity);
        values.put(SearchHistoryItem.FROM_COUNTRY, fcountry);
        values.put(SearchHistoryItem.TO_CITY, tcity);
        values.put(SearchHistoryItem.TO_COUNTRY, tcountry);
        values.put(SearchHistoryItem.DATE, time);

        if (existing.getCount() > 0)
        {
            existing.moveToFirst();
            resolver.update(SearchHistoryItem.CONTENT_URI, values, SearchHistoryItem._ID + " = ?", new String[] { String.valueOf(existing.getLong(0)) });
        }
        else
        {
            resolver.insert(SearchHistoryItem.CONTENT_URI, values);
        }

        existing.close();
    }

    public static ArrayList<Route> getLastSearches(Context context, int count)
    {
        ContentResolver resolver = context.getContentResolver();
        Cursor results = resolver.query(SearchHistoryItem.CONTENT_URI,
                                        new String[] { SearchHistoryItem.FROM_CITY, SearchHistoryItem.FROM_COUNTRY, SearchHistoryItem.TO_CITY, SearchHistoryItem.TO_COUNTRY },
                                        null,
                                        null,
                                        SearchHistoryItem.DATE + " ASC");

        ArrayList<Route> searches = new ArrayList<>();

        int fromIndex = results.getColumnIndex(SearchHistoryItem.FROM_CITY);
        int fromCountry = results.getColumnIndex(SearchHistoryItem.FROM_COUNTRY);
        int toIndex = results.getColumnIndex(SearchHistoryItem.TO_CITY);
        int toCountry = results.getColumnIndex(SearchHistoryItem.TO_COUNTRY);

        for (int i = 0; i < count; i++)
        {
            if (!results.moveToNext())
                break;

            Route route = new Route(new City(results.getString(fromIndex), results.getString(fromCountry)), new City(results.getString(toIndex), results.getString(toCountry)));
            searches.add(route);
        }

        results.close();
        return searches;
    }

    public static void deleteOldHistoryEntries(Context context, int min)
    {
        ContentResolver resolver = context.getContentResolver();
        Cursor results = resolver.query(SearchHistoryItem.CONTENT_URI,
                                        new String[] { SearchHistoryItem._ID },
                                        null,
                                        null,
                                        SearchHistoryItem.DATE + " DESC");

        if (results.getCount() > min)
        {
            results.move(min);

            ArrayList<Integer> ids = new ArrayList<>();

            while (!results.isAfterLast())
            {
                ids.add(results.getInt(0));
                results.moveToNext();
            }

            for (Integer id : ids)
            {
                resolver.delete(SearchHistoryItem.CONTENT_URI, SearchHistoryItem._ID + " = ?", new String[] { String.valueOf(id) });
            }
        }

        results.close();
    }

    public static boolean isSubscribedForNotification(Context context, City from, City to, Calendar date)
    {
        ContentResolver resolver = context.getContentResolver();

        Cursor results = resolver.query(Notification.CONTENT_URI,
                                        new String[] { "COUNT(" + SearchHistoryItem._ID + ") AS count" },
                                        Notification.FROM_CITY + " = ? AND " + Notification.FROM_COUNTRY + " = ? AND " +
                                        Notification.TO_CITY + " = ? AND " + Notification.TO_COUNTRY + " = ? AND " +
                                        Notification.DATE + " = ?",
                                        new String[] { from.getDisplayName(), from.getCountryCode(),
                                                       to.getDisplayName(), to.getCountryCode(),
                                                       String.valueOf(LocaleUtil.getMidnightCalendar(date).getTimeInMillis())},
                                        null);

        results.moveToFirst();
        boolean isSubscribed = results.getInt(0) > 0;
        results.close();
        
        return isSubscribed;
    }

    public static ArrayList<NotificationSubscription> getNotificationSubscriptions(Context context)
    {
        ContentResolver resolver = context.getContentResolver();
        Cursor results = resolver.query(Notification.CONTENT_URI, null, null, null, Notification.REGISTERED_DATE + " DESC");
        int idIndex = results.getColumnIndex(Notification._ID);
        int fromIndex = results.getColumnIndex(Notification.FROM_CITY);
        int fromCountryIndex = results.getColumnIndex(Notification.FROM_COUNTRY);
        int toIndex = results.getColumnIndex(Notification.TO_CITY);
        int toCountryIndex = results.getColumnIndex(Notification.TO_COUNTRY);
        int dateIndex = results.getColumnIndex(Notification.DATE);

        ArrayList<NotificationSubscription> subscriptions = new ArrayList<>();
        while (results.moveToNext())
        {
            Calendar date = Calendar.getInstance(LocaleUtil.getLocalTimezone());
            date.setTimeInMillis(results.getLong(dateIndex));
            NotificationSubscription subscription = new NotificationSubscription(results.getInt(idIndex),
                    new City(results.getString(fromIndex), results.getString(fromCountryIndex)),
                    new City(results.getString(toIndex), results.getString(toCountryIndex)),
                    date);
            subscriptions.add(subscription);
        }

        results.close();
        return subscriptions;
    }

    public static void addNotificationSubscription(Context context, City from, City to, Calendar date)
    {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values  = new ContentValues();

        values.put(Notification.FROM_CITY, from.getDisplayName());
        values.put(Notification.FROM_COUNTRY, from.getCountryCode());
        values.put(Notification.TO_CITY, to.getDisplayName());
        values.put(Notification.TO_COUNTRY, to.getCountryCode());
        values.put(Notification.DATE, LocaleUtil.getMidnightCalendar(date).getTimeInMillis());
        resolver.insert(Notification.CONTENT_URI, values);
    }

    public static void deleteNotificationSubscription(Context context, City from, City to, Calendar date)
    {
        ContentResolver resolver = context.getContentResolver();
        resolver.delete(Notification.CONTENT_URI,
                        Notification.FROM_CITY + " = ? AND " + Notification.FROM_COUNTRY + " = ? AND " +
                        Notification.TO_CITY + " = ? AND " + Notification.TO_COUNTRY + " = ? AND " +
                        Notification.DATE + " = ?",
                        new String[] { from.getDisplayName(), from.getCountryCode(),
                                to.getDisplayName(), to.getCountryCode(),
                                String.valueOf(LocaleUtil.getMidnightCalendar(date).getTimeInMillis())});
    }

    public static void pruneOldNotifications(Context context)
    {
        ContentResolver resolver = context.getContentResolver();

        Calendar time = LocaleUtil.getMidnightCalendar(Calendar.getInstance(LocaleUtil.getLocalTimezone()));
        resolver.delete(Notification.CONTENT_URI, Notification.DATE + " < ?", new String[] { String.valueOf(time.getTimeInMillis()) });
    }
}
