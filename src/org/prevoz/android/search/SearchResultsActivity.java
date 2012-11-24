package org.prevoz.android.search;

import java.util.Calendar;

import org.prevoz.android.City;
import org.prevoz.android.MainActivity;
import org.prevoz.android.R;
import org.prevoz.android.util.LocaleUtil;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.flurry.android.FlurryAgent;

public class SearchResultsActivity extends SherlockFragmentActivity
{
	private City from;
	private City to;
	private Calendar when;
	private int[] highlightIds;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_results);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		// Extract passed intent data
		Bundle data = getIntent().getExtras();
		from = null;
		to = null;
		
		if (data.containsKey("from"))
		{
			from = new City(data.getString("from"), data.getString("fromCountry"));
		}
		
		if (data.containsKey("to"))
		{
			to = new City(data.getString("to"), data.getString("toCountry"));
		}
		
		when = Calendar.getInstance(LocaleUtil.getLocalTimezone());
		when.setTimeInMillis(data.getLong("when"));
		highlightIds = data.getIntArray("highlights");
	}
	
	public City getFrom()
	{
		return from;
	}
	
	public City getTo()
	{
		return to;
	}
	
	public Calendar getWhen()
	{
		return when;
	}
	
	public int[] getHighlights()
	{
		return highlightIds;
	}
	
	@Override
	protected void onStart() 
	{
		super.onStart();
		FlurryAgent.setReportLocation(false);
		FlurryAgent.onStartSession(this, getString(R.string.flurry_apikey));
	}

	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		if (item.getItemId() == android.R.id.home)
		{
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
}
