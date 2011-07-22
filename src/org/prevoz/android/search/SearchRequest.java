package org.prevoz.android.search;

import java.util.Calendar;

import org.prevoz.android.RideType;

import android.app.Activity;

public class SearchRequest
{
	private Activity context = null;
	private RideType searchType = null;
	
	private String from;
	private String to;
	private Calendar when;
	
	public SearchRequest(Activity context,
						 RideType searchType,
						 String from,
						 String to,
						 Calendar when)
	{
		this.context = context;
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
}
