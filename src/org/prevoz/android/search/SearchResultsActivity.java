package org.prevoz.android.search;

import java.util.Calendar;

import org.prevoz.android.R;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class SearchResultsActivity extends FragmentActivity
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

}
