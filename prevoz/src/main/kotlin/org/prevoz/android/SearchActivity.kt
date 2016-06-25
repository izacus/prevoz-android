package org.prevoz.android

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_search.*
import org.prevoz.android.model.Route
import org.prevoz.android.search.CityAutocompleteAdapter
import org.prevoz.android.search.SearchHistoryAdapter
import org.prevoz.android.search.SearchPresenter
import org.prevoz.android.search.SearchView
import org.prevoz.android.util.StringUtil
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

class SearchActivity : AppCompatActivity(), SearchView {

    @Inject
    lateinit var presenter : SearchPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        (application as PrevozApplication).component().inject(this)

        search_edit_from?.setAdapter(CityAutocompleteAdapter(this, presenter.database))
        search_edit_to?.setAdapter(CityAutocompleteAdapter(this, presenter.database))

        search_button_start.setOnClickListener { startSearch(search_edit_from.text.toString(),
                                                             search_edit_to.text.toString(),
                                                             search_picker_date.getSelectedDate()) }
    }

    override fun onStart() {
        super.onStart()
        presenter.bind(this)
    }

    override fun onStop() {
        super.onStop()
        presenter.unbind()
    }

    fun startSearch(fromString: String, toString: String, date: ZonedDateTime) {
        val from = StringUtil.splitStringToCity(fromString)
        val to = StringUtil.splitStringToCity(toString)
        presenter.startSearch(from, to, date)
    }

    override fun showHistory(routes: List<Route>) {
        search_recycler.layoutManager = LinearLayoutManager(this)
        search_recycler.adapter = SearchHistoryAdapter(this, routes)
    }

}
