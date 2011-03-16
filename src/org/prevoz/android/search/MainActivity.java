package org.prevoz.android.search;

import org.prevoz.android.R;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

public class MainActivity extends FragmentActivity
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
	}


}