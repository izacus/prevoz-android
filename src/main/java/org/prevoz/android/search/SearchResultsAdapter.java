package org.prevoz.android.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import org.prevoz.android.R;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.util.LocaleUtil;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SearchResultsAdapter extends BaseAdapter implements StickyListHeadersAdapter
{
    private final Context context;

    private List<RestRide> results;
    private final LayoutInflater inflater;

    public SearchResultsAdapter(Context context, List<RestRide> results)
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
            TextView time = (TextView) v.findViewById(R.id.item_result_time);
            TextView price = (TextView) v.findViewById(R.id.item_result_price);
            TextView driver = (TextView) v.findViewById(R.id.item_result_driver);

            v.setTag(new ResultsViewHolder(time, price, driver));
        }

        ResultsViewHolder holder = (ResultsViewHolder) v.getTag();
        RestRide ride = results.get(position);
        holder.time.setText(LocaleUtil.getFormattedTime(ride.date));


        if (ride.price == null || ride.price == 0)
        {
            holder.price.setVisibility(View.INVISIBLE);
        }
        else
        {
            holder.price.setText(String.format(Locale.GERMAN, "%1.1f €", ride.price));
            holder.price.setVisibility(View.VISIBLE);
        }

        holder.driver.setText(ride.author);
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

    public void setResults(List<RestRide> rides)
    {
        buildResults(rides);
        notifyDataSetChanged();
    }

    private void buildResults(List<RestRide> rides)
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
            TextView titleView = (TextView) v.findViewById(R.id.search_item_title);
            v.setTag(titleView);
        }

        RestRide item = results.get(position);

        String titleText = LocaleUtil.getLocalizedCityName(context, item.fromCity, item.fromCountry) +
                           " - " +
                           LocaleUtil.getLocalizedCityName(context, item.toCity, item.toCountry);

        TextView titleView = (TextView) v.getTag();
        titleView.setText(titleText);
        return v;
    }

    @Override
    public long getHeaderId(int position)
    {
        RestRide ride = results.get(position);
        return (ride.fromCity.hashCode() + ride.fromCountry.hashCode()) * (ride.toCity.hashCode() + ride.toCountry.hashCode());
    }

    public void removeRide(Long id)
    {
        // Find ride
        RestRide ride = null;
        for (RestRide r : results)
        {
            if (r.id == id)
            {
                ride = r;
                break;
            }
        }

        if (ride == null)   return;

        results.remove(ride);
        notifyDataSetChanged();
    }

    private static class ResultsViewHolder
    {
        final TextView time;
        final TextView price;
        final TextView driver;

        private ResultsViewHolder(TextView time, TextView price, TextView driver)
        {
            this.time = time;
            this.price = price;
            this.driver = driver;
        }
    }
}
