package org.prevoz.android.search

import com.crashlytics.android.Crashlytics
import com.hannesdorfmann.mosby.mvp.MvpPresenter
import de.greenrobot.event.EventBus
import org.prevoz.android.ApplicationComponent
import org.prevoz.android.api.ApiClient
import org.prevoz.android.api.rest.RestRide
import org.prevoz.android.api.rest.RestSearchResults
import org.prevoz.android.auth.AuthenticationUtils
import org.prevoz.android.events.Events
import org.prevoz.android.model.City
import org.prevoz.android.model.PrevozDatabase
import org.prevoz.android.model.Route
import org.prevoz.android.util.LocaleUtil
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import retrofit.RetrofitError
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import javax.inject.Inject

class SearchResultsPresenter(component: ApplicationComponent) : MvpPresenter<SearchResultsFragment> {
    init {
        component.inject(this)
    }

    @Inject lateinit var database : PrevozDatabase
    @Inject lateinit var authUtils : AuthenticationUtils

    var view : SearchResultsFragment? = null

    // Current search in progress
    var currentSearchSubscription: Subscription? = null
    var route : Route? = null
    var date: LocalDate? = null
    var results : List<RestRide>? = null
    var highlightRideIds: IntArray = intArrayOf()

    override fun attachView(view: SearchResultsFragment?) {
        this.view = view
        EventBus.getDefault().register(this)

        val results = results
        if (results == null) showHistory() else view?.showResults(results, route, highlightRideIds)
    }


    fun search(route: Route, date: LocalDate, highlightRideIds: IntArray) {
        view?.hideList()
        this.route = route
        this.highlightRideIds = highlightRideIds
        this.date = date
        currentSearchSubscription = ApiClient.getAdapter().search(route.from?.displayName, route.from?.countryCode,
                                                                  route.to?.displayName, route.to?.countryCode,
                                                                  date.format(DateTimeFormatter.ISO_LOCAL_DATE), false)
                .map { results ->
                    // Preload database caches
                    LocaleUtil.getFormattedCurrency(1.0)
                    for (ride in results?.results ?: emptyList()) {
                        ride.getLocalizedFrom(database)
                        ride.getLocalizedTo(database)
                    }

                    results
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( { results -> showResults(results?.results ?: listOf(), route, highlightRideIds) },
                            { throwable -> handleError(throwable as RetrofitError) },
                            { EventBus.getDefault().post(Events.SearchComplete()) })
    }

    fun handleError(error: RetrofitError) {
        if (error.response.status == 403 && ApiClient.getBearer() != null) {
            Crashlytics.logException(error.cause)
            authUtils.logout().subscribeOn(Schedulers.io()).subscribe()
            ApiClient.setBearer(null)
            search(route!!, date!!, highlightRideIds)
        } else {
            view?.showSearchError()
        }
    }

    fun showResults(results: List<RestRide>, route: Route, highlightRideIds: IntArray) {
        this.results = results
        view?.showResults(results, route, highlightRideIds)
    }

    fun showHistory() {
        database.getLastSearches(5)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ history -> view?.showHistory(history, results != null) },
                           { throwable -> Crashlytics.logException(throwable) } )
    }

    override fun detachView(retainInstance: Boolean) {
        EventBus.getDefault().unregister(this)
    }

    fun onEventMainThread(e: Events.NewSearchEvent) {
        search(e.route, e.date, e.rideIds)
    }

    fun onEventMainThread(e: Events.ClearSearchEvent) {
        showHistory()
    }
}