package org.prevoz.android.search;

import java.util.Calendar;

import android.app.Activity;

public class SearchRequest
{
	private Activity context = null;
	
	private String from;
	private String to;
	private Calendar when;
	
	public SearchRequest(Activity context,
						 String from,
						 String to,
						 Calendar when)
	{
		this.context = context;
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
}
