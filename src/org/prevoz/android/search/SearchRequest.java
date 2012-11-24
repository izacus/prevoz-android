package org.prevoz.android.search;

import java.util.Calendar;

import org.prevoz.android.City;

import android.app.Activity;

public class SearchRequest
{
	private Activity context = null;
	
	private City from;
	private City to;
	private Calendar when;
	
	public SearchRequest(Activity context,
						 City from,
						 City to,
						 Calendar when)
	{
		this.context = context;
		this.from = from;
		this.to = to;
		this.when = when;
	}

	public City getFrom() {
		return from;
	}

	public City getTo() {
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
