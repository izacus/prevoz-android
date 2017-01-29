package org.prevoz.android.myrides

import com.crashlytics.android.Crashlytics
import com.hannesdorfmann.mosby.mvp.MvpPresenter
import de.greenrobot.event.EventBus
import org.prevoz.android.ApplicationComponent
import org.prevoz.android.MainActivity
import org.prevoz.android.api.ApiClient
import org.prevoz.android.api.rest.RestRide
import org.prevoz.android.auth.AuthenticationUtils
import org.prevoz.android.events.Events
import retrofit.RetrofitError
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import javax.inject.Inject

class MyRidesPresenter(component: ApplicationComponent) : MvpPresenter<MyRidesFragment> {
    init {
        component.inject(this)
    }

    @Inject lateinit var authUtils : AuthenticationUtils

    var view : MyRidesFragment? = null

    override fun attachView(view: MyRidesFragment?) {
        this.view = view
        EventBus.getDefault().register(this)
        checkForAuthentication()
    }

    override fun detachView(retainInstance: Boolean) {
        this.view = null
    }

    fun checkForAuthentication() {
        if (authUtils.isAuthenticated) {
            loadRides()
        } else {
            view?.showLoginPrompt()
        }
    }

    private fun loadRides() {
        view?.showLoadingThrobber()
        val myRides = ApiClient.getAdapter().myRides
                .flatMap { results ->
                    if (results == null || results.results == null) {
                        Observable.empty<List<RestRide>>()
                    } else {
                        Observable.from(results.results!!)
                    }
                }

        val bookmarks = ApiClient.getAdapter().bookmarkedRides
                .flatMap { results ->
                    if (results == null || results.results == null) {
                        Observable.empty<List<RestRide>>()
                    } else {
                        Observable.from(results.results!!)
                    }
                }

        myRides.mergeWith(bookmarks)
                .toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    results ->
                    @Suppress("UNCHECKED_CAST")
                    if (results.isEmpty()) {
                        view?.showEmptyView()
                    } else {
                        view?.showMyRides(results as List<RestRide>)
                    }
                }, {
                    e -> handleLoadingError(e)
                })
    }

    fun handleLoadingError(e: Throwable) {
        Crashlytics.logException(e.cause)
        if (e is RetrofitError) {
            if (e.response?.status == 403 || e.response?.status == 401) {
                authUtils.logout().subscribeOn(Schedulers.io()).toBlocking().firstOrDefault(null)
                view?.showLoginPrompt()
            }
        } else {
            view?.showLoadingError()
        }
    }

    fun login() {
        authUtils.requestAuthentication(view?.activity, MainActivity.REQUEST_CODE_AUTHORIZE_MYRIDES)
    }


    fun onEventMainThread(e: Events.MyRideStatusUpdated) {
        loadRides()
    }

    fun onEventMainThread(e: Events.LoginStateChanged) {
        checkForAuthentication()
    }
}