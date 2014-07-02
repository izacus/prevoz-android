package org.prevoz.android.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;
import org.prevoz.android.R;
import org.prevoz.android.model.City;
import org.prevoz.android.model.Route;
import org.prevoz.android.provider.Country;
import org.prevoz.android.provider.Location;
import org.prevoz.android.provider.SearchHistoryItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ContentUtils
{
    private static final int CONTENT_DB_VERSION = 1;
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

        if (constraint == null || constraint.toString().trim().length() == 0)
        {
            return contentResolver.query(Location.CONTENT_URI,
                    CITY_CURSOR_COLUMNS,
                    null,
                    null,
                    Location.SORT_NUMBER + " DESC");
        }
        else
        {
            String cs = constraint.toString();

            // Unforunately arguments do not work with LIKE filter
            String query = Location.NAME + " LIKE '" + cs + "%' OR " + Location.NAME_ASCII + " LIKE '" + cs + "%'";
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
        Log.i("Database","Adding search to history " + from + " - " + to);

        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(SearchHistoryItem.FROM_CITY, from == null ? null : from.getDisplayName());
        values.put(SearchHistoryItem.FROM_COUNTRY, from == null ? null : from.getCountryCode());
        values.put(SearchHistoryItem.TO_CITY, to == null ? null : to.getDisplayName());
        values.put(SearchHistoryItem.TO_COUNTRY, to == null ? null : to.getCountryCode());
        values.put(SearchHistoryItem.DATE, date.getTime());
        resolver.insert(SearchHistoryItem.CONTENT_URI, values);
    }

    public static ArrayList<Route> getLastSearches(Context context, int count)
    {
        ContentResolver resolver = context.getContentResolver();
        Cursor results = resolver.query(SearchHistoryItem.CONTENT_URI,
                                        new String[] { SearchHistoryItem.FROM_CITY, SearchHistoryItem.FROM_COUNTRY, SearchHistoryItem.TO_CITY, SearchHistoryItem.TO_COUNTRY },
                                        null,
                                        null,
                                        SearchHistoryItem.DATE + " DESC");

        ArrayList<Route> searches = new ArrayList<Route>();

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
            Log.i("Database", "Deleting old last search entries.");
            results.move(min);

            ArrayList<Integer> ids = new ArrayList<Integer>();

            while (!results.isAfterLast())
            {
                ids.add(results.getInt(0));
                results.moveToNext();
            }

            for (Integer id : ids)
            {
                resolver.delete(SearchHistoryItem.CONTENT_URI, "id = ?", new String[] { String.valueOf(id) });
            }
        }

        results.close();
    }
}
