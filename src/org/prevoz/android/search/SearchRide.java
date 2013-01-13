package org.prevoz.android.search;

import java.util.Calendar;
import java.util.Date;

import org.prevoz.android.City;

public class SearchRide implements Comparable<SearchRide>
{
	private int id;
	private City from;
	private City to;
	private String author;
	private Double price;
	private Calendar time;

	public SearchRide(int id, City from, City to, String author,
			Double price, Calendar time)
	{
		this.id = id;
		this.from = from;
		this.to = to;
		this.author = author;
		this.price = price;
		this.time = time;
	}

	public int getId()
	{
		return id;
	}

	public City getFrom()
	{
		return from;
	}

	public City getTo()
	{
		return to;
	}

	public String getAuthor()
	{
		return author;
	}

	public Double getPrice()
	{
		return price;
	}

	public Calendar getTime()
	{
		return time;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((author == null) ? 0 : author.hashCode());
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + id;
		result = prime * result + ((price == null) ? 0 : price.hashCode());
		result = prime * result + ((time == null) ? 0 : time.hashCode());
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof SearchRide)
			return this.compareTo((SearchRide)obj) == 0;
		
		return false;
	}

	public int compareTo(SearchRide another)
	{
        if (this.getFrom().equals(another.getFrom()) && this.getTo().equals(another.getTo()))
        {
            return this.getTime().compareTo(another.getTime());
        }

		return (this.getFrom().compareTo(another.getFrom()) == 0) ? this
				.getTo().compareTo(another.getTo()) : this.getFrom().compareTo(
				another.getFrom());
	}
}
