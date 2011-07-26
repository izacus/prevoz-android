package org.prevoz.android;

import java.util.Calendar;

import org.prevoz.android.R;
import org.prevoz.android.auth.AuthenticationManager;
import org.prevoz.android.search.SearchFormFragment;
import org.prevoz.android.search.SearchResultsActivity;
import org.prevoz.android.util.Database;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.DatePicker;

public class MainActivity extends FragmentActivity implements OnDateSetListener
{
	public static final int DIALOG_SEARCH_DATE = 0;
	
	// For picking search date
	private DatePickerDialog datePicker;
	private GoogleAnalyticsTracker tracker;
	
	public void startSearch(String from, String to, Calendar when)
	{
		// Start new activity with search results
		Intent intent = new Intent(getApplication(), SearchResultsActivity.class);
		Bundle dataBundle = new Bundle();
		dataBundle.putString("from", from);
		dataBundle.putString("to", to);
		dataBundle.putLong("when", when.getTimeInMillis());
		
		intent.putExtras(dataBundle);
		this.startActivity(intent);
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		// Start GA
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.start(getString(R.string.ga_identity), this);
		tracker.trackEvent("Application", "Start", getString(R.string.app_version), 0);
		
		// Sanitize search database
		Database.deleteHistoryEntries(this, 10);
		
		setContentView(R.layout.main_activity);		
		Calendar now = Calendar.getInstance();
		datePicker = new DatePickerDialog(this, this, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
		
		// Refresh login status for it to be ready for user
		AuthenticationManager.getInstance().getAuthenticationStatus(this, null);
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
	protected void onActivityResult(int requestCode, 
									int resultCode, 
									Intent intent) 
	{
		super.onActivityResult(requestCode, resultCode, intent);
		
		SearchFormFragment searchFormFrag = (SearchFormFragment) getSupportFragmentManager().findFragmentById(R.id.search_form_fragment);
		searchFormFrag.onParentActivityResult(requestCode, resultCode, intent);
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

	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
		tracker.stop();
	}
	
}