package org.prevoz.android.search;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;

import org.prevoz.android.R;
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
		
		if (highlights != null)
		{
			for (int highlight : highlights)
			{
				this.highlights.add(highlight);
			}
		}
		
		timeFormatter = LocaleUtil.getSimpleDateFormat("HH:mm");
	}

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
			row = inflater.inflate(R.layout.search_result, null);
			wrapper = new SearchResultViewWrapper(row, rides.get(position).getId());
			row.setTag(wrapper);
		}
		else
		{
			row = convertView;
			wrapper = (SearchResultViewWrapper) row.getTag();
		}
		
		if (defaultDriverTypeface == null)
			defaultDriverTypeface = wrapper.getDriver().getTypeface();
		
		if (defaultPriceTypeface == null)
			defaultPriceTypeface = wrapper.getPrice().getTypeface();
		
		if (defaultTimeTypeface == null)
			defaultTimeTypeface = wrapper.getTime().getTypeface();

		wrapper.getTime().setText(timeFormatter.format(rides.get(position).getTime()));

		if (rides.get(position).getPrice() != null)
		{
			wrapper.getPrice().setText(String.format(LocaleUtil.getLocale(), "%1.1f €", rides.get(position).getPrice()));
		}
		else
		{
			wrapper.getPrice().setText("");
		}

		wrapper.getDriver().setText(rides.get(position).getAuthor());
		wrapper.setId(rides.get(position).getId());
		
		if (highlights.contains(rides.get(position).getId()))
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
		
		return row;
	}
}
