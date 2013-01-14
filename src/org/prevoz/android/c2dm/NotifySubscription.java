package org.prevoz.android.c2dm;

import org.prevoz.android.City;

import java.util.Calendar;

public class NotifySubscription 
{
	private Integer id;
	private City from;
	private City to;
	private Calendar date;
	
	public NotifySubscription(City from, City to, Calendar date)
	{
		this.from = from;
		this.to = to;
		this.date = date;
		this.id = null;
	}
	
	public NotifySubscription(int id, City from, City to, Calendar date)
	{
		this(from, to, date);
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public City getFrom() {
		return from;
	}

	public City getTo() {
		return to;
	}

	public Calendar getDate() {
		return date;
	}
}
