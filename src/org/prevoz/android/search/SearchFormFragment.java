package org.prevoz.android.search;

import java.util.Calendar;

import org.prevoz.android.R;
import org.prevoz.android.util.LocaleUtil;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

public class SearchFormFragment extends Fragment 
{
	private Button buttonDate;
	private AutoCompleteTextView fromField;
	private AutoCompleteTextView toField;
	
	private Calendar selectedDate;
	
	private void prepareFormFields()
	{
		buttonDate = (Button)getActivity().findViewById(R.id.date_button);
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
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) 
	{
		super.onActivityCreated(savedInstanceState);
		
		selectedDate = Calendar.getInstance();
		
		prepareFormFields();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, 
							 ViewGroup container,
							 Bundle savedInstanceState) 
	{
		View newView = inflater.inflate(R.layout.search_form_frag, container);
		return newView;
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
}
