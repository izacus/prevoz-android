package org.prevoz.android.c2dm;

import java.util.Calendar;

public class NotifySubscription 
{
	private Integer id;
	private String from;
	private String to;
	private Calendar date;
	
	public NotifySubscription(String from, String to, Calendar date)
	{
		this.from = from;
		this.to = to;
		this.date = date;
		this.id = null;
	}
	
	public NotifySubscription(int id, String from, String to, Calendar date) 
	{
		this(from, to, date);
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public String getFrom() {
		return from;
	}

	public String getTo() {
		return to;
	}

	public Calendar getDate() {
		return date;
	}
}
