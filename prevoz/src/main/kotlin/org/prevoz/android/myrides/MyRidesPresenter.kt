package org.prevoz.android.myrides

import android.content.Intent
import android.net.ConnectivityManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import com.crashlytics.android.Crashlytics
import com.hannesdorfmann.mosby.mvp.MvpPresenter
import de.greenrobot.event.EventBus
import org.prevoz.android.ApplicationComponent
import org.prevoz.android.R
import org.prevoz.android.api.ApiClient
import org.prevoz.android.api.rest.RestRide
import org.prevoz.android.auth.AuthenticationUtils
import org.prevoz.android.events.Events
import retrofit2.HttpException
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import javax.inject.Inject


class MyRidesPresenter(component: ApplicationComponent) : MvpPresenter<MyRidesFragment> {
    init {
        component.inject(this)
    }

    @Inject lateinit var authUtils : AuthenticationUtils
    @Inject lateinit var connectivityService : ConnectivityManager

    var view : MyRidesFragment? = null
    var myRidesSubscription : Subscription? = null

    override fun attachView(view: MyRidesFragment?) {
        this.view = view
        EventBus.getDefault().register(this)
        checkForAuthentication()
    }

    override fun detachView(retainInstance: Boolean) {
        this.view = null
        this.myRidesSubscription?.unsubscribe()
        this.myRidesSubscription = null
    }

    fun checkForAuthentication() {
        if (authUtils.isAuthenticated) {
            loadRides()
        } else {
            view?.showLoginPrompt()
        }
    }

    private fun loadRides() {
        if (myRidesSubscription != null) return
        if (connectivityService.activeNetworkInfo == null || !connectivityService.activeNetworkInfo.isConnected) {
            view?.showNetworkError()
            return
        }

        view?.showLoadingThrobber()
        val myRides = ApiClient.getAdapter().myRides
                .subscribeOn(Schedulers.io())
                .flatMap { results ->
                    if (results == null || results.results == null) {
                        Observable.empty<List<RestRide>>()
                    } else {
                        Observable.from(results.results!!)
                    }
                }

        val bookmarks = ApiClient.getAdapter().getBookmarkedRides(System.currentTimeMillis())
                .subscribeOn(Schedulers.io())
                .flatMap { results ->
                    if (results == null || results.results == null) {
                        Observable.empty<List<RestRide>>()
                    } else {
                        Observable.from(results.results!!)
                    }
                }

        myRidesSubscription = myRides.mergeWith(bookmarks)
                .toList()
                .subscribeOn(Schedulers.io())
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
                }, {
                    myRidesSubscription = null
                })
    }

    fun handleLoadingError(e: Throwable) {
        Crashlytics.logException(e.cause)
        if (e is HttpException) {
            if (e.code() == 403 || e.code() == 401) {
                authUtils.logout()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            view?.showLoginPrompt()
                        });
            }
        }

        view?.showLoadingError()
    }

    fun login() {
        authUtils.requestAuthentication(view?.activity, 0)
    }


    @Suppress("UNUSED_PARAMETER")
    fun onEventMainThread(e: Events.MyRideStatusUpdated) {
        view?.updateRideInList(e.ride)
        if (authUtils.isAuthenticated) {
            loadRides()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onEventMainThread(e: Events.LoginStateChanged) {
        checkForAuthentication()
    }

    fun addRide() {
        val i = Intent(view?.context, NewRideActivity::class.java)
        ActivityCompat.startActivity(view?.context, i, ActivityOptionsCompat.makeCustomAnimation(view?.context, R.anim.slide_up, 0).toBundle())
    }
}