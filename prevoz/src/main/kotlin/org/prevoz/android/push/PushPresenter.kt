package org.prevoz.android.push

import com.hannesdorfmann.mosby.mvp.MvpPresenter
import de.greenrobot.event.EventBus
import org.prevoz.android.ApplicationComponent
import org.prevoz.android.auth.AuthenticationUtils
import org.prevoz.android.events.Events
import org.prevoz.android.model.NotificationSubscription
import org.prevoz.android.model.Route
import rx.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class PushPresenter(applicationComponent: ApplicationComponent) : MvpPresenter<PushFragment> {
    init {
        applicationComponent.inject(this)
    }

    @Inject lateinit var pushManager : PushManager

    var view : PushFragment? = null

    override fun attachView(view: PushFragment?) {
        this.view = view
        EventBus.getDefault().register(this)
        loadNotifications()
    }

    override fun detachView(retainInstance: Boolean) {
        EventBus.getDefault().unregister(this)
        this.view = null
    }

    fun loadNotifications() {
        pushManager.subscriptions
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { notifications ->
                    view?.showNotifications(notifications)
                }
    }

    fun unsubscribeFrom(subscription: NotificationSubscription) {
        val route = Route(subscription.from, subscription.to)
        pushManager.setSubscriptionStatus(route, subscription.date, false).subscribe()
    }

    fun onEventMainThread(e: Events.NotificationSubscriptionStatusChanged) {
        loadNotifications()
    }
}