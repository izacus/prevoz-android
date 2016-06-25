package org.prevoz.android.search

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.prevoz.android.R
import org.prevoz.android.api.rest.RestRide
import org.prevoz.android.util.LocaleUtil
import org.threeten.bp.format.DateTimeFormatter

class SearchResultsAdapter(val results : List<RestRide>) : RecyclerView.Adapter<SearchResultsAdapter.SearchResultHolder>() {

    val hourFormatter = DateTimeFormatter.ofPattern("HH:mm")

    override fun onBindViewHolder(holder: SearchResultHolder?, position: Int) {
        val result = results[position]
        holder!!.tvDriver.text = result.author
        holder.tvTime.text = hourFormatter.format(result.date)

        val price = result.price
        if (price == null) {
            holder.tvPrice.visibility = View.INVISIBLE
        } else {
            holder.tvPrice.visibility = View.VISIBLE
            holder.tvPrice.text = LocaleUtil.getFormattedCurrency(price)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): SearchResultHolder? {
        val v = LayoutInflater.from(parent!!.context).inflate(R.layout.item_search_result, parent, false)
        return SearchResultHolder(v)
    }

    override fun getItemCount(): Int {
        return results.size
    }

    inner class SearchResultHolder(val view : View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView
        val tvDriver: TextView
        val tvPrice : TextView

        init {
            tvTime = view.findViewById(R.id.item_result_time) as TextView
            tvDriver = view.findViewById(R.id.item_result_driver) as TextView
            tvPrice = view.findViewById(R.id.item_result_price) as TextView
        }

    }
}