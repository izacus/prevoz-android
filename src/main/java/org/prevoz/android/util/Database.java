package org.prevoz.android.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import org.prevoz.android.R;
import org.prevoz.android.model.City;
import org.prevoz.android.model.NotificationSubscription;
import org.prevoz.android.model.Route;
import org.prevoz.android.provider.Location;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Handles SQLite database operations
 *
 * @author Jernej Virag
 *
 */
public class Database
{
/*    private static class DatabaseHelper extends SQLiteOpenHelper
    {
        private final Context storedContext;

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

            db.execSQL("CREATE TABLE IF NOT EXISTS notify_subscriptions (ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "from_loc TEXT NOT NULL,"
                    + "from_country TEXT NOT NULL,"
                    + "to_loc TEXT NOT NULL,"
                    + "to_country TEXT NOT NULL,"
                    + "date INTEGER NOT NULL, "
                    + "registered_date INTEGER NOT NULL)");

        }
    } */

    private static final SimpleDateFormat sqlDateFormatter = LocaleUtil.getSimpleDateFormat("yyyy-MM-dd 00:00:00");

/*    public static City getClosestCity(Context context, double latitude,
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
    } */


    public static boolean isSubscribedForNotification(Context context, City from, City to, Calendar date)
    {
        SimpleDateFormat sqlDateFormatter = LocaleUtil.getSimpleDateFormat("yyyy-MM-dd 00:00:00");
        SQLiteDatabase database = new DatabaseHelper(context).getReadableDatabase();

        Log.d("Database", "Checking for subscription: " + from + " " + to + " - " + date);

        try
        {
            Cursor results = database.query("notify_subscriptions",
                                            new String[] { "id", "from_loc", "from_country", "to_loc", "to_country", "date" },
                                            "from_loc = ? AND from_country = ? AND to_loc = ? AND to_country = ? AND date = ?",
                                            new String[] { from.getDisplayName(), from.getCountryCode(),
                                                           to.getDisplayName(), to.getCountryCode(),
                                                           sqlDateFormatter.format(date.getTime())},
                                            null, null, "registered_date");

            return results.getCount() > 0;
        }
        catch (SQLException e)
        {
            return false;
        }
        finally
        {
            database.close();
        }
    }

    public static ArrayList<NotificationSubscription> getNotificationSubscriptions(Context context)
    {
        SQLiteDatabase database = new DatabaseHelper(context).getReadableDatabase();
        try
        {
            Cursor results = database.query("notify_subscriptions", new String[] { "id", "from_loc", "from_country", "to_loc", "to_country", "date" }, null, null, null, null, "registered_date");
            int idIndex = results.getColumnIndex("ID");
            int fromIndex = results.getColumnIndex("from_loc");
            int fromCountryIndex = results.getColumnIndex("from_country");
            int toIndex = results.getColumnIndex("to_loc");
            int toCountryIndex = results.getColumnIndex("to_country");
            int dateIndex = results.getColumnIndex("date");


            ArrayList<NotificationSubscription> subscriptions = new ArrayList<NotificationSubscription>();
            while (results.moveToNext())
            {
                Calendar date = Calendar.getInstance(LocaleUtil.getLocalTimezone());
                date.setTime(sqlDateFormatter.parse(results.getString(dateIndex)));
                NotificationSubscription subscription = new NotificationSubscription(results.getInt(idIndex),
                        new City(results.getString(fromIndex), results.getString(fromCountryIndex)),
                        new City(results.getString(toIndex), results.getString(toCountryIndex)),
                        date);
                subscriptions.add(subscription);
            }

            results.close();
            database.close();
            return subscriptions;
        }
        catch (SQLException e)
        {
            database.close();
        }
        catch (ParseException e)
        {
            Log.e("Database", "Couldn't parse date in DB!", e);
            database.close();
        }

        return new ArrayList<NotificationSubscription>();
    }

    public static void addNotificationSubscription(Context context, City from, City to, Calendar date)
    {
        SQLiteDatabase database = new DatabaseHelper(context).getWritableDatabase();
        ContentValues values  = new ContentValues();

        Log.d("Database", "Adding subscription: " + from + " " + to + " - " + date);

        values.put("from_loc", from.getDisplayName());
        values.put("from_country", from.getCountryCode());
        values.put("to_loc", to.getDisplayName());
        values.put("to_country", to.getCountryCode());
        values.put("date",  sqlDateFormatter.format(date.getTime()));
        values.put("registered_date", System.currentTimeMillis());
        database.insert("notify_subscriptions", null, values);
        database.close();
    }

    public static void deleteNotificationSubscription(Context context, City from, City to, Calendar date)
    {
        SimpleDateFormat sqlDateFormatter = LocaleUtil.getSimpleDateFormat("yyyy-MM-dd 00:00:00");
        SQLiteDatabase database = new DatabaseHelper(context).getWritableDatabase();
        Log.d("Database", "Deleting subscription: " + from + " " + to + " - " + date);

        database.delete("notify_subscriptions",
                        "from_loc = ? AND from_country = ? AND to_loc = ? AND to_country = ? AND date = ?",
                        new String[] { from.getDisplayName(), from.getCountryCode(),
                                to.getDisplayName(), to.getCountryCode(),
                                sqlDateFormatter.format(date.getTime())});
        database.close();
    }

    public static void pruneOldNotifications(Context context)
    {
        SQLiteDatabase database = new DatabaseHelper(context).getWritableDatabase();
        Calendar time = Calendar.getInstance(LocaleUtil.getLocalTimezone());
        time.set(Calendar.HOUR_OF_DAY, 0);
        time.set(Calendar.MINUTE, 0);
        time.set(Calendar.SECOND, 0);
        database.delete("notify_subscriptions", "date < ?", new String[] { String.valueOf(time.getTimeInMillis()) });
        database.close();
    }
}