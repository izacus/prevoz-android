package org.prevoz.android;

import java.util.Calendar;

import org.prevoz.android.auth.AuthenticationManager;
import org.prevoz.android.c2dm.GCMTools;
import org.prevoz.android.c2dm.NotificationManager;
import org.prevoz.android.search.SearchFormFragment;
import org.prevoz.android.search.SearchResultsActivity;
import org.prevoz.android.util.Database;

import android.content.Intent;
import android.os.Bundle;

import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockFragmentActivity;

public class MainActivity extends RoboSherlockFragmentActivity
{
	public static final int DIALOG_SEARCH_DATE = 0;
	
	public void startSearch(City from, City to, Calendar when)
	{
		// Start new activity with search results
		Intent intent = new Intent(getApplication(), SearchResultsActivity.class);
		Bundle dataBundle = new Bundle();
		
		if (from != null)
		{
			dataBundle.putString("from", from.getDisplayName());
			dataBundle.putString("fromCountry", from.getCountryCode());
		}
		
		if (to != null)
		{
			dataBundle.putString("to", to.getDisplayName());
			dataBundle.putString("toCountry", to.getCountryCode());
		}
		
		dataBundle.putLong("when", when.getTimeInMillis());
		
		intent.putExtras(dataBundle);
		this.startActivity(intent);
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getSupportActionBar().setHomeButtonEnabled(false);
		
		// Attempt C2DM services registration
		NotificationManager.getInstance(getApplicationContext());
		
		// Sanitize search database
		Database.deleteHistoryEntries(this, 10);
		setContentView(R.layout.main_activity);

        GCMTools.checkRegisterGCM(this);

		// Refresh login status for it to be ready for user
		AuthenticationManager.getInstance().getAuthenticationStatus(this, null);
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		Database.pruneOldNotifications(this);
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
	protected void onDestroy() 
	{
		super.onDestroy();
	}
}