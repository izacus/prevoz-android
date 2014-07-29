package org.prevoz.android.search;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.prevoz.android.R;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.util.LocaleUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class SearchResultsAdapter extends BaseAdapter implements StickyListHeadersAdapter
{
    private final Context context;

    private List<RestRide> results;
    private Set<Integer> highlights;
    private final LayoutInflater inflater;

    public SearchResultsAdapter(Context context, List<RestRide> results, int[] highlights)
    {
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        buildResults(results, highlights);
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

        SpannableStringBuilder time = new SpannableStringBuilder(LocaleUtil.getFormattedTime(ride.date));
        if (highlights.contains(ride.id.intValue()))
        {
            time.append("*");
            time.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.prevoztheme_color)), time.length() - 1, time.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            time.setSpan(new RelativeSizeSpan(0.6f), time.length() - 1, time.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            time.setSpan(new SuperscriptSpan(), time.length() - 1, time.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        holder.time.setText(time);

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

    public synchronized void setResults(List<RestRide> rides, int[] highlights)
    {
        buildResults(rides, highlights);
        notifyDataSetChanged();
    }

    private void buildResults(List<RestRide> rides, int[] highlightIds)
    {
        Collections.sort(rides);
        highlights = new HashSet<Integer>();
        for (int id : highlights)
            highlights.add(id);
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

    public void removeRide(Long id)
    {
        // Find ride
        RestRide ride = null;
        for (RestRide r : results)
        {
            if (r.id.equals(id))
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
