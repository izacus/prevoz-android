package org.prevoz.android.myrides;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;

import org.prevoz.android.R;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.model.City;
import org.prevoz.android.model.Route;
import org.prevoz.android.ride.RideInfoActivity;
import org.prevoz.android.util.LocaleUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class MyRidesAdapter extends RecyclerView.Adapter<MyRidesAdapter.ResultsViewHolder> implements StickyRecyclerHeadersAdapter<MyRidesAdapter.HeadersViewHolder>
{
    @NonNull private final FragmentActivity context;
    @NonNull private final List<RestRide> myrides;
    @NonNull private final LayoutInflater inflater;
    @NonNull private final LocaleUtil localeUtil;

    public MyRidesAdapter(@NonNull FragmentActivity context, @NonNull LocaleUtil localeUtil)
    {
        this.context = context;
        this.inflater = (LayoutInflater) Objects.requireNonNull(context.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        this.myrides = new ArrayList<>();
        this.localeUtil = localeUtil;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public ResultsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.item_myride, parent, false);
        return new ResultsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultsViewHolder holder, int position) {
        final RestRide ride = myrides.get(position);
        SpannableStringBuilder time = new SpannableStringBuilder(LocaleUtil.getFormattedTime(ride.date));

        holder.time.setText(time);
        holder.date.setText(localeUtil.getShortFormattedDate(ride.date));

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
        holder.card.setOnClickListener(v -> {
            RideInfoActivity.show(context, ride, ride.isAuthor ? RideInfoActivity.PARAM_ACTION_EDIT : RideInfoActivity.PARAM_ACTION_SHOW, 0);
        });
    }

    @Override
    public long getItemId(int position)
    {
        return myrides.get(position).id;
    }

    @Override
    public long getHeaderId(int position) {
        return myrides.get(position).isAuthor ? 0 : 1;
    }

    @Override
    public HeadersViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
        View v = inflater.inflate(R.layout.item_search_title, parent, false);
        return new HeadersViewHolder(v);
    }

    @Override
    public void onBindHeaderViewHolder(HeadersViewHolder headersViewHolder, int position) {
        headersViewHolder.text.setText(!myrides.get(position).isAuthor ? context.getString(R.string.myrides_header_bookmarks) : context.getString(R.string.myrides_header_published_rides));
    }

    @Override
    public int getItemCount() {
        return myrides.size();
    }

    public void setRides(List<RestRide> rides) {
        if (rides == null || rides.size() == 0) return;
        myrides.clear();
        myrides.addAll(rides);
        Collections.sort(myrides, (r1, r2) -> {
            if (r1.isAuthor && !r2.isAuthor)
                return 1;

            if (r2.isAuthor && !r1.isAuthor)
                return -1;

            int dateCompare = r1.date.compareTo(r2.date);
            if (dateCompare == 0)
                return r1.compareTo(r2);

            return dateCompare;
        });

        notifyDataSetChanged();
    }

    public void clear() {
        if (myrides.size() > 0) {
            myrides.clear();
            notifyDataSetChanged();
        }
    }

    public void updateRide(@NonNull RestRide ride) {
        if (myrides.size() == 0) return;
        for (int i = 0; i < myrides.size(); i++) {
            if (myrides.get(i).id == null) continue;
            if (myrides.get(i).id.equals(ride.id)) {
                myrides.set(i, ride);
                notifyItemChanged(i);
                return;
            }
        }
    }

    static class ResultsViewHolder extends RecyclerView.ViewHolder {
        final View card;
        final TextView time;
        final TextView price;
        final TextView path;
        final TextView date;

        ResultsViewHolder(View v) {
            super(v);
            card = v.findViewById(R.id.item_myride_card);
            time = (TextView) v.findViewById(R.id.item_myride_time);
            date = (TextView) v.findViewById(R.id.item_myride_date);
            price = (TextView) v.findViewById(R.id.item_myride_price);
            path = (TextView) v.findViewById(R.id.item_myride_path);
        }
    }

    static class HeadersViewHolder extends RecyclerView.ViewHolder {

        final TextView text;

        HeadersViewHolder(View itemView) {
            super(itemView);
            text = (TextView)itemView.findViewById(R.id.search_item_title);
        }
    }
}
