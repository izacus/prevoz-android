package org.prevoz.android.search;

import org.prevoz.android.util.Database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.TextView;

public class LocationAutocompleteAdapter extends CursorAdapter implements
		Filterable
{
	private SQLiteDatabase database = null;

	public LocationAutocompleteAdapter(Context context, Cursor c)
	{
		super(context, c);
		database = Database.getSettingsDatabase(context);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor)
	{
		int nameColumn = cursor.getColumnIndexOrThrow("name");
		String name = cursor.getString(nameColumn);

		((TextView) view).setText(name.toString());

		Log.d(this.toString(), "Name set to " + ((TextView) view).getText());
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent)
	{
		LayoutInflater inflater = LayoutInflater.from(context);
		// Create new simple dropdown item
		TextView dropDown = (TextView) inflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
		dropDown.setTextColor(Color.BLACK);

		return dropDown;
	}

	@Override
	public CharSequence convertToString(Cursor cursor)
	{
		int nameColumn = cursor.getColumnIndexOrThrow("name");
		String name = cursor.getString(nameColumn);

		return name;
	}

	@Override
	public Cursor runQueryOnBackgroundThread(CharSequence constraint)
	{
		Cursor cur = Database.getCitiesStartingWith(database, constraint.toString());
		Log.i(this.toString(), "Retrieved " + cur.getCount() + " records.");

		return cur;
	}
	
	
}
