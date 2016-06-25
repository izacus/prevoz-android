package org.prevoz.android.search

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.prevoz.android.R
import org.prevoz.android.model.Route

class SearchHistoryAdapter(val context: Context, var data : List<Route>) : RecyclerView.Adapter<SearchHistoryHolder>() {

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): SearchHistoryHolder? {
        val v = LayoutInflater.from(context).inflate(R.layout.item_search_history, parent, false)
        return SearchHistoryHolder(v)
    }

    override fun onBindViewHolder(holder: SearchHistoryHolder?, position: Int) {
        val route = data.get(position)
        holder?.text?.text = route.toString()
    }
}

class SearchHistoryHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {

    val text: TextView

    init {
        text = itemView.findViewById(R.id.item_history_text) as TextView
    }

}
