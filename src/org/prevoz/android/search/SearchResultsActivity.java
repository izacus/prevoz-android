package org.prevoz.android.search;

import java.util.Calendar;

import org.prevoz.android.R;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.flurry.android.FlurryAgent;

public class SearchResultsActivity extends SherlockFragmentActivity
{
	private String from;
	private String to;
	private Calendar when;
	private int[] highlightIds;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_results);
		// Extract passed intent data
		Bundle data = getIntent().getExtras();
		from = data.getString("from");
		to = data.getString("to");
		when = Calendar.getInstance();
		when.setTimeInMillis(data.getLong("when"));
		highlightIds = data.getIntArray("highlights");
	}
	
	public String getFrom()
	{
		return from;
	}
	
	public String getTo()
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

}
