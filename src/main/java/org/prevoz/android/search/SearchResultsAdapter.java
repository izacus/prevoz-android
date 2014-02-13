package org.prevoz.android.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.prevoz.android.R;
import org.prevoz.android.api.rest.RestSearchResults;
import org.prevoz.android.api.rest.RestSearchRide;
import org.prevoz.android.util.LocaleUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class SearchResultsAdapter extends BaseAdapter implements StickyListHeadersAdapter
{
    private static final SimpleDateFormat timeFormatter = LocaleUtil.getSimpleDateFormat("HH:mm");

    private List<ResultItem> results;
    private final LayoutInflater inflater;

    public SearchResultsAdapter(Context context, List<RestSearchRide> results)
    {
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        buildResults(results);
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
        return results.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;


        Result result = results.get(position);

        ResultItem rideItem = (ResultItem)result;

        if (v == null)
        {
            v = inflater.inflate(R.layout.item_search_result, parent, false);
        }

        RestSearchRide ride = rideItem.ride;
        TextView time = (TextView) v.findViewById(R.id.item_result_time);
        time.setText(timeFormatter.format(ride.date));

        TextView price = (TextView) v.findViewById(R.id.item_result_price);
        price.setText(String.format(Locale.GERMAN, "%1.1f €", ride.price));

        TextView driver = (TextView) v.findViewById(R.id.item_result_driver);
        driver.setText(ride.author);
        return v;
    }

    @Override
    public boolean hasStableIds()
    {
        return true;
    }

    @Override
    public boolean areAllItemsEnabled()
    {
        return false;
    }

    public void setResults(List<RestSearchRide> rides)
    {
        buildResults(rides);
        notifyDataSetChanged();
    }

    private void buildResults(List<RestSearchRide> rides)
    {
        Collections.sort(rides);

        ArrayList<ResultItem> results = new ArrayList<ResultItem>();
        for (RestSearchRide ride : rides)
        {
            ResultItem item = new ResultItem();
            item.ride = ride;
            results.add(item);
        }

        this.results = results;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;
        if (v == null)
        {
            v = inflater.inflate(R.layout.item_search_title, parent, false);
        }

        ResultItem item = results.get(position);
        TextView titleView = (TextView) v.findViewById(R.id.search_item_title);
        titleView.setText(item.getTitle());
        return v;
    }

    @Override
    public long getHeaderId(int position)
    {
        ResultItem item = results.get(position);
        return item.ride.fromCity.hashCode() * item.ride.toCity.hashCode();
    }


    private interface Result
    {
        public long getId();
    };

    private static class ResultItem implements Result
    {
        RestSearchRide ride;

        @Override
        public long getId()
        {
            return ride.id;
        }

        public String getTitle() { return ride.fromCity + " - " + ride.toCity; }
    }
}
