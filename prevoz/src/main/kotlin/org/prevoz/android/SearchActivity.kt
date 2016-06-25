package org.prevoz.android

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_search.*
import org.prevoz.android.model.Route
import org.prevoz.android.search.SearchHistoryAdapter
import org.prevoz.android.search.SearchPresenter
import org.prevoz.android.search.SearchView
import javax.inject.Inject

class SearchActivity : AppCompatActivity(), SearchView {

    @Inject
    lateinit var presenter : SearchPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        (application as PrevozApplication).component().inject(this)

        search_button_start?.setOnClickListener {  }
    }

    override fun onStart() {
        super.onStart()
        presenter.bind(this)
    }

    override fun onStop() {
        super.onStop()
        presenter.unbind()
    }

    override fun showHistory(routes: List<Route>) {
        search_recycler.layoutManager = LinearLayoutManager(this)
        search_recycler.adapter = SearchHistoryAdapter(this, routes)
    }

}
