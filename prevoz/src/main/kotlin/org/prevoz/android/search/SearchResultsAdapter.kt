package org.prevoz.android.search

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import org.prevoz.android.R
import org.prevoz.android.api.rest.RestRide
import org.prevoz.android.model.PrevozDatabase
import org.prevoz.android.util.LocaleUtil
import org.threeten.bp.format.DateTimeFormatter
import rx.Observable
import java.util.*

class SearchResultsAdapter(val database : PrevozDatabase, val rideClickedCallback : ((RestRide) -> Unit) ) :
        SectionedRecyclerViewAdapter<SearchResultsAdapter.SearchHolder>() {

    var results: List<RestRide> = listOf()
    var sections: List<String> = listOf()
    var groupedResults : Map<String, List<RestRide>> = mapOf()

    init {
        setHasStableIds(true)
    }

    fun setData(results : List<RestRide>) {
        this.results = results.sortedWith(Comparator { a, b -> a.compareTo(b) })
        this.groupedResults = this.results.groupBy { (it.fromCity ?: "") + (it.toCity ?: "") }
        this.sections = this.groupedResults.keys.sorted().toList()
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: SearchHolder?, section: Int, relativePosition: Int, absolutePosition: Int) {
        val viewHolder = holder as SearchResultHolder
        val result = results[absolutePosition]

        viewHolder.tvDriver.text = result.author
        viewHolder.tvTime.text = hourFormatter.format(result.date)

        val price = result.price
        if (price == null) {
            viewHolder.tvPrice.visibility = View.INVISIBLE
        } else {
            viewHolder.tvPrice.visibility = View.VISIBLE
            viewHolder.tvPrice.text = LocaleUtil.getFormattedCurrency(price)
        }

        viewHolder.view.setOnClickListener {
            rideClickedCallback.invoke(result)
        }
    }

    override fun getItemCount(section: Int): Int {
        return groupedResults[sections[section]]!!.size
    }

    override fun onBindHeaderViewHolder(holder: SearchHolder?, section: Int) {
        val viewHolder = holder as SearchHeaderHolder

        val sectionData = groupedResults[sections[section]]
        if (sectionData != null && sectionData.size > 0) {
            viewHolder.tvTitle.text = sectionData[0].getLocalizedFrom(database) + " - " + sectionData[0].getLocalizedTo(database)
        }

        viewHolder.view.setOnClickListener(null)
    }

    override fun getSectionCount(): Int {
        return sections.size
    }

    val hourFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", LocaleUtil.getLocale())

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): SearchHolder? {
        val v : View
        if (viewType == VIEW_TYPE_HEADER) {
            v = LayoutInflater.from(parent!!.context).inflate(R.layout.item_search_title, parent, false)
            return SearchHeaderHolder(v)
        } else {
            v = LayoutInflater.from(parent!!.context).inflate(R.layout.item_search_result, parent, false)
            return SearchResultHolder(v)
        }

    }

    inner open class SearchHolder(val view: View): RecyclerView.ViewHolder(view) {}

    inner class SearchResultHolder(view: View) : SearchHolder(view) {
        val tvTime: TextView
        val tvDriver: TextView
        val tvPrice : TextView

        init {
            tvTime = view.findViewById(R.id.item_result_time) as TextView
            tvDriver = view.findViewById(R.id.item_result_driver) as TextView
            tvPrice = view.findViewById(R.id.item_result_price) as TextView
        }
    }

    inner class SearchHeaderHolder(view: View) : SearchHolder(view) {
        val tvTitle: TextView

        init {
            tvTitle = view.findViewById(R.id.search_item_title) as TextView
        }

    }
}