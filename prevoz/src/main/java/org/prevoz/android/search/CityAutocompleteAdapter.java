package org.prevoz.android.search;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.FilterQueryProvider;
import android.widget.TextView;

import org.prevoz.android.R;
import org.prevoz.android.model.PrevozDatabase;
import org.prevoz.android.provider.Location;
import org.prevoz.android.util.LocaleUtil;

public class CityAutocompleteAdapter extends SimpleCursorAdapter implements FilterQueryProvider, SimpleCursorAdapter.CursorToStringConverter
{
    @NonNull
    private final PrevozDatabase database;

    @NonNull
    private final LocaleUtil localeUtil;

    public CityAutocompleteAdapter(@NonNull Context context, @NonNull PrevozDatabase database, @NonNull LocaleUtil localeUtil)
    {
        super(context,
              R.layout.item_autocomplete,
              database.cityCursor("", null),
              new String[] { Location.NAME},
              new int[] { R.id.city_name },
              0);

        setFilterQueryProvider(this);
        setCursorToStringConverter(this);
        this.database = database;
        this.localeUtil = localeUtil;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor)
    {
        super.bindView(view, context, cursor);
        int idx = cursor.getColumnIndex(Location.COUNTRY);

        TextView countryName = view.findViewById(R.id.city_country);
        String countryCode = cursor.getString(idx);
        if (LocaleUtil.getCurrentCountryCode().equals(countryCode))
        {
            countryName.setVisibility(View.GONE);
        }
        else
        {
            countryName.setVisibility(View.VISIBLE);
            countryName.setText(localeUtil.getLocalizedCountryName(countryCode));
        }
    }

    @Override
    public Cursor runQuery(CharSequence constraint)
    {
        return database.cityCursor(constraint == null ? null : constraint.toString(), null);
    }

    @Override
    public CharSequence convertToString(Cursor cursor)
    {
        int colIndex = cursor.getColumnIndex(Location.NAME);
        int ctrIndex = cursor.getColumnIndex(Location.COUNTRY);
        return localeUtil.getLocalizedCityName(cursor.getString(colIndex), cursor.getString(ctrIndex));
    }
}
