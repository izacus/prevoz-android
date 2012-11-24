package org.prevoz.android.search;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.prevoz.android.City;
import org.prevoz.android.CitySelectorActivity;
import org.prevoz.android.MainActivity;
import org.prevoz.android.R;
import org.prevoz.android.Route;
import org.prevoz.android.c2dm.NotificationManager;
import org.prevoz.android.c2dm.NotificationsActivity;
import org.prevoz.android.my_rides.MyRidesActivity;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.StringUtil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.flurry.android.FlurryAgent;

public class SearchFormFragment extends SherlockFragment 
{
	private static final int FROM_CITY_REQUEST = 1;
	private static final int TO_CITY_REQUEST = 2;
	
	private Button buttonDate;
	private Button buttonSearch;
	
	private Button buttonFrom;
	private Button buttonTo;
	
	private MenuItem buttonNotifications;
	
	private ListView lastSearches;
	
	private City from = null;
	private City to = null;
	private Calendar selectedDate;
	
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) 
	{
		inflater.inflate(R.menu.menu_search_form, menu);
		buttonNotifications = menu.findItem(R.id.menu_search_notifications);
		updateNotificationButtonVisibility();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId())
		{
			case R.id.menu_search_myrides:
				FlurryAgent.logEvent("SearchForm - My Rides");
				Intent myRidesIntent = new Intent(getActivity(), MyRidesActivity.class);
				startActivity(myRidesIntent);
				return true;
				
			case R.id.menu_search_notifications:
				startActivity(new Intent(getActivity(), NotificationsActivity.class));
				return true;
		
			default: 
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) 
	{
		super.onActivityCreated(savedInstanceState);
		
		selectedDate = Calendar.getInstance(LocaleUtil.getLocalTimezone());
		if (savedInstanceState != null)
		{
			if (savedInstanceState.containsKey("selected_date") && savedInstanceState.getLong("selected_date") > selectedDate.getTimeInMillis())
			{
				selectedDate.setTimeInMillis(savedInstanceState.getLong("selected_date"));
			}
			
			if (savedInstanceState.containsKey("from"))
			{
				from = new City(savedInstanceState.getString("from"), savedInstanceState.getString("fromCountry"));
			}
			
			if (savedInstanceState.containsKey("to"))
			{
				from = new City(savedInstanceState.getString("to"), savedInstanceState.getString("toCountry"));
			}
		}
		
		prepareFormFields();
		
		FlurryAgent.onPageView();
	}

	
	
	@Override
	public void onResume() 
	{
		super.onResume();
		populateLastSearchList();
		updateNotificationButtonVisibility();
	}
	
	private void updateNotificationButtonVisibility()
	{
		if (buttonNotifications != null)
		{
			if (NotificationManager.getInstance(getActivity().getApplicationContext()).notificationsAvailable())
			{
				buttonNotifications.setVisible(true);
			}
			else
			{
				buttonNotifications.setVisible(false);
			}
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
		
		if (from != null)
		{
			outState.putString("from", from.getDisplayName());
			outState.putString("fromCountry", from.getCountryCode());
		}
		
		if (to != null)
		{
			outState.putString("to", to.getDisplayName());
			outState.putString("toCountry", to.getCountryCode());
		}
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
					from = null;
					StringUtil.setLocationButtonText(buttonFrom, from, getString(R.string.all_locations));
					return;
				case TO_CITY_REQUEST:
					to = null;
					StringUtil.setLocationButtonText(buttonTo, to, getString(R.string.all_locations));
					return;
			}
		}
		
		switch(requestCode)
		{
			case FROM_CITY_REQUEST:
				from = new City(intent.getStringExtra("city"), intent.getStringExtra("country"));
				StringUtil.setLocationButtonText(buttonFrom, from, getString(R.string.all_locations));
				break;
			case TO_CITY_REQUEST:
				to = new City(intent.getStringExtra("city"), intent.getStringExtra("country"));
				StringUtil.setLocationButtonText(buttonTo, to, getString(R.string.all_locations));
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
		Database.addSearchToHistory(getActivity(), from, to, Calendar.getInstance(LocaleUtil.getLocalTimezone()).getTime());
		HashMap<String, String> params = new HashMap<String, String>();
		
		params.put("from", from == null ? "" : from.getDisplayName());
		params.put("fromCountry", from == null ? "" : from.getCountryCode());
		params.put("to", to == null ? "" : to.getDisplayName());
		params.put("toCountry", to == null ? "" : to.getCountryCode());
		FlurryAgent.logEvent("SearchForm - Search", params);
		
		MainActivity mainActivity = (MainActivity) getActivity();
		mainActivity.startSearch(from, to, selectedDate);
	}
}
