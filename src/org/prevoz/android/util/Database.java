package org.prevoz.android.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

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
			super(context, "settings.db", null, 7);
			this.storedContext = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE search_history (ID INTEGER PRIMARY KEY AUTOINCREMENT," +
					   "from_loc TEXT NOT NULL, " +
					   "to_loc TEXT NOT NULL, " +
					   "date DATE NOT NULL)");
			
			db.execSQL("CREATE TABLE notify_subscriptions (ID INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ "from_loc TEXT NOT NULL," 
					+ "to_loc TEXT NOT NULL,"
					+ "date INTEGER NOT NULL, "
					+ "registered_date INTEGER NOT NULL)");

			// Load SQL location script from raw folder and insert all relevant
			// data into database
			String[] locationSQL = FileUtil.readSQLStatements(storedContext,
					R.raw.locations);

			for (String statement : locationSQL)
			{
				if (statement.contains("nomelj"))
				{
					Log.d("", "");
				}

				db.execSQL(statement);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			if (oldVersion > 3)
			{
				db.execSQL("DROP TABLE locations");
			}
			
			if (oldVersion < 7)
			{
				db.execSQL("DROP TABLE favorites");
			}
			
			if (oldVersion > 5)
			{
				db.execSQL("DROP TABLE search_history");
			}

			onCreate(db);
		}
	}
	
	public static NotifySubscription getNotificationSubscription(Context context, String from, String to, Calendar date)
	{
		ArrayList<NotifySubscription> subscriptions = Database.getNotificationSubscriptions(context);
		
		for (NotifySubscription subscription : subscriptions)
		{
			if (subscription.getFrom().equalsIgnoreCase(from) && 
			    subscription.getTo().equalsIgnoreCase(to) &&
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
		Cursor results = database.query("notify_subscriptions", new String[] { "id", "from_loc", "to_loc", "date" }, null, null, null, null, "registered_date");
		int idIndex = results.getColumnIndex("ID");
		int fromIndex = results.getColumnIndex("from_loc");
		int toIndex = results.getColumnIndex("to_loc");
		int dateIndex = results.getColumnIndex("date");
		
		
		ArrayList<NotifySubscription> subscriptions = new ArrayList<NotifySubscription>();
		while (results.moveToNext())
		{
			Calendar date = Calendar.getInstance();
			date.setTimeInMillis(results.getLong(dateIndex));
			NotifySubscription subscription = new NotifySubscription(results.getInt(idIndex),
																	 results.getString(fromIndex),
																	 results.getString(toIndex),
																	 date);
			subscriptions.add(subscription);
		}
		
		results.close();
		database.close();
		return subscriptions;
	}
	
	public static void addNotificationSubscription(Context context, String from, String to, Calendar date)
	{
		SQLiteDatabase database = new DatabaseHelper(context).getWritableDatabase();
		ContentValues values  = new ContentValues();
		values.put("from_loc", from);
		values.put("to_loc", to);
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
		Calendar time = Calendar.getInstance();
		time.set(Calendar.HOUR_OF_DAY, 0);
		time.set(Calendar.MINUTE, 0);
		time.set(Calendar.SECOND, 0);
		database.delete("notify_subscriptions", "date < ?", new String[] { String.valueOf(time.getTimeInMillis()) });
		database.close();
	}
	
	public static void addSearchToHistory(Context context, String from, String to, Date date)
	{
		Log.i("Database","Adding search to history " + from + " - " + to);
		
		SimpleDateFormat sqlDateFormatter = LocaleUtil.getSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SQLiteDatabase database = new DatabaseHelper(context).getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("from_loc", from);
		values.put("to_loc", to);
		values.put("date", sqlDateFormatter.format(date));
		database.insert("search_history", null, values);
		database.close();
	}
	
	public static ArrayList<Route> getLastSearches(Context context, int count)
	{
		Log.i("Database", "Retrieving last " + count + " search records...");
		
		SQLiteDatabase database = new DatabaseHelper(context).getReadableDatabase();
		
		ArrayList<Route> searches = new ArrayList<Route>();
		Cursor results = database.query(true, "search_history", new String[] { "from_loc", "to_loc" }, null, null, null, null, "date DESC", String.valueOf(count));
		
		int fromIndex = results.getColumnIndex("from_loc");
		int toIndex = results.getColumnIndex("to_loc");
		
		while(results.moveToNext())
		{
			Route route = new Route(results.getString(fromIndex), results.getString(toIndex), null);
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
	public static Cursor getCitiesStartingWith(SQLiteDatabase database,
			String what)
	{
		// There's an Android bug when using pre-built queries with LIKE so
		// rawQuery has to be done

		Cursor cursor = database.rawQuery(
				"SELECT _id, name FROM locations WHERE (name LIKE '" + what
						+ "%' " + "OR name_ascii LIKE '" + what + "%') "
						+ "AND country = 'SI' ORDER BY name", null);

		return cursor;
	}

	public static String getClosestCity(Context context, double latitude,
			double longtitude)
	{
		SQLiteDatabase database = new DatabaseHelper(context)
				.getReadableDatabase();

		String query = "SELECT name, ABS(lat - " + latitude + ") + ABS(long - "
				+ longtitude + ") AS distance " + "FROM locations "
				+ "ORDER BY distance LIMIT 1";

		SQLiteCursor cursor = (SQLiteCursor) database.rawQuery(query, null);
		cursor.moveToFirst();

		int nameColumn = cursor.getColumnIndex("name");
		String city = cursor.getString(nameColumn);

		database.close();

		return city;
	}
}
