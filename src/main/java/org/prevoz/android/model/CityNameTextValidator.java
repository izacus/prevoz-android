package org.prevoz.android.model;

import android.content.Context;
import android.database.Cursor;
import android.widget.AutoCompleteTextView;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.StringUtil;

/**
 * ${FILE_NAME}
 * <p/>
 * Created on 18/05/14
 */
public class CityNameTextValidator implements AutoCompleteTextView.Validator
{
    private final Context context;

    public CityNameTextValidator(Context ctx)
    {
        this.context = ctx;
    }

    @Override
    public boolean isValid(CharSequence text)
    {
        return Database.cityExists(Database.getSettingsDatabase(context), text.toString());
    }

    @Override
    public CharSequence fixText(CharSequence invalidText)
    {
        City c = StringUtil.splitStringToCity(invalidText.toString());
        if (c == null)
            return null;

        Cursor cityCandidates = Database.getCityCursor(Database.getSettingsDatabase(context), c.getDisplayName(), c.getCountryCode());
        if (cityCandidates.moveToNext())
        {
            int idx = cityCandidates.getColumnIndex("name");
            int cidx = cityCandidates.getColumnIndex("country");
            return LocaleUtil.getLocalizedCityName(context, cityCandidates.getString(idx), cityCandidates.getString(cidx));
        }

        return null;
    }
}
