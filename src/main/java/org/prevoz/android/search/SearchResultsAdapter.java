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
import java.util.List;
import java.util.Locale;

public class SearchResultsAdapter extends BaseAdapter
{
    private static final SimpleDateFormat timeFormatter = LocaleUtil.getSimpleDateFormat("HH:mm");

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
        {
            v = inflater.inflate(R.layout.item_search_result, parent, false);
        }

        RestSearchRide ride = results.get(position);
        TextView time = (TextView) v.findViewById(R.id.item_result_time);
        time.setText(timeFormatter.format(ride.date));

        TextView path = (TextView) v.findViewById(R.id.item_result_path);
        path.setText(ride.getFrom().toString().replace(' ', '\u00A0') + " - " + ride.getTo().toString().replace(' ', '\u00A0'));      // Do not break spaces inside the city name

        TextView price = (TextView) v.findViewById(R.id.item_result_price);
        price.setText(String.format(Locale.GERMAN, "%1.1f€", ride.price));

        TextView driver = (TextView) v.findViewById(R.id.item_result_driver);
        driver.setText(ride.author);
        return v;
    }
}
