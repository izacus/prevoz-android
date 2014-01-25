package org.prevoz.android.search;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FilterQueryProvider;
import org.prevoz.android.util.Database;

public class CityAutocompleteAdapter extends SimpleCursorAdapter implements FilterQueryProvider, SimpleCursorAdapter.CursorToStringConverter
{
    private SQLiteDatabase db;


    public CityAutocompleteAdapter(Context context, SQLiteDatabase db)
    {
        super(context,
              android.R.layout.simple_dropdown_item_1line,
              Database.getCityCursor(db, ""),
              new String[] { "name" },
              new int[] { android.R.id.text1},
              0);

        this.db = db;
        setFilterQueryProvider(this);
        setCursorToStringConverter(this);
    }

    @Override
    public Cursor runQuery(CharSequence constraint)
    {
        return Database.getCityCursor(db, constraint == null ? "" : constraint.toString());
    }

    @Override
    public CharSequence convertToString(Cursor cursor)
    {
        int colIndex = cursor.getColumnIndex("name");
        return cursor.getString(colIndex);
    }
}
