package org.prevoz.android.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.prevoz.android.R;
import org.prevoz.android.api.rest.RestSearchRide;
import org.prevoz.android.util.LocaleUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SearchResultsAdapter extends BaseAdapter
{
    private static final SimpleDateFormat timeFormatter = LocaleUtil.getSimpleDateFormat("HH:mm");

    private List<Result> results;
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
        if (result instanceof ResultTitle)
        {
            ResultTitle resultTitle = (ResultTitle)result;

            if (v == null)
            {
                v = inflater.inflate(R.layout.item_search_title, parent, false);
            }

            TextView titleView = (TextView) v.findViewById(R.id.search_item_title);
            titleView.setText(resultTitle.title);

            return v;
        }
        else
        {
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
    }

    @Override
    public int getViewTypeCount()
    {
        return 2;
    }

    @Override
    public int getItemViewType(int position)
    {
        if (results.get(position) instanceof ResultTitle)
            return 0;

        return 1;
    }

    @Override
    public boolean isEnabled(int position)
    {
        return results.get(position) instanceof ResultItem;
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

    private void buildResults(List<RestSearchRide> rides)
    {
        Collections.sort(rides);

        ArrayList<Result> results = new ArrayList<Result>();
        String lastPair = "";
        for (RestSearchRide ride : rides)
        {
            String pair = ride.fromCity + " - " + ride.toCity;

            if (!(pair).equals(lastPair))
            {
                ResultTitle title = new ResultTitle();
                title.title = pair;
                results.add(title);
            }

            ResultItem item = new ResultItem();
            item.ride = ride;
            results.add(item);

            lastPair = pair;
        }

        this.results = results;
    }


    private interface Result
    {
        public long getId();
    };

    private static class ResultTitle implements Result
    {
        String title;


        @Override
        public long getId()
        {
            return title.hashCode();
        }
    }

    private static class ResultItem implements Result
    {
        RestSearchRide ride;

        @Override
        public long getId()
        {
            return ride.id;
        }
    }
}
