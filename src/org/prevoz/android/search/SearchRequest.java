package org.prevoz.android.search;

import java.util.Calendar;

import org.prevoz.android.RideType;

import android.app.Activity;
import android.os.Handler;

public class SearchRequest
{
	private Activity context = null;
	private RideType searchType = null;
	
	private String from;
	private String to;
	private Calendar when;
	
	private Handler callback;

	public SearchRequest(Activity context,
						 Handler callback,
						 RideType searchType,
						 String from,
						 String to,
						 Calendar when)
	{
		this.context = context;
		this.callback = callback;
		this.searchType = searchType;
		
		this.from = from;
		this.to = to;
		this.when = when;
	}

	public String getFrom() {
		return from;
	}

	public String getTo() {
		return to;
	}

	public Calendar getWhen() {
		return when;
	}

	public synchronized Activity getContext()
	{
		return context;
	}

	public RideType getSearchType()
	{
		return searchType;
	}
	
	public synchronized Handler getCallback()
	{
		return callback;
	}
	
	public synchronized void contextChanged(Activity context, Handler callback)
	{
		this.context = context;
		this.callback = callback;
	}
}
