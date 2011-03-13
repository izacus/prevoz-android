package org.prevoz.android.search;

import org.prevoz.android.R;

import android.app.Activity;
import android.os.Bundle;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

public class MainActivity extends Activity
{
	// Activity form fields
	private Button buttonDate;
	private AutoCompleteTextView fromField;
	private AutoCompleteTextView toField;
	
	private void prepareFormFields()
	{
		buttonDate = (Button)findViewById(R.id.date_button);
		fromField = (AutoCompleteTextView)findViewById(R.id.from_field);
		toField = (AutoCompleteTextView)findViewById(R.id.to_field);
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		prepareFormFields();
		
		// Initialize date display
		buttonDate.setText("danes");
		
		// Initialize locations autocomplete
		LocationAutocompleteAdapter locAdapter = new LocationAutocompleteAdapter(this, null);
		fromField.setAdapter(locAdapter);
		toField.setAdapter(locAdapter);
		
		// Set treshold for autocomplete
		fromField.setThreshold(1);
		toField.setThreshold(1);
	}


}