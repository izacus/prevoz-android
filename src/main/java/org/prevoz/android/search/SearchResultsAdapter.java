package org.prevoz.android.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.prevoz.android.api.rest.RestSearchRide;

import java.util.List;

public class SearchResultsAdapter extends BaseAdapter
{
    private final List<RestSearchRide> results;
    private final LayoutInflater inflater;

    public SearchResultsAdapter(Context context, List<RestSearchRide> results)
    {
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.results = results;
    }

    @Override
    public int getCount()
    {
        return results.size();
    }

    @Override
    public Object getItem(int position)
    {
        return results.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return results.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;
        if (v == null)
            v = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);

        RestSearchRide ride = results.get(position);
        ((TextView)v.findViewById(android.R.id.text1)).setText(ride.fromCity + " - " + ride.toCity);
        return v;
    }
}
