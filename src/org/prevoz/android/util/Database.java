package org.prevoz.android.util;

import java.util.ArrayList;
import java.util.HashMap;

import org.prevoz.android.Route;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Handles SQLite database operations
 * @author Jernej Virag
 *
 */
public class Database
{   
    private static class DatabaseHelper extends SQLiteOpenHelper
    {
	public DatabaseHelper(Context context)
	{
	    super(context, "settings.db", null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
    	   db.execSQL("CREATE TABLE favorites (ID INTEGER PRIMARY KEY AUTOINCREMENT," +
    			 		       "from_loc TEXT NOT NULL," +
    			 		       "to_loc TEXT NOT NULL)");
	    
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
	    // Nothing TBD
	}
    }
    
    
    public static void addFavorite(Context context, String from, String to)
    {
	Log.i("Database - AddFavorite", "Adding to favorites " + from + " - " + to);
	SQLiteDatabase database = new DatabaseHelper(context).getWritableDatabase();
	
	// Check if entry exists
	Cursor results = database.query("favorites", 
					new String[] { "from_loc", "to_loc" }, 
					"from_loc = ? AND to_loc = ?", 
					new String[] { from, to }, 
					null, 
					null, 
					null);
	
	if (results.getCount() > 0)
	{
	    Log.i("Database - AddFavorite", "Pair already exists, skipping add.");
	}
	else
	{
	    ContentValues values = new ContentValues();
	    values.put("from_loc", from);
	    values.put("to_loc", to);
	    database.insert("favorites", null, values);
	    
	    Log.i("Database - AddFavorite", "Inserted new value into database.");
	}
	
	results.close();
	database.close();
    }
    
    public static ArrayList<Route> getFavorites(Context context)
    {
	Log.i("Database - GetFavorites", "Retrieving list of favorites...");
	
	SQLiteDatabase database = new DatabaseHelper(context).getReadableDatabase();
	
	ArrayList<Route> routes = new ArrayList<Route>();
	
	Cursor results = database.query("favorites", 
					new String[] { "from_loc", "to_loc" }, 
					null, 
					null, 
					null, 
					null, 
					"from_loc");	// Order by
	
	if (results.getCount() > 0)
	{
	
        	int from_index = results.getColumnIndex("from_loc");
        	int to_index = results.getColumnIndex("to_loc");
        	
        	results.moveToFirst();
        	
        	while (!results.isAfterLast())
        	{
        	    routes.add(new Route(results.getString(from_index), results.getString(to_index)));
        	    results.moveToNext();
        	}
	
	}
	
	results.close();
	database.close();
	
	return routes;
    }
    
    public static void deleteFavorite(Context context, String from, String to)
    {
	Log.i("Database - DeleteFavorite", "Deleting " + from + " - " + to);
	
	SQLiteDatabase database = new DatabaseHelper(context).getWritableDatabase();
	database.delete("favorites", "from_loc = ? AND to_loc = ?", new String[] { from, to });
	database.close();
    }
}
