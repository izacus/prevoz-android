package org.prevoz.android.myrides

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.hannesdorfmann.mosby.mvp.MvpFragment
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration
import org.prevoz.android.PrevozApplication
import org.prevoz.android.R
import org.prevoz.android.api.rest.RestRide

class MyRidesFragment : MvpFragment<MyRidesFragment, MyRidesPresenter>() {

    @BindView(R.id.myrides_list)
    lateinit var myridesList: RecyclerView

    @BindView(R.id.empty_view)
    lateinit var emptyView: View

    @BindView(R.id.empty_text)
    lateinit var emptyText: TextView

    @BindView(R.id.empty_button)
    lateinit var loginButton: Button

    @BindView(R.id.myrides_throbber)
    lateinit var throbber: ProgressBar

    @BindView(R.id.myrides_add)
    lateinit var addButton: FloatingActionButton

    lateinit var adapter : MyRidesAdapter

    override fun createPresenter(): MyRidesPresenter {
        return MyRidesPresenter((activity.application as PrevozApplication).component())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = MyRidesAdapter(activity)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val views = inflater?.inflate(R.layout.fragment_myrides, container, false)
        ButterKnife.bind(this, views as View)
        myridesList.setHasFixedSize(true)
        myridesList.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        myridesList.layoutManager = layoutManager
        myridesList.addItemDecoration(StickyRecyclerHeadersDecoration(adapter))
        myridesList.adapter = adapter
        loginButton.text = "Prijavite se"
        return views
    }

    override fun onStart() {
        super.onStart()
        presenter.checkForAuthentication()
    }

    @OnClick(R.id.empty_button)
    fun onLoginButtonClick() {
        presenter.login()
    }

    @OnClick(R.id.myrides_add)
    fun onAddButtonClick() {
        presenter.addRide()
    }

    fun showLoginPrompt() {
        emptyText.text = "Za ogled in dodajanje zaznamkov ter prevozov morate biti prijavljeni."
        emptyView.visibility = View.VISIBLE
        myridesList.visibility = View.INVISIBLE
        throbber.visibility = View.INVISIBLE
        loginButton.visibility = View.VISIBLE
        addButton.visibility = View.INVISIBLE
    }

    fun showLoadingThrobber() {
        emptyView.visibility = View.INVISIBLE
        myridesList.visibility = View.INVISIBLE
        throbber.visibility = View.VISIBLE
        loginButton.visibility = View.GONE
        addButton.visibility = View.VISIBLE
        adapter.clear()
    }

    fun showMyRides(results: List<RestRide>) {
        adapter.setRides(results)
        myridesList.visibility = View.VISIBLE
        emptyView.visibility = View.INVISIBLE
        loginButton.visibility = View.GONE
        throbber.visibility = View.INVISIBLE
        addButton.visibility = View.VISIBLE
    }

    fun showEmptyView() {
        myridesList.visibility = View.INVISIBLE
        throbber.visibility = View.INVISIBLE
        emptyView.visibility = View.VISIBLE
        loginButton.visibility = View.GONE
        addButton.visibility = View.VISIBLE
        emptyText.text = "Nimate objavljenih ali zaznamovanih prevozov."
    }

    fun showLoadingError() {
        myridesList.visibility = View.INVISIBLE
        throbber.visibility = View.INVISIBLE
        emptyView.visibility = View.VISIBLE
        loginButton.visibility = View.GONE
        addButton.visibility = View.VISIBLE
        emptyText.text = "Pri nalaganju vaših prevozov je prišlo do napake."
    }

    fun updateRideInList(ride: RestRide) {
        adapter.updateRide(ride)
    }
}