package org.prevoz.android.search

import android.widget.EditText
import org.prevoz.android.api.PrevozApi
import org.prevoz.android.model.City
import org.prevoz.android.model.PrevozDatabase
import org.prevoz.android.model.Route
import org.threeten.bp.ZonedDateTime
import rx.android.schedulers.AndroidSchedulers

class SearchPresenter(val database: PrevozDatabase, val prevozApi: PrevozApi) {
    var view: SearchView? = null

    fun bind(view: SearchView) {
        this.view = view
        database.getLastSearches(5)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ view.showHistory(it) })
    }

    fun unbind() {
        this.view = null
    }

    fun startSearch(from: City, to: City, date: ZonedDateTime) {

    }
}

interface SearchView {
    fun showHistory(routes: List<Route>)
}