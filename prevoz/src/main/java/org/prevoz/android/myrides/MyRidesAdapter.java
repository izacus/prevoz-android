package org.prevoz.android.myrides;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.CardView;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.prevoz.android.R;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.model.City;
import org.prevoz.android.model.Route;
import org.prevoz.android.ride.RideInfoFragment;
import org.prevoz.android.util.LocaleUtil;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class MyRidesAdapter extends BaseAdapter implements StickyListHeadersAdapter
{
    private final FragmentActivity context;

    private List<RestRide> results;
    private final LayoutInflater inflater;

    public MyRidesAdapter(FragmentActivity context, List<RestRide> results)
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
            v = inflater.inflate(R.layout.item_myride, parent, false);
            CardView c = (CardView)v.findViewById(R.id.item_myride_card);
            TextView time = (TextView) v.findViewById(R.id.item_myride_time);
            TextView date = (TextView) v.findViewById(R.id.item_myride_date);
            TextView price = (TextView) v.findViewById(R.id.item_myride_price);
            TextView driver = (TextView) v.findViewById(R.id.item_myride_path);

            v.setTag(new ResultsViewHolder(c, time, date, price, driver));
        }

        final ResultsViewHolder holder = (ResultsViewHolder) v.getTag();
        final RestRide ride = results.get(position);

        SpannableStringBuilder time = new SpannableStringBuilder(LocaleUtil.getFormattedTime(ride.date));

        holder.time.setText(time);
        holder.date.setText(LocaleUtil.getShortFormattedDate(context.getResources(), ride.date));

        if (ride.price == null || ride.price == 0)
        {
            holder.price.setVisibility(View.INVISIBLE);
        }
        else
        {
            holder.price.setText(String.format(Locale.GERMAN, "%1.1f €", ride.price));
            holder.price.setVisibility(View.VISIBLE);
        }

        Route r = new Route(new City(ride.fromCity, ride.fromCountry), new City(ride.toCity, ride.toCountry));
        holder.path.setText(r.toString());

        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RideInfoFragment rideInfo = RideInfoFragment.newInstance(ride);
                FragmentTransaction ft = context.getSupportFragmentManager().beginTransaction();
                ft.add(rideInfo, null);
                ft.commitAllowingStateLoss();
            }
        });


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

        TextView titleView = (TextView) v.getTag();
        if (position >= results.size())
        {
            titleView.setText("");
            return v;
        }


        RestRide item = results.get(position);
        String titleText = LocaleUtil.getLocalizedCityName(context, item.fromCity, item.fromCountry) +
                           " - " +
                           LocaleUtil.getLocalizedCityName(context, item.toCity, item.toCountry);

        titleView.setText(titleText);
        return v;
    }

    @Override
    public long getHeaderId(int position)
    {
        // Guard for some rare corner-cases
        if (position >= results.size()) return -1;

        RestRide ride = results.get(position);
        return (ride.fromCity.hashCode() + ride.fromCountry.hashCode()) * (ride.toCity.hashCode() + ride.toCountry.hashCode());
    }

    private static class ResultsViewHolder
    {
        final CardView card;
        final TextView time;
        final TextView price;
        final TextView path;
        final TextView date;

        private ResultsViewHolder(CardView card, TextView time, TextView date, TextView price, TextView path)
        {
            this.card = card;
            this.time = time;
            this.date = date;
            this.price = price;
            this.path = path;
        }
    }
}
