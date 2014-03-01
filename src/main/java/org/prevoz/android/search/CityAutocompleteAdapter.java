package org.prevoz.android.search;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FilterQueryProvider;
import android.widget.TextView;

import org.prevoz.android.R;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.LocaleUtil;

public class CityAutocompleteAdapter extends SimpleCursorAdapter implements FilterQueryProvider, SimpleCursorAdapter.CursorToStringConverter
{
    private SQLiteDatabase db;

    public CityAutocompleteAdapter(Context context, SQLiteDatabase db)
    {
        super(context,
              R.layout.item_autocomplete,
              Database.getCityCursor(db, "", null),
              new String[] { "name"},
              new int[] { R.id.city_name },
              0);

        this.db = db;
        setFilterQueryProvider(this);
        setCursorToStringConverter(this);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor)
    {
        super.bindView(view, context, cursor);
        int idx = cursor.getColumnIndex("country");

        TextView countryName = (TextView) view.findViewById(R.id.city_country);
        String countryCode = cursor.getString(idx);
        if (LocaleUtil.getCurrentCountryCode().equals(countryCode))
        {
            countryName.setVisibility(View.GONE);
        }
        else
        {
            countryName.setVisibility(View.VISIBLE);
            countryName.setText(LocaleUtil.getLocalizedCountryName(context, countryCode));
        }
    }

    @Override
    public Cursor runQuery(CharSequence constraint)
    {
        return Database.getCityCursor(db, constraint == null ? "" : constraint.toString(), null);
    }

    @Override
    public CharSequence convertToString(Cursor cursor)
    {
        int colIndex = cursor.getColumnIndex("name");
        int ctrIndex = cursor.getColumnIndex("country");
        return LocaleUtil.getLocalizedCityName(mContext, cursor.getString(colIndex), cursor.getString(ctrIndex));
    }
}
