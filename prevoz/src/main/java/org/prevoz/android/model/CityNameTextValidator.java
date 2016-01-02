package org.prevoz.android.model;

import android.content.Context;
import android.database.Cursor;
import android.widget.AutoCompleteTextView;

import org.prevoz.android.provider.Location;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.StringUtil;

/**
 * ${FILE_NAME}
 * <p/>
 * Created on 18/05/14
 */
public class CityNameTextValidator implements AutoCompleteTextView.Validator
{
    protected final PrevozDatabase database;

    public CityNameTextValidator(Context ctx, PrevozDatabase database) {
        this.database = database;
    }

    @Override
    public boolean isValid(CharSequence text)
    {
        return database.cityExists(text.toString()).toBlocking().value();
    }

    @Override
    public CharSequence fixText(CharSequence invalidText)
    {
        City c = StringUtil.splitStringToCity(invalidText.toString());
        if (c == null)
            return null;


        Cursor cityCandidates = database.cityCursor(c.getDisplayName(), c.getCountryCode());
        if (cityCandidates.moveToNext())
        {
            int idx = cityCandidates.getColumnIndex(Location.NAME);
            int cidx = cityCandidates.getColumnIndex(Location.COUNTRY);
            return LocaleUtil.getLocalizedCityName(database, cityCandidates.getString(idx), cityCandidates.getString(cidx));
        }

        return null;
    }
}
