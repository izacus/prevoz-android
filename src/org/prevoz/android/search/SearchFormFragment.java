package org.prevoz.android.search;

import org.prevoz.android.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

public class SearchFormFragment extends Fragment 
{
	private Button buttonDate;
	private AutoCompleteTextView fromField;
	private AutoCompleteTextView toField;
	
	
	private void prepareFormFields()
	{
		buttonDate = (Button)getActivity().findViewById(R.id.date_button);
		fromField = (AutoCompleteTextView)getActivity().findViewById(R.id.from_field);
		toField = (AutoCompleteTextView)getActivity().findViewById(R.id.to_field);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) 
	{
		super.onActivityCreated(savedInstanceState);
		prepareFormFields();
		
		// Initialize date display
		buttonDate.setText("danes");
		
		// Initialize locations autocomplete
		LocationAutocompleteAdapter locAdapter = new LocationAutocompleteAdapter(getActivity(), null);
		fromField.setAdapter(locAdapter);
		toField.setAdapter(locAdapter);
		
		// Set treshold for autocomplete
		fromField.setThreshold(1);
		toField.setThreshold(1);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, 
							 ViewGroup container,
							 Bundle savedInstanceState) 
	{
		View newView = inflater.inflate(R.layout.search_form_frag, container);
		return newView;
	}
	
	
}
