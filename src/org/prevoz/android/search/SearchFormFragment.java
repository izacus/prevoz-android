package org.prevoz.android.search;

import java.util.ArrayList;
import java.util.Calendar;

import org.prevoz.android.CitySelectorActivity;
import org.prevoz.android.MainActivity;
import org.prevoz.android.R;
import org.prevoz.android.Route;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.LocaleUtil;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class SearchFormFragment extends Fragment 
{
	private static final int FROM_CITY_REQUEST = 1;
	private static final int TO_CITY_REQUEST = 2;
	
	private Button buttonDate;
	private Button buttonSearch;
	
	private Button buttonFrom;
	private Button buttonTo;
	
	private ListView lastSearches;
	
	private String from = "";
	private String to = "";
	private Calendar selectedDate;
	
	private void prepareFormFields()
	{
		lastSearches = (ListView)getActivity().findViewById(R.id.search_last_list);
		
		buttonDate = (Button)getActivity().findViewById(R.id.date_button);
		buttonSearch = (Button)getActivity().findViewById(R.id.search_button);
		
		buttonFrom = (Button)getActivity().findViewById(R.id.from_button);
		setLocationButtonText(buttonFrom, from);
		buttonFrom.setOnClickListener(new OnClickListener() 
		{
			public void onClick(View v) 
			{
				Intent intent = new Intent(getActivity(), CitySelectorActivity.class);
				getActivity().startActivityForResult(intent, FROM_CITY_REQUEST);
			}
		});

		buttonTo = (Button)getActivity().findViewById(R.id.to_button);
		setLocationButtonText(buttonTo, to);
		buttonTo.setOnClickListener(new OnClickListener() 
		{
			public void onClick(View v) 
			{
				Intent intent = new Intent(getActivity(), CitySelectorActivity.class);
				getActivity().startActivityForResult(intent, TO_CITY_REQUEST);
			}
		});
		
		// Set initial date
		buttonDate.setText(localizeDate(selectedDate));
		buttonDate.setOnClickListener(new OnClickListener() 
		{
			
			public void onClick(View v) 
			{
				getActivity().showDialog(MainActivity.DIALOG_SEARCH_DATE);
			}
		});
		
		// Set search button action
		buttonSearch.setOnClickListener(new OnClickListener()
		{
			
			public void onClick(View arg0)
			{
				startSearch();
			}
		});
	}
	
	private void populateLastSearchList()
	{
		// Last search list is not always displayed
		if (lastSearches == null)
			return;
		
		ArrayList<Route> routes = Database.getLastSearches(getActivity(), 3);
		
		if (routes.size() > 0)
		{
			ArrayAdapter<Route> lastSearchesAdapter = new ArrayAdapter<Route>(getActivity(), R.layout.last_search_item, routes);
			
			lastSearches.setAdapter(lastSearchesAdapter);
			
			// Set click handler to populate form data
			lastSearches.setOnItemClickListener(new OnItemClickListener() 
			{
				public void onItemClick(AdapterView<?> parent, 
										View view,
										int position, 
										long id) 
				{
					Route route = (Route) parent.getItemAtPosition(position);
					from = route.getFrom();
					setLocationButtonText(buttonFrom, route.getFrom());
					to = route.getTo();
					setLocationButtonText(buttonTo, route.getTo());
				}
			});
			
			getActivity().findViewById(R.id.last_search_label).setVisibility(View.VISIBLE);
			lastSearches.setVisibility(View.VISIBLE);
		}
		else
		{
			getActivity().findViewById(R.id.last_search_label).setVisibility(View.INVISIBLE);
			lastSearches.setVisibility(View.INVISIBLE);
		}
		
	}
	
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) 
	{
		super.onActivityCreated(savedInstanceState);
		
		selectedDate = Calendar.getInstance();
		if (savedInstanceState != null)
		{
			if (savedInstanceState.containsKey("selected_date"))
			{
				selectedDate.setTimeInMillis(savedInstanceState.getLong("selected_date"));
			}
			
			if (savedInstanceState.containsKey("from") && savedInstanceState.containsKey("to"))
			{
				from = savedInstanceState.getString("from");
				to = savedInstanceState.getString("to");
			}
		}
		
		prepareFormFields();
	}

	
	
	@Override
	public void onResume() 
	{
		super.onResume();
		populateLastSearchList();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, 
							 ViewGroup container,
							 Bundle savedInstanceState) 
	{		
		View newView = inflater.inflate(R.layout.search_form_frag, container, false);
		return newView;
	}
	
	
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putLong("selected_date", selectedDate.getTimeInMillis());
		outState.putString("from", from);
		outState.putString("to", to);
	}

	private void setLocationButtonText(Button button, String location)
	{
		if (location == null || location.length() == 0)
		{
			button.setTextColor(Color.LTGRAY);
			button.setText(getString(R.string.all_locations));
		}
		else
		{
			button.setTextColor(Color.BLACK);
			button.setText(location);
		}
	}
	
	/**
	 * Builds a localized date string with day name
	 */
	private String localizeDate(Calendar date)
	{
		Resources resources = getResources();
		
		Calendar now = Calendar.getInstance();
		// Check for today and tomorrow
		if (date.get(Calendar.ERA) == now.get(Calendar.ERA) && 
			date.get(Calendar.YEAR) == now.get(Calendar.YEAR))
		{
			// Today
			if (date.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR))
			{
				return resources.getString(R.string.today);
			}
			
			// Add one day to now to get tomorrows date
			now.roll(Calendar.DAY_OF_YEAR, 1);
			
			// Tomorrow, because we added one day to now
			if (date.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR))
			{
				return resources.getString(R.string.tomorrow);
			}
		}

		StringBuilder dateString = new StringBuilder();

		dateString.append(LocaleUtil.getDayName(resources, date) + ", ");
		dateString.append(LocaleUtil.getFormattedDate(resources, date));

		return dateString.toString();
	}
	
	public void setSelectedDate(Calendar date)
	{
		this.selectedDate = date;
		buttonDate.setText(localizeDate(selectedDate));
	}
	
	public void onParentActivityResult(int requestCode,
									   int resultCode,
									   Intent intent)
	{
		if (resultCode != Activity.RESULT_OK)
		{
			switch (requestCode)
			{
				case FROM_CITY_REQUEST:
					from = "";
					setLocationButtonText(buttonFrom, from);
					return;
				case TO_CITY_REQUEST:
					to = "";
					setLocationButtonText(buttonTo, to);
					return;
			}
		}
		
		switch(requestCode)
		{
			case FROM_CITY_REQUEST:
				from = intent.getStringExtra("city");
				setLocationButtonText(buttonFrom, from);
				break;
			case TO_CITY_REQUEST:
				to = intent.getStringExtra("city");
				setLocationButtonText(buttonTo, to);
				break;
				
			default:
				Log.e(this.toString(), "Handling activity result that does not belong here!");
				return;
		}
	}
	
	private void startSearch()
	{
		Log.i(this.toString(), "Starting search for " + from + " - " + to);
		
		// Record search request
		Database.addSearchToHistory(getActivity(), from, to, Calendar.getInstance().getTime());
		
		MainActivity mainActivity = (MainActivity) getActivity();
		mainActivity.startSearch(from, to, selectedDate);
	}
}
