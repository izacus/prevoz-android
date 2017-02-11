package org.prevoz.android.search

import android.net.ConnectivityManager
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import com.crashlytics.android.answers.CustomEvent
import com.crashlytics.android.answers.SearchEvent
import com.hannesdorfmann.mosby.mvp.MvpPresenter
import de.greenrobot.event.EventBus
import org.prevoz.android.ApplicationComponent
import org.prevoz.android.api.ApiClient
import org.prevoz.android.api.rest.RestRide
import org.prevoz.android.auth.AuthenticationUtils
import org.prevoz.android.events.Events
import org.prevoz.android.model.PrevozDatabase
import org.prevoz.android.model.Route
import org.prevoz.android.push.PushManager
import org.prevoz.android.util.LocaleUtil
import org.prevoz.android.util.ViewUtils
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
    @Inject lateinit var pushManager : PushManager
    @Inject lateinit var connectivityService : ConnectivityManager

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
        if (connectivityService.activeNetworkInfo == null || !connectivityService.activeNetworkInfo.isConnected) {
            view?.showNetworkError()
            EventBus.getDefault().post(Events.SearchComplete())
            return
        }

        view?.hideList()
        view?.hideNotificationButton()

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
            view?.hideNotificationButton()
        }
    }

    fun showResults(results: List<RestRide>, route: Route, highlightRideIds: IntArray) {
        this.results = results
        Answers.getInstance().logSearch(SearchEvent().putQuery(route.toString()))
        if (results.isEmpty()) {
            view?.showEmptyMessage()
        } else {
            view?.showResults(results, route, highlightRideIds)
            showNotificationButtonIfAvailable()
        }
    }

    fun showHistory() {
        val animate = results != null
        view?.hideNotificationButton()
        database.getLastSearches(5)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ history -> view?.showHistory(history, animate) },
                           { throwable -> Crashlytics.logException(throwable) } )
    }

    override fun detachView(retainInstance: Boolean) {
        EventBus.getDefault().unregister(this)
        view = null
    }

    /**
     * Displays the push notification button if the services are available.
     */
    fun showNotificationButtonIfAvailable() {
        if (!pushManager.isPushAvailable) return
        val route = route
        val date = date

        if (route == null || date == null) return
        if (route.from == null || route.to == null) return

        view?.showNotificationButton()
        view?.updateNotificationButtonText(pushManager.isSubscribed(route, date))
    }

    fun switchNotificationState() {
        val route = route
        val date = date
        if (route == null || date == null) return
        view?.setNotificationButtonThrobber(true)

        val subscribe = !pushManager.isSubscribed(route, date)
        Answers.getInstance().logCustom(if (subscribe) CustomEvent("Subscribe to push") else CustomEvent("Unsubscribe from push"))
        pushManager.setSubscriptionStatus(route, date, subscribe)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( {
                                view?.setNotificationButtonThrobber(false)
                            },
                            { throwable ->
                                ViewUtils.showMessage(view?.activity, "Obveščanja ni bilo mogoče vklopiti.", true)
                                view?.setNotificationButtonThrobber(false) })
    }

    fun onEventMainThread(e: Events.NewSearchEvent) {
        search(e.route, e.date, e.rideIds)
    }

    fun onEventMainThread(e: Events.ClearSearchEvent) {
        showHistory()
        results = null
    }

    fun onEventMainThread(e: Events.NotificationSubscriptionStatusChanged) {
        if (e.route != route && e.date != date) return
        view?.updateNotificationButtonText(e.subscribed)
        view?.setNotificationButtonThrobber(false)
    }

    fun onEventMainThread(e: Events.MyRideStatusUpdated) {
        if (e.deleted) {
            view?.removeDisplayedRide(e.ride)
        } else {
            view?.updateDisplayedRide(e.ride)
        }
    }
}