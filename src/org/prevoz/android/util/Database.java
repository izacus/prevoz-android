package org.prevoz.android.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.prevoz.android.R;
import org.prevoz.android.Route;

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
			super(context, "settings.db", null, 6);
			this.storedContext = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE search_history (ID INTEGER PRIMARY KEY AUTOINCREMENT," +
					   "from_loc TEXT NOT NULL, " +
					   "to_loc TEXT NOT NULL, " +
					   "date DATE NOT NULL)");
			
			db.execSQL("CREATE TABLE favorites (ID INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "from_loc TEXT NOT NULL,"
					+ "to_loc TEXT NOT NULL,"
					+ "type INTEGER NOT NULL)");

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
			db.execSQL("DROP TABLE favorites");

			if (oldVersion > 3)
			{
				db.execSQL("DROP TABLE locations");
			}

			onCreate(db);
		}
	}
	
	public static void addSearchToHistory(Context context, String from, String to, Date date)
	{
		Log.i("Database","Adding search to history " + from + " - " + to);
		
		SimpleDateFormat sqlDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
