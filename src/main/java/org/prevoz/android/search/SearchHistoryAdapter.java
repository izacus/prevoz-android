package org.prevoz.android.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import org.prevoz.android.R;
import org.prevoz.android.model.Route;
import org.prevoz.android.util.ContentUtils;
import org.prevoz.android.util.Database;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

import java.util.List;

public class SearchHistoryAdapter extends BaseAdapter implements StickyListHeadersAdapter
{
    private final LayoutInflater inflater;
    private final List<Route> searchHistory;

    public SearchHistoryAdapter(Context ctx)
    {
        this.searchHistory = ContentUtils.getLastSearches(ctx, 5);
        this.inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;
        if (v == null)
        {
            v = inflater.inflate(R.layout.item_search_title, parent, false);
            TextView title = (TextView) v.findViewById(R.id.search_item_title);
            title.setText("Zadnja iskanja");
        }

        return v;
    }

    @Override
    public long getHeaderId(int position)
    {
        return 0;
    }

    @Override
    public int getCount()
    {
        return searchHistory.size();
    }

    @Override
    public Object getItem(int position)
    {
        return searchHistory.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return searchHistory.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;

        if (v == null)
        {
            v = inflater.inflate(R.layout.item_search_history, parent, false);
        }

        TextView txt = (TextView) v.findViewById(R.id.item_history_text);
        txt.setText(searchHistory.get(position).toString());
        return v;
    }
}
