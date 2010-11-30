package org.prevoz.android.search;

import java.util.Date;

public class SearchRide implements Comparable<SearchRide>
{
	private int id;
	private String from;
	private String to;
	private String author;
	private Double price;
	private Date time;

	public SearchRide(int id, String from, String to, String author,
			Double price, Date time)
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

	public String getFrom()
	{
		return from;
	}

	public String getTo()
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

	public Date getTime()
	{
		return time;
	}

	public int compareTo(SearchRide another)
	{
		return (this.getFrom().compareTo(another.getFrom()) == 0) ? this
				.getTo().compareTo(another.getTo()) : this.getFrom().compareTo(
				another.getFrom());
	}
}
