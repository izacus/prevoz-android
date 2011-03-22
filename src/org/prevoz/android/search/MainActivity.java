package org.prevoz.android.search;

import java.util.Calendar;

import org.prevoz.android.R;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.widget.DatePicker;

public class MainActivity extends FragmentActivity implements OnDateSetListener
{
	public static final int DIALOG_SEARCH_DATE = 0;
	
	// For picking search date
	private DatePickerDialog datePicker;
	
	public void startSearch(String from, String to, Calendar when)
	{
		// Show the search results fragment
		SearchResultsFragment newSearch = new SearchResultsFragment(from, to, when);
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
		transaction.replace(R.id.search_form_fragment, newSearch);		
		transaction.addToBackStack(null);
		transaction.commit();
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		
		Calendar now = Calendar.getInstance();
		datePicker = new DatePickerDialog(this, this, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
	}

	public void onDateSet(DatePicker picker, int year, int month, int dayOfMonth) 
	{
		Calendar date = Calendar.getInstance();
		date.set(Calendar.YEAR, year);
		date.set(Calendar.MONTH, month);
		date.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		
		SearchFormFragment searchForm = (SearchFormFragment)getSupportFragmentManager().findFragmentById(R.id.search_form_fragment);
		searchForm.setSelectedDate(date);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) 
	{
		switch(id)
		{
			case DIALOG_SEARCH_DATE:
				return datePicker;
			default:
				return super.onCreateDialog(id);
		}
	}
}