package org.prevoz.android.search

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.hannesdorfmann.mosby.mvp.MvpFragment
import org.prevoz.android.ApplicationComponent
import org.prevoz.android.PrevozApplication
import org.prevoz.android.R
import org.prevoz.android.api.rest.RestRide
import org.prevoz.android.model.PrevozDatabase
import org.prevoz.android.model.Route
import org.prevoz.android.ui.ListDisappearAnimation
import org.prevoz.android.ui.ListFlyupAnimator
import org.prevoz.android.util.ViewUtils
import se.emilsjolander.stickylistheaders.StickyListHeadersListView
import javax.inject.Inject


class SearchResultsFragment : MvpFragment<SearchResultsFragment, SearchResultsPresenter>() {

    @Inject lateinit var database : PrevozDatabase

    var resultList: StickyListHeadersListView? = null
    var resultListEmpty: View? = null

    lateinit var searchNotifyButtonContainer: View
    lateinit var searchNotifyButton: View
    lateinit var searchNotifyButtonIcon: ImageView
    lateinit var searchNotifyButtonProgress: ProgressBar
    lateinit var searchNofityButtonText: TextView

    lateinit var headerFragmentView: View

    override fun createPresenter(): SearchResultsPresenter {
        return SearchResultsPresenter((activity.application as PrevozApplication).component())
    }

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (context.applicationContext as PrevozApplication).component().inject(this)

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
        resultList = views.findViewById(R.id.search_results_list) as StickyListHeadersListView?
        resultList?.addHeaderView(headerFragmentView, null, true)
        resultListEmpty = views.findViewById(R.id.search_results_empty)

        if (childFragmentManager.findFragmentByTag("SearchFragment") == null) {
            val ft = childFragmentManager.beginTransaction()
            ft.replace(R.id.search_form, SearchFragment(), "SearchFragment")
            ft.commit()
        }

        return views
    }

    fun showHistory(history: List<Route>, animate: Boolean) {
        val activity = activity ?: return
        val adapter = SearchHistoryAdapter(activity, history)
        if (animate && resultList?.adapter != null) {
            ListDisappearAnimation(resultList).animate()
        }

        resultListEmpty?.visibility = View.INVISIBLE
        resultList?.adapter = adapter
        if (animate) ListFlyupAnimator(resultList).animate()
    }

    fun showResults(results: List<RestRide>, askedForRoute: Route?, highlightRideIds: IntArray) {
        if (resultList?.adapter is SearchResultsAdapter) {
            val adapter = resultList?.adapter as SearchResultsAdapter
            adapter.setResults(results, highlightRideIds, askedForRoute)
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                resultList?.smoothScrollToPosition(1)
            }
        } else {
            val adapter = SearchResultsAdapter(activity, database, results, highlightRideIds, askedForRoute)
            resultList?.adapter = adapter
        }
        resultListEmpty?.visibility = View.INVISIBLE

        if (results.isEmpty()) {
            showEmptyMessage()
        } else {
            ListFlyupAnimator(resultList).animate()
        }
    }

    fun updateDisplayedRide(ride: RestRide) {
        if (resultList?.adapter is SearchResultsAdapter) {
            (resultList?.adapter as SearchResultsAdapter).updateRide(ride)
        }
    }

    fun removeDisplayedRide(ride: RestRide) {
        if (resultList?.adapter is SearchResultsAdapter) {
            (resultList?.adapter as SearchResultsAdapter).removeRide(ride.id)
        }
    }

    fun hideList() {
        if (resultList?.adapter != null) {
            ListDisappearAnimation(resultList).animate()
        }

        resultListEmpty?.visibility = View.INVISIBLE
    }

    fun showSearchError() {
        val activity = activity ?: return
        ViewUtils.showMessage(activity, "Napaka med iskanjem, a internet deluje?", true)
    }

    fun showNetworkError() {
        val activity = activity ?: return
        ViewUtils.showMessage(activity, "Ni mogoče iskati, internet ni na voljo!", true)
    }

    fun showEmptyMessage() {
        resultListEmpty?.visibility = View.VISIBLE
    }

    fun showNotificationButton() {
        searchNotifyButtonContainer.clearAnimation()
        searchNotifyButtonContainer.visibility = View.VISIBLE
        searchNotifyButtonIcon.visibility = View.VISIBLE
        searchNotifyButtonContainer.isEnabled = true
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

    fun showingResults(): Boolean {
        return resultList?.adapter is SearchResultsAdapter || resultListEmpty?.visibility == View.VISIBLE
    }
}
