package org.prevoz.android.search

import android.content.Context
import android.database.Cursor
import android.view.View
import android.widget.FilterQueryProvider
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import org.prevoz.android.R
import org.prevoz.android.model.PrevozDatabase
import org.prevoz.android.provider.Location
import org.prevoz.android.util.LocaleUtil

class CityAutocompleteAdapter(val context: Context, val database: PrevozDatabase) :
        SimpleCursorAdapter(context, R.layout.item_autocomplete, database.cityCursor("", null),
                            arrayOf(Location.NAME), intArrayOf(R.id.city_name), 0),
        FilterQueryProvider, SimpleCursorAdapter.CursorToStringConverter {

    init {
        filterQueryProvider = this
        cursorToStringConverter = this
    }

    override fun runQuery(constraint: CharSequence?): Cursor? {
        return database.cityCursor(constraint?.toString(), null)
    }

    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        super.bindView(view, context, cursor)
        val countryName = (view!!.findViewById(R.id.city_country)!! as TextView)
        val countryCode = cursor!!.getString(cursor.getColumnIndex(Location.COUNTRY))

        if (LocaleUtil.getCurrentCountryCode() == countryCode) {
            countryName.visibility = View.GONE
        } else {
            countryName.visibility = View.VISIBLE
            countryName.text = LocaleUtil.getLocalizedCountryName(database, countryCode)
        }
    }

    override fun convertToString(cursor: Cursor?): CharSequence? {
        val colIndex = cursor!!.getColumnIndex(Location.NAME)
        val ctrIndex = cursor.getColumnIndex(Location.COUNTRY)
        return LocaleUtil.getLocalizedCityName(database, cursor.getString(colIndex), cursor.getString(ctrIndex))
    }
}