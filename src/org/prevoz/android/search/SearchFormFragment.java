package org.prevoz.android.search;

import java.util.ArrayList;
import java.util.Calendar;

import org.prevoz.android.CitySelectorActivity;
import org.prevoz.android.MainActivity;
import org.prevoz.android.R;
import org.prevoz.android.Route;
import org.prevoz.android.my_rides.MyRidesActivity;
import org.prevoz.android.c2dm.NotificationManager;
import org.prevoz.android.c2dm.NotificationsActivity;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.StringUtil;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.ImageButton;
import android.widget.ListView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class SearchFormFragment extends Fragment 
{
	private static final int FROM_CITY_REQUEST = 1;
	private static final int TO_CITY_REQUEST = 2;
	
	private ImageButton notifications;
	
	private Button buttonDate;
	private Button buttonSearch;
	
	private Button buttonFrom;
	private Button buttonTo;
	
	private ImageButton buttonMyRides;
	
	private ListView lastSearches;
	
	private String from = "";
	private String to = "";
	private Calendar selectedDate;
	
	private GoogleAnalyticsTracker tracker;
	
	private void prepareFormFields()
	{
		lastSearches = (ListView)getActivity().findViewById(R.id.search_last_list);
		
		buttonDate = (Button)getActivity().findViewById(R.id.date_button);
		buttonSearch = (Button)getActivity().findViewById(R.id.search_button);
		
		buttonFrom = (Button)getActivity().findViewById(R.id.from_button);
		StringUtil.setLocationButtonText(buttonFrom, from, getString(R.string.all_locations));
		buttonFrom.setOnClickListener(new OnClickListener() 
		{
			public void onClick(View v) 
			{
				Intent intent = new Intent(getActivity(), CitySelectorActivity.class);
				getActivity().startActivityForResult(intent, FROM_CITY_REQUEST);
			}
		});

		buttonTo = (Button)getActivity().findViewById(R.id.to_button);
		StringUtil.setLocationButtonText(buttonTo, to, getString(R.string.all_locations));
		buttonTo.setOnClickListener(new OnClickListener() 
		{
			public void onClick(View v) 
			{
				Intent intent = new Intent(getActivity(), CitySelectorActivity.class);
				getActivity().startActivityForResult(intent, TO_CITY_REQUEST);
			}
		});
		
		// Set initial date
		buttonDate.setText(LocaleUtil.localizeDate(getResources(), selectedDate));
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
		
		buttonMyRides = (ImageButton)getActivity().findViewById(R.id.search_my_rides);
		buttonMyRides.setOnClickListener(new OnClickListener() 
		{	
			public void onClick(View arg0) 
			{
				tracker.trackEvent("SearchForm", "MyRidesTap", "", 0);
				Intent myRidesIntent = new Intent(getActivity(), MyRidesActivity.class);
				startActivity(myRidesIntent);
			}
		});
		
		
		notifications = (ImageButton)getActivity().findViewById(R.id.show_active_notifications);
		notifications.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(getActivity(), NotificationsActivity.class));
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
					StringUtil.setLocationButtonText(buttonFrom, route.getFrom(), getString(R.string.all_locations));
					to = route.getTo();
					StringUtil.setLocationButtonText(buttonTo, route.getTo(), getString(R.string.all_locations));
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
			if (savedInstanceState.containsKey("selected_date") && savedInstanceState.getLong("selected_date") > selectedDate.getTimeInMillis())
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
		
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.trackPageView("/SearchForm");
	}

	
	
	@Override
	public void onResume() 
	{
		super.onResume();
		populateLastSearchList();
		
		if (NotificationManager.getInstance().notificationsAvailable())
		{
			notifications.setVisibility(View.VISIBLE);
		}
		else
		{
			notifications.setVisibility(View.GONE);
		}
			
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
	
	public void setSelectedDate(Calendar date)
	{
		this.selectedDate = date;
		buttonDate.setText(LocaleUtil.localizeDate(getResources(), selectedDate));
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
					StringUtil.setLocationButtonText(buttonFrom, from, getString(R.string.all_locations));
					tracker.trackEvent("SearchForm", "FromCitySelect", "AllCities", 0);
					return;
				case TO_CITY_REQUEST:
					to = "";
					StringUtil.setLocationButtonText(buttonTo, to, getString(R.string.all_locations));
					tracker.trackEvent("SearchForm", "ToCitySelect", "AllCities", 0);
					return;
			}
		}
		
		switch(requestCode)
		{
			case FROM_CITY_REQUEST:
				from = intent.getStringExtra("city");
				StringUtil.setLocationButtonText(buttonFrom, from, getString(R.string.all_locations));
				tracker.trackEvent("SearchForm", "FromCitySelect", from, 0);
				break;
			case TO_CITY_REQUEST:
				to = intent.getStringExtra("city");
				StringUtil.setLocationButtonText(buttonTo, to, getString(R.string.all_locations));
				tracker.trackEvent("SearchForm", "ToCitySelect", to, 0);
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
		
		tracker.trackEvent("SearchForm", "StartSearch", from + "-" + to, 0);
		
		MainActivity mainActivity = (MainActivity) getActivity();
		mainActivity.startSearch(from, to, selectedDate);
	}
}
