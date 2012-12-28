package org.prevoz.android.search;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import org.prevoz.android.R;
import org.prevoz.android.rideinfo.Ride;
import org.prevoz.android.util.LocaleUtil;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Adapter for displaying list of search results in ListView
 * 
 * @author Jernej Virag
 * 
 */
public class SearchResultAdapter extends ArrayAdapter<SearchRide>
{
	/**
	 * Wrapper inside each result display in list to speed up new element
	 * creation
	 * 
	 * @author Jernej Virag
	 * 
	 */
	public static class SearchResultViewWrapper
	{
		private int rideId = 0;

		private View base = null;
		private TextView time = null;
        private TextView date = null;
		private TextView price = null;
		private TextView driver = null;

		/**
		 * Base row displayed
		 * 
		 * @param base
		 */
		public SearchResultViewWrapper(View base, int rideId)
		{
			this.base = base;
			this.rideId = rideId;
		}

		public int getRideId()
		{
			return rideId;
		}

		public TextView getTime()
		{
			if (this.time == null)
				this.time = (TextView) base.findViewById(R.id.time);

			return time;
		}

		public TextView getPrice()
		{
			if (this.price == null)
				this.price = (TextView) base.findViewById(R.id.price);

			return price;
		}

		public TextView getDriver()
		{
			if (this.driver == null)
				this.driver = (TextView) base.findViewById(R.id.driver);

			return driver;
		}

        public TextView getDate()
        {
            if (this.date == null)
                this.date = (TextView) base.findViewById(R.id.date);

            return date;
        }

		public void setId(int id)
		{
			this.rideId = id;
		}
	}

	private Activity context;
	private ArrayList<SearchRide> rides;
	private SimpleDateFormat timeFormatter;
	private HashSet<Integer> highlights;
	
	public SearchResultAdapter(Activity context, ArrayList<SearchRide> rides, int[] highlights)
	{
		super(context, R.layout.search_result, R.id.price, rides);
		this.context = context;
		this.rides = rides;
		this.highlights = new HashSet<Integer>();
        this.currentDate = Calendar.getInstance(LocaleUtil.getLocalTimezone());
		
		if (highlights != null)
		{
			for (int highlight : highlights)
			{
				this.highlights.add(highlight);
			}
		}
		
		timeFormatter = LocaleUtil.getSimpleDateFormat("HH:mm");
	}

    private Calendar currentDate = null;
	private Typeface defaultDriverTypeface = null;
	private Typeface defaultPriceTypeface = null;
	private Typeface defaultTimeTypeface = null;
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View row = null;
		SearchResultViewWrapper wrapper = null;

		if (convertView == null)
		{
			LayoutInflater inflater = context.getLayoutInflater();
			row = inflater.inflate(R.layout.search_result, parent, false);
			wrapper = new SearchResultViewWrapper(row, rides.get(position).getId());
			row.setTag(wrapper);
		}
		else
		{
			row = convertView;
			wrapper = (SearchResultViewWrapper) row.getTag();
		}

        SearchRide ride = rides.get(position);
		
		if (defaultDriverTypeface == null)
			defaultDriverTypeface = wrapper.getDriver().getTypeface();
		
		if (defaultPriceTypeface == null)
			defaultPriceTypeface = wrapper.getPrice().getTypeface();
		
		if (defaultTimeTypeface == null)
			defaultTimeTypeface = wrapper.getTime().getTypeface();

		wrapper.getTime().setText(timeFormatter.format(ride.getTime().getTime()));

		if (ride.getPrice() != null)
		{
			wrapper.getPrice().setText(String.format(LocaleUtil.getLocale(), "%1.1f €", ride.getPrice()));
		}
		else
		{
			wrapper.getPrice().setText("? €");
		}

		wrapper.getDriver().setText(ride.getAuthor());
		wrapper.setId(ride.getId());

		if (highlights.contains(ride.getId()))
		{
			wrapper.getDriver().setTypeface(defaultDriverTypeface, Typeface.BOLD);
			wrapper.getPrice().setTypeface(defaultPriceTypeface, Typeface.BOLD);
			wrapper.getTime().setTypeface(defaultTimeTypeface, Typeface.BOLD);
		}
		else
		{
			wrapper.getDriver().setTypeface(defaultDriverTypeface, Typeface.NORMAL);
			wrapper.getPrice().setTypeface(defaultPriceTypeface, Typeface.NORMAL);
			wrapper.getTime().setTypeface(defaultTimeTypeface, Typeface.NORMAL);
		} 

        if (currentDate.get(Calendar.YEAR) != ride.getTime().get(Calendar.YEAR) ||
            currentDate.get(Calendar.MONTH) != ride.getTime().get(Calendar.MONTH)||
            currentDate.get(Calendar.DATE) != ride.getTime().get(Calendar.DATE))
        {
            wrapper.getDate().setText(LocaleUtil.getShortFormattedDate(context.getResources(), ride.getTime()));
            wrapper.getDate().setVisibility(View.VISIBLE);
        }
        else
        {
            wrapper.getDate().setVisibility(View.GONE);
        }

		return row;
	}
}
