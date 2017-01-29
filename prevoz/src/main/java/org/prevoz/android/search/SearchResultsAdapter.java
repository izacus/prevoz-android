package org.prevoz.android.search;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentActivity;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.prevoz.android.R;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.model.Bookmark;
import org.prevoz.android.model.City;
import org.prevoz.android.model.PrevozDatabase;
import org.prevoz.android.model.Route;
import org.prevoz.android.ride.RideInfoActivity;
import org.prevoz.android.util.LocaleUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

public class SearchResultsAdapter extends BaseAdapter implements StickyListHeadersAdapter
{
    private final FragmentActivity context;
    private final PrevozDatabase database;

    private List<RestRide> results;
    private Set<Integer> highlights;
    private final LayoutInflater inflater;

    public SearchResultsAdapter(@NonNull FragmentActivity context, @NonNull PrevozDatabase database, @NonNull List<RestRide> results, @NonNull int[] highlights, @Nullable Route askedForRoute)
    {
        this.context = context;
        this.database = database;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        buildResults(results, highlights, askedForRoute);
    }

    @Override
    public int getCount()
    {
        return results.size();
    }

    @Override
    public RestRide getItem(int position)
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
            View c = v.findViewById(R.id.item_result_card);
            TextView time = (TextView) v.findViewById(R.id.item_result_time);
            TextView price = (TextView) v.findViewById(R.id.item_result_price);
            TextView driver = (TextView) v.findViewById(R.id.item_result_driver);
            ImageView bookmark = (ImageView) v.findViewById(R.id.item_result_bookmark);

            v.setTag(new ResultsViewHolder(c, time, price, driver, bookmark));
        }

        final ResultsViewHolder holder = (ResultsViewHolder) v.getTag();
        final RestRide ride = results.get(position);

        SpannableStringBuilder time = new SpannableStringBuilder(LocaleUtil.getFormattedTime(ride.date));
        if (highlights.contains(ride.id.intValue()))
        {
            time.append("*");
            time.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.prevoztheme_color)), time.length() - 1, time.length(), SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
            time.setSpan(new RelativeSizeSpan(0.6f), time.length() - 1, time.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            time.setSpan(new SuperscriptSpan(), time.length() - 1, time.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }

        if (Bookmark.shouldShow(ride.bookmark)) {
            holder.bookmark.getDrawable().setColorFilter(new PorterDuffColorFilter(context.getResources().getColor(R.color.prevoztheme_color), PorterDuff.Mode.SRC_ATOP));
            holder.bookmark.setVisibility(View.VISIBLE);
        } else {
            holder.bookmark.setVisibility(View.INVISIBLE);
        }

        holder.time.setText(time);

        if (ride.price == null || ride.price == 0)
        {
            holder.price.setVisibility(View.INVISIBLE);
        }
        else
        {
            holder.price.setText(LocaleUtil.getFormattedCurrency(ride.price));
            holder.price.setVisibility(View.VISIBLE);
        }

        holder.driver.setText(ride.author);
        holder.card.setOnClickListener(v1 -> {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(holder.card, 0, 0, holder.card.getWidth(), holder.card.getHeight());
            RideInfoActivity.show(context, ride, options.toBundle());
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

    public synchronized void setResults(@NonNull List<RestRide> rides, @NonNull int[] highlights, @Nullable Route askedForRoute)
    {
        buildResults(rides, highlights, askedForRoute);
        notifyDataSetChanged();
    }

    private void buildResults(@NonNull List<RestRide> rides, @NonNull int[] highlightIds, @Nullable Route askedForRoute)
    {
        Collections.sort(rides, (r1, r2) -> {
            // Check if this is a preferred route
            int cityNameCompare = 0;
            if (askedForRoute != null && !r1.getRoute().equals(r2.getRoute())) {
                if (r1.getRoute().equals(askedForRoute)) {
                    cityNameCompare = -1;
                } else if (r2.getRoute().equals(askedForRoute)) {
                    cityNameCompare = 1;
                }
            }

            if (cityNameCompare == 0) {
                cityNameCompare = (r1.fromCity + r1.toCity).compareTo(r2.fromCity + r2.toCity);
            }

            if (cityNameCompare == 0) {
                if (r1.date != null && r2.date != null) {
                    return r1.date.compareTo(r2.date);
                } else if (r1.published != null && r2.published != null) {
                    return r1.compareTo(r2.published);
                }
            }

            return cityNameCompare;
        });

        highlights = new HashSet<>();
        for (int id : highlightIds)
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
        String titleText = LocaleUtil.getLocalizedCityName(database, item.fromCity, item.fromCountry) +
                           " - " +
                           LocaleUtil.getLocalizedCityName(database, item.toCity, item.toCountry);

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

    public void updateRide(RestRide ride) {
        int rideIndex = -1;
        for (int i = 0; i < results.size(); i++) {
            if (ride.id.equals(results.get(i).id)) {
                rideIndex = i;
                break;
            }
        }

        if (rideIndex == -1) return;
        results.remove(rideIndex);
        results.add(rideIndex, ride);
        notifyDataSetChanged();
    }

    private static class ResultsViewHolder
    {
        final View card;
        final TextView time;
        final TextView price;
        final TextView driver;
        final ImageView bookmark;

        private ResultsViewHolder(View card, TextView time, TextView price, TextView driver, ImageView bookmark)
        {
            this.card = card;
            this.time = time;
            this.price = price;
            this.driver = driver;
            this.bookmark = bookmark;
        }
    }
}
