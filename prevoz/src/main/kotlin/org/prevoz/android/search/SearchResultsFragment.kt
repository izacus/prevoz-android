package org.prevoz.android.search

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.ViewCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import com.crashlytics.android.Crashlytics

import org.prevoz.android.PrevozFragment
import org.prevoz.android.R
import org.prevoz.android.api.ApiClient
import org.prevoz.android.api.rest.RestRide
import org.prevoz.android.api.rest.RestSearchResults
import org.prevoz.android.events.Events
import org.prevoz.android.model.City
import org.prevoz.android.model.Route
import org.prevoz.android.ui.ListDisappearAnimation
import org.prevoz.android.ui.ListFlyupAnimator
import org.prevoz.android.util.LocaleUtil
import org.prevoz.android.util.ViewUtils
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

import java.util.ArrayList

import butterknife.BindView
import butterknife.ButterKnife
import com.hannesdorfmann.mosby.mvp.MvpFragment
import de.greenrobot.event.EventBus
import icepick.Icepick
import icepick.State
import org.prevoz.android.ApplicationComponent
import org.prevoz.android.PrevozApplication
import org.prevoz.android.model.PrevozDatabase
import retrofit.RetrofitError
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter
import se.emilsjolander.stickylistheaders.StickyListHeadersListView
import javax.inject.Inject


class SearchResultsFragment(component: ApplicationComponent) : MvpFragment<SearchResultsFragment, SearchResultsPresenter>() {
    init {
        component.inject(this)
    }

    @Inject lateinit var database : PrevozDatabase

    @BindView(R.id.search_results_list)
    lateinit var resultList: StickyListHeadersListView

    lateinit var searchNotifyButtonContainer: View
    lateinit var searchNotifyButton: View
    lateinit var searchNotifyButtonIcon: ImageView
    lateinit var searchNotifyButtonProgress: ProgressBar
    lateinit var searchNofityButtonText: TextView

    lateinit var headerFragmentView: View
    lateinit var adapter: StickyListHeadersAdapter

    override fun createPresenter(): SearchResultsPresenter {
        return SearchResultsPresenter((activity.application as PrevozApplication).component())
    }

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        headerFragmentView = getLayoutInflater(savedInstanceState).inflate(R.layout.header_search_form, null, false)

        searchNotifyButtonContainer = headerFragmentView.findViewById(R.id.search_notify_button_container)
        searchNofityButtonText = headerFragmentView.findViewById(R.id.search_notify_button_text) as TextView
        searchNotifyButtonIcon = headerFragmentView.findViewById(R.id.search_notify_button_icon) as ImageView
        searchNotifyButtonProgress = headerFragmentView.findViewById(R.id.search_notify_button_progress) as ProgressBar
        searchNotifyButton = headerFragmentView.findViewById(R.id.search_notify_button)
        searchNotifyButtonContainer.setOnClickListener { v -> presenter.switchNotificationState() }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val views = inflater!!.inflate(R.layout.fragment_search_list, container, false)
        ButterKnife.bind(this, views)
        resultList.addHeaderView(headerFragmentView, null, true)

        if (childFragmentManager.findFragmentByTag("SearchFragment") == null) {
            val ft = childFragmentManager.beginTransaction()
            ft.replace(R.id.search_form, SearchFragment(), "SearchFragment")
            ft.commit()
        }

