package org.prevoz.android.search;

import java.util.Date;


public class SearchRide implements Comparable<SearchRide>
{
    private int id;
    private String from;
    private String to;
    private Double price;
    private Date time;
    
    public SearchRide(int id, String from, String to, Double price, Date time)
    {
	this.id = id;
	this.from = from;
	this.to = to;
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

    public Double getPrice()
    {
        return price;
    }

    public Date getTime()
    {
        return time;
    }

    @Override
    public int compareTo(SearchRide another)
    {
	return (this.getFrom().compareTo(another.getFrom()) == 0) ? this.getTo().compareTo(another.getTo()) : this.getFrom().compareTo(another.getFrom());
    }
}
