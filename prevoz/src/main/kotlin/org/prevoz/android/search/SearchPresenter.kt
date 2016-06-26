package org.prevoz.android.search

import org.prevoz.android.api.PrevozApi
import org.prevoz.android.api.rest.RestRide
import org.prevoz.android.model.City
import org.prevoz.android.model.PrevozDatabase
import org.prevoz.android.model.Route
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import rx.android.schedulers.AndroidSchedulers


class SearchPresenter(val database: PrevozDatabase, val prevozApi: PrevozApi) {
    val searchDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

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

    fun startSearch(from: City?, to: City?, date: ZonedDateTime) {
        prevozApi.search(from?.displayName, from?.countryCode,
                         to?.displayName, to?.countryCode,
                         searchDateFormatter.format(date))
                 .map { it.results }
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribe { view?.showResults(it) }
    }
}

interface SearchView {
    fun showHistory(routes: List<Route>)
    fun showResults(results: List<RestRide>)
}