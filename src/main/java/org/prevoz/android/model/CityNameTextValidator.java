package org.prevoz.android.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.widget.AutoCompleteTextView;
import org.prevoz.android.provider.Location;
import org.prevoz.android.util.ContentUtils;
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
        return ContentUtils.doesCityExist(context, text.toString());
    }

    @Override
    public CharSequence fixText(CharSequence invalidText)
    {
        City c = StringUtil.splitStringToCity(invalidText.toString());
        if (c == null)
            return null;


        Cursor cityCandidates = ContentUtils.getCityCursor(context, c.getDisplayName(), c.getCountryCode());
        if (cityCandidates.moveToNext())
        {
            int idx = cityCandidates.getColumnIndex(Location.NAME);
            int cidx = cityCandidates.getColumnIndex(Location.COUNTRY);
            return LocaleUtil.getLocalizedCityName(context, cityCandidates.getString(idx), cityCandidates.getString(cidx));
        }

        return null;
    }
}
