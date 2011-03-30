package org.prevoz.android.search;

import java.util.Calendar;

import org.prevoz.android.GPSManager;
import org.prevoz.android.R;
import org.prevoz.android.util.LocaleUtil;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SearchFormFragment extends Fragment 
{
	private Button buttonDate;
	private Button buttonGps;
	private Button buttonSearch;
	private AutoCompleteTextView fromField;
	private AutoCompleteTextView toField;
	
	private Calendar selectedDate;
	
	private void prepareFormFields()
	{
		buttonDate = (Button)getActivity().findViewById(R.id.date_button);
		buttonGps = (Button)getActivity().findViewById(R.id.gps_button);
		buttonSearch = (Button)getActivity().findViewById(R.id.search_button);
		fromField = (AutoCompleteTextView)getActivity().findViewById(R.id.from_field);
		toField = (AutoCompleteTextView)getActivity().findViewById(R.id.to_field);
		
		// Initialize locations autocomplete
		LocationAutocompleteAdapter locAdapter = new LocationAutocompleteAdapter(getActivity(), null);
		fromField.setAdapter(locAdapter);
		toField.setAdapter(locAdapter);
		
		// Set treshold for autocomplete
		fromField.setThreshold(1);
		toField.setThreshold(1);
		
		// Set initial date
		buttonDate.setText(localizeDate(selectedDate));
		buttonDate.setOnClickListener(new OnClickListener() 
		{
			
			public void onClick(View v) 
			{
				getActivity().showDialog(MainActivity.DIALOG_SEARCH_DATE);
			}
		});
		
		// Set GPS button action
		buttonGps.setOnClickListener(new OnClickListener() 
		{
			
			public void onClick(View arg0) 
			{
				fillInCurrentLocation(fromField);
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
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) 
	{
		super.onActivityCreated(savedInstanceState);
		
		selectedDate = Calendar.getInstance();
		if (savedInstanceState != null && savedInstanceState.containsKey("selected_date"))
		{
			selectedDate.setTimeInMillis(savedInstanceState.getLong("selected_date"));
		}
		
		
		prepareFormFields();
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
	
	private void fillInCurrentLocation(final TextView view)
	{
		view.setEnabled(false);
		
		final String currentText = view.getText().toString();
		final String currentHint = view.getHint().toString();
		
		view.setText("");
		view.setHint(getResources().getString(R.string.search_gps_locating));
		
		// Determine location
		final GPSManager gpsManager = new GPSManager();
		Handler callback = new Handler()
		{
			@Override
			public void handleMessage(Message msg) 
			{
				view.setHint(currentHint);
				
				if (msg.what == GPSManager.GPS_PROVIDER_UNAVALABLE)
				{
					Toast.makeText(getActivity(), R.string.search_gps_error, Toast.LENGTH_SHORT).show();
					// Restore old entry
					view.setText(currentText);
					return;
				}
				
				view.setText(gpsManager.getCurrentCity());
			}
		};
		
		gpsManager.findCurrentCity(getActivity(), callback);
	}
	
	private void startSearch()
	{
		String from = fromField.getText().toString();
		String to = toField.getText().toString();
		
		MainActivity mainActivity = (MainActivity) getActivity();
		mainActivity.startSearch(from, to, selectedDate);
	}
}
