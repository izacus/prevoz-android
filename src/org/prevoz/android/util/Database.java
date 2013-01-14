package org.prevoz.android.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.prevoz.android.City;
import org.prevoz.android.R;
import org.prevoz.android.Route;
import org.prevoz.android.c2dm.NotifySubscription;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

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
			super(context, "settings.db", null, 11);
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
			if (oldVersion < 10)
			{
				db.execSQL("DROP TABLE IF EXISTS favorites");
				db.execSQL("DROP TABLE IF EXISTS search_history");
				
				onCreate(db);
			}
            else if (oldVersion <= 10)
            {
                db.execSQL("ALTER TABLE notify_subscriptions ADD COLUMN from_country TEXT DEFAULT 'SI'");
                db.execSQL("ALTER TABLE notify_subscriptions ADD COLUMN to_country TEXT DEFAULT 'SI'");
            }
		}
	}
	
	public static NotifySubscription getNotificationSubscription(Context context, City from, City to, Calendar date)
	{
		ArrayList<NotifySubscription> subscriptions = Database.getNotificationSubscriptions(context);
		
		for (NotifySubscription subscription : subscriptions)
		{
			if (subscription.getFrom().equals(from) &&
			    subscription.getTo().equals(to) &&
			    subscription.getDate().get(Calendar.DATE) == date.get(Calendar.DATE) &&
			    subscription.getDate().get(Calendar.MONTH) == date.get(Calendar.MONTH) &&
			    subscription.getDate().get(Calendar.YEAR) == date.get(Calendar.YEAR))
			{
				return subscription;
			}
		}
		
		return null;
	}
	
	public static ArrayList<NotifySubscription> getNotificationSubscriptions(Context context)
	{
		SQLiteDatabase database = new DatabaseHelper(context).getReadableDatabase();
		Cursor results = database.query("notify_subscriptions", new String[] { "id", "from_loc", "from_country", "to_loc", "to_country", "date" }, null, null, null, null, "registered_date");
		int idIndex = results.getColumnIndex("ID");
		int fromIndex = results.getColumnIndex("from_loc");
        int fromCountryIndex = results.getColumnIndex("from_country");
		int toIndex = results.getColumnIndex("to_loc");
        int toCountryIndex = results.getColumnIndex("to_country");
		int dateIndex = results.getColumnIndex("date");
		
		
		ArrayList<NotifySubscription> subscriptions = new ArrayList<NotifySubscription>();
		while (results.moveToNext())
		{
			Calendar date = Calendar.getInstance(LocaleUtil.getLocalTimezone());
			date.setTimeInMillis(results.getLong(dateIndex));
			NotifySubscription subscription = new NotifySubscription(results.getInt(idIndex),
																	 new City(results.getString(fromIndex), results.getString(fromCountryIndex)),
																	 new City(results.getString(toIndex), results.getString(toCountryIndex)),
																	 date);
			subscriptions.add(subscription);
		}
		
		results.close();
		database.close();
		return subscriptions;
	}
	
	public static void addNotificationSubscription(Context context, City from, City to, Calendar date)
	{
		SQLiteDatabase database = new DatabaseHelper(context).getWritableDatabase();
		ContentValues values  = new ContentValues();
		values.put("from_loc", from.getDisplayName());
        values.put("from_country", from.getCountryCode());
		values.put("to_loc", to.getDisplayName());
        values.put("to_country", to.getCountryCode());
		values.put("date", date.getTimeInMillis());
		values.put("registered_date", System.currentTimeMillis());
		database.insert("notify_subscriptions", null, values);
		database.close();
	}
	
	public static void deleteNotificationSubscription(Context context, int id)
	{
		SQLiteDatabase database = new DatabaseHelper(context).getWritableDatabase();
		database.delete("notify_subscriptions", "id = ?", new String[] { String.valueOf(id) });
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
			Route route = new Route(new City(results.getString(fromIndex), results.getString(fromCountry)), new City(results.getString(toIndex), results.getString(toCountry)), null);
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

	public static SQLiteDatabase getSettingsDatabase(Context context)
	{
		return new DatabaseHelper(context).getReadableDatabase();
	}
	
	/**
	 * Returns all cities starting with string
	 * 
	 * @param database
	 *            database on which to do query on (to prevent handle leaks)
	 * @param what
	 *            string to look for
	 * @return result cursos
	 */
	public static List<City> getCitiesStartingWith(SQLiteDatabase database, String what)
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
						+ "ORDER BY sort_num DESC", null);
		}
		
		ArrayList<City> cities = new ArrayList<City>();
		
		int nameIdx = cursor.getColumnIndex("name");
		int countryIdx = cursor.getColumnIndex("country");
		
		while (cursor.moveToNext())
		{
			cities.add(new City(cursor.getString(nameIdx), cursor.getString(countryIdx)));
		}
		
		cursor.close();
		return cities;
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
