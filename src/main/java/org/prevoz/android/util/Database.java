package org.prevoz.android.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.database.SQLException;
import org.prevoz.android.R;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import org.prevoz.android.model.City;
import org.prevoz.android.model.Route;

/**
 * Handles SQLite database operations
 *
 * @author Jernej Virag
 *
 */
public class Database
{
    private static class DatabaseHelper extends SQLiteOpenHelper
    {
        private Context storedContext;

        public DatabaseHelper(Context context)
        {
            super(context, "settings.db", null, 14);
            this.storedContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL("CREATE TABLE IF NOT EXISTS search_history (ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "from_loc TEXT NOT NULL, " +
                    "from_country TEXT NOT NULL," +
                    "to_loc TEXT NOT NULL, " +
                    "to_country TEXT NOT NULL," +
                    "date DATE NOT NULL)");

            reloadCities(db);
            reloadCountries(db);
        }

        private void reloadCities(SQLiteDatabase db)
        {
            db.execSQL("DROP TABLE IF EXISTS locations");
            db.execSQL("DROP INDEX IF EXISTS name_ascii_index");
            db.execSQL("DROP INDEX IF EXISTS name_index");

            // Load SQL location script from raw folder and insert all relevant
            // data into database
            String[] locationSQL = FileUtil.readSQLStatements(storedContext, R.raw.locations);

            for (String statement : locationSQL)
            {
                Log.d(this.toString(), "SQL: " + statement);
                db.execSQL(statement);
            }
        }

        private void reloadCountries(SQLiteDatabase db)
        {
            db.execSQL("DROP TABLE IF EXISTS countries");
            db.execSQL("DROP INDEX IF EXISTS country_code_index");
            db.execSQL("DROP INDEX IF EXISTS country_language_index");

            // Load SQL location script from raw folder and insert all relevant
            // data into database
            String[] locationSQL = FileUtil.readSQLStatements(storedContext, R.raw.countries);

            for (String statement : locationSQL)
            {
                db.execSQL(statement);
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            if (oldVersion < 14)
            {
                if (oldVersion < 13)
                {
                    db.execSQL("DROP TABLE IF EXISTS search_history");
                }

                db.execSQL("DROP TABLE IF EXISTS notify_subscriptions");
                db.execSQL("DROP TABLE IF EXISTS favorites");
                onCreate(db);
            }
        }
    }

    public static synchronized SQLiteDatabase getSettingsDatabase(Context context)
    {
        return new DatabaseHelper(context).getReadableDatabase();
    }

    public static void addSearchToHistory(Context context, City from, City to, Date date)
    {
        Log.i("Database","Adding search to history " + from + " - " + to);

        SimpleDateFormat sqlDateFormatter = LocaleUtil.getSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SQLiteDatabase database = new DatabaseHelper(context).getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("from_loc", from == null ? "" : from.getDisplayName());
        values.put("from_country", from == null ? "" : from.getCountryCode());
        values.put("to_loc", to == null ? "" : to.getDisplayName());
        values.put("to_country", to == null ? "" : to.getCountryCode());
        values.put("date", sqlDateFormatter.format(date));
        database.insert("search_history", null, values);
        database.close();
    }

    public static ArrayList<Route> getLastSearches(Context context, int count)
    {
        Log.i("Database", "Retrieving last " + count + " search records...");

        SQLiteDatabase database = new DatabaseHelper(context).getReadableDatabase();

        ArrayList<Route> searches = new ArrayList<Route>();
        Cursor results = database.query(true, "search_history", new String[] { "from_loc", "from_country", "to_loc", "to_country" }, null, null, null, null, "date DESC", String.valueOf(count));

        int fromIndex = results.getColumnIndex("from_loc");
        int fromCountry = results.getColumnIndex("from_country");
        int toIndex = results.getColumnIndex("to_loc");
        int toCountry = results.getColumnIndex("to_country");

        while(results.moveToNext())
        {
            Route route = new Route(new City(results.getString(fromIndex), results.getString(fromCountry)), new City(results.getString(toIndex), results.getString(toCountry)));
            searches.add(route);
        }

        results.close();
        database.close();

        return searches;
    }


    public static void deleteHistoryEntries(Context context, int min)
    {
        SQLiteDatabase database = new DatabaseHelper(context).getWritableDatabase();

        Cursor results = database.query(true, "search_history", new String[] { "ID" }, null, null, null, null, "date DESC", null);

        if (results.getCount() > min)
        {
            Log.i("Database", "Deleting old last search entries.");

            results.move(min);

            ArrayList<Integer> ids = new ArrayList<Integer>();

            while(!results.isAfterLast())
            {
                ids.add(results.getInt(0));
                results.moveToNext();
            }

            for (Integer id : ids)
            {
                database.delete("search_history", "id = ?", new String[] { String.valueOf(id) });
            }
        }

        results.close();

        database.close();
    }



    public static boolean cityExists(SQLiteDatabase database, String city)
    {
        Cursor cursor = database.rawQuery("SELECT _id FROM locations WHERE name = ?", new String[] { city });
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public static Cursor getCityCursor(SQLiteDatabase database, String what, String country)
    {
        // There's an Android bug when using pre-built queries with LIKE so
        // rawQuery has to be done

        Cursor cursor = null;

        if (what.trim().equals(""))
        {
            cursor = database.rawQuery(
                    "SELECT _id, name, country FROM locations"
                            + " ORDER BY sort_num DESC", null);
        }
        else
        {
            cursor = database.rawQuery(
                    "SELECT _id, name, country FROM locations WHERE (name LIKE '" + what
                            + "%' " + "OR name_ascii LIKE '" + what + "%') "
                            + (country != null ? " AND country = '" + country + "' " : "")
                            + "ORDER BY sort_num DESC", null);
        }

        return cursor;
    }

    public static City getClosestCity(Context context, double latitude,
                                      double longtitude)
    {
        SQLiteDatabase database = new DatabaseHelper(context)
                .getReadableDatabase();

        String query = "SELECT name, country, ABS(lat - " + latitude + ") + ABS(long - "
                + longtitude + ") AS distance " + "FROM locations "
                + "ORDER BY distance LIMIT 1";

        SQLiteCursor cursor = (SQLiteCursor) database.rawQuery(query, null);
        cursor.moveToFirst();

        int nameColumn = cursor.getColumnIndex("name");
        int countryColumn = cursor.getColumnIndex("country");
        City city = new City(cursor.getString(nameColumn), cursor.getString(countryColumn));
        database.close();
        return city;
    }

    public static String getLocalCountryName(Context context, String locale, String countryCode)
    {
        String name = countryCode;

        SQLiteDatabase database = new DatabaseHelper(context).getReadableDatabase();
        Cursor result = database.query("countries", new String[] { "name" }, "language = ? AND country_code = ?", new String[] { locale, countryCode }, null, null, null);

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
        database.close();
        return name;
    }

    public static String getLocalCityName(Context context, String city)
    {
        SQLiteDatabase database = new DatabaseHelper(context).getReadableDatabase();
        Cursor result = database.query("locations", new String[] { "name" }, "name_ascii = ?", new String[] { city }, null, null, null);

        String name = city;
        if (result.moveToFirst())
        {
            int ci = result.getColumnIndex("name");
            name = result.getString(ci);
        }

        result.close();
        database.close();

        return name;
    }
}