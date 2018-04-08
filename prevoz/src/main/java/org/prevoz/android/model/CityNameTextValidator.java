package org.prevoz.android.model;

import android.database.Cursor;
import android.widget.AutoCompleteTextView;

import org.prevoz.android.provider.Location;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.StringUtil;

public class CityNameTextValidator implements AutoCompleteTextView.Validator
{
    private final LocaleUtil localeUtil;
    private final PrevozDatabase database;

    public CityNameTextValidator(LocaleUtil localeUtil, PrevozDatabase prevozDatabase) {
        this.localeUtil = localeUtil;
        this.database = prevozDatabase;
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
            return localeUtil.getLocalizedCityName(cityCandidates.getString(idx), cityCandidates.getString(cidx));
        }

        return null;
    }
}
