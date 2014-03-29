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
    private final Context context;

    private List<RestSearchRide> results;
    private final LayoutInflater inflater;

    public SearchResultsAdapter(Context context, List<RestSearchRide> results)
    {
        this.context = context;
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
        return results.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;

        if (v == null)
        {
            v = inflater.inflate(R.layout.item_search_result, parent, false);
        }

        RestSearchRide ride = results.get(position);
        TextView time = (TextView) v.findViewById(R.id.item_result_time);
        time.setText(timeFormatter.format(ride.date));

        TextView price = (TextView) v.findViewById(R.id.item_result_price);

        if (ride.price == 0)
        {
            price.setVisibility(View.INVISIBLE);
        }
        else
        {
            price.setText(String.format(Locale.GERMAN, "%1.1f €", ride.price));
            price.setVisibility(View.VISIBLE);
        }

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
        this.results = rides;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent)
    {
        View v = convertView;
        if (v == null)
        {
            v = inflater.inflate(R.layout.item_search_title, parent, false);
        }

        RestSearchRide item = results.get(position);
        TextView titleView = (TextView) v.findViewById(R.id.search_item_title);

        String titleText = LocaleUtil.getLocalizedCityName(context, item.fromCity, item.fromCountry) +
                           " - " +
                           LocaleUtil.getLocalizedCityName(context, item.toCity, item.toCountry);

        titleView.setText(titleText);
        return v;
    }

    @Override
    public long getHeaderId(int position)
    {
        RestSearchRide ride = results.get(position);
        return (ride.fromCity.hashCode() + ride.fromCountry.hashCode()) * (ride.toCity.hashCode() + ride.toCountry.hashCode());
    }
}
