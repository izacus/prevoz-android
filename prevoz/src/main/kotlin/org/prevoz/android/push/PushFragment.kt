package org.prevoz.android.push

import android.os.Bundle
import android.support.transition.TransitionManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.BindView
import butterknife.ButterKnife
import com.hannesdorfmann.mosby.mvp.MvpFragment
import org.prevoz.android.PrevozApplication
import org.prevoz.android.R
import org.prevoz.android.model.NotificationSubscription
import org.prevoz.android.model.Route
import org.prevoz.android.util.LocaleUtil
import org.prevoz.android.util.ViewUtils

class PushFragment : MvpFragment<PushFragment, PushPresenter>() {

    @BindView(R.id.notifications_list)
    lateinit var notificationList: RecyclerView

    @BindView(R.id.empty_view)
    lateinit var emptyView: ViewGroup

    override fun createPresenter(): PushPresenter {
        return PushPresenter((activity!!.application as PrevozApplication).component())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val views = inflater.inflate(R.layout.fragment_notifications, container, false)
        ButterKnife.bind(this, views as View)
        ViewUtils.setupEmptyView(notificationList, emptyView, "Niste prijavljeni na nobena obvestila.")
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        notificationList.layoutManager = layoutManager
        return views
    }

    fun showNotifications(notifications: List<NotificationSubscription>) {
        TransitionManager.beginDelayedTransition(emptyView)
        if (notifications.isEmpty()) {
            notificationList.visibility = View.INVISIBLE
            emptyView.visibility = View.VISIBLE
        } else {
            notificationList.visibility = View.VISIBLE
            emptyView.visibility = View.INVISIBLE
            notificationList.adapter = PushNotificationsAdapter(context, notifications, {
                subscription -> showNotificationRemoveDialog(subscription)
            })
        }
    }

    private fun showNotificationRemoveDialog(subscription : NotificationSubscription) {
        AlertDialog.Builder(activity!!, R.style.Prevoz_Theme_Dialog)
                .setTitle(String.format("%s - %s", subscription.from.toString(), subscription.to.toString()))
                .setMessage(String.format("Ali se res želite odjaviti od obveščanja v %s?", LocaleUtil.getNotificationDayName(resources, subscription.date).toLowerCase()))
                .setNegativeButton("Prekliči", null)
                .setPositiveButton("Odjavi") { dialog, which ->
                    presenter.unsubscribeFrom(subscription)
                }.show()
    }
}