        return views
    }

    fun showHistory(history: List<Route>, animate: Boolean) {
        val activity = activity ?: return
        adapter = SearchHistoryAdapter(activity, history)
        if (animate && resultList.adapter != null) {
            ListDisappearAnimation(resultList).animate()
        }

        resultList.adapter = adapter
        if (animate) ListFlyupAnimator(resultList).animate()
    }

    fun showResults(results: List<RestRide>, askedForRoute: Route?, highlightRideIds: IntArray) {
        if (resultList.adapter is SearchResultsAdapter) {
            val adapter = resultList.adapter as SearchResultsAdapter
            adapter.setResults(results, highlightRideIds, askedForRoute)
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                resultList.smoothScrollToPosition(1)
            }
        } else {
            val adapter = SearchResultsAdapter(activity, database, results, highlightRideIds, askedForRoute)
            resultList.adapter = adapter
        }

        if (results.isEmpty()) {
            ViewUtils.showMessage(activity, R.string.search_no_results, true)
        } else {
            ListFlyupAnimator(resultList).animate()
        }
    }

    fun updateDisplayedRide(ride: RestRide) {
        if (resultList.adapter is SearchResultsAdapter) {
            (resultList.adapter as SearchResultsAdapter).updateRide(ride)
        }
    }

    fun removeDisplayedRide(ride: RestRide) {
        if (resultList.adapter is SearchResultsAdapter) {
            (resultList.adapter as SearchResultsAdapter).removeRide(ride.id)
        }
    }

    fun hideList() {
        if (resultList.adapter != null) {
            ListDisappearAnimation(resultList).animate()
        }
    }

    fun showSearchError() {
        val activity = activity ?: return
        ViewUtils.showMessage(activity, "Napaka med iskanjem, a internet deluje?", true)
    }

    fun showNotificationButton() {
        searchNotifyButtonContainer.clearAnimation()
        searchNotifyButtonContainer.visibility = View.VISIBLE
        searchNotifyButtonContainer.animate().alpha(1.0f).setDuration(200).setListener(null)
    }

    fun setNotificationButtonThrobber(visible: Boolean) {
        searchNotifyButton.isEnabled = !visible
        searchNotifyButtonIcon.visibility = if (visible) View.INVISIBLE else View.VISIBLE
        searchNotifyButtonProgress.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    fun updateNotificationButtonText(subscribed: Boolean) {
        if (subscribed) {
            searchNotifyButtonIcon.setImageResource(R.drawable.ic_action_cancel)
            searchNofityButtonText.text = "Prenehaj z obveščanjem"
        } else {
            searchNotifyButtonIcon.setImageResource(R.drawable.ic_action_bell)
            searchNofityButtonText.text = "Obveščaj me o novih prevozih"
        }
    }

    fun hideNotificationButton() {
        if (searchNotifyButtonContainer.visibility == View.GONE) return
        searchNotifyButtonContainer.clearAnimation()
        ViewCompat.animate(searchNotifyButtonContainer).alpha(0.0f).setDuration(200).withEndAction { searchNotifyButtonContainer.visibility = View.GONE }
    }


    /*
    private fun showNotificationsButton() {
        searchNotifyButtonContainer.clearAnimation()

        if (!shouldShowNotificationButton ||
                searchNotifyButtonContainer.visibility == View.VISIBLE ||
                !pushManager.isPushAvailable) {
            return
        }

        // Show notifications button

        updateNotificationButtonText()
        searchNotifyButtonContainer.visibility = View.VISIBLE
        searchNotifyButtonContainer.animate().alpha(1.0f).setDuration(200).setListener(null)
    }

    private fun updateNotificationButtonText() {
        pushManager.isSubscribed(lastFrom, lastTo, lastDate)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { subscribed ->
                    if (subscribed!!) {
                        searchNotifyButtonIcon.setImageResource(R.drawable.ic_action_cancel)
                        searchNofityButtonText.text = "Prenehaj z obveščanjem"
                    } else {
                        searchNotifyButtonIcon.setImageResource(R.drawable.ic_action_bell)
                        searchNofityButtonText.text = "Obveščaj me o novih prevozih"
                    }
                }
    }

    private fun hideNotificationsButton() {
        if (searchNotifyButtonContainer.visibility == View.GONE)
            return

        searchNotifyButtonContainer.clearAnimation()
        ViewCompat.animate(searchNotifyButtonContainer).alpha(0.0f).setDuration(200).withEndAction { searchNotifyButtonContainer.visibility = View.GONE }
    }

    private fun clickNotificationButton() {
        searchNotifyButton.isEnabled = false
        searchNotifyButtonIcon.visibility = View.INVISIBLE
        searchNotifyButtonProgress.visibility = View.VISIBLE

        pushManager.isSubscribed(lastFrom, lastTo, lastDate)
                .subscribe { subscribed -> pushManager.setSubscriptionStatus(activity, lastFrom, lastTo, lastDate, !subscribed) }
    }


    fun onEventMainThread(e: Events.NotificationSubscriptionStatusChanged) {
        updateNotificationButtonText()
        searchNotifyButton.isEnabled = true
        searchNotifyButtonIcon.visibility = View.VISIBLE
        searchNotifyButtonProgress.visibility = View.INVISIBLE
    }

    fun onEventMainThread(e: Events.MyRideStatusUpdated) {
        if (adapter != null && adapter is SearchResultsAdapter) {
            val srAdapter = adapter as SearchResultsAdapter?
            if (e.deleted)
                srAdapter!!.removeRide(e.ride.id)
            srAdapter!!.updateRide(e.ride)
            srAdapter.notifyDataSetChanged()
        }
    }
    */

    fun showingResults(): Boolean {
        return adapter is SearchResultsAdapter
    }
}